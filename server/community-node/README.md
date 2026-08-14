# SecureChat community node

The community-node bundle opens its configuration on every start. Operators should not maintain a comma-separated control-plane list.

## Launcher configuration

Launch `Start-SecureChatNode.cmd` on Windows, `start-securechat-node.sh` on Linux, or `Start-SecureChatNode.command` on macOS.

On Windows, the launcher always shows one configuration window. Existing values from `securechat.conf` are prefilled, so you can start unchanged or edit them before launch:

- a `LAN` / `Public` reachability dropdown,
- for `Public`, an `Automatic (sslip.io)` / `Own address` dropdown,
- the own public domain/host field when `Own address` is selected,
- one required control-plane directory URL field.

The directory URL is the node's single configured source of control-plane addresses. There is no bootstrap URL. The launcher keeps the last successful addresses in its runtime configuration so an already configured node can start while the directory or every Control Plane is temporarily unavailable. A fresh node without cached addresses keeps retrying the directory instead of exiting.

The answers are persisted in `securechat.conf`. Later starts reuse that file and do not ask again. The URL can be changed manually there later.

A fresh bundle starts with:

```properties
CONFIGURED=false
MODE=
PUBLIC_DOMAIN=
CONTROL_PLANE_DIRECTORY_URL=
```

After setup it becomes similar to:

```properties
CONFIGURED=true
MODE=lan
PUBLIC_DOMAIN=
CONTROL_PLANE_DIRECTORY_URL=https://directory.example.com/control-planes.json
```

The directory document uses the same simple format as the app:

```json
{
  "controlPlanes": [
    "https://cp1.example.com",
    "https://cp2.example.com"
  ]
}
```

The directory body is parsed as JSON regardless of whether the server labels it `application/json` or `text/plain`. The node keeps retrying unavailable directory and Control Plane endpoints while it runs, so temporary Control Plane outages do not terminate the node.

## LAN mode

The launcher automatically advertises the machine's LAN address on port `8490`:

```text
ws://<lan-ip>:8490/v1/gateway
http://<lan-ip>:8490
```

## Public mode

Public mode advertises:

```text
wss://<PUBLIC_DOMAIN>/v1/gateway
https://<PUBLIC_DOMAIN>
```

If `PUBLIC_DOMAIN` is blank, the launcher detects the public IPv4 address and derives an `sslip.io` hostname.
## Operator index

Open `/index` on the Community Node to get relative links to gateway health and connection count, gateway information, advertised Control Planes, federation health/capabilities, and mailbox health. The page contains no deployment-specific host names.

