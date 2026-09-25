# Sparrow Independent Control Plane Directory

This operator-managed service is separate from the public combined Control Plane + Community Node bundle, but its **source** belongs under `server/control-plane-directory/`. Never include its image, private volumes, admin token or installer in the public release ZIP.

Run the private Windows installer `Start-SparrowDirectory.cmd` here. For an independent deployment leave the optional combined-server folder **empty**: the installer creates its own Directory container and its own Caddy HTTPS reverse proxy, assuming TCP 80/443 are free. If an existing Sparrow Caddy owns 80/443 on the same IP, optionally select the installed combined-server folder to share its Caddy route. Sharing ingress makes external directory availability depend on that proxy; it does not make directory storage, signing or registration depend on any Control Plane. See `README-INSTALL.md`.

Public API: `GET /health`, `GET /.well-known/sparrow-directory` (public bootstrap metadata), `GET /v1/control-planes` (signed directory), `POST /v1/registrations/challenge`, and `POST /v1/registrations`. Valid DNS/public HTTPS endpoint possession **and** Ed25519 signing-key possession are verified before each new Control Plane is approved automatically. Existing revoked identities are not silently reinstated; endpoint/key changes require a separate authenticated process. Never forward the administrator listener or include administrator credentials in distributed clients.

Android pins the first HTTPS-fetched directory signing key **only after** verifying its signed snapshot. A new directory URL must present the already pinned key; no directory signing key needs to be embedded in Android build properties. Local cache preserves previously verified plane candidates during outages. A compromised signing key requires separate secure recovery (not implemented), not URL editing alone.

The signing key and SQLite database persist in Docker volume `sparrow-central-directory-state` and must never be regenerated on ordinary restart/update. No actual Windows Docker or Android emulator end-to-end verification has been performed for these changes.
