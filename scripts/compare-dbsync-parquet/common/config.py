"""Configuration loading for the DB Sync Parquet comparison tool."""

import json
import os
from urllib.parse import quote, urlparse, urlunparse


TOOL_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


DEFAULTS = {
    "store_url": "postgresql://localhost:5432/yaci_store",
    "store_user": None,
    "store_password": None,
    "store_schema": "yaci_store",
    "dbsync_parquet_dir": os.path.abspath(os.path.join(TOOL_DIR, "../export-dbsync/output")),
    "parquet_files": {},
    "start_epoch": None,
    "end_epoch": None,
    "models": None,
    "reward_types": ["treasury", "reserves", "proposal_refund"],
    "include_zero_amount": False,
    "max_mismatches": 50,
    "duckdb_memory_limit": None,
    "reports_dir": os.path.join(TOOL_DIR, "reports"),
    "logs_dir": os.path.join(TOOL_DIR, "logs"),
    "quiet": False,
}

PATH_KEYS = {"dbsync_parquet_dir", "reports_dir", "logs_dir"}


def load_config(config_path):
    with open(config_path, "r", encoding="utf-8") as f:
        return json.load(f)


def normalize_list(value, *, separator=","):
    if value is None:
        return None
    if isinstance(value, str):
        return [item.strip() for item in value.split(separator) if item.strip()]
    return list(value)


def apply_credentials(url, user, password):
    """Override the userinfo component of a PostgreSQL URL if provided."""
    if not url or (user is None and password is None):
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

    return urlunparse(parsed._replace(netloc=f"{userinfo}{host}"))


def redact_url(url):
    """Return a connection URL safe for logs."""
    if not url:
        return url
    parsed = urlparse(url)
    if parsed.password is None:
        return url

    user = quote(parsed.username or "", safe="")
    host = parsed.hostname or ""
    if parsed.port:
        host = f"{host}:{parsed.port}"
    return urlunparse(parsed._replace(netloc=f"{user}:****@{host}"))


def resolve_path(path, base_dir):
    if path is None:
        return None
    path = os.path.expanduser(path)
    if os.path.isabs(path):
        return path
    return os.path.abspath(os.path.join(base_dir, path))


def resolve_config(args, valid_models):
    """Resolve configuration with priority: CLI args > JSON config file > defaults."""
    cfg = DEFAULTS.copy()

    if args.config:
        config_dir = os.path.dirname(os.path.abspath(args.config))
        file_cfg = load_config(args.config)
        for key in PATH_KEYS:
            if key in file_cfg:
                file_cfg[key] = resolve_path(file_cfg[key], config_dir)
        if "parquet_files" in file_cfg:
            file_cfg["parquet_files"] = {
                key: resolve_path(value, config_dir)
                for key, value in (file_cfg.get("parquet_files") or {}).items()
            }
        cfg.update(file_cfg)

    cli_mapping = {
        "store_url": "store_url",
        "store_user": "store_user",
        "store_password": "store_password",
        "store_schema": "store_schema",
        "dbsync_parquet_dir": "dbsync_parquet_dir",
        "start_epoch": "start_epoch",
        "end_epoch": "end_epoch",
        "models": "models",
        "reward_types": "reward_types",
        "include_zero_amount": "include_zero_amount",
        "max_mismatches": "max_mismatches",
        "duckdb_memory_limit": "duckdb_memory_limit",
        "reports_dir": "reports_dir",
        "logs_dir": "logs_dir",
        "quiet": "quiet",
    }

    for attr, cfg_key in cli_mapping.items():
        cli_val = getattr(args, attr, None)
        if cli_val is not None:
            if cfg_key in PATH_KEYS:
                cfg[cfg_key] = resolve_path(cli_val, os.getcwd())
            else:
                cfg[cfg_key] = cli_val

    if args.epoch is not None:
        cfg["start_epoch"] = args.epoch
        cfg["end_epoch"] = args.epoch

    cfg["models"] = normalize_list(cfg.get("models")) or list(valid_models)
    invalid_models = [model for model in cfg["models"] if model not in valid_models]
    if invalid_models:
        raise ValueError(
            "invalid model(s): "
            + ", ".join(invalid_models)
            + ". Choices: "
            + ", ".join(valid_models)
        )

    cfg["reward_types"] = normalize_list(cfg.get("reward_types")) or []
    invalid_reward_types = [
        rtype for rtype in cfg["reward_types"]
        if rtype not in ("treasury", "reserves", "proposal_refund")
    ]
    if invalid_reward_types:
        raise ValueError("invalid reward_type(s): " + ", ".join(invalid_reward_types))

    if cfg.get("start_epoch") is None or cfg.get("end_epoch") is None:
        raise ValueError("provide --epoch or both start_epoch and end_epoch")

    cfg["start_epoch"] = int(cfg["start_epoch"])
    cfg["end_epoch"] = int(cfg["end_epoch"])
    if cfg["end_epoch"] < cfg["start_epoch"]:
        raise ValueError("end_epoch must be >= start_epoch")

    cfg["max_mismatches"] = int(cfg["max_mismatches"] or 0)
    cfg["include_zero_amount"] = bool(cfg["include_zero_amount"])
    cfg["quiet"] = bool(cfg["quiet"])

    if not cfg.get("store_url"):
        raise ValueError("store_url is required")

    cfg["store_url"] = apply_credentials(
        cfg["store_url"],
        cfg.get("store_user"),
        cfg.get("store_password"),
    )
    cfg["dbsync_parquet_dir"] = resolve_path(cfg["dbsync_parquet_dir"], os.getcwd())
    cfg["reports_dir"] = resolve_path(cfg["reports_dir"], os.getcwd())
    cfg["logs_dir"] = resolve_path(cfg["logs_dir"], os.getcwd())
    cfg["parquet_files"] = {
        key: resolve_path(value, os.getcwd())
        for key, value in (cfg.get("parquet_files") or {}).items()
    }

    return cfg
