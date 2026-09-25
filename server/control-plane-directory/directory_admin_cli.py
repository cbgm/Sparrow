"""Operator-local review via `docker exec`; admin credentials never go on CLI."""
import json
import os
import sys
import urllib.request


def main():
    if len(sys.argv) not in (2, 3) or sys.argv[1] not in ("pending", "approve"):
        raise SystemExit("Usage: directory_admin_cli.py pending | approve <registration-id>")
    action = sys.argv[1]
    if action == "approve" and (len(sys.argv) != 3 or not sys.argv[2].isalnum() and not
                               all(c.isalnum() or c in "-_" for c in sys.argv[2])):
        raise SystemExit("Invalid registration identifier")
    token = os.environ["SPARROW_DIRECTORY_ADMIN_TOKEN"]
    url = "http://127.0.0.1:9081/admin/v1/registrations"
    if action == "approve":
        url += "/" + sys.argv[2] + "/approve"
    request = urllib.request.Request(url, data=b"" if action == "approve" else None,
                                     headers={"Authorization": "Bearer " + token},
                                     method="POST" if action == "approve" else "GET")
    with urllib.request.urlopen(request, timeout=8) as response:
        print(response.read().decode("utf-8"))


if __name__ == "__main__":
    main()
