import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


TOOLS_DIR = Path(__file__).resolve().parent
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

import drep_dist_batch_compare as tool


class ParseEpochSpecTest(unittest.TestCase):
    def test_parse_epoch_spec_supports_ranges_and_deduplicates(self):
        self.assertEqual(
            tool.parse_epoch_spec("624-626,628,626,630-631"),
            [624, 625, 626, 628, 630, 631],
        )

    def test_parse_epoch_spec_rejects_reverse_ranges(self):
        with self.assertRaises(ValueError):
            tool.parse_epoch_spec("630-624")


class CommandParsingTest(unittest.TestCase):
    def test_inject_default_command_uses_compare_range(self):
        self.assertEqual(
            tool.inject_default_command(["--epochs", "624"]),
            ["compare-range", "--epochs", "624"],
        )

    def test_inject_default_command_preserves_explicit_subcommand(self):
        self.assertEqual(
            tool.inject_default_command(["prepare-range", "--epochs", "624"]),
            ["prepare-range", "--epochs", "624"],
        )

    def test_parse_args_loads_defaults_from_env_file(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            env_file = Path(temp_dir) / "drep.env"
            env_file.write_text(
                "\n".join(
                    [
                        "STORE_HOST=10.0.0.10",
                        "STORE_PORT=15432",
                        "STORE_DB=yaci_store",
                        "STORE_USER=yaci",
                        "STORE_PASSWORD=dbpass",
                        "DBSYNC_HOST=10.0.0.20",
                        "DBSYNC_PORT=25678",
                        "DBSYNC_DB=cexplorer",
                        "DBSYNC_USER=dbsync",
                        "DBSYNC_PASSWORD=dbsyncpass",
                        "WORKERS=3",
                        'REPORT_DIR="/tmp/drep compare reports"',
                        "KEEP_JIT=true",
                    ]
                ),
                encoding="utf-8",
            )

            with patch.dict("os.environ", {}, clear=True):
                args = tool.parse_args(
                    [
                        "compare-range",
                        "--env-file",
                        str(env_file),
                        "--epochs",
                        "624",
                    ]
                )

        self.assertEqual(args.store_host, "10.0.0.10")
        self.assertEqual(args.store_port, 15432)
        self.assertEqual(args.dbsync_host, "10.0.0.20")
        self.assertEqual(args.dbsync_port, 25678)
        self.assertEqual(args.workers, 3)
        self.assertEqual(args.report_dir, "/tmp/drep compare reports")
        self.assertTrue(args.keep_jit)
        self.assertEqual(args.env_file, str(env_file.resolve()))

    def test_parse_args_prefers_cli_over_env_file(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            env_file = Path(temp_dir) / "drep.env"
            env_file.write_text(
                "\n".join(
                    [
                        "STORE_HOST=10.0.0.10",
                        "STORE_PORT=15432",
                        "STORE_DB=yaci_store",
                        "STORE_USER=yaci",
                        "STORE_PASSWORD=dbpass",
                    ]
                ),
                encoding="utf-8",
            )

            with patch.dict("os.environ", {}, clear=True):
                args = tool.parse_args(
                    [
                        "prepare-range",
                        "--env-file",
                        str(env_file),
                        "--epochs",
                        "624",
                        "--store-host",
                        "127.0.0.1",
                        "--store-port",
                        "5432",
                    ]
                )

        self.assertEqual(args.store_host, "127.0.0.1")
        self.assertEqual(args.store_port, 5432)


class EnvFileParsingTest(unittest.TestCase):
    def test_load_env_file_supports_export_quotes_and_inline_comments(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            env_file = Path(temp_dir) / "drep.env"
            env_file.write_text(
                "\n".join(
                    [
                        "# comment",
                        "export STORE_HOST=10.0.0.10",
                        'STORE_PASSWORD="db pass"',
                        "WORKERS=2 # keep low on local machine",
                    ]
                ),
                encoding="utf-8",
            )

            values = tool.load_env_file(str(env_file))

        self.assertEqual(
            values,
            {
                "STORE_HOST": "10.0.0.10",
                "STORE_PASSWORD": "db pass",
                "WORKERS": "2",
            },
        )


class CompareRowsTest(unittest.TestCase):
    def test_compare_rows_matches_when_amounts_align(self):
        dbsync_rows = [
            {"drep_hash": "aa", "drep_view": "drep1", "amount": "10"},
            {"drep_hash": "bb", "drep_view": "abstain", "amount": "5"},
            {"drep_hash": "cc", "drep_view": "no_confidence", "amount": "7"},
        ]
        store_rows = [
            {"drep_hash": "aa", "drep_id": "drep1", "amount": "10", "drep_type": "ADDR_KEYHASH"},
            {"drep_hash": "00000000000000000000000000000000000000000000000000000000", "drep_id": "", "amount": "5", "drep_type": "ABSTAIN"},
            {"drep_hash": "00000000000000000000000000000000000000000000000000000000", "drep_id": "", "amount": "7", "drep_type": "NO_CONFIDENCE"},
        ]

        mismatch_count, mismatches = tool.compare_rows(624, dbsync_rows, store_rows)

        self.assertEqual(mismatch_count, 0)
        self.assertEqual(mismatches, [])

    def test_compare_rows_detects_missing_hash_and_virtual_mismatch(self):
        dbsync_rows = [
            {"drep_hash": "aa", "drep_view": "drep1", "amount": "10"},
            {"drep_hash": "bb", "drep_view": "abstain", "amount": "6"},
        ]
        store_rows = [
            {"drep_hash": "aa", "drep_id": "drep1", "amount": "11", "drep_type": "ADDR_KEYHASH"},
            {"drep_hash": "dd", "drep_id": "drep2", "amount": "9", "drep_type": "ADDR_KEYHASH"},
            {"drep_hash": "00000000000000000000000000000000000000000000000000000000", "drep_id": "", "amount": "5", "drep_type": "ABSTAIN"},
        ]

        mismatch_count, mismatches = tool.compare_rows(624, dbsync_rows, store_rows)
        mismatch_types = {item["type"] for item in mismatches}

        self.assertEqual(mismatch_count, 3)
        self.assertEqual(
            mismatch_types,
            {"amount_mismatch", "missing_in_dbsync", "abstain_mismatch"},
        )


class HardcodedExclusionsTest(unittest.TestCase):
    def test_load_hardcoded_exclusions_reads_mainnet_entries_from_java_source(self):
        exclusions = tool.load_hardcoded_exclusions(tool.PUBLIC_PROTOCOL_MAGICS["mainnet"])

        self.assertGreater(len(exclusions), 0)
        self.assertEqual(exclusions[0].drep_type, "ADDR_KEYHASH")

    def test_build_hardcoded_exclusion_condition_is_empty_when_disabled(self):
        exclusions = [
            tool.DRepDelegationExclusion(
                address="stake1test",
                drep_hash="abc",
                drep_type="ADDR_KEYHASH",
                slot=1,
                tx_index=2,
                cert_index=3,
            )
        ]

        self.assertEqual(tool.build_hardcoded_exclusion_condition(exclusions, False), "")


class StepTimingParserTest(unittest.TestCase):
    def test_parse_step_timings_extracts_named_durations(self):
        output = "\n".join(
            [
                "STEP_TIMING|ss_last_withdrawal_create|12.345",
                "noise",
                "STEP_TIMING|tmp_drep_dist_insert_regular|987.65",
            ]
        )

        self.assertEqual(
            tool.parse_step_timings(output),
            [
                {"name": "ss_last_withdrawal_create", "duration_ms": 12.345},
                {"name": "tmp_drep_dist_insert_regular", "duration_ms": 987.65},
            ],
        )


class MetaParserTest(unittest.TestCase):
    def test_parse_meta_lines_extracts_values(self):
        output = "\n".join(
            [
                "META|missing_address_cache_hit|true",
                "noise",
                "META|missing_address_count|57444",
            ]
        )

        self.assertEqual(
            tool.parse_meta_lines(output),
            {
                "missing_address_cache_hit": "true",
                "missing_address_count": "57444",
            },
        )


class SqlGenerationTest(unittest.TestCase):
    def build_context(self):
        return tool.EpochExecutionContext(
            epoch=623,
            is_bootstrap_phase=False,
            pv9_max_epoch=536,
            max_bootstrap_phase_epoch=536,
            exclusions=[],
        )

    def build_settings(self):
        return tool.QuerySettings(
            lock_timeout="5s",
            statement_timeout="0",
            work_mem="1GB",
            maintenance_work_mem="2GB",
            temp_buffers="512MB",
            parallel_workers_per_gather=4,
            disable_jit=True,
            effective_cache_size="48GB",
            random_page_cost="1.1",
            effective_io_concurrency=64,
            parallel_setup_cost="0",
            parallel_tuple_cost="0.01",
        )

    def test_render_prepare_sql_only_writes_to_debug_schema(self):
        sql = tool.render_prepare_sql(
            store_schema="yaci_store",
            debug_schema="drep_debug",
            snapshot_epoch=624,
            epoch_context=self.build_context(),
            settings=self.build_settings(),
        )

        self.assertIn('INSERT INTO "drep_debug".missing_address_epoch', sql)
        self.assertIn('INSERT INTO "drep_debug".pv9_cleared_address_epoch', sql)
        self.assertNotIn("INSERT INTO drep_dist", sql)
        self.assertNotIn("UPDATE drep_dist", sql)

    def test_render_active_proposal_deposits_sql_uses_replay_safe_exists_logic(self):
        sql = tool.render_active_proposal_deposits_sql(623)

        self.assertIn("WHERE g.epoch = 623", sql)
        self.assertIn("OR EXISTS (", sql)
        self.assertIn("s.status = 'ACTIVE'", sql)
        self.assertIn("s.epoch = 623", sql)
        self.assertNotIn("LEFT JOIN gov_action_proposal_status", sql)
        self.assertNotIn("s.status IS NULL", sql)

    def test_render_compare_sql_never_targets_official_drep_dist_table(self):
        sql = tool.render_compare_sql(
            store_schema="yaci_store",
            debug_schema="drep_debug",
            snapshot_epoch=624,
            epoch_context=self.build_context(),
            settings=self.build_settings(),
            store_output_path="/tmp/store.csv",
            run_id="run-1",
        )

        self.assertIn('FROM "drep_debug".missing_address_epoch', sql)
        self.assertIn("CREATE TEMP TABLE tmp_drep_dist", sql)
        self.assertIn('DELETE FROM "drep_debug".shadow_drep_dist', sql)
        self.assertIn('INSERT INTO "drep_debug".shadow_drep_dist', sql)
        self.assertNotIn("INSERT INTO drep_dist", sql)
        self.assertNotIn("DELETE FROM drep_dist", sql)


if __name__ == "__main__":
    unittest.main()
