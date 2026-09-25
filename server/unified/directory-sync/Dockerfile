# Public READ-ONLY client image. Build once from this runtime bundle; never
# include central Directory Server sources, private identities or admin keys.
FROM python:3.12-slim
# Only dedicated named cache volumes are writable at runtime. This process
# never receives the Sparrow server identity, admin keys, or Docker socket.
RUN pip install --no-cache-dir cryptography==46.0.4
WORKDIR /app
COPY control_plane_directory_client.py control_plane_directory_sync.py ./
ENTRYPOINT ["python", "-u", "/app/control_plane_directory_sync.py"]
