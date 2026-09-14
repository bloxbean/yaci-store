#!/usr/bin/env python3

import json
import os
import sys
import unittest
from dataclasses import replace
from decimal import Decimal


TEST_DIR = os.path.dirname(os.path.abspath(__file__))
COMPARE_DIR = os.path.dirname(TEST_DIR)
FIXTURE_DIR = os.path.join(TEST_DIR, "fixtures", "adastat")
sys.path.insert(0, COMPARE_DIR)

from adastat_voting_stats import (  # noqa: E402
    ALL_FIELDS,
    CC_FIELDS,
    DREP_FIELDS,
    HARD_FORK_INITIATION,
    INFO_ACTION,
    NEW_COMMITTEE,
    NO_CONFIDENCE,
    PARAMETER_CHANGE,
    SPO_FIELDS,
    AdaStatCCInputs,
    AdaStatDRepInputs,
    AdaStatSPOInputs,
    IdentityMismatchError,
    ReferenceContractError,
    ResponseSchemaError,
    UnsupportedActionIndex,
    YaciProposal,
    approval_ratio,
    build_expected_stats,
    check_eligibility,
    compute_drep_stats,
    compute_spo_stats,
    encode_adastat_action_id,
    normalize_action_type,
    normalize_actual_value,
    parse_adastat_response,
    parse_legacy_proposal_id,
    parse_yaci_voting_stats,
)


def load_fixture(name):
    with open(os.path.join(FIXTURE_DIR, name), "r", encoding="utf-8") as fixture:
        return json.load(fixture)


class ActionIdentityTest(unittest.TestCase):
    def test_encodes_decimal_action_index_as_two_hex_characters(self):
        tx_hash = "ab" * 32
        self.assertEqual(tx_hash + "00", encode_adastat_action_id(tx_hash.upper(), 0))
        self.assertEqual(tx_hash + "0a", encode_adastat_action_id(tx_hash, 10))
        self.assertEqual((tx_hash, 10), parse_legacy_proposal_id(tx_hash.upper() + "#10"))

    def test_rejects_unsupported_action_index(self):
        with self.assertRaises(UnsupportedActionIndex):
            encode_adastat_action_id("ab" * 32, 256)
        with self.assertRaises(ReferenceContractError):
            parse_legacy_proposal_id(("ab" * 32) + "#-1")

    def test_normalizes_all_action_type_aliases(self):
        cases = {
            "NoConfidence": "NO_CONFIDENCE",
            "NEW_COMMITTEE": "NEW_COMMITTEE",
            "newconstitution": "NEW_CONSTITUTION",
            "HARD_FORK_INITIATION_ACTION": "HARD_FORK_INITIATION",
            "treasurywithdrawals": "TREASURY_WITHDRAWALS",
            "ParameterChange": "PARAMETER_CHANGE",
            "info_action": "INFO_ACTION",
        }
        for raw, expected in cases.items():
            with self.subTest(raw=raw):
                self.assertEqual(expected, normalize_action_type(raw))


class AdaStatParsingTest(unittest.TestCase):
    def test_parses_preview_fixture_strictly(self):
        reference = parse_adastat_response(load_fixture("preview_new_committee_1369.json"))
        self.assertEqual(NEW_COMMITTEE, reference.action_type)
        self.assertEqual(1369, reference.ratified_epoch)
        self.assertEqual(1371, reference.tip_epoch)
        self.assertEqual(118420753549838, reference.drep.yes)
        self.assertIsNone(reference.cc.total)

    def test_rejects_missing_partial_and_float_stake_fields(self):
        payload = load_fixture("preview_new_committee_1369.json")
        del payload["data"]["drep_yes_stake"]
        with self.assertRaises(ResponseSchemaError):
            parse_adastat_response(payload)

        payload = load_fixture("preview_new_committee_1369.json")
        payload["data"]["drep_yes_stake"] = 1.25
        with self.assertRaises(ResponseSchemaError):
            parse_adastat_response(payload)

        payload = load_fixture("preview_new_committee_1369.json")
        payload["data"]["pool_yes_stake"] = None
        reference = parse_adastat_response(payload)
        with self.assertRaises(ResponseSchemaError):
            build_expected_stats(reference)

    def test_parses_yaci_json_with_decimal_precision(self):
        parsed = parse_yaci_voting_stats('{"drep_approval_ratio": 0.5060}')
        self.assertEqual(Decimal("0.5060"), parsed["drep_approval_ratio"])
        with self.assertRaises(ReferenceContractError):
            parse_yaci_voting_stats("[]")


class EligibilityTest(unittest.TestCase):
    def setUp(self):
        self.reference = parse_adastat_response(load_fixture("preview_new_committee_1369.json"))
        self.base = YaciProposal(
            epoch=1369,
            tx_hash=self.reference.tx_hash,
            index=0,
            action_type="NEW_COMMITTEE",
            status="RATIFIED",
            voting_stats={},
        )

    def test_accepts_matching_terminal_epoch(self):
        self.assertTrue(check_eligibility(self.base, self.reference).eligible)

    def test_rejects_live_and_mismatched_epoch(self):
        active = replace(self.base, status="ACTIVE")
        self.assertEqual("INCONCLUSIVE_LIVE", check_eligibility(active, self.reference).reason)
        wrong_epoch = replace(self.base, epoch=1370)
        self.assertEqual(
            "INCONCLUSIVE_EPOCH_MISMATCH",
            check_eligibility(wrong_epoch, self.reference).reason,
        )

    def test_rejects_identity_mismatch(self):
        with self.assertRaises(IdentityMismatchError):
            check_eligibility(replace(self.base, tx_hash="cd" * 32), self.reference)


class VotingStatsDerivationTest(unittest.TestCase):
    def test_preview_fixture_covers_drep_and_spo_only(self):
        reference = parse_adastat_response(load_fixture("preview_new_committee_1369.json"))
        normalized = build_expected_stats(reference)

        self.assertEqual(18, normalized.compared_fields)
        self.assertEqual(set(CC_FIELDS), set(normalized.unavailable))
        self.assertEqual(118420753549838, normalized.values["drep_yes_vote_stake"])
        self.assertEqual(3847386581331, normalized.values["drep_do_not_vote_stake"])
        self.assertEqual(1816626528145935, normalized.values["spo_yes_vote_stake"])
        self.assertEqual(1435578557474937, normalized.values["spo_do_not_vote_stake"])

    def test_mainnet_fixture_covers_drep_and_cc_only(self):
        reference = parse_adastat_response(load_fixture("mainnet_treasury_644.json"))
        normalized = build_expected_stats(reference)

        self.assertEqual(15, normalized.compared_fields)
        self.assertEqual(set(SPO_FIELDS), set(normalized.unavailable))
        self.assertEqual(1099403720427060, normalized.values["drep_do_not_vote_stake"])
        self.assertEqual(1, normalized.values["cc_do_not_vote"])
        self.assertEqual(Decimal("0.8571"), normalized.values["cc_approval_ratio"])

    def test_no_confidence_moves_predefined_stake_to_yes(self):
        drep = AdaStatDRepInputs(
            total=100,
            yes=10,
            no=5,
            abstain=2,
            always_abstain=20,
            always_no_confidence=15,
            inactive=0,
        )
        values, _ = compute_drep_stats(drep, NO_CONFIDENCE)
        self.assertIsNotNone(values)
        self.assertEqual(25, values["drep_total_yes_stake"])
        self.assertEqual(53, values["drep_total_no_stake"])
        self.assertEqual(22, values["drep_total_abstain_stake"])

    def test_spo_truth_table_matches_yaci_rules(self):
        spo = AdaStatSPOInputs(
            total=100,
            yes=10,
            no=5,
            abstain=2,
            always_abstain=20,
            always_no_confidence=15,
        )

        bootstrap, _ = compute_spo_stats(spo, INFO_ACTION, True)
        self.assertEqual(10, bootstrap["spo_total_yes_stake"])
        self.assertEqual(5, bootstrap["spo_total_no_stake"])
        self.assertEqual(85, bootstrap["spo_total_abstain_stake"])

        no_confidence, _ = compute_spo_stats(spo, NO_CONFIDENCE, False)
        self.assertEqual(25, no_confidence["spo_total_yes_stake"])
        self.assertEqual(53, no_confidence["spo_total_no_stake"])
        self.assertEqual(22, no_confidence["spo_total_abstain_stake"])

        hard_fork, _ = compute_spo_stats(spo, HARD_FORK_INITIATION, True)
        self.assertEqual(10, hard_fork["spo_total_yes_stake"])
        self.assertEqual(88, hard_fork["spo_total_no_stake"])
        self.assertEqual(2, hard_fork["spo_total_abstain_stake"])

    def test_all_body_action_produces_exactly_23_fields(self):
        fixture = load_fixture("preview_new_committee_1369.json")
        fixture["data"]["type"] = "parameterchange"
        fixture["data"]["cc_total"] = 7
        fixture["data"]["cc_yes"] = 4
        fixture["data"]["cc_no"] = 1
        fixture["data"]["cc_abstain"] = 1
        normalized = build_expected_stats(parse_adastat_response(fixture))
        self.assertEqual(PARAMETER_CHANGE, normalized.reference.action_type)
        self.assertEqual(set(ALL_FIELDS), set(normalized.values))
        self.assertFalse(normalized.unavailable)

    def test_rejects_negative_derived_remainder(self):
        drep = AdaStatDRepInputs(
            total=10,
            yes=11,
            no=0,
            abstain=0,
            always_abstain=0,
            always_no_confidence=0,
            inactive=0,
        )
        with self.assertRaises(ReferenceContractError):
            compute_drep_stats(drep, INFO_ACTION)

    def test_rounding_and_actual_normalization_are_exact(self):
        self.assertEqual(Decimal("0.1667"), approval_ratio(1, 5))
        self.assertEqual(
            Decimal("0.5060"),
            normalize_actual_value("drep_approval_ratio", Decimal("0.5060")),
        )
        self.assertEqual(12, normalize_actual_value("cc_yes", "12"))
        with self.assertRaises(ReferenceContractError):
            normalize_actual_value("cc_yes", True)

    def test_all_action_types_have_explicit_coverage(self):
        expected_counts = {
            "noconfidence": 18,
            "newcommittee": 18,
            "newconstitution": 15,
            "hardforkinitiation": 23,
            "treasurywithdrawals": 15,
            "parameterchange": 23,
            "infoaction": 23,
        }
        for action_type, expected_count in expected_counts.items():
            with self.subTest(action_type=action_type):
                fixture = load_fixture("preview_new_committee_1369.json")
                fixture["data"]["type"] = action_type
                if expected_count in (15, 23):
                    fixture["data"]["cc_total"] = 3
                    fixture["data"]["cc_yes"] = 1
                    fixture["data"]["cc_no"] = 1
                    fixture["data"]["cc_abstain"] = 0
                if expected_count == 15:
                    fixture["data"]["pool_total_stake"] = None
                    fixture["data"]["pool_yes_stake"] = None
                    fixture["data"]["pool_no_stake"] = None
                    fixture["data"]["pool_abstain_stake"] = None
                    fixture["data"]["pool_always_abstain_stake"] = None
                    fixture["data"]["pool_always_no_confidence_stake"] = None
                normalized = build_expected_stats(parse_adastat_response(fixture))
                self.assertEqual(expected_count, normalized.compared_fields)


if __name__ == "__main__":
    unittest.main()
