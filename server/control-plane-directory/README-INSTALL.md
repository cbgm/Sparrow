# Independent Sparrow Control Plane Directory — operator installer

Launch `Start-SparrowDirectory.cmd` in this directory on Windows with Docker Desktop running. **No Control Plane or Community Node needs to be installed or running.** The source lives under `server/control-plane-directory` but this directory, its image and its secrets must **never** be shipped in the public unified server bundle.

- Leave **Optional combined server folder** empty to run the Directory with its own Caddy HTTPS container. This requires public TCP 80/443 on this host to be free, your directory hostname to resolve to the host, and router/firewall forwarding. Installer creates the Docker network, key, persistent volume and Caddy automatically. It does not touch a Sparrow server.
- If the combined server's Caddy already owns TCP 80/443 on the SAME public IP, Docker cannot run an independent second HTTPS proxy on those ports. To keep both on the same host, explicitly select the existing combined server folder to share that one ingress; the directory *service* remains a separate container, but its external HTTPS will depend on the shared Caddy. For fully independent ingress, use another public IP / machine or a separate always-on reverse proxy.
- The installer preserves `sparrow-central-directory-state` (SQLite data and directory signing identity). Never delete that Docker volume to troubleshoot a deployment.
- Verified Control Planes register **automatically** after the directory checks public DNS, HTTPS endpoint ownership and Ed25519 private-key possession. No pending-registration approval is required. Admin revocation and identity/endpoint rotation remain separately protected.
- `/.well-known/sparrow-directory` publishes a public bootstrap key; Android verifies it over valid HTTPS on first connection, verifies the signed snapshot and pins the key in persistent storage. The key and URL need not be inserted into `local.properties`. The existing directory URL build variable is only an optional *initial test bootstrap*; users can edit the URL in Settings. A moved URL must present the same already-pinned key.
- A compromised active signing key is **not** recovered merely by changing the URL. Safely recovering from a compromised signing key requires a separately authorized recovery mechanism and is not provided by this patch. Do not weaken identity-pinning checks to bypass a warning.

The directory requires reachable HTTPS to be used by Android. Never forward its loopback-only admin listener to Caddy or publish the admin token. No untrusted Certificate Authority or TLS bypass is needed.

## Linux + operator-only packaging

On an independent Linux Docker host with publicly reachable ports 80/443 and
DNS for the directory hostname, extract `sparrow-directory-linux.zip` and run:

```bash
chmod +x Start-SparrowDirectory.sh
./Start-SparrowDirectory.sh --hostname directory.example.com
```

The Linux script installs the Directory and its own Caddy without querying or
changing any combined Sparrow Server. On the same IP as an existing Caddy
already listening on ports 80/443, use a distinct host/IP or an independently
managed always-on front proxy; two projects cannot claim the same ingress.

Build BOTH private operator installer archives from the Sparrow source tree:

```bash
python3 server/control-plane-directory/Build-SparrowDirectoryInstaller.py
```

Generated ZIPs remain in `server/control-plane-directory/dist/` and must not be
copied into the public unified server release bundle. The template in
`CI-WORKFLOW-JOB.yml` is intended for merging into your CURRENT release workflow
under `jobs:` without changing its existing Android signing/release logic.
Workflow-run artifacts are **not** Release downloads, but they may still be
visible to people with workflow-artifact access. Use a private repository or
restricted internal storage if the installer itself must not be publicly obtainable.
