package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.yaci.store.governanceaggr.util.DRepExpiryUtil;
import org.junit.jupiter.api.Test;

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
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        84L, 8, 10, 9
                ), null, Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8), List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                8,
                27);

        // v10
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        84L, 8, 10, 10
                ),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                8,
                18);

        /* TEST CASE 2 */
        // v9
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        84L, 8, 10, 9
                ),
                new DRepExpiryUtil.DRepInteractionInfo(9, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                9,
                19);

        // v10
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        84L, 8, 10, 9
                ),
                new DRepExpiryUtil.DRepInteractionInfo(9, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                9,
                19);

        /* TEST CASE 3 */
        // v9
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        85L, 8, 10, 9
                ),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                8,
                27);
        // v10
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        85L, 8, 10, 10
                ),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                8,
                18);

        /* TEST CASE 4 */
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        85L, 8, 10, 9
                ),
                new DRepExpiryUtil.DRepInteractionInfo(9, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                9,
                19);
        // v10
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        85L, 8, 10, 10
                ),
                new DRepExpiryUtil.DRepInteractionInfo(9, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                9,
                19
        );
        /* TEST CASE 5 */
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        86L, 8, 10, 9
                ),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                8,
                18);
        // v10
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        86L, 8, 10, 10
                ),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                8,
                18);
        /* TEST CASE 6 */
        // v9
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        86L, 8, 10, 9
                ),
                new DRepExpiryUtil.DRepInteractionInfo(9, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                9,
                19);
        // v10
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        86L, 8, 10, 10
                ),
                new DRepExpiryUtil.DRepInteractionInfo(9, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                9,
                19);
        /* TEST CASE 7 */
        // v9
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        125L, 12, 10, 9
                ),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                12,
                23);
        // v10
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        125L, 12, 10, 10
                ),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                12,
                22);
        /* TEST CASE 8 */
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        125L, 12, 10, 9
                ),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                13,
                24);
        // v10
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        125L, 12, 10, 10
                ),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                13,
                23);
        /* TEST CASE 9 */
        // v9
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        135L, 13, 10, 9
                ),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                13,
                25);
        // v10
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(
                        135L, 13, 10, 10
                ),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
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
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 9),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)),
                0,
                11,
                18);
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 10),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)),
                0,
                11,
                18);

        /* TEST CASE 2 */
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 9),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)),
                0,
                12,
                19);
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 10),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)),
                0,
                12,
                19);

        /* TEST CASE 3 */
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 9),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                13,
                20);
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 10),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                13,
                20);

        /* TEST CASE 4 */
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 9),
                new DRepExpiryUtil.DRepInteractionInfo(10, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)),
                0,
                11,
                20);
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 10),
                new DRepExpiryUtil.DRepInteractionInfo(10, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)),
                0,
                11,
                20);

        /* TEST CASE 5 */
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 9),
                new DRepExpiryUtil.DRepInteractionInfo(10, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)),
                0,
                12,
                21);
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 10),
                new DRepExpiryUtil.DRepInteractionInfo(10, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)),
                0,
                12,
                21);

        /* TEST CASE 6 */
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 9),
                new DRepExpiryUtil.DRepInteractionInfo(10, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                13,
                22);
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 10),
                new DRepExpiryUtil.DRepInteractionInfo(10, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                13,
                22);

        /* TEST CASE 7 */
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 9),
                new DRepExpiryUtil.DRepInteractionInfo(10, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                14,
                22);
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(88L, 8, 10, 10),
                new DRepExpiryUtil.DRepInteractionInfo(10, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                14,
                22);

        /* TEST CASE 8 */
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(115L, 11, 10, 9),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)),
                0,
                11,
                21);
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(115L, 11, 10, 10),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)),
                0,
                11,
                21);

        /* TEST CASE 9 */
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(115L, 11, 10, 9),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                13,
                23);
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(115L, 11, 10, 10),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                13,
                23);

        /* TEST CASE 10 */
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(115L, 11, 10, 9),
                new DRepExpiryUtil.DRepInteractionInfo(13, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                13,
                23);
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(115L, 11, 10, 10),
                new DRepExpiryUtil.DRepInteractionInfo(13, 10),
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                13,
                23);

        /* TEST CASE 11 */
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(125L, 12, 10, 9),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                13,
                24);
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(125L, 12, 10, 10),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3)
                ),
                0,
                13,
                23);

        /* TEST CASE 12 */
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(134L, 13, 10, 9),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(135L, 13, 3)
                ),
                0,
                13,
                25);
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(134L, 13, 10, 10), null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(135L, 13, 3)
                ),
                0,
                13,
                23);

        /* TEST CASE 13 */
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(135L, 13, 10, 9),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(135L, 13, 3)
                ), 0,
                13,
                25);
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(135L, 13, 10, 10),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(135L, 13, 3)
                ),
                0,
                13,
                23);

        /* TEST CASE 14 */
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(136L, 13, 10, 9),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(135L, 13, 3)
                ),
                0,
                13,
                23);
        assertExpiry(new DRepExpiryUtil.DRepRegistrationInfo(136L, 13, 10, 10),
                null,
                Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(85L, 8, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(135L, 13, 3)
                ),
                0,
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
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(1L, 0, 10, 9),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(115L, 11, 3)
                ),
                0,
                11,
                16);
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(1L, 0, 10, 10),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                List.of(),
                0,
                11,
                15);

        /* TEST CASE 2 */
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(64L, 6, 10, 9),
                null,
                Set.of(0, 4, 5, 6),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3)
                ),
                0,
                8,
                19);
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(64L, 6, 10, 10),
                null,
                Set.of(0, 4, 5, 6),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3)
                ),
                0,
                8,
                16);

        /* TEST CASE 3 */
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(64L, 6, 10, 9),
                null,
                Set.of(0, 4, 5, 6, 10),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3)
                ),
                0,
                10,
                20);
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(64L, 6, 10, 10),
                null,
                Set.of(0, 4, 5, 6, 10),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3)
                ),
                0,
                10,
                17);

        /* TEST CASE 4 */
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(64L, 6, 10, 9),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3)
                ),
                0,
                11,
                21);
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(64L, 6, 10, 10),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3)
                ),
                0,
                11,
                18);

        /* TEST CASE 5 */
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(65L, 6, 10, 9),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3)
                ),
                0,
                11,
                21);
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(65L, 6, 10, 10),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3)
                ),
                0,
                11,
                18);

        /* TEST CASE 6 */
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(65L, 6, 10, 9),
                new DRepExpiryUtil.DRepInteractionInfo(8, 10),
                Set.of(0, 4, 5, 6, 10, 11),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3)
                ),
                0,
                11,
                20);
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(65L, 6, 10, 10),
                new DRepExpiryUtil.DRepInteractionInfo(8, 10),
                Set.of(0, 4, 5, 6, 10, 11),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3)
                ),
                0,
                11,
                20);

        /* TEST CASE 7 */
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(66L, 6, 10, 9),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3)
                ),
                0,
                11,
                18);
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(66L, 6, 10, 10),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3)
                ),
                0,
                11,
                18);

        /* TEST CASE 8 */
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(66L, 6, 10, 9),
                new DRepExpiryUtil.DRepInteractionInfo(8, 10),
                Set.of(0, 4, 5, 6, 10),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3)
                ),
                0,
                10,
                19);
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(66L, 6, 10, 10),
                new DRepExpiryUtil.DRepInteractionInfo(8, 10),
                Set.of(0, 4, 5, 6, 10),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3)
                ),
                0,
                10,
                19);

        /* TEST CASE 9 */
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(66L, 6, 10, 9),
                new DRepExpiryUtil.DRepInteractionInfo(8, 10),
                Set.of(0, 4, 5, 6, 10, 11),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3)
                ),
                0,
                11,
                20);
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(66L, 6, 10, 10),
                new DRepExpiryUtil.DRepInteractionInfo(8, 10),
                Set.of(0, 4, 5, 6, 10, 11),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3)
                ),
                0,
                11,
                20);

        /* TEST CASE 10 */
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(114L, 11, 10, 9),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(115L, 11, 3)
                ),
                0,
                11,
                23);
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(114L, 11, 10, 10),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(115L, 11, 3)
                ),
                0,
                11,
                21);

        /* TEST CASE 11 */
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(115L, 11, 10, 9),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(115L, 11, 3)
                ),
                0,
                11,
                23);
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(115L, 11, 10, 10),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(115L, 11, 3)
                ),
                0,
                11,
                21);

        /* TEST CASE 12 */
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(116L, 11, 10, 9),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(115L, 11, 3)
                ),
                0,
                11,
                21);
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(116L, 11, 10, 10),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(115L, 11, 3)
                ),
                0,
                11,
                21);

        /* TEST CASE 13 */
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(140L, 14, 10, 9),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(115L, 11, 3)
                ),
                0,
                14,
                24);
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(140L, 14, 10, 10),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(115L, 11, 3)
                ),
                0,
                14,
                24);

        /* TEST CASE 14 */
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(150L, 15, 10, 9),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(115L, 11, 3)
                ),
                0,
                15,
                26);
        assertExpiry(
                new DRepExpiryUtil.DRepRegistrationInfo(150L, 15, 10, 10),
                null,
                Set.of(0, 4, 5, 6, 10, 11),
                List.of(
                        new DRepExpiryUtil.ProposalSubmissionInfo(5L, 0, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(65L, 6, 3),
                        new DRepExpiryUtil.ProposalSubmissionInfo(115L, 11, 3)
                ),
                0,
                15,
                25);
    }

    private void assertExpiry(DRepExpiryUtil.DRepRegistrationInfo registration,
                              DRepExpiryUtil.DRepInteractionInfo lastInteraction,
                              Set<Integer> dormantEpochs,
                              List<DRepExpiryUtil.ProposalSubmissionInfo> proposalsUpToRegistration,
                              int eraFirstEpoch,
                              int evaluatedEpoch,
                              int expected) {

        var actual = DRepExpiryUtil.calculateDRepExpiry(
                registration,
                lastInteraction,
                dormantEpochs,
                proposalsUpToRegistration,
                eraFirstEpoch,
                evaluatedEpoch);

        assertThat(actual).isEqualTo(expected);
    }
}
