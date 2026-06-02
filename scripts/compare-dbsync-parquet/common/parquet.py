"""Parquet file resolution and schema checks."""

import glob
import os
import re


DATASET_COLUMNS = {
    "adapot": {"epoch_no", "slot_no", "treasury", "reserves"},
    "epoch_stake": {"epoch_no", "stake_address", "amount", "pool_id"},
    "reward_rest": {"stake_address", "type", "amount", "earned_epoch", "spendable_epoch"},
    "drep_distr": {"epoch_no", "drep_hash", "drep_id", "has_script", "amount", "active_until"},
    "gov_action_proposal": {
        "tx_hash",
        "index",
        "type",
        "ratified_epoch",
        "enacted_epoch",
        "dropped_epoch",
        "expired_epoch",
        "expiration",
        "submit_epoch",
    },
}


MODEL_DATASETS = {
    "adapot": {"adapot"},
    "epoch_stake": {"epoch_stake"},
    "reward_rest": {"reward_rest"},
    "drep_amount": {"drep_distr"},
    "drep_active_until": {"drep_distr"},
    "gov_action_proposal_status": {"gov_action_proposal"},
}


FILENAME_RE = re.compile(r"^(?P<dataset>.+)_from(?P<start>\d+)(?:_to(?P<end>\d+))?\.parquet$")


def required_datasets(models):
    datasets = set()
    for model in models:
        datasets.update(MODEL_DATASETS[model])
    return sorted(datasets)


def parse_export_filename(dataset, path):
    name = os.path.basename(path)
    match = FILENAME_RE.match(name)
    if not match or match.group("dataset") != dataset:
        return None
    start_epoch = int(match.group("start"))
    end_epoch = int(match.group("end")) if match.group("end") else None
    return start_epoch, end_epoch


def covers_range(parsed_range, start_epoch, end_epoch):
    file_start, file_end = parsed_range
    if file_start > start_epoch:
        return False
    if file_end is not None and file_end < end_epoch:
        return False
    return True


def choose_best_candidate(dataset, candidates, start_epoch, end_epoch):
    parsed_candidates = []
    for path in candidates:
        parsed = parse_export_filename(dataset, path)
        if parsed and covers_range(parsed, start_epoch, end_epoch):
            parsed_candidates.append((path, parsed))

    if not parsed_candidates:
        return None

    # Prefer the narrowest file that still covers the requested range.
    def sort_key(item):
        path, (file_start, file_end) = item
        effective_end = file_end if file_end is not None else 2_147_483_647
        return (start_epoch - file_start, effective_end - end_epoch, path)

    return sorted(parsed_candidates, key=sort_key)[0][0]


def resolve_one(dataset, cfg):
    explicit = (cfg.get("parquet_files") or {}).get(dataset)
    if explicit:
        if not os.path.exists(explicit):
            raise FileNotFoundError(f"configured parquet file for {dataset} does not exist: {explicit}")
        return explicit

    parquet_dir = cfg["dbsync_parquet_dir"]
    start_epoch = cfg["start_epoch"]
    end_epoch = cfg["end_epoch"]

    exact = os.path.join(parquet_dir, f"{dataset}_from{start_epoch}_to{end_epoch}.parquet")
    if os.path.exists(exact):
        return exact

    unbounded = os.path.join(parquet_dir, f"{dataset}_from{start_epoch}.parquet")
    if os.path.exists(unbounded):
        return unbounded

    candidates = glob.glob(os.path.join(parquet_dir, f"{dataset}_from*.parquet"))
    candidates = [
        path for path in candidates
        if parse_export_filename(dataset, path)
        and covers_range(parse_export_filename(dataset, path), start_epoch, end_epoch)
    ]
    if len(candidates) == 1:
        return candidates[0]
    if len(candidates) > 1:
        return choose_best_candidate(dataset, candidates, start_epoch, end_epoch)

    raise FileNotFoundError(
        f"no parquet file found for {dataset} covering epochs {start_epoch} -> {end_epoch} "
        f"under {parquet_dir}"
    )


def resolve_parquet_files(cfg, datasets):
    return {dataset: resolve_one(dataset, cfg) for dataset in datasets}


def validate_schemas(duck, parquet_files):
    for dataset, path in parquet_files.items():
        expected = DATASET_COLUMNS[dataset]
        actual = set(duck.columns(path))
        missing = sorted(expected - actual)
        if missing:
            raise ValueError(
                f"{dataset} parquet is missing required column(s): {', '.join(missing)} "
                f"({path})"
            )
