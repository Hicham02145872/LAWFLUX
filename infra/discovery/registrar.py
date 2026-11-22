"""Mirror Docker Compose DNS records into the local Consul service catalog."""
import json
import os
import socket
import time
import urllib.request

CONSUL = os.environ.get("CONSUL_URL", "http://discovery-service:8500")
SERVICES = {"identity-service": ("lawflux-identity", 9091),
            "case-service": ("lawflux-cases", 9092),
            "document-service": ("lawflux-documents", 9093),
            "billing-service": ("lawflux-billing", 9094)}


def consul(path, payload=None):
    method = "PUT" if payload is not None or "/deregister/" in path else "GET"
    data = json.dumps(payload).encode() if payload is not None else None
    request = urllib.request.Request(CONSUL + path, data=data, method=method,
                                     headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(request, timeout=3) as response:
        return json.load(response) if path.endswith("/services") else response.read()


def desired():
    result = {}
    for host, (name, port) in SERVICES.items():
        try:
            addresses = {row[4][0] for row in socket.getaddrinfo(host, port,
                                                                   family=socket.AF_INET,
                                                                   type=socket.SOCK_STREAM)}
        except socket.gaierror:
            continue
        for address in addresses:
            service_id = name + "-" + address.replace(".", "-")
            result[service_id] = {"ID": service_id, "Name": name,
                                  "Address": address, "Port": port,
                                  "Check": {"TCP": f"{address}:{port}",
                                            "Interval": "5s", "Timeout": "2s"}}
    return result


while True:
    try:
        wanted = desired()
        current = consul("/v1/agent/services")
        for service_id, definition in wanted.items():
            if service_id not in current:
                consul("/v1/agent/service/register", definition)
                print("registered", service_id, flush=True)
        for service_id in current:
            if service_id.startswith("lawflux-") and service_id not in wanted:
                consul("/v1/agent/service/deregister/" + service_id)
                print("deregistered", service_id, flush=True)
    except Exception as exc:
        print("discovery retry:", exc, flush=True)
    time.sleep(2)
