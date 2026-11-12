#!/usr/bin/env python3
"""usage: faultctl.py CTLPORT command [args...]"""
import socket, sys
s = socket.create_connection(("127.0.0.1", int(sys.argv[1])), timeout=10)
s.sendall((" ".join(sys.argv[2:]) + "\n").encode())
print(s.makefile().read().strip())
