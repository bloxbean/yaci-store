"""Small file-backed logger used by compare-dbsync-parquet."""

import os
import sys


class Logger:
    def __init__(self, log_file, quiet=False):
        self.log_file = log_file
        self.quiet = quiet
        os.makedirs(os.path.dirname(log_file), exist_ok=True)
        with open(log_file, "w", encoding="utf-8") as f:
            f.write("")

    def log(self, message=""):
        if not self.quiet:
            print(message)
        with open(self.log_file, "a", encoding="utf-8") as f:
            f.write(message + "\n")

    def error(self, message, exc=None):
        err_msg = f"ERROR: {message}"
        if exc is not None:
            err_msg += f"\n  {type(exc).__name__}: {exc}"
        print(err_msg, file=sys.stderr)
        with open(self.log_file, "a", encoding="utf-8") as f:
            f.write(err_msg + "\n")
