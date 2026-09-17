#!/usr/bin/env python3
"""MCP stdio adapter for a running Studio project. Standard library only; stdout is protocol-only."""
import argparse
import json
import sys
import urllib.request
from urllib.parse import urlparse


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--connection", required=True)
    args = parser.parse_args()
    # Do not send local model requests or the bearer token through a configured HTTP proxy.
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    for line in sys.stdin:
        request = None
        try:
            request = json.loads(line)
            with open(args.connection, encoding="utf-8") as file:
                connection = json.load(file)
            url = connection["url"]
            parsed = urlparse(url)
            if parsed.scheme != "http" or parsed.hostname != "127.0.0.1" or parsed.path != "/rpc":
                raise ValueError("Studio connection must be a loopback /rpc endpoint")
            message = urllib.request.Request(url, data=line.encode("utf-8"), headers={
                "Content-Type": "application/json", "Authorization": "Bearer " + connection["token"]})
            with opener.open(message, timeout=120) as response:
                body = response.read()
            if body:
                print(json.dumps(json.loads(body), ensure_ascii=False), flush=True)
        except Exception as error:
            if isinstance(request, dict) and "id" in request:
                print(json.dumps({"jsonrpc": "2.0", "id": request["id"], "error": {
                    "code": -32000, "message": "Studio bridge unavailable: " + str(error)}}), flush=True)
            else:
                print("Studio bridge: " + str(error), file=sys.stderr, flush=True)


if __name__ == "__main__":
    main()
