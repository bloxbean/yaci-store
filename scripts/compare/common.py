"""
Shared utilities for compare scripts: config loading, Logger, DB connection.
"""

import json
import os
import sys
from urllib.parse import urlparse, urlunparse, quote

try:
    import psycopg2
except ImportError:
    print("ERROR: psycopg2 library is not installed.")
    print("Run: pip3 install psycopg2-binary")
    sys.exit(1)


# ============================================================
# Default connection config
# ============================================================
DEFAULT_DBSYNC_URL = "postgresql://dbsync:dbsync@10.4.10.135:5678/cexplorer"
DEFAULT_STORE_URL = "postgresql://yaci:dbpass@10.4.10.112:5432/yaci_store"
DEFAULT_STORE_SCHEMA = "yaci_store"


# ============================================================
# Config loading
# ============================================================
def load_config(config_path):
    """Load config from a JSON file. Returns a dict."""
    with open(config_path, "r") as f:
        return json.load(f)


def add_common_args(parser):
    """Add common arguments (config file, DB connections, output) to an ArgumentParser."""
    parser.add_argument("--config", metavar="FILE", help="Path to JSON config file (CLI args override file values)")
    parser.add_argument("--dbsync-url", help="DB Sync connection URL")
    parser.add_argument("--dbsync-user", help="DB Sync username (override URL userinfo)")
    parser.add_argument("--dbsync-password", help="DB Sync password (override URL userinfo)")
    parser.add_argument("--store-url", help="Yaci Store connection URL")
    parser.add_argument("--store-user", help="Yaci Store username (override URL userinfo)")
    parser.add_argument("--store-password", help="Yaci Store password (override URL userinfo)")
    parser.add_argument("--store-schema", help="Yaci Store schema name")
    parser.add_argument("--quiet", action="store_true", default=None, help="Write to log file only, do not print to console")


def resolve_config(args):
    """
    Merge config from: defaults -> config file -> CLI args.
    CLI args always win. Config file overrides defaults.
    Modifies args in-place and returns it.
    """
    # Start with defaults
    defaults = {
        "dbsync_url": DEFAULT_DBSYNC_URL,
        "store_url": DEFAULT_STORE_URL,
        "store_schema": DEFAULT_STORE_SCHEMA,
        "quiet": False,
        "max_mismatches": 0,
        "delay": 0,
    }

    # Layer config file on top of defaults
    if args.config:
        file_cfg = load_config(args.config)
        defaults.update(file_cfg)

    # Map CLI arg names (with hyphens) to config keys (with underscores)
    cli_mapping = {
        "dbsync_url": "dbsync_url",
        "dbsync_user": "dbsync_user",
        "dbsync_password": "dbsync_password",
        "store_url": "store_url",
        "store_user": "store_user",
        "store_password": "store_password",
        "store_schema": "store_schema",
        "quiet": "quiet",
        "max_mismatches": "max_mismatches",
        "delay": "delay",
    }

    for attr, cfg_key in cli_mapping.items():
        cli_val = getattr(args, attr, None)
        if cli_val is None:
            # CLI arg not provided -> use config/default
            setattr(args, attr, defaults.get(cfg_key, None))

    # Apply user/pass overrides to URLs if provided
    args.dbsync_url = apply_credentials(args.dbsync_url, args.dbsync_user, args.dbsync_password)
    args.store_url = apply_credentials(args.store_url, args.store_user, args.store_password)

    return args


def apply_credentials(url, user, password):
    """Override the userinfo component of a postgresql URL with user/password if provided."""
    if not url or (not user and not password):
        return url
    parsed = urlparse(url)
    existing_user = parsed.username or ""
    existing_pass = parsed.password or ""
    new_user = user if user is not None else existing_user
    new_pass = password if password is not None else existing_pass

    host = parsed.hostname or ""
    if parsed.port:
        host = f"{host}:{parsed.port}"

    userinfo = ""
    if new_user or new_pass:
        userinfo = quote(new_user, safe="")
        if new_pass:
            userinfo += ":" + quote(new_pass, safe="")
        userinfo += "@"

    netloc = f"{userinfo}{host}"
    return urlunparse(parsed._replace(netloc=netloc))


# ============================================================
# Logger
# ============================================================
class Logger:
    def __init__(self, log_file, quiet=False):
        self.log_file = log_file
        self.quiet = quiet
        os.makedirs(os.path.dirname(log_file), exist_ok=True)
        with open(log_file, "w") as f:
            f.write("")

    def log(self, message=""):
        if not self.quiet:
            print(message)
        with open(self.log_file, "a") as f:
            f.write(message + "\n")

    def error(self, message, exc=None):
        err_msg = f"ERROR: {message}"
        if exc:
            err_msg += f"\n  {type(exc).__name__}: {exc}"
        print(err_msg, file=sys.stderr)
        with open(self.log_file, "a") as f:
            f.write(err_msg + "\n")


# ============================================================
# DB helpers
# ============================================================
def normalize_hash(h):
    """Normalize drep/pool hash: lowercase hex, strip 0x prefix, accept bytes."""
    if h is None:
        return None
    if isinstance(h, (bytes, bytearray, memoryview)):
        h = bytes(h).hex()
    h = str(h)
    if h.startswith("0x") or h.startswith("0X"):
        h = h[2:]
    return h.lower()


def connect(url, schema=None):
    conn = psycopg2.connect(url)
    if schema:
        with conn.cursor() as cur:
            cur.execute(f"SET search_path TO {schema}")
    return conn
