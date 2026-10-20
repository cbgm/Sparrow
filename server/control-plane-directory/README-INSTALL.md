# Independent Sparrow Control Plane Directory — operator installer

Double-click `Start-SparrowDirectory.cmd` (Windows), `Start-SparrowDirectory.command` (macOS) or run
`./Start-SparrowDirectory.sh` (Linux) with Docker running. A window opens in the same style as the
Sparrow Server installer; it needs Python 3.9+ (on Linux also `python3-tk`). Started from the source
code, it first asks where to create your Directory folder (like the server installer), copies the
settings of the running Directory so the new folder takes it over, and opens there. **No Control Plane or
Community Node needs to be installed or running.** The source lives under
`server/control-plane-directory`; this folder, its image and its secrets are **never** shipped in
the public server bundle.

The window shows the Directory URL and public key (with Copy), its status, and the buttons
**Install / Start**, **Stop**, **Status**, **Logs** and **Servers** (the registered Control Planes).

- **Hostname:** by default the free `directory-<public-ip>.sslip.io` name. Untick *Automatic* to use
  your own DNS name.
- **HTTPS proxy — Sparrow Server's proxy (same IP):** the running Sparrow Server is found
  automatically. Its Caddy gets a marked Directory route (the server installer keeps it on every
  Start), the server's `sparrow.conf` files get the Directory URL and key, and its Control Plane is
  registered and verified. The Directory *service* stays a separate container.
- **HTTPS proxy — Own proxy:** the Directory runs its own Caddy and needs free public ports 80/443
  on this host, DNS for the hostname and router forwarding. No Sparrow Server is touched.
- The installer preserves `sparrow-central-directory-state` (SQLite data and signing identity).
  Never delete that Docker volume to troubleshoot a deployment.
- Verified Control Planes register **automatically** after the Directory checks public DNS, HTTPS
  endpoint ownership and Ed25519 private-key possession. A registered Control Plane may move to a
  new HTTPS endpoint only when the **same signing identity** completes a fresh challenge.
  Public-key rotation and revoked identities still require separate operator recovery.
- `/.well-known/sparrow-directory` publishes a public bootstrap key; Android verifies it over valid
  HTTPS, verifies the signed snapshot and pins the key. Users can edit the Directory URL in the
  app's settings; a moved URL must present the same pinned key.
- **Dynamic IP:** an automatic `directory-<ip>.sslip.io` name changes with the public IPv4. The
  server installer moves the route on its next Start; **Start** here then updates the Directory's
  own settings. The signing identity is kept. Apps that only know the old name need the new URL
  once in their settings, unless the Directory uses a stable DNS/DDNS name.
- A compromised signing key is **not** recovered by changing the URL. Do not weaken
  identity-pinning checks to bypass a warning.

Never forward the loopback-only admin listener to Caddy or publish the admin token.

## Without a desktop

```bash
./Start-SparrowDirectory.sh                       # install / start with the automatic name
./Start-SparrowDirectory.sh install --hostname directory.example.com --standalone
./Start-SparrowDirectory.sh status|logs|planes|stop
```

## Operator-only packaging

```bash
python3 server/control-plane-directory/Build-SparrowDirectoryInstaller.py
```

Creates `sparrow-directory-windows.zip` and `sparrow-directory-linux.zip` (Linux and macOS) in
`server/control-plane-directory/dist/`. They must not be copied into the public server release
bundle. Workflow-run artifacts are **not** Release downloads, but people with workflow-artifact
access can see them; use a private repository or restricted storage if needed.
