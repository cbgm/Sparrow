# SecureChat control plane

The control-plane bundle is configured through its launcher on every start.

## Launcher configuration

Double-click `Start-SecureChatControlPlane.cmd`.

The launcher always shows one configuration window. Existing values from `securechat.conf` are preselected, so you can start unchanged or edit them before launch:

- a `LAN` / `Public` reachability dropdown,
- for `Public`, an `Automatic (sslip.io)` / `Own address` dropdown,
- the own public domain/host field when `Own address` is selected.

Automatic public addressing detects the public IPv4 address and derives `<public-ip>.sslip.io`.

It also creates a stable random `CONTROL_PLANE_ID`. That ID is included inside the signed node directory, allowing clients to recognize that several URLs are aliases for the same logical control plane.

The answers are persisted in `securechat.conf`; later starts reuse them without prompting.

A fresh bundle also contains a visible `secrets` directory for `firebase-admin.json` and registry authority material.

A fresh bundle contains:

```properties
CONFIGURED=false
MODE=
PUBLIC_DOMAIN=
CONTROL_PLANE_ID=
```

After saving from the launcher it becomes similar to:

```properties
CONFIGURED=true
MODE=lan
PUBLIC_DOMAIN=
CONTROL_PLANE_ID=79ea0e34327b4cf8b57d6d1e434e6ceb
```

Existing bundles without `CONFIGURED` are migrated automatically and receive a stable instance ID without forcing the operator through setup again.

## LAN mode

LAN mode exposes the control-plane edge on port `8390`.

## Public mode

Public mode uses HTTPS on the configured domain. If `PUBLIC_DOMAIN` is blank, the launcher detects the public IPv4 address and derives an `sslip.io` hostname.

A control plane intentionally does not contain a list of other control planes. Community nodes and apps obtain the multi-control-plane set from discovery sources instead.

