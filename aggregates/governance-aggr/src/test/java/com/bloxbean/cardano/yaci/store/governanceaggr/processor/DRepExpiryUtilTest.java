package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.yaci.store.governanceaggr.util.DRepExpiryUtil;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class DRepExpiryUtilTest {

    /*
    ╔════════════════════════════════════════════════════════════════════════════════╗
    ║ Scenario 1:                                                                    ║
    ║   - Governance action lifetime = 3                                             ║
    ║   - DRep activity = 10                                                         ║
    ║   - One proposal submitted at slot 85 (epoch 8)                                ║
    ║   - Dormant epochs begin from 12                                               ║
    ║                                                                                ║
    ║        proposal                                                                ║
    ║          |                                                                     ║
    ║    ──────x────┬────────┬────────┬────────►                                     ║
    ║         85    90      100      110                                             ║
    ║       (slot) (epoch 9) (epoch 10) (epoch 11)                                   ║
    ║                                                                                ║
    ╚════════════════════════════════════════════════════════════════════════════════╝
   */
    @Test
    public void testScenario1() {
        /* TEST CASE 1 */
        // v9
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        84L, 8, 10, 9
                ), null, Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                null,
                0,
                8,
                27);

        // v10
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        84L, 8, 10, 10
                ),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                8,
                18);

        /* TEST CASE 2 */
        // v9
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        84L, 8, 10, 9
                ),
                new DRepExpiryUtil.DRepInteractionInfo(9, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                0,
                9,
                19);

        // v10
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        84L, 8, 10, 9
                ),
                new DRepExpiryUtil.DRepInteractionInfo(9, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                9,
                19);

        /* TEST CASE 3 */
        // v9
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        85L, 8, 10, 9
                ),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                0,
                8,
                27);
        // v10
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        85L, 8, 10, 10
                ),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                8,
                18);

        /* TEST CASE 4 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        85L, 8, 10, 9
                ),
                new DRepExpiryUtil.DRepInteractionInfo(9, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                0,
                9,
                19);
        // v10
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        85L, 8, 10, 10
                ),
                new DRepExpiryUtil.DRepInteractionInfo(9, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                9,
                19
        );
        /* TEST CASE 5 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        86L, 8, 10, 9
                ),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                0,
                8,
                18);
        // v10
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        86L, 8, 10, 10
                ),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                8,
                18);
        /* TEST CASE 6 */
        // v9
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        86L, 8, 10, 9
                ),
                new DRepExpiryUtil.DRepInteractionInfo(9, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                0,
                9,
                19);
        // v10
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        86L, 8, 10, 10
                ),
                new DRepExpiryUtil.DRepInteractionInfo(9, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                9,
                19);
        /* TEST CASE 7 */
        // v9
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        125L, 12, 10, 9
                ),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12),
                new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                0,
                12,
                23);
        // v10
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        125L, 12, 10, 10
                ),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12),
                12,
                22);
        /* TEST CASE 8 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        125L, 12, 10, 9
                ),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                0,
                13,
                24);
        // v10
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        125L, 12, 10, 10
                ),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                13,
                23);
        /* TEST CASE 9 */
        // v9
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        135L, 13, 10, 9
                ),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                0,
                13,
                25);
        // v10
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        135L, 13, 10, 10
                ),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                13,
                23);
    }

    /*
    ╔════════════════════════════════════════════════════════════════════════════════════════════════════════╗
    ║ Scenario 2:                                                                                            ║
    ║   - Two proposals: at slot 85 (epoch 8) and slot 135 (epoch 13)                                        ║
    ║   - Dormant period (epoch 12-13)                                                                       ║
    ║                                                                                                        ║
    ║         proposal_1               proposal_2                                                            ║
    ║            |                        |                                                                  ║
    ║    ─────────x───────────┬───────┬───────┬──────────x─────────────►                                     ║
    ║           85           120     125     130       135                                                   ║
    ║        (epoch 8)               (epoch 12–13 dormant)                                                   ║
    ╚════════════════════════════════════════════════════════════════════════════════════════════════════════╝
   */
    @Test
    public void testScenario2() {
        /* TEST CASE 1 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 9),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                0,
                11,
                18);
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 10),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                11,
                18);

        /* TEST CASE 2 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 9),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12),
                new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                0,
                12,
                19);
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 10),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12),
                12,
                19);

        /* TEST CASE 3 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 9),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                0,
                13,
                20);
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 10),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                13,
                20);

        /* TEST CASE 4 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 9),
                new DRepExpiryUtil.DRepInteractionInfo(10, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                0,
                11,
                20);
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 10),
                new DRepExpiryUtil.DRepInteractionInfo(10, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                11,
                20);

        /* TEST CASE 5 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 9),
                new DRepExpiryUtil.DRepInteractionInfo(10, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12),
                new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                0,
                12,
                21);
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 10),
                new DRepExpiryUtil.DRepInteractionInfo(10, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12),
                12,
                21);

        /* TEST CASE 6 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 9),
                new DRepExpiryUtil.DRepInteractionInfo(10, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                0,
                13,
                22);
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 10),
                new DRepExpiryUtil.DRepInteractionInfo(10, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                13,
                22);

        /* TEST CASE 7 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 9),
                new DRepExpiryUtil.DRepInteractionInfo(10, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                0,
                14,
                22);
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 10),
                new DRepExpiryUtil.DRepInteractionInfo(10, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                14,
                22);

        /* TEST CASE 8 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(115L, 11, 10, 9),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                0,
                11,
                21);
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(115L, 11, 10, 10),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                11,
                21);

        /* TEST CASE 9 */
        assertExpiryV9(new DRepExpiryUtil.DRepRegistrationInfo(115L, 11, 10, 9),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                0,
                13,
                23);
        assertExpiryV10(new DRepExpiryUtil.DRepRegistrationInfo(115L, 11, 10, 10),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                13,
                23);

        /* TEST CASE 10 */
        assertExpiryV9(new DRepExpiryUtil.DRepRegistrationInfo(115L, 11, 10, 9),
                new DRepExpiryUtil.DRepInteractionInfo(13, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                0,
                13,
                23);
        assertExpiryV10(new DRepExpiryUtil.DRepRegistrationInfo(115L, 11, 10, 10),
                new DRepExpiryUtil.DRepInteractionInfo(13, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                13,
                23);

        /* TEST CASE 11 */
        assertExpiryV9(new DRepExpiryUtil.DRepRegistrationInfo(125L, 12, 10, 9),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                0,
                13,
                24);
        assertExpiryV10(new DRepExpiryUtil.DRepRegistrationInfo(125L, 12, 10, 10),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                13,
                23);

        /* TEST CASE 12 */
        assertExpiryV9(new DRepExpiryUtil.DRepRegistrationInfo(134L, 13, 10, 9),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                0,
                13,
                25);
        assertExpiryV10(new DRepExpiryUtil.DRepRegistrationInfo(134L, 13, 10, 10),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                13,
                23);

        /* TEST CASE 13 */
        assertExpiryV9(new DRepExpiryUtil.DRepRegistrationInfo(135L, 13, 10, 9),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                new DRepExpiryUtil.ProposalSubmissionInfo(135L, 13, 3),
                0,
                13,
                25);
        assertExpiryV10(new DRepExpiryUtil.DRepRegistrationInfo(135L, 13, 10, 10),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                13,
                23);

        /* TEST CASE 14 */
        assertExpiryV9(new DRepExpiryUtil.DRepRegistrationInfo(136L, 13, 10, 9),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                new DRepExpiryUtil.ProposalSubmissionInfo(135L, 13, 3),
                0,
                13,
                23);
        assertExpiryV10(new DRepExpiryUtil.DRepRegistrationInfo(136L, 13, 10, 10),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                13,
                23);
    }

    /*
     ╔═════════════════════════════════════════════════════════════════════════════════════════════════════════════════╗
     ║ Scenario 3:                                                                                                     ║
     ║   - Three proposals submitted at slot 5 (epoch 0), slot 65 (epoch 6), and slot 115 (epoch 11)                   ║
     ║   - Two dormant periods:                                                                                        ║
     ║       • Epochs 4, 5, 6                                                                                          ║
     ║       • Epochs 10, 11                                                                                           ║
     ║                                                                                                                 ║
     ║         proposal_1         proposal_2         proposal_3                                                        ║
     ║             |                  |                  |                                                             ║
     ║    ──────────x────┬───────┬───────┬───────x───────┬───────┬───────x───────────────────────────────────────►     ║
     ║            slot 5        60      64      66      70     110     115                                             ║
     ║         (epoch 0)               (epoch 4–6)             (epoch 10–11)                                           ║
     ╚═════════════════════════════════════════════════════════════════════════════════════════════════════════════════╝
    */

    @Test
    public void testScenario3() {
        /* TEST CASE 1 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(1L, 0, 10, 9),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                null,
                0,
                11,
                16);
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(1L, 0, 10, 10),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                11,
                15);

        /* TEST CASE 2 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(64L, 6, 10, 9),
                null,
                Set.of(0, 4, 5, 6),
                new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                0,
                8,
                19);
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(64L, 6, 10, 10),
                null,
                Set.of(0, 4, 5, 6),
                8,
                16);

        /* TEST CASE 3 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(64L, 6, 10, 9),
                null,
                Set.of(0, 4, 5, 6, 10),
                new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                0,
                10,
                20);
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(64L, 6, 10, 10),
                null,
                Set.of(0, 4, 5, 6, 10),
                10,
                17);

        /* TEST CASE 4 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(64L, 6, 10, 9),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                0,
                11,
                21);
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(64L, 6, 10, 10),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                11,
                18);

        /* TEST CASE 5 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(65L, 6, 10, 9),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3),
                0,
                11,
                21);
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(65L, 6, 10, 10),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                11,
                18);

        /* TEST CASE 6 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(65L, 6, 10, 9),
                new DRepExpiryUtil.DRepInteractionInfo(8, 10),
                Set.of(0, 4, 5, 6, 10, 11),
                new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3),
                0,
                11,
                20);
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(65L, 6, 10, 10),
                new DRepExpiryUtil.DRepInteractionInfo(8, 10),
                Set.of(0, 4, 5, 6, 10, 11),
                11,
                20);

        /* TEST CASE 7 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(66L, 6, 10, 9),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3),
                0,
                11,
                18);
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(66L, 6, 10, 10),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                11,
                18);

        /* TEST CASE 8 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(66L, 6, 10, 9),
                new DRepExpiryUtil.DRepInteractionInfo(8, 10),
                Set.of(0, 4, 5, 6, 10),
                new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3),
                0,
                10,
                19);
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(66L, 6, 10, 10),
                new DRepExpiryUtil.DRepInteractionInfo(8, 10),
                Set.of(0, 4, 5, 6, 10),
                10,
                19);

        /* TEST CASE 9 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(66L, 6, 10, 9),
                new DRepExpiryUtil.DRepInteractionInfo(8, 10),
                Set.of(0, 4, 5, 6, 10, 11),
                new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3),
                0,
                11,
                20);
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(66L, 6, 10, 10),
                new DRepExpiryUtil.DRepInteractionInfo(8, 10),
                Set.of(0, 4, 5, 6, 10, 11),
                11,
                20);

        /* TEST CASE 10 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(114L, 11, 10, 9),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3),
                0,
                11,
                23);
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(114L, 11, 10, 10),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                11,
                21);

        /* TEST CASE 11 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(115L, 11, 10, 9),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                new DRepExpiryUtil.ProposalSubmissionInfo(115L, 11, 3),
                0,
                11,
                23);
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(115L, 11, 10, 10),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                11,
                21);

        /* TEST CASE 12 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(116L, 11, 10, 9),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                new DRepExpiryUtil.ProposalSubmissionInfo(115L, 11, 3),
                0,
                11,
                21);
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(116L, 11, 10, 10),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                11,
                21);

        /* TEST CASE 13 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(140L, 14, 10, 9),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                new DRepExpiryUtil.ProposalSubmissionInfo(115L, 11, 3),
                0,
                14,
                24);
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(140L, 14, 10, 10),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                14,
                24);

        /* TEST CASE 14 */
        assertExpiryV9(
                new DRepExpiryUtil.DRepRegistrationInfo(150L, 15, 10, 9),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                new DRepExpiryUtil.ProposalSubmissionInfo(115L, 11, 3),
                0,
                15,
                26);
        assertExpiryV10(
                new DRepExpiryUtil.DRepRegistrationInfo(150L, 15, 10, 10),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                15,
                25);
    }

    @Test
    public void shouldCalculateDbSyncActiveUntilAcrossSanchonetPv9ProposalFlushes() {
        var registration = new DRepExpiryUtil.DRepRegistrationInfo(42596698L, 493, 20, 9, 0, 0);
        var proposals = List.of(
                new DRepExpiryUtil.ProposalSubmissionInfo(50184338L, 580, 60, 0, 0),
                new DRepExpiryUtil.ProposalSubmissionInfo(51202227L, 592, 60, 0, 0)
        );
        // activeProposalEpochs must include the epoch after each proposal submission
        // (the proposal is still in newProposals at that boundary before ratification)
        var activeProposalEpochs = new HashSet<Integer>();
        activeProposalEpochs.add(581); // proposal at 580 still in newProposals at boundary 580→581
        activeProposalEpochs.addAll(rangeClosed(593, 598)); // proposal at 592 active from 593

        assertThat(DRepExpiryUtil.calculateDRepActiveUntil(
                registration, List.of(), proposals, activeProposalEpochs, 493, 570))
                .isEqualTo(513);

        assertThat(DRepExpiryUtil.calculateDRepActiveUntil(
                registration, List.of(), proposals, activeProposalEpochs, 493, 582))
                .isEqualTo(600);

        assertThat(DRepExpiryUtil.calculateDRepActiveUntil(
                registration, List.of(), proposals, activeProposalEpochs, 493, 593))
                .isEqualTo(611);

        var voteAt598 = new DRepExpiryUtil.DRepInteractionInfo(598, 20, 51679073L, 0, 0);
        assertThat(DRepExpiryUtil.calculateDRepActiveUntil(
                registration, List.of(voteAt598), proposals, activeProposalEpochs, 493, 599))
                .isEqualTo(618);
    }

    @Test
    public void shouldKeepRawActiveUntilStableDuringDormantPeriodAfterVote() {
        var registration = new DRepExpiryUtil.DRepRegistrationInfo(1L, 990, 20, 10, 0, 0);
        var proposals = List.of(
                new DRepExpiryUtil.ProposalSubmissionInfo(100L, 993, 15, 0, 0),
                new DRepExpiryUtil.ProposalSubmissionInfo(300L, 1013, 15, 0, 0)
        );
        var interactions = List.of(
                new DRepExpiryUtil.DRepInteractionInfo(993, 20, 101L, 1, 0),
                new DRepExpiryUtil.DRepInteractionInfo(1013, 20, 301L, 1, 0)
        );
        var activeProposalEpochs = Set.<Integer>of();

        assertThat(DRepExpiryUtil.calculateDRepActiveUntil(
                registration, interactions, proposals, activeProposalEpochs, 990, 1000))
                .isEqualTo(1013);

        assertThat(DRepExpiryUtil.calculateDRepActiveUntil(
                registration, interactions, proposals, activeProposalEpochs, 990, 1014))
                .isEqualTo(1033);
    }

    @Test
    public void shouldNotCountProposalSubmissionEpochAsDormantForActiveUntil() {
        var registration = new DRepExpiryUtil.DRepRegistrationInfo(78488970L, 908, 20, 10, 0, 0);
        var proposals = List.of(
                new DRepExpiryUtil.ProposalSubmissionInfo(78488971L, 908, 15, 0, 0),
                new DRepExpiryUtil.ProposalSubmissionInfo(79111020L, 915, 15, 0, 0)
        );
        var voteAt910 = new DRepExpiryUtil.DRepInteractionInfo(910, 20, 78695050L, 0, 0);
        // Proposal at 908 is active at epochs 908-911, ratified at boundary 911→912
        // (still in newProposals at boundary 911→912, so epoch 912 is also non-dormant)
        var activeProposalEpochs = Set.of(908, 909, 910, 911, 912, 916);

        assertThat(DRepExpiryUtil.calculateDRepActiveUntil(
                registration, List.of(voteAt910), proposals, activeProposalEpochs, 908, 916))
                .isEqualTo(933);
    }

    @Test
    public void shouldNotReviveInactiveDRepWhenProposalFlushesDormantCounter() {
        var registration = new DRepExpiryUtil.DRepRegistrationInfo(1L, 0, 3, 10, 0, 0);
        var proposals = List.of(
                new DRepExpiryUtil.ProposalSubmissionInfo(0L, 0, 4, 0, 0),
                new DRepExpiryUtil.ProposalSubmissionInfo(60L, 6, 4, 0, 0)
        );
        // Proposal at epoch 0 with lifetime 4: gasExpiresAfter=4, active for epochs 0-4
        var activeProposalEpochs = Set.of(0, 1, 2, 3, 4);

        assertThat(DRepExpiryUtil.calculateDRepActiveUntil(
                registration, List.of(), proposals, activeProposalEpochs, 0, 6))
                .isEqualTo(3);
    }

    @Test
    public void shouldFixPv10OffByOneWhenProposalFlushesInDormantEpoch() {
        // DRep af0dae registered at epoch 670 (PV10, dRepActivity=20)
        // No votes. Proposals at epochs 683, 685, 687.
        // activeProposalEpochs: proposal at 613 (lifetime=60) active until 673,
        // so epochs 593-673 are non-dormant. Proposal at 683 makes epoch 684 non-dormant.
        var registration = new DRepExpiryUtil.DRepRegistrationInfo(57890157L, 670, 20, 10, 0, 0);
        var proposals = List.of(
                new DRepExpiryUtil.ProposalSubmissionInfo(59080597L, 683, 60, 0, 0),
                new DRepExpiryUtil.ProposalSubmissionInfo(59184828L, 685, 60, 0, 0),
                new DRepExpiryUtil.ProposalSubmissionInfo(59388112L, 687, 60, 0, 0)
        );
        var activeProposalEpochs = new HashSet<Integer>();
        activeProposalEpochs.addAll(rangeClosed(593, 673));
        activeProposalEpochs.add(684); // proposal at 683 still in newProposals at boundary 683→684

        // At evaluatedEpoch=683: counter=10 (epochs 674-683), flush: 690+10=700
        assertThat(DRepExpiryUtil.calculateDRepActiveUntil(
                registration, List.of(), proposals, activeProposalEpochs, 493, 683))
                .isEqualTo(700);
    }

    private void assertExpiryV9(DRepExpiryUtil.DRepRegistrationInfo registration,
                                DRepExpiryUtil.DRepInteractionInfo lastInteraction,
                                Set<Integer> dormantEpochs,
                                DRepExpiryUtil.ProposalSubmissionInfo latestProposalUpToRegistration,
                                int eraFirstEpoch,
                                int evaluatedEpoch,
                                int expected) {

        var actual = DRepExpiryUtil.calculateDRepExpiryV9(
                registration,
                lastInteraction,
                dormantEpochs,
                latestProposalUpToRegistration,
                eraFirstEpoch,
                evaluatedEpoch);

        assertThat(actual).isEqualTo(expected);
    }

    private void assertExpiryV10(DRepExpiryUtil.DRepRegistrationInfo registration,
                                 DRepExpiryUtil.DRepInteractionInfo lastInteraction,
                                 Set<Integer> dormantEpochs,
                                 int evaluatedEpoch,
                                 int expected) {

        var actual = DRepExpiryUtil.calculateDRepExpiryV10(
                registration,
                lastInteraction,
                dormantEpochs,
                evaluatedEpoch);

        assertThat(actual).isEqualTo(expected);
    }

    private Set<Integer> rangeClosed(int startInclusive, int endInclusive) {
        Set<Integer> result = new HashSet<>();
        for (int epoch = startInclusive; epoch <= endInclusive; epoch++) {
            result.add(epoch);
        }
        return result;
    }
}
