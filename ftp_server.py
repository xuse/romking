"""
Simple FTP server with anonymous login.
Uses pyftpdlib library.
"""
from pyftpdlib.authorizers import DummyAuthorizer
from pyftpdlib.handlers import FTPHandler
from pyftpdlib.servers import FTPServer
import os

def main():
    # FTP root directory - current directory
    FTP_ROOT = os.path.dirname(os.path.abspath(__file__))
    FTP_PORT = 2121

    authorizer = DummyAuthorizer()
    # Anonymous user with read-only access
    authorizer.add_anonymous(FTP_ROOT, perm="elradfmw")

    handler = FTPHandler
    handler.authorizer = authorizer
    handler.banner = "Romking FTP Server ready."
    handler.passive_ports = range(60000, 60100)

    server = FTPServer(("0.0.0.0", FTP_PORT), handler)
    server.max_cons = 10
    server.max_cons_per_ip = 5

    print(f"FTP Server starting on ftp://127.0.0.1:{FTP_PORT}")
    print(f"Root directory: {FTP_ROOT}")
    print("Anonymous login enabled (full access)")
    print("Press Ctrl+C to stop.")
    server.serve_forever()

if __name__ == "__main__":
    main()
