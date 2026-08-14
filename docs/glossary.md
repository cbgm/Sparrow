# Glossary

**Control Plane**  
The discovery/control deployment containing node registry, presence directory and push service behind Caddy.

**Control Plane directory**  
External JSON document containing a `controlPlanes` array. It is the configurable source of Control Plane URLs for app builds and Community Nodes.

**Community Node**  
The message-routing deployment containing gateway, federation and mailbox services behind Caddy.

**Caddy**  
Reverse proxy/edge server that exposes friendly HTTP(S)/WSS paths and forwards them to Docker-internal services.

**Docker image**  
Packaged filesystem/runtime for one server service.

**Docker Compose**  
Configuration/tool used to run multiple images, networks, volumes and dependencies as one deployment.

**PostgreSQL**  
Durable relational database used by registry, push, federation queue and mailbox services.

**Redis**  
Fast in-memory datastore used for short-lived presence routes.

**Routing ID**  
Protocol identifier used to locate a client route without using a human contact name/phone number directly as the wire destination.

**Presence route**  
Short-lived signed mapping from a client routing identity to its current Community Node/connection.

**ProtocolOutbox**  
Persistent client boundary where feature-owned packets are queued before final transport preparation/sending.

**Federation**  
Signed Community Node-to-Community Node forwarding path when sender and recipient are not on the same gateway.

**Mailbox**  
Recipient-selected capability-protected offline store containing opaque encrypted envelopes.

**FCM**  
Firebase Cloud Messaging; used on Android as a wake-up mechanism for pending/offline delivery.

**Safety number**  
Human-comparable value derived from both parties' current public identity keys by `SafetyNumberGenerator`.

**Security epoch**  
Group membership/key version used to determine current active members and the group encryption state for Group traffic.

**Cooldown**  
Temporary client diagnostic/selection state for a failed node. Cooldown nodes are not routing candidates and display zero live connections.

**BuildKonfig**  
KMP Gradle plugin used to expose build-time configuration such as `CONTROL_PLANE_DIRECTORY_URL` to common code.

**R8**  
Android release optimizer/minifier. Release builds also shrink resources; mapping files are retained privately in CI for de-obfuscation.

**Release candidate**  
Artifacts created from a `release/**` branch push. They are change-aware and are not automatically the official GitHub Release.

**Full release**  
A `v*` tagged build that rebuilds/publishes the complete APK + server image + launcher package set and combined full ZIP.
