# Sparrow unified server installer (Windows, Linux, macOS)

The server bundle contains only the installer, component configuration, and runtime-management scripts. Source-only validation and recovery rehearsal scripts are not distributed.

Windows: Start-SparrowServer.cmd
Linux: ./Start-SparrowServer.sh --help
macOS: ./Start-SparrowServer.command (or ./Start-SparrowServer.sh --help)

On Linux/macOS, install a fresh combined public server with shared Caddy and automatic sslip.io hostnames:

    ./Start-SparrowServer.sh install --component combined --mode public --auto-dns

Use the same `install` action in the original installed folder to start/update without clearing server identity, stored messages, databases, or volumes. To configure Firebase Admin on that deployment:

    ./Start-SparrowServer.sh install --component combined --mode public --firebase-file /outside-the-installation/firebase-admin.json

Management:

    ./Start-SparrowServer.sh status --component combined
    ./Start-SparrowServer.sh stop --component combined
    ./Start-SparrowServer.sh start --component combined
    ./Start-SparrowServer.sh logs --component combined

The installed public addresses are printed by `status` and `install`. Operation logs are stored under ./logs in the installation folder. Public HTTPS and message delivery require external validation on the actual server.

Only the explicit `reinstall-public --component combined --mode public --confirm-delete-data` action deletes data and generates fresh server identities. Never use this for a server whose stored state must be retained. Never extract a new distribution ZIP over a configured deployment; update the manager scripts in its original folder and keep runtime files, secrets, and volumes.
