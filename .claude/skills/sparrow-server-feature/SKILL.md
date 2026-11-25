---
name: sparrow-server-feature
description: Rules and steps for changing or adding something on the Sparrow server side (Ktor services under server/, the Python control-plane directory and installer, Docker deployments). Use for any server change.
---

# Sparrow server

The server is a set of small, independent JVM services built with Ktor, plus Python tools and Docker deployments.
Background: `docs/server/`, `docs/architecture/dependency-rules.md` (rule 14) and "Add a server endpoint" in
`docs/development/extending.md`.

## The most important rules

1. **Remember nothing about users.** A service stores only what the function strictly needs, and only as long as it
   needs it. No IP addresses in logs or storage, no message content or identifiers in logs.
2. **Check everything a client sends:** sizes, signatures, proof of work, replay and rate limits.
3. **Services are independent.** They never import each other's internals; they talk over HTTP and the models in
   `server:protocol`. Shared low-level code lives in `server:security`, `server:persistence`, `server:observability`.
4. **The wire format matches the app.** Every model in `server:protocol` has a matching `…Dto` in the app
   (for example `AliasClaim`, `FederatedEnvelope`); change both together. Endpoints are versioned (`/v1/…`) and
   changes stay backward compatible: nodes and apps update at different times.
5. **Open network:** anyone can run a node; nothing may depend on a node being approved by an operator.

## Structure inside a service

Each service (`gateway`, `mailbox`, `node-registry`, `presence-directory`, `link-preview`, `exchange-rates`,
`federation`) keeps flat packages in this order:

```text
Application.kt     startup, wiring
…Runtime           configuration (environment variables) and lifecycle
…Routes            HTTP only: parse, check, call logic, answer
logic classes      plain Kotlin without Ktor, unit tested
…Store             interface for persistence
Postgres…/Redis…   store implementations
```

When a service grows past about 25 files or its logic gets complex, it gets `domain` / `data` packages like the app.

## Steps for a change

1. Decide which service owns it. A new service only for a real independent responsibility.
2. Settle the privacy question first: what does the service learn, store, log, and for how long?
3. Protocol: new or changed models in `server:protocol`, the matching app `…Dto` and mapper, a versioned endpoint.
4. Routes → logic → store, following the structure above. Authentication, replay and rate limits for every new
   endpoint.
5. Configuration only through environment variables with safe defaults.
6. Operations: update the Dockerfile and the compose files that run the service (`server/docker-compose*.yml`,
   `server/community-node`, `server/control-plane`); Caddy only if the path must be public; link operator-relevant
   pages from the deployment's `/index`.
7. The installer (`server/installer`) and the start scripts must keep working on Windows, Linux and macOS, and the
   GitHub release flows must not break. The installer stays a simple graphical tool: no terminal, no browser.
8. Python parts (`server/control-plane-directory`, `server/installer`) keep their existing style and tests.

## Tests

- Route tests with Ktor `testApplication`.
- Store tests against the interface, plus integration tests for Postgres and Redis.
- When the protocol changes: a test that the app's `…Dto` and the server model encode to the same JSON.
- The Docker smoke test of the affected deployment.

## Hand over

Same as in `sparrow-architecture`: never commit or push, suggest a commit name, explain every detail up front,
including what a node operator has to do after updating.
