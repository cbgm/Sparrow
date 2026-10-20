# One-shot PUBLIC Control Plane registration client. This is NOT the central
# Directory Server. No admin credentials and no key generation are present.
FROM python:3.12-slim
RUN pip install --no-cache-dir cryptography==46.0.4
WORKDIR /app
COPY control_plane_directory_client.py control_plane_directory_registration.py ./
ENTRYPOINT ["python", "-u", "/app/control_plane_directory_registration.py"]
