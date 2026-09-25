#!/usr/bin/env python3
"""Non-destructive multi-instance Community Node manager.

Each node owns a distinct Compose project and state; shared Caddy is owned by
this unified installation, not by a node or a Control Plane container.
"""
import argparse
from contextlib import contextmanager
import ipaddress
import json
import os
from pathlib import Path
import re
import shutil
import socket
import subprocess
import sys
import time
import uuid
from urllib.parse import urlsplit
from urllib.request import urlopen

# The updated manager may be launched from a newly extracted bundle while the
# existing Combined deployment lives elsewhere. Its code/templates come from
# SOURCE; new instances and proxy routes must live beside the EXISTING
# deployment instead of creating a second proxy and duplicate server state.
SOURCE = Path(__file__).resolve().parent
ROOT = SOURCE
INSTANCES = ROOT / 'node-instances'
ROUTES = INSTANCES / 'routes'
PROXY = ROOT / 'public-proxy'
TEMPLATE = SOURCE / 'community-node'
NETWORK = 'sparrow-public-edge'
ID = re.compile(r'[a-z][a-z0-9-]{1,30}[a-z0-9]\Z')
DOMAIN = re.compile(r'(?=.{1,253}$)[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+\Z')
TEMPLATES = ('docker-compose.yml', 'docker-compose.release.yml',
             'docker-compose.production.yml', 'docker-compose.shared-proxy.yml',
             'Caddyfile', 'index.html', 'bootstrap-community-node.sh', 'Bootstrap-CommunityNode.ps1',
             'docker-compose.directory-sync.yml')
BACKENDS = ('mailbox', 'federation', 'gateway')
IMPORT = 'import /etc/caddy/node-routes/*.caddy'


class NodeError(Exception):
    pass


def require(ok, message):
    if not ok:
        raise NodeError(message)


def cmd(*args, cwd=None, capture=False, check=True, env=None):
    try:
        result = subprocess.run(args, cwd=cwd, env=env, text=True,
                                stdout=subprocess.PIPE if capture else None,
                                stderr=subprocess.PIPE if capture else None)
    except FileNotFoundError as exc:
        raise NodeError(f'Missing command: {args[0]}') from exc
    if check and result.returncode:
        raise NodeError(f'{args[0]} exited {result.returncode}: '
                        + (result.stderr or result.stdout or '').strip()[:600])
    return result


def docker(*args, **kwargs):
    return cmd('docker', *args, **kwargs)


def atomic(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + '.pending')
    temporary.write_text(value, encoding='utf-8')
    if path.name.startswith('.') or path.name in ('sparrow.conf', 'node-instance.json'):
        temporary.chmod(0o600)
    temporary.replace(path)


def properties(path):
    result = {}
    if path.is_file():
        for line in path.read_text(encoding='utf-8').splitlines():
            if '=' in line and not line.startswith('#'):
                k, v = line.split('=', 1)
                result[k.strip()] = v.strip()
    return result


def instance_dirs():
    if not INSTANCES.is_dir():
        return []
    return sorted(d for d in INSTANCES.iterdir() if d.is_dir() and (d / 'node-instance.json').is_file())


def metadata(directory):
    data = json.loads((directory / 'node-instance.json').read_text(encoding='utf-8'))
    require(data['id'] == directory.name and ID.fullmatch(data['id']), 'Invalid node instance metadata.')
    return data


def find(instance_id):
    require(ID.fullmatch(instance_id or '') is not None, 'Invalid instance ID.')
    directory = INSTANCES / instance_id
    require((directory / 'node-instance.json').is_file(), f'Unknown instance: {instance_id}')
    return directory, metadata(directory)


def compose(directory, *action):
    conf = properties(directory / 'sparrow.conf')
    files = ('docker-compose.yml', 'docker-compose.release.yml')
    if conf.get('MODE') == 'public':
        files += ('docker-compose.production.yml', 'docker-compose.shared-proxy.yml')
    args = ['docker', 'compose', '--env-file', str(directory / '.env.runtime')]
    for name in files:
        args += ['-f', str(directory / name)]
    return cmd(*args, *action, cwd=directory)


def ownership(directory, meta):
    project = 'sparrow-community-' + meta['id']
    rows = docker('ps', '--all', '--filter', f'label=com.docker.compose.project={project}',
                  '--format', '{{.Label "com.docker.compose.project.working_dir"}}', capture=True).stdout.splitlines()
    for row in rows:
        require(row.strip(), f'Cannot establish Docker ownership of {project}.')
        require(host_working_dir(row) == directory.resolve(),
                f'{project} belongs to {row.strip()}, not this installation; refusing takeover.')
    if not (directory / '.env.runtime').is_file():
        # Never recreate identity if runtime was lost but Docker data survived.
        volumes = docker('volume', 'ls', '-q', capture=True).stdout.splitlines()
        require(not any(v.startswith(project + '_') or v == project + '-directory-cache' for v in volumes),
                f'{project} has persistent data but runtime configuration is missing. Restore runtime first.')


def host_working_dir(value):
    """Normalize Compose working-directory labels from Docker Desktop."""
    value = value.strip()
    if os.name == 'nt':
        normalized = value.replace('\\', '/')
        match = re.fullmatch(r'/(?:run/desktop/mnt/host|host_mnt|mnt)/([a-zA-Z])/(.*)', normalized)
        if match:
            value = match.group(1).upper() + ':/' + match.group(2)
    return Path(value).resolve()


def docker_project_directory(project, service=None):
    args = ['ps', '--all', '--filter', f'label=com.docker.compose.project={project}']
    if service:
        args += ['--filter', f'label=com.docker.compose.service={service}']
    args += ['--format', '{{.Label "com.docker.compose.project.working_dir"}}']
    # Failure to inspect Docker is NOT evidence of a fresh installation. A
    # silent fallback could make a new bundle claim an already-used project.
    result = docker(*args, capture=True)
    rows = [r.strip() for r in result.stdout.splitlines()]
    if not rows:
        return None
    require(all(rows), f'Cannot read Docker Compose working_dir label for {project}.')
    owners = {host_working_dir(row) for row in rows}
    require(len(owners) == 1,
            f'Containers of {project} have different Compose workdirs: ' +
            ', '.join(str(owner) for owner in sorted(owners)))
    return owners.pop()


def combined_owners():
    return (docker_project_directory('sparrow-control-plane'),
            docker_project_directory('sparrow-community-node'),
            docker_project_directory('sparrow-public-proxy', 'caddy'))


def existing_candidates(cp_owner, node_owner, proxy_owner):
    candidates = []
    for owner, expected in ((proxy_owner, 'public-proxy'),
                            (node_owner, 'community-node'),
                            (cp_owner, 'control-plane')):
        if owner is not None and owner.name.lower() == expected:
            root = owner.parent
            if root not in candidates:
                candidates.append(root)
    return candidates


def candidate_problem(candidate, cp_owner, node_owner, proxy_owner):
    cp_dir = candidate / 'control-plane'
    node_dir = candidate / 'community-node'
    proxy_dir = candidate / 'public-proxy'
    if cp_owner != cp_dir.resolve():
        return f'Control Plane Compose owner is {cp_owner}, expected {cp_dir.resolve()}'
    if node_owner is not None and node_owner != node_dir.resolve():
        return f'Community Node Compose owner is {node_owner}, expected {node_dir.resolve()}'
    if proxy_owner is not None and proxy_owner != proxy_dir.resolve():
        return f'Public proxy Compose owner is {proxy_owner}, expected {proxy_dir.resolve()}'
    conf_path = cp_dir / 'sparrow.conf'
    if not conf_path.is_file():
        return f'Original Control Plane config missing: {conf_path}'
    conf = properties(conf_path)
    if conf.get('MODE') not in ('lan', 'public'):
        return f'Original Control Plane MODE missing or invalid in {conf_path}'
    if conf['MODE'] == 'public':
        if proxy_owner is None:
            return 'No Docker-owned sparrow-public-proxy/caddy container was found for the public installation'
        for path in (proxy_dir / 'docker-compose.yml', proxy_dir / 'Caddyfile'):
            if not path.is_file():
                return f'Original public proxy file missing: {path}'
        if not conf.get('PUBLIC_DOMAIN'):
            # A missing control-plane runtime alone is fine. Another already
            # installed component may have the advertised HTTPS plane origin.
            runtimes = (properties(cp_dir / '.env.runtime'),
                        properties(node_dir / '.env.runtime'))
            if not any(any(url.strip().startswith('https://') for url in
                           r.get('ADVERTISED_CONTROL_PLANE_URLS', '').split(',')) for r in runtimes):
                return ('Control Plane PUBLIC_DOMAIN missing, and no existing runtime '
                        'advertises its HTTPS origin')
    # The ORIGINAL node and CP .env.runtime files are not needed to create an
    # independent node when the CP configuration and shared proxy are owned and
    # verifiable. Do not recreate, update or change either old component.
    return ''


def relink_moved_deployment(new_root):
    """Explicit Windows move recovery: keep Docker's ORIGINAL paths via a junction.

    Changing Compose working_dir labels or recreating the running Combined
    project is NOT safe. Only create a filesystem alias after verifying that
    the moved Caddy configuration matches the live proxy and the original
    labeled root truly no longer exists. No Docker container is restarted.
    """
    require(os.name == 'nt', 'Folder relinking is supported only on Windows.')
    cp_owner, node_owner, proxy_owner = combined_owners()
    require(cp_owner is not None and node_owner is not None and proxy_owner is not None,
            'Relinking requires all three original Combined Docker projects to be identifiable.')
    old_root = cp_owner.parent
    require(cp_owner == old_root / 'control-plane' and
            node_owner == old_root / 'community-node' and
            proxy_owner == old_root / 'public-proxy',
            'Compose projects do not share one original installation root; refusing relink.')
    # A pre-existing directory, even if empty, must never be replaced. This
    # includes dangling links/junctions, which Path.exists() could miss.
    require(not os.path.lexists(str(old_root)),
            f'Original Docker path still exists: {old_root}. No link was made.')
    moved = Path(new_root).expanduser().resolve(strict=True)
    require(moved.is_dir() and moved != old_root and
            not str(moved).lower().startswith(str(old_root).lower() + os.sep),
            'Choose the actual moved installation folder, not the original or its child.')
    cp_conf = properties(moved / 'control-plane' / 'sparrow.conf')
    require(cp_conf.get('CONFIGURED') == 'true' and cp_conf.get('MODE') == 'public' and
            cp_conf.get('PUBLIC_DOMAIN'),
            'Selected folder does not contain a configured original Public Control Plane.')
    for rel in ('control-plane/.env.runtime', 'community-node/.env.runtime',
                'public-proxy/docker-compose.yml', 'public-proxy/Caddyfile'):
        require((moved / rel).is_file(),
                f'Moved installation lacks {rel}; no link was made.')
    proxy_id = docker('ps', '--filter',
                      'label=com.docker.compose.project=sparrow-public-proxy',
                      '--filter', 'label=com.docker.compose.service=caddy',
                      '--format', '{{.ID}}', capture=True).stdout.strip().splitlines()
    require(len(proxy_id) == 1, 'Cannot verify exactly one running original public proxy.')
    live = docker('exec', proxy_id[0], 'cat', '/etc/caddy/Caddyfile',
                  capture=True, check=False)
    require(live.returncode == 0,
            'Cannot read the existing proxy configuration in the running Caddy container.')
    expected = (moved / 'public-proxy' / 'Caddyfile').read_text(encoding='utf-8')
    require(live.stdout.replace('\r\n', '\n').strip() ==
            expected.replace('\r\n', '\n').strip() and
            cp_conf['PUBLIC_DOMAIN'] in expected,
            'The moved proxy configuration does not match the RUNNING public proxy '
            'and Control Plane hostname. No link was made.')
    # PowerShell creates an NTFS directory junction without modifying the
    # target folder, its secrets, or any Docker volumes. It leaves the Docker
    # working_dir and file bind-mount paths valid at their original location.
    # No implicit relocation: operator must explicitly select/confirm target.
    quote = lambda value: "'" + str(value).replace("'", "''") + "'"
    command = ('New-Item -ItemType Junction -Path ' + quote(old_root) +
               ' -Target ' + quote(moved) + ' -ErrorAction Stop | Out-Null')
    cmd('powershell.exe', '-NoProfile', '-NonInteractive', '-Command', command,
        capture=True)
    require(old_root.is_dir() and old_root.resolve() == moved,
            'The junction command returned, but the original path does not resolve '
            'to the selected installation. Inspect the paths before retrying.')
    print('Created original-path junction: ' + str(old_root) + ' -> ' + str(moved))
    print('No Docker containers or volumes were changed. Close and reopen the Node Manager.')


def select_existing_deployment():
    """Identify a Docker-owned Combined installation without changing it.

    The deployed paired node / CP may lack their .env.runtime files after a
    bundle move. For a NEW node, verified CP config and proxy ownership are
    sufficient; modifying or restarting the original components is forbidden.
    """
    global ROOT, INSTANCES, ROUTES, PROXY, EXISTING_COMBINED
    cp_owner, node_owner, proxy_owner = combined_owners()
    if cp_owner is None:
        require(node_owner is None and proxy_owner is None,
                'Control Plane Compose project not found, but an existing Sparrow '
                f'node ({node_owner}) or proxy ({proxy_owner}) was found. '
                'Refusing to create a separate deployment on the same host.')
        return
    candidates = existing_candidates(cp_owner, node_owner, proxy_owner)
    errors = []
    for candidate in candidates:
        problem = candidate_problem(candidate, cp_owner, node_owner, proxy_owner)
        if problem:
            errors.append(f'{candidate}: {problem}')
            continue
        ROOT = candidate
        INSTANCES = ROOT / 'node-instances'
        ROUTES = INSTANCES / 'routes'
        PROXY = ROOT / 'public-proxy'
        EXISTING_COMBINED = True
        return
    detail = ('; '.join(errors) if errors else
              'Docker workdirs do not have the expected control-plane, '
              'community-node or public-proxy directory names. '
              f'Control Plane owner: {cp_owner}; node owner: {node_owner}; proxy owner: {proxy_owner}')
    raise NodeError('Cannot verify the original Combined installation. ' + detail +
                    '. No node was created and the running deployment was not modified. '
                    'Use the copyable detection details to identify which original path is missing.')


def diagnosis():
    """Read-only, copyable deployment report; never include runtime secrets."""
    lines = ['Sparrow Node Manager — deployment detection (read-only)',
             f'Manager bundle: {SOURCE}', 'No identities, secrets, or credentials are shown.']
    try:
        cp_owner, node_owner, proxy_owner = combined_owners()
    except (NodeError, OSError, ValueError) as exc:
        return '\n'.join(lines + ['Docker inspection failed: ' + str(exc)])
    for title, owner in (('Control Plane', cp_owner),
                         ('Paired Community Node', node_owner),
                         ('Public proxy (caddy)', proxy_owner)):
        lines.append(f'{title} Compose working directory: {owner or "NOT FOUND"}')
    for candidate in existing_candidates(cp_owner, node_owner, proxy_owner):
        lines.append(f'Candidate installation: {candidate}')
        for rel in ('control-plane/sparrow.conf', 'control-plane/.env.runtime',
                    'community-node/.env.runtime', 'public-proxy/docker-compose.yml',
                    'public-proxy/Caddyfile'):
            lines.append(f'  {rel}: {"present" if (candidate / rel).is_file() else "MISSING"}')
        lines.append('  Verification: ' +
                     (candidate_problem(candidate, cp_owner, node_owner, proxy_owner) or 'OK'))
    return '\n'.join(lines)


EXISTING_COMBINED = False


def proxy_ownership():
    rows = docker('ps', '--all', '--filter', 'label=com.docker.compose.project=sparrow-public-proxy',
                  '--format', '{{.Label "com.docker.compose.project.working_dir"}}', capture=True).stdout.splitlines()
    for row in rows:
        require(row.strip() and host_working_dir(row) == PROXY.resolve(),
                'Public port 80/443 is owned by a different Sparrow proxy installation; cannot modify its routes.')
    if not rows:
        volumes = docker('volume', 'ls', '-q', capture=True).stdout.splitlines()
        require(not any(v.startswith('sparrow-public-proxy_') for v in volumes),
                'Proxy data exists without locally verifiable container ownership. Restore the original installation.')


@contextmanager
def manager_lock():
    """Serialize node IDs, host ports, per-node identities, and shared routes."""
    lock_file = ROOT / '.sparrow-nodes-manager.lock'
    with lock_file.open('a+b') as lock:
        try:
            if os.name == 'nt':
                import msvcrt
                lock.seek(0)
                if lock.read(1) != b'0':
                    lock.seek(0)
                    lock.write(b'0')
                    lock.flush()
                lock.seek(0)
                msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
            else:
                import fcntl
                lock_file.chmod(0o600)
                fcntl.flock(lock.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
        except OSError as exc:
            raise NodeError('Another Sparrow Node manager is active in this installation.') from exc
        try:
            yield
        finally:
            if os.name == 'nt':
                lock.seek(0)
                msvcrt.locking(lock.fileno(), msvcrt.LK_UNLCK, 1)
            else:
                fcntl.flock(lock.fileno(), fcntl.LOCK_UN)


def public_hostname(value):
    require(DOMAIN.fullmatch(value or '') is not None, 'Enter a valid Public DNS hostname.')
    return value


def plane_url(value, mode):
    p = urlsplit(value)
    require(p.scheme in (('https',) if mode == 'public' else ('http', 'https')) and
            p.hostname and not p.username and not p.password and
            p.path in ('', '/') and not p.query and not p.fragment,
            'Control Plane must be an HTTP(S) origin (HTTPS in Public mode), without a path or credentials.')
    return value.rstrip('/')


def directory_url(value):
    p = urlsplit(value)
    require(p.scheme == 'https' and p.hostname and not p.username and not p.password and
            p.path in ('', '/') and not p.query and not p.fragment and p.port in (None, 443),
            'Directory Server must be an HTTPS origin with no path or credentials.')
    return value.rstrip('/')


def port_for_new_node():
    used = {8490}
    for d in instance_dirs():
        used.add(metadata(d)['port'])
    # Five-port spacing covers default HTTP and internal diagnostic bindings.
    for port in range(8500, 55000, 5):
        if port in used:
            continue
        # Host HTTP, diagnostics and PostgreSQL probes must all be available.
        # A running unrelated container/application may already own a port.
        sockets = []
        try:
            for candidate in (port, port + 2, port + 3, port + 4, port + 10000, port + 10001):
                sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                sockets.append(sock)
                sock.bind(('0.0.0.0', candidate))
        except OSError:
            continue
        finally:
            for sock in sockets:
                sock.close()
        return port
    raise NodeError('No available node port range.')


def auto_hostname(instance_id, ip):
    if not ip:
        ip = urlopen('https://api.ipify.org', timeout=8).read(64).decode().strip()
    try:
        address = ipaddress.IPv4Address(ip)
    except ipaddress.AddressValueError as exc:
        raise NodeError('Public hostname needs a valid WAN IPv4 address.') from exc
    require(address.is_global, 'Public hostname requires a globally routable IPv4 address.')
    return f'{instance_id}-{str(address).replace(".", "-")}.sslip.io'


def all_public_hosts():
    hosts = set()
    for part in ('community-node', 'control-plane'):
        if ((ROOT / part / '.env.runtime').is_file() or
                (part == 'control-plane' and existing_combined_plane('public'))):
            conf = properties(ROOT / part / 'sparrow.conf')
            if conf.get('MODE') == 'public':
                hosts.add(conf.get('PUBLIC_DOMAIN', ''))
    for directory in instance_dirs():
        meta = metadata(directory)
        if meta['mode'] == 'public':
            hosts.add(meta['hostname'])
    return hosts


def existing_combined_plane(mode):
    """Use the already installed Combined Control Plane, not a new service."""
    paired = ROOT / 'control-plane'
    conf = properties(paired / 'sparrow.conf')
    # Public CP configuration is already tied to the Docker-owned Combined
    # deployment; its original runtime file need not be readable here.
    if not EXISTING_COMBINED and not (paired / '.env.runtime').is_file() and not (ROOT / 'community-node' / '.env.runtime').is_file():
        return ''
    if mode == 'public' and conf.get('MODE') == 'public':
        host = conf.get('PUBLIC_DOMAIN', '').strip()
        if host:
            return plane_url('https://' + host, mode)
        # Some existing installations persist the advertised origin only in
        # .env.runtime. Never confuse the standalone directory with a plane.
        runtime = properties(paired / '.env.runtime')
        node_runtime = properties(ROOT / 'community-node' / '.env.runtime')
        urls = (runtime.get('ADVERTISED_CONTROL_PLANE_URLS', '') or
                node_runtime.get('ADVERTISED_CONTROL_PLANE_URLS', '')).split(',')
        for url in urls:
            if url.strip().startswith('https://'):
                return plane_url(url.strip(), mode)
    if mode == 'lan' and conf.get('MODE') == 'lan':
        # Prefer the advertised host address over localhost: remote clients
        # must reach the same Control Plane as this node.
        runtime = properties(paired / '.env.runtime')
        node_runtime = properties(ROOT / 'community-node' / '.env.runtime')
        values = (runtime.get('ADVERTISED_CONTROL_PLANE_URLS', '') or
                  node_runtime.get('ADVERTISED_CONTROL_PLANE_URLS', ''))
        candidates = [v.strip() for v in values.split(',') if v.strip()]
        if candidates:
            return plane_url(candidates[0], mode)
    return ''


def bundled_directory_trust(url):
    """Reuse an already *independently configured* matching URL + trust pin.

    Never associate an unrelated, user-typed URL with an existing pin: this
    could present the appearance of verified discovery for an unknown service.
    """
    if not url:
        return ''
    for conf_path in (TEMPLATE / 'sparrow.conf',
                      ROOT / 'control-plane' / 'sparrow.conf'):
        conf = properties(conf_path)
        configured_url = conf.get('CONTROL_PLANE_DIRECTORY_URL', '')
        if configured_url and directory_url(configured_url) == url:
            key = conf.get('CONTROL_PLANE_DIRECTORY_PUBLIC_KEY', '').strip()
            if key:
                return key
    return ''


def add(args):
    require(args.name and 1 <= len(args.name.strip()) <= 70 and '\n' not in args.name,
            'Provide a short display name.')
    instance_id = args.id or ('node-' + uuid.uuid4().hex[:10])
    require(ID.fullmatch(instance_id) is not None, 'ID must be 3–32 lowercase letters, digits or hyphens.')
    directory = INSTANCES / instance_id
    require(not directory.exists(), f'Instance already exists: {instance_id}')
    manual = [plane_url(x.strip(), args.mode) for x in args.control_plane_url.split(',') if x.strip()]
    # When adding a node alongside Combined, use its *existing* CP by default.
    # This does not install a second Control Plane or change its identity.
    if not manual and not args.directory_url:
        paired = existing_combined_plane(args.mode)
        if paired:
            manual = [paired]
    # An optional directory uses its matching bundle/Combined trust pin, never
    # a key fetched from the server itself. The common manual-CP path needs
    # neither directory discovery nor a public-key field.
    url = directory_url(args.directory_url) if args.directory_url else ''
    if not manual and not url:
        bundled = properties(TEMPLATE / 'sparrow.conf')
        candidate = bundled.get('CONTROL_PLANE_DIRECTORY_URL', '').strip()
        if candidate and bundled_directory_trust(directory_url(candidate)):
            url = directory_url(candidate)
    key = (args.directory_public_key or bundled_directory_trust(url)) if url else ''
    if url:
        require(key, 'Unknown directory signer: provision a verified pin in the release configuration or use the advanced --directory-public-key CLI option.')
    require(manual or (url and key),
            'No existing Control Plane URL could be determined from the verified deployment. Enter its HTTPS URL or configure a trusted directory.')
    host = ''
    if args.mode == 'public':
        # Reject a second extracted bundle trying to create node configuration
        # beside a proxy it does not own, before any state or secrets are made.
        proxy_owner = docker_project_directory('sparrow-public-proxy', 'caddy')
        require(proxy_owner is None or proxy_owner == PROXY.resolve(),
                'The existing Public proxy belongs to another installation. Open the updated Node Manager with Docker running so it can adopt the original Combined deployment safely.')
        host = public_hostname(args.node_domain or (auto_hostname(instance_id, args.public_ip) if args.auto_dns else ''))
        require(host not in all_public_hosts(), f'Hostname already assigned: {host}')
    paired_images = properties(ROOT / 'control-plane' / 'sparrow.conf')
    bundled_images = properties(TEMPLATE / 'sparrow.conf')
    image_prefix = (args.image_prefix or paired_images.get('SPARROW_IMAGE_PREFIX') or
                    bundled_images.get('SPARROW_IMAGE_PREFIX') or 'ghcr.io/cbgm/sparrow')
    image_tag = (args.image_tag or paired_images.get('SPARROW_IMAGE_TAG') or
                 bundled_images.get('SPARROW_IMAGE_TAG') or 'latest')
    require(re.fullmatch(r'[a-z0-9.-]+(?:/[a-z0-9._-]+)+', image_prefix) and
            re.fullmatch(r'[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}', image_tag), 'Invalid image reference.')
    port = port_for_new_node()
    data = {'id': instance_id, 'name': args.name.strip(), 'mode': args.mode,
            'hostname': host, 'port': port, 'project': 'sparrow-community-' + instance_id}
    directory.mkdir(parents=True, exist_ok=False)
    (directory / 'secrets').mkdir()
    try:
        for name in TEMPLATES:
            shutil.copy2(TEMPLATE / name, directory / name)
        # Windows PowerShell's existing signed-discovery helper is relative
        # to the parent of the per-node deployment directory.
        for helper in ('control_plane_directory_client.py', 'Get-SparrowVerifiedDirectory.ps1'):
            dest = INSTANCES / helper
            # Helpers must match the new templates; never modify the original
            # Combined installation's helpers, credentials or configuration.
            shutil.copy2(SOURCE / helper, dest)
        overlay = directory / 'docker-compose.shared-proxy.yml'
        content = overlay.read_text(encoding='utf-8')
        # Every public backend receives its own unique edge-network alias.
        content = content.replace('sparrow-node-', f'sparrow-{instance_id}-')
        atomic(overlay, content)
        atomic(directory / 'node-instance.json', json.dumps(data, indent=2) + '\n')
        paired_plane = properties(ROOT / 'control-plane' / 'sparrow.conf')
        paired_host = paired_plane.get('PUBLIC_DOMAIN', '') if existing_combined_plane(args.mode) else ''
        # A node connected to a different Control Plane must not advertise the
        # unrelated paired Control Plane merely because Combined is installed.
        local_plane = paired_host if args.mode == 'public' and paired_host and f'https://{paired_host}' in manual else ''
        settings = {'CONFIGURED': 'true', 'MODE': args.mode, 'PUBLIC_DOMAIN': host,
                    'SHARED_PROXY': 'true' if args.mode == 'public' else 'false',
                    'COMMUNITY_NODE_PROJECT_NAME': data['project'],
                    'COMMUNITY_NODE_HTTP_PORT': str(port),
                    'MAILBOX_DIAGNOSTIC_PORT': str(port + 2),
                    'FEDERATION_DIAGNOSTIC_PORT': str(port + 3),
                    'GATEWAY_DIAGNOSTIC_PORT': str(port + 4),
                    'MAILBOX_DATABASE_PORT': str(port + 10000),
                    'FEDERATION_DATABASE_PORT': str(port + 10001),
                    'COMMUNITY_NODE_DIRECTORY_CACHE_VOLUME': data['project'] + '-directory-cache',
                    'CONTROL_PLANE_DIRECTORY_URL': url,
                    'CONTROL_PLANE_DIRECTORY_PUBLIC_KEY': key,
                    'CONTROL_PLANE_URLS': ','.join(manual),
                    'LOCAL_CONTROL_PLANE_DOMAIN': local_plane,
                    'SPARROW_IMAGE_PREFIX': image_prefix, 'SPARROW_IMAGE_TAG': image_tag}
        for value in settings.values():
            require(re.fullmatch(r'[A-Za-z0-9._:/@+,=-]*', value) is not None,
                    'Config contains unsupported characters.')
        atomic(directory / 'sparrow.conf', ''.join(f'{k}={v}\n' for k, v in settings.items()))
    except Exception:
        shutil.rmtree(directory)
        raise
    print(f'Added {data["name"]} [{instance_id}]; distinct Compose project {data["project"]}.')
    print('Install explicitly with: nodes install --id ' + instance_id)


def configure_directory(args):
    directory, meta = find(args.id)
    settings = properties(directory / 'sparrow.conf')
    new_url = directory_url(args.directory_url)
    old_pin = settings.get('CONTROL_PLANE_DIRECTORY_PUBLIC_KEY', '')
    old_url = settings.get('CONTROL_PLANE_DIRECTORY_URL', '')
    # Existing pins are valid only for the directory to which they were bound.
    # GUI URL edits use the matching release/Combined trust configuration;
    # a different privately operated directory requires an explicit advanced
    # CLI trust-pin provisioning step.
    pin = (args.directory_public_key or
           (old_pin if old_url == new_url else '') or
           bundled_directory_trust(new_url))
    require(pin, 'Unknown directory signer: use a preconfigured trusted directory or provision its verification key using the advanced CLI option.')
    require(not installed(directory) or not old_pin or old_pin == pin,
            'Changing an installed directory signing trust anchor requires a separate verified migration.')
    # Authenticate before changing the configured endpoint, if possible. A
    # cached signature from the same pinned key may preserve an offline node.
    from control_plane_directory_client import discover
    urls, state = discover(new_url, pin, directory / '.control-plane-directory-verified.json')
    require(urls or settings.get('CONTROL_PLANE_URLS', ''),
            'The directory has no Control Planes and this node has no manual plane.')
    if installed(directory):
        ownership(directory, meta)
    settings['CONTROL_PLANE_DIRECTORY_URL'] = new_url
    settings['CONTROL_PLANE_DIRECTORY_PUBLIC_KEY'] = pin
    atomic(directory / 'sparrow.conf', ''.join(f'{k}={v}\n' for k, v in settings.items()))
    if installed(directory):
        # Do not overwrite CONTROL_PLANE_URL in the installed runtime. The
        # selected registry/identity and all durable volumes stay unchanged;
        # the authenticated background worker receives the updated source.
        start_directory_worker(directory)
    print(f'Directory URL saved for {meta["name"]}: {new_url}. Source: {state}.')
    print('Existing node registration target and signing identity were not changed.')


def own_proxy_is_running():
    result = docker('ps', '--filter', 'label=com.docker.compose.project=sparrow-public-proxy',
                    '--filter', 'label=com.docker.compose.service=caddy',
                    '--format', '{{.ID}}', capture=True)
    return bool(result.stdout.strip())


def proxy_compose(*action):
    """Mount independent route snippets even with a pre-multi-node proxy.

    An additive Compose override preserves the original file, certificate
    volumes, proxy project and unrelated services. Only Caddy may recreate.
    """
    base = PROXY / 'docker-compose.yml'
    require(base.is_file(), f'Missing original shared-proxy Compose file: {base}')
    args = ['docker', 'compose', '-p', 'sparrow-public-proxy',
            '--project-directory', str(PROXY), '-f', str(base)]
    if '/etc/caddy/node-routes' not in base.read_text(encoding='utf-8'):
        overlay = PROXY / 'docker-compose.node-routes.yml'
        expected = ('services:\n  caddy:\n    volumes:\n'
                    '      - ../node-instances/routes:/etc/caddy/node-routes:ro\n')
        if overlay.exists():
            require(overlay.read_text(encoding='utf-8') == expected,
                    'An existing custom proxy routes override needs review; refusing to replace it.')
        else:
            atomic(overlay, expected)
        args += ['-f', str(overlay)]
    return cmd(*args, *action, cwd=PROXY)


def shared_route_mount_is_live():
    """A running proxy with the route-directory mount needs only caddy reload.

    First installation against a pre-multi-node Combined proxy still needs one
    Compose recreate to attach that mount; subsequent node installs must not
    interrupt active TLS connections just to publish another route file.
    """
    ids = docker('ps', '--filter', 'label=com.docker.compose.project=sparrow-public-proxy',
                 '--filter', 'label=com.docker.compose.service=caddy',
                 '--format', '{{.ID}}', capture=True).stdout.splitlines()
    if len(ids) != 1:
        return False
    mounts = docker('inspect', ids[0], '--format', '{{json .Mounts}}', capture=True).stdout
    try:
        return any(mount.get('Destination') == '/etc/caddy/node-routes' and
                   mount.get('Type') == 'bind'
                   for mount in json.loads(mounts))
    except (ValueError, TypeError):
        return False


def ensure_proxy():
    proxy_ownership()
    existing = PROXY / 'Caddyfile'
    if not existing.is_file():
        # Public routing belongs to ONE independent edge process. A node-only
        # installation is allowed to start it without provisioning a CP.
        ports = docker('ps', '--format', '{{.Names}} {{.Ports}}', capture=True).stdout
        require(not re.search(r'(?:0\.0\.0\.0|\[::\]):(?:80|443)->', ports),
                '80/443 are owned by another container; configure one common edge proxy instead.')
        atomic(existing, '# Sparrow public edge (node-only)\n' + IMPORT + '\n')
    network = docker('network', 'inspect', NETWORK, capture=True, check=False)
    if network.returncode:
        docker('network', 'create', '--driver', 'bridge', NETWORK)
    ROUTES.mkdir(parents=True, exist_ok=True)
    (ROOT / 'control-plane' / 'directory-registration-proofs').mkdir(parents=True, exist_ok=True)
    # A running proxy that already has the routes bind-mount can incorporate
    # the new snippet with validate + reload in publish_route(). Running Compose
    # up here can unnecessarily recreate Caddy and drop active TLS handshakes.
    if shared_route_mount_is_live():
        return
    # Older Combined deployments require ONE recreate to attach the routes
    # mount; do not touch the Control Plane, directory worker or node services.
    proxy_compose('up', '-d', '--no-deps', 'caddy')
    require(shared_route_mount_is_live(),
            'Shared proxy started without the independent node-routes mount.')


def route_for(directory, meta):
    text = (directory / 'Caddyfile').read_text(encoding='utf-8')
    marker = '{$COMMUNITY_NODE_SITE_ADDRESS}'
    require(marker in text, 'Unexpected node Caddy template.')
    prefix = text.split(marker, 1)[0]
    require('(sparrow_community_routes)' in prefix, 'Unexpected node Caddy route snippet.')
    prefix = prefix.replace('sparrow_community_routes', 'sparrow_' + meta['id'].replace('-', '_') + '_routes')
    for name in BACKENDS:
        source = name + ':' + {'gateway': '8094', 'federation': '8093', 'mailbox': '8092'}[name]
        target = f'sparrow-{meta["id"]}-{source}'
        require(source in prefix, 'Unexpected upstream in node Caddy route.')
        prefix = prefix.replace(source, target)
    prefix = prefix.replace('root * /srv', 'root * /srv/community-node')
    return prefix.rstrip() + '\n\n' + meta['hostname'] + ' {\n    import sparrow_' + meta['id'].replace('-', '_') + '_routes\n}\n'


def publish_route(directory, meta):
    require(meta['mode'] == 'public', 'Not a Public node.')
    ROUTES.mkdir(parents=True, exist_ok=True)
    path = ROUTES / (meta['id'] + '.caddy')
    previous = path.read_text(encoding='utf-8') if path.exists() else None
    main = PROXY / 'Caddyfile'
    before = main.read_text(encoding='utf-8') if main.is_file() else ''
    # Append-only; existing CP/Node routes are never rewritten here. Ownership
    # is verified before touching another public endpoint's configuration.
    proxy_ownership()
    atomic(path, route_for(directory, meta))
    try:
        if IMPORT not in before:
            atomic(main, before.rstrip() + '\n\n' + IMPORT + '\n')
        ensure_proxy()
        proxy_compose('exec', '-T', 'caddy', 'caddy', 'validate', '--config', '/etc/caddy/Caddyfile')
        proxy_compose('exec', '-T', 'caddy', 'caddy', 'reload', '--config', '/etc/caddy/Caddyfile')
    except Exception:
        if previous is None:
            path.unlink(missing_ok=True)
        else:
            atomic(path, previous)
        if before:
            atomic(main, before)
        raise


def start_directory_worker(directory):
    conf = properties(directory / 'sparrow.conf')
    if not conf.get('CONTROL_PLANE_DIRECTORY_URL') or not conf.get('CONTROL_PLANE_DIRECTORY_PUBLIC_KEY'):
        return
    # Unlike the central directory SERVER, this is a public, independently
    # pinned discovery CLIENT. One worker and one verified cache per node.
    args = ['docker', 'compose', '--env-file', str(directory / '.env.runtime')]
    files = ['docker-compose.yml', 'docker-compose.release.yml']
    if conf.get('MODE') == 'public':
        files += ['docker-compose.production.yml', 'docker-compose.shared-proxy.yml']
    files.append('docker-compose.directory-sync.yml')
    for name in files:
        args += ['-f', str(directory / name)]
    args += ['--profile', 'directory', 'up', '-d', '--no-deps', '--build', 'directory-sync']
    try:
        cmd(*args, cwd=directory)
        print('Signed directory worker running (isolated per-node verified cache).')
    except NodeError as exc:
        print('Signed directory background refresh deferred; existing node and signed cache unchanged: ' + str(exc)[:220])


# Older bundles passed a scalar command to a list-form `sh -ec` entrypoint.
# Compose tokenized that string: sh received `mkdir` as its script instead of
# the intended compound command. Instances created before the template fix
# keep a PRIVATE copy of docker-compose.yml, so update only this exact faulty
# line on their first retry. Never replace the rest of an instance's Compose
# file, credentials, identity or volumes.
OLD_BLOB_INIT = ('    command: "mkdir -p /var/lib/sparrow/blobs && '
                 'chown -R 65532:65532 /var/lib/sparrow/blobs"')
NEW_BLOB_INIT = ('    command: ["mkdir -p /var/lib/sparrow/blobs && '
                 'chown -R 65532:65532 /var/lib/sparrow/blobs"]')


def repair_old_blob_init_template(directory):
    path = directory / 'docker-compose.yml'
    content = path.read_text(encoding='utf-8')
    if OLD_BLOB_INIT not in content:
        return False
    require(content.count(OLD_BLOB_INIT) == 1 and
            '  blob-storage-init:' in content and
            '    entrypoint: ["/bin/sh", "-ec"]' in content,
            'Unexpected blob initializer definition; refusing to change instance Compose.')
    atomic(path, content.replace(OLD_BLOB_INIT, NEW_BLOB_INIT))
    print('Corrected the original blob-storage-init shell command in this instance only.')
    return True


def blob_init_container(meta):
    result = docker('ps', '--all',
                    '--filter', f'label=com.docker.compose.project={meta["project"]}',
                    '--filter', 'label=com.docker.compose.service=blob-storage-init',
                    '--format', '{{.ID}}', capture=True, check=False)
    return result.stdout.strip().splitlines()[0] if result.returncode == 0 and result.stdout.strip() else ''


def blob_init_diagnostic(meta):
    # Only this tiny init container: broad service logs may contain credentials.
    try:
        container = blob_init_container(meta)
        if not container:
            return '\nblob-storage-init: no container was created.'
        status = docker('inspect', '--format',
                        '{{.State.Status}} (exit {{.State.ExitCode}}): {{.State.Error}}',
                        container, capture=True, check=False)
        logs = docker('logs', '--tail', '35', container, capture=True, check=False)
        body = (logs.stdout or '') + (logs.stderr or '')
        return ('\nblob-storage-init ' + status.stdout.strip() +
                '\n' + (body.strip() or '(no container logs)')[:2000])
    except (NodeError, OSError):
        return '\nUnable to retrieve blob-storage-init logs. Inspect its container logs directly.'


def retry_failed_blob_init(directory, meta):
    container = blob_init_container(meta)
    if not container:
        return
    result = docker('inspect', '--format', '{{.State.Status}} {{.State.ExitCode}}',
                    container, capture=True, check=False)
    state = result.stdout.strip().split()
    if result.returncode or len(state) != 2 or state[0] != 'exited' or state[1] == '0':
        return
    print('Previous blob-storage-init failed; recreating only the one-shot initializer '
          'with the corrected command (all persistent node data stays intact).')
    try:
        compose(directory, 'up', '-d', '--no-deps', '--force-recreate', 'blob-storage-init')
    except NodeError as exc:
        raise NodeError(str(exc) + blob_init_diagnostic(meta)) from exc


def installed(directory):
    return (directory / '.env.runtime').is_file()


def install(args):
    directory, meta = find(args.id)
    ownership(directory, meta)
    settings = properties(directory / 'sparrow.conf')
    if installed(directory):
        require(properties(directory / '.env.runtime').get('COMMUNITY_NODE_PROJECT_NAME') == meta['project'],
                'Installed runtime belongs to another Compose project.')
        require(not getattr(args, 'directory_url', '') and not getattr(args, 'control_plane_url', '') and not getattr(args, 'directory_public_key', ''),
                'Reconfiguring an installed node requires a separate reviewed registration change.')
    else:
        require(not getattr(args, 'directory_url', '') and not getattr(args, 'control_plane_url', '') and not getattr(args, 'directory_public_key', ''),
                'Edit the instance configuration before its first install; do not override pinned identities implicitly.')
        if settings.get('CONTROL_PLANE_DIRECTORY_URL') and not settings.get('CONTROL_PLANE_URLS'):
            from control_plane_directory_client import discover
            candidates, state = discover(settings['CONTROL_PLANE_DIRECTORY_URL'],
                                         settings['CONTROL_PLANE_DIRECTORY_PUBLIC_KEY'],
                                         directory / '.control-plane-directory-verified.json')
            require(candidates, 'Verified directory does not list any Control Plane.')
            if state.startswith('verified-cache'):
                # An existing registered node may continue using its already
                # pinned peer during an outage. A FIRST install must not use
                # an expired snapshot to enroll a new, previously untrusted CP.
                snapshot = json.loads((directory / '.control-plane-directory-verified.json').read_text(encoding='utf-8'))
                expiry = snapshot['payload']['validUntilEpochMilliseconds']
                require(type(expiry) is int and expiry > int(time.time() * 1000),
                        'Cached signed directory has expired. New node enrollment requires a fresh verified snapshot or explicit Control Plane origin.')
            print('Signed Control Plane directory: ' + state)
        if meta['mode'] == 'public':
            network = docker('network', 'inspect', NETWORK, capture=True, check=False)
            if network.returncode:
                docker('network', 'create', '--driver', 'bridge', NETWORK)
        # Bootstrap runs ONLY once. A second install updates images, never keys,
        # persistent Docker volumes or an existing registration choice.
        if os.name != 'nt':
            env = os.environ.copy()
            env['CONTROL_PLANE_DIRECTORY_CLIENT'] = str(INSTANCES / 'control_plane_directory_client.py')
            cmd('bash', str(directory / 'bootstrap-community-node.sh'), cwd=directory, env=env)
        else:
            try:
                cmd('powershell.exe', '-STA', '-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass',
                    '-File', str(directory / 'Bootstrap-CommunityNode.ps1'), '-Headless', cwd=directory)
            except NodeError as exc:
                raise NodeError(str(exc) + blob_init_diagnostic(meta)) from exc
    if installed(directory) and not args.update:
        # A failed FIRST bootstrap can leave a valid .env.runtime and an exited
        # init container behind. Never rebootstrap (which might rewrite secrets)
        # or use `down -v`; repair this instance's Compose and retry the init.
        repair_old_blob_init_template(directory)
        retry_failed_blob_init(directory, meta)
        try:
            compose(directory, 'up', '-d', '--no-recreate')
        except NodeError as exc:
            raise NodeError(str(exc) + blob_init_diagnostic(meta)) from exc
    if meta['mode'] == 'public':
        publish_route(directory, meta)
    if installed(directory) and args.update:
        # Backend-only updates; Compose project stays fixed and volumes survive.
        compose(directory, 'pull', *BACKENDS)
        compose(directory, 'up', '-d', '--no-deps', '--pull', 'never', *BACKENDS)
    print(f'{meta["name"]} [{meta["id"]}] installed. Endpoint: ' +
          (f'https://{meta["hostname"]}' if meta['mode'] == 'public' else f'LAN port {meta["port"]}'))
    print('Node registration: confirm this node is advertised in the selected Control Plane /v1/nodes.')


def stop_directory_worker(directory):
    project = 'sparrow-community-' + metadata(directory)['id']
    result = docker('ps', '--filter', f'label=com.docker.compose.project={project}',
                    '--filter', 'label=com.docker.compose.service=directory-sync',
                    '--format', '{{.ID}}', capture=True)
    if result.stdout.strip():
        docker('stop', *result.stdout.splitlines())


def registration_state(directory):
    """Best-effort status only; never mistake a directory HTTP response for cryptographic trust."""
    runtime_values = properties(directory / '.env.runtime')
    endpoint = runtime_values.get('CLIENT_ENDPOINT', '')
    candidates = runtime_values.get('ADVERTISED_CONTROL_PLANE_URLS', '').split(',')
    if not endpoint or not any(candidates):
        return 'unavailable (no advertised Control Plane or gateway endpoint)'
    problems = []
    for candidate in candidates[:8]:
        candidate = candidate.strip().rstrip('/')
        if not candidate:
            continue
        try:
            with urlopen(candidate + '/v1/nodes', timeout=4) as response:
                raw = response.read(512 * 1024 + 1)
            if len(raw) > 512 * 1024:
                problems.append('node directory too large')
                continue
            document = json.loads(raw)
            serialized = json.dumps(document)
            if endpoint in serialized and 'signature' in serialized.lower():
                return ('advertised by ' + candidate +
                        ' (directory signature not independently verified by this status view)')
            problems.append(candidate + ': this endpoint not listed in the response')
        except (OSError, ValueError) as exc:
            problems.append(candidate + ': ' + str(exc)[:90])
    return 'not confirmed yet; ' + '; '.join(problems[:2])


def lifecycle(args):
    directory, meta = find(args.id)
    ownership(directory, meta)
    require(installed(directory), 'Node is not installed. Run nodes install first.')
    if args.action == 'update':
        args.update = True
        return install(args)
    if args.action == 'start':
        repair_old_blob_init_template(directory)
        retry_failed_blob_init(directory, meta)
        try:
            compose(directory, 'up', '-d', '--no-recreate')
        except NodeError as exc:
            raise NodeError(str(exc) + blob_init_diagnostic(meta)) from exc
        if meta['mode'] == 'public':
            publish_route(directory, meta)
        start_directory_worker(directory)
    elif args.action == 'stop':
        compose(directory, 'stop')
        # Optional profile service is independent of the normal Compose stop.
        stop_directory_worker(directory)
    elif args.action == 'status':
        compose(directory, 'ps', '--all')
        print('Endpoint: ' + (f'https://{meta["hostname"]}' if meta['mode'] == 'public' else f'LAN :{meta["port"]}'))
        print('Registration: ' + registration_state(directory))
    elif args.action == 'logs':
        compose(directory, 'logs', '--no-color', '--tail', '120')


def remove(args):
    directory, meta = find(args.id)
    ownership(directory, meta)
    if installed(directory):
        # Default remove is a stop + unpublish, NOT 'down -v', a volume prune,
        # a directory delete or an identity/FCM reset.
        compose(directory, 'stop')
        stop_directory_worker(directory)
    route = ROUTES / (meta['id'] + '.caddy')
    if route.exists():
        proxy_ownership()
        old = route.read_text(encoding='utf-8')
        route.unlink()
        try:
            if own_proxy_is_running():
                proxy_compose('exec', '-T', 'caddy', 'caddy', 'reload', '--config', '/etc/caddy/Caddyfile')
        except Exception:
            atomic(route, old)
            raise
    print(f'{meta["name"]} stopped and unpublished. Data, identity, instance config and volumes preserved.')
    print('Run nodes start --id ' + args.id + ' to restore the node.')


def list_nodes():
    if not instance_dirs():
        print('No managed Community Node instances. Add one with: nodes add --name "Node A" ...')
    for d in instance_dirs():
        m = metadata(d)
        endpoint = f'https://{m["hostname"]}' if m['mode'] == 'public' else f'LAN port {m["port"]}'
        state = 'installed' if installed(d) else 'configured'
        print(f'{m["id"]}\t{m["name"]}\t{m["mode"]}\t{state}\t{endpoint}')


def main():
    p = argparse.ArgumentParser(description='Manage independent Community Nodes; Combined is managed separately.')
    sub = p.add_subparsers(dest='action', required=True)
    sub.add_parser('list')
    sub.add_parser('info', help='Read-only detection of the existing Combined installation (JSON)')
    sub.add_parser('diagnose', help='Copyable read-only Docker and local-path detection report')
    move_cmd = sub.add_parser('relink', help='Explicitly create an original-path junction for a MOVED Windows installation')
    move_cmd.add_argument('--moved-to', required=True, help='Actual moved original installation root, NOT an extracted new installer bundle')
    add_cmd = sub.add_parser('add')
    add_cmd.add_argument('--name', required=True)
    add_cmd.add_argument('--id', default='')
    add_cmd.add_argument('--mode', choices=('lan', 'public'), default='lan')
    add_cmd.add_argument('--node-domain', default='')
    add_cmd.add_argument('--auto-dns', action='store_true')
    add_cmd.add_argument('--public-ip', default='')
    add_cmd.add_argument('--control-plane-url', default='', help='Existing explicit Control Plane origin(s), comma-separated')
    add_cmd.add_argument('--directory-url', default='', help='Independent signed Directory Server HTTPS origin')
    add_cmd.add_argument('--directory-public-key', default='', help='Independent pinned directory verification key')
    add_cmd.add_argument('--image-prefix', default='')
    add_cmd.add_argument('--image-tag', default='')
    for action in ('install', 'start', 'stop', 'update', 'status', 'logs', 'remove', 'configure-directory'):
        action_cmd = sub.add_parser(action)
        action_cmd.add_argument('--id', required=True)
        if action == 'configure-directory':
            action_cmd.add_argument('--directory-url', required=True)
            action_cmd.add_argument('--directory-public-key', default='')
        if action == 'install':
            action_cmd.add_argument('--update', action='store_true')
            action_cmd.add_argument('--directory-url', default='')
            action_cmd.add_argument('--directory-public-key', default='')
            action_cmd.add_argument('--control-plane-url', default='')
    args = p.parse_args()
    try:
        if args.action == 'diagnose':
            print(diagnosis())
            return 0
        if args.action == 'relink':
            relink_moved_deployment(args.moved_to)
            return 0
        select_existing_deployment()
        if args.action == 'info':
            conf = properties(ROOT / 'control-plane' / 'sparrow.conf')
            mode = conf.get('MODE', '') if EXISTING_COMBINED else ''
            print(json.dumps({'deploymentRoot': str(ROOT), 'instancesRoot': str(INSTANCES),
                              'mode': mode, 'controlPlaneUrl': existing_combined_plane(mode) if mode in ('public', 'lan') else '',
                              'existingCombined': bool(mode)}))
        elif args.action == 'list':
            list_nodes()
        else:
            with manager_lock():
                if args.action == 'add':
                    add(args)
                else:
                    require(shutil.which('docker'), 'Docker CLI is required.')
                    if args.action == 'install':
                        install(args)
                    elif args.action == 'remove':
                        remove(args)
                    elif args.action == 'configure-directory':
                        configure_directory(args)
                    else:
                        lifecycle(args)
    except (NodeError, OSError, ValueError, KeyError, ImportError, subprocess.TimeoutExpired) as exc:
        print('Sparrow nodes: ' + str(exc), file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
