# Independent Sparrow Control Plane Directory — operator installer

Double-click `Start-SparrowDirectory.cmd` (Windows), `Start-SparrowDirectory.command` (macOS) or run
`./Start-SparrowDirectory.sh` (Linux) with Docker running. A window opens in the same style as the
Sparrow Server installer (`directory_window.py`, engine `directory_installer.py`); it needs Python
3.9+ (on Linux also `python3-tk`). **No Control Plane or Community Node needs to be installed or
running.** The source lives under
`server/control-plane-directory`; this folder, its image and its secrets are **never** shipped in
the public server bundle.

## Starting from the source code

Started from `server/control-plane-directory` of a source checkout, the window first asks where to
create your Directory folder (default `Desktop/Sparrow Directory`, or the home folder without a
desktop folder). *Create and open* runs `Build-SparrowDirectoryInstaller.py --install <folder>`: the
folder must be new or empty, the installer files for this platform are copied into it, and the
`private/` settings (admin token, hostname, Caddyfile) of the Directory that is currently running in
Docker — or, if none runs, of the source folder — are copied along. The window then opens in the new
folder; its next **Install / Start** recreates the same `sparrow-central-directory` container from
there, so the new folder takes the running Directory over with its signing identity and data (the
Docker volume is shared, never copied). Afterwards the old folder can be deleted.

## The installer window

The window shows the Directory URL and public key (with Copy), its status, and the buttons
**Install / Start**, **Stop**, **Status**, **Logs** and **Servers** (the registered Control Planes).

- **Hostname:** by default the free `directory-<public-ip>.sslip.io` name. Untick *Automatic* to use
  your own DNS name.
- **HTTPS proxy — Sparrow Server's proxy (same IP):** preselected when a Sparrow Server runs on this
  machine; the running server is found automatically (its folder can also be chosen by hand). Its Caddy gets a marked Directory route (the server installer keeps it on every
  Start), the server's `sparrow.conf` files get the Directory URL and key, and its Control Plane is
  registered and verified. The Directory *service* stays a separate container.
- **HTTPS proxy — Own proxy:** preselected when no Sparrow Server runs here. The Directory runs its
  own Caddy (`sparrow-central-directory-caddy`) and needs free public ports 80/443 on this host, DNS
  for the hostname and router forwarding; Install refuses when another container already publishes
  80/443. No Sparrow Server is touched.
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

## Phone verification

The Directory verifies the phone numbers for all Control Planes; they need no Firebase file themselves.
Under **Phone verification (Firebase)** tick **Verify phone numbers for all Control Planes**, choose the
**Firebase Admin file** (the service account JSON of the app's Firebase project, `sparrow-a9048`) and click
Start (command line: `install --phone-verification --firebase-admin-file <file>`). The file is copied to
`private/firebase-admin.json` and mounted read-only into the phone verifier only; leave the field empty
later to keep it. The phone verifier runs next to the Directory. It is a separate container (`sparrow-phone-verifier`, same image,
`phone_verifier.py`) with its own keys in the Docker volume `sparrow-phone-verifier-state`; it never
sees the Directory signing key.

- On every Install / Start the Directory key certifies the verifier's two public keys (an Ed25519
  attestation key and an X25519 request key) into `private/phone-key-certificate.json`. The keys are
  created once and kept.
- The app sends its Firebase phone sign-in token sealed for the request key, through two relays, to
  `POST /v1/phone-attestations`. The verifier checks the token with the Firebase Admin SDK and that
  file (signature, project, expiry, and that the sign-in was not revoked) and answers with a signed
  attestation "this phone alias belongs to this device", sealed so only the app reads it. Nothing is
  stored or logged: no token, number or alias.
- **Sign-up slots.** Before the app has Firebase send the SMS, it reserves a slot at
  `POST /v1/phone-attestations/slots` (sealed, through the relays, with a small proof of work). The
  answer is a random slot token valid for one hour; the attestation needs it and uses it up. A slot that is
  not used within the hour is free again.
- **Verifications per day** (`install --phone-verifications-per-day N`, 0 = no limit) caps the sign-ups per
  UTC day for all Control Planes together: a day counts its attestations plus the open slots. When it is
  full, the slot request is answered with `429 PHONE_VERIFICATION_LIMIT` and the start of the next UTC day;
  the app then shows "Today's sign-ups are full … try again after …" and **sends no SMS**. The verifier keeps
  just the day, its count and the hashes of the open slots (`daily-count.json` in its volume, so a restart
  neither resets the day nor loses a slot), nothing about users. **Status** and the window show today's count.
- **Resends** (`install --phone-verification-resends N`, default 1) is how often the app may send the SMS
  code again for one sign-up; the app reads it from the slot.
- For the beta, 8 a day with 1 resend stays within Firebase's 10 free SMS a day. For production set the
  daily maximum to 0 (no limit) or higher and allow 2–3 resends; the app needs no update.
- The slots limit sign-ups, but not SMS that someone requests from Firebase directly with the app's Firebase
  details. Against that, set a budget with automatic billing stop, block expensive SMS regions and restrict
  the SMS regions in the Firebase console (see *Firebase costs* below).
- Every Control Plane's presence directory checks the attestation with the Directory key it already
  pins (`CONTROL_PLANE_DIRECTORY_PUBLIC_KEY`) and refuses phone numbers without one; only the
  operator's test numbers need none.
- The Caddy route gets `/v1/phone-attestations`, `/v1/phone-attestations/keys` and
  `/v1/phone-attestations/slots` for the verifier (an existing installation gets the new path with the next
  Install / Start).
  Untick the box (or `install --no-phone-verification`) to stop it and remove the paths again.

## Firebase costs

The SMS are sent by Firebase, not by the Directory, so their cost is set in the Google consoles:

1. **Budget with an automatic stop.** In the Google Cloud console of the app's Firebase project, create a budget
   (Billing → Budgets & alerts), e.g. a few euros a month, with a Pub/Sub topic, and connect Google's function that
   turns billing off when the budget is reached ("Disable billing usage with notifications" in the Cloud Billing
   documentation). Once it is reached, Firebase sends no more SMS until billing is turned on again; the project
   is used only for phone sign-in, so nothing else stops.
2. **SMS regions.** In the Firebase console under Authentication → Settings → *SMS region policy*, allow the
   regions your testers are in, or deny the expensive ones. New projects allow no region until one is set.
3. **API key.** While the app is not installed from the Play Store, Play Integrity cannot vouch for it and
   Firebase falls back to a reCAPTCHA check before each SMS; that check needs the API key usable for
   `PROJECT_ID.firebaseapp.com`. Do not restrict the key to the Android app until the app comes from the Play
   Store; then turn on App Check with Play Integrity as well.

The first ten SMS a day are not billed; beyond that the price depends on the country (Google Cloud Identity
Platform pricing).

## Updating

**Update from source...** (card *Updates*) asks for the root of your Sparrow source checkout, copies the
Directory program files of that version into this folder (never `private/`) and starts the Directory with its
saved settings, which rebuilds its image from the new files. The signing identity, data, hostname, proxy mode
and phone verification setting are kept; the window then reopens with the new version. The source folder is
remembered for the next update.

## Without a desktop

```bash
./Start-SparrowDirectory.sh                       # install / start with the automatic name
./Start-SparrowDirectory.sh install --hostname directory.example.com --standalone
./Start-SparrowDirectory.sh install --server-folder ~/"Sparrow Server"
./Start-SparrowDirectory.sh update --source ~/Sparrow     # update from a source checkout and start
./Start-SparrowDirectory.sh status|logs|planes|stop|detect
```

Without arguments the script installs or starts with the automatic hostname, sharing the proxy of a
running Sparrow Server or, if none runs, with its own proxy. On Windows run the same commands with
`py -3 directory_installer.py ...`.

## Operator-only packaging

```bash
python3 server/control-plane-directory/Build-SparrowDirectoryInstaller.py
```

Creates `sparrow-directory-windows.zip` and `sparrow-directory-linux.zip` (Linux and macOS) in
`server/control-plane-directory/dist/`. They must not be copied into the public server release
bundle. Workflow-run artifacts are **not** Release downloads, but people with workflow-artifact
access can see them; use a private repository or restricted storage if needed.
