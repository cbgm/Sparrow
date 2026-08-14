# syntax=docker/dockerfile:1.7

FROM gradle:9.6.1-jdk21 AS build

WORKDIR /workspace

COPY --chown=gradle:gradle . .

RUN --mount=type=cache,target=/home/gradle/.gradle,sharing=locked \
    gradle \
    :server:node-registry:installDist \
    :server:presence-directory:installDist \
    :server:mailbox:installDist \
    :server:federation:installDist \
    :server:gateway:installDist \
    :server:push:installDist \
    --no-daemon \
    --no-configuration-cache \
    --no-parallel \
    --max-workers=2

FROM eclipse-temurin:21-jre

ARG SERVICE

ENV SERVICE=${SERVICE}

WORKDIR /app

COPY --from=build "/workspace/server/${SERVICE}/build/install/${SERVICE}/" /app/

RUN mkdir -p /data && \
    chown -R 65532:65532 /app /data

USER 65532:65532

ENTRYPOINT ["/bin/sh", "-c", "exec /app/bin/$SERVICE"]
