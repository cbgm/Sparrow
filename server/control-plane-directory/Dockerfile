# Operator-only image. NEVER add to the public unified Sparrow server bundle.
FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir -r /app/requirements.txt
COPY directory_service.py directory_admin_cli.py /app/
RUN useradd --uid 10001 --home /nonexistent --no-create-home --shell /usr/sbin/nologin directory
USER 10001:10001
ENTRYPOINT ["python", "-u", "/app/directory_service.py"]
CMD ["serve", "--key-file", "/state/signing.pem", "--database", "/state/directory.sqlite", "--public-host", "0.0.0.0"]
