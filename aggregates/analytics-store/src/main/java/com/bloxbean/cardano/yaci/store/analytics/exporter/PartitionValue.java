package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.core.service.EraService;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Represents a partition value for table exports.
 *
 * This sealed interface ensures type safety and exhaustive pattern matching
 * for different partition types (date-based, epoch-based, monthly).
 */
public sealed interface PartitionValue permits
        PartitionValue.DatePartition,
        PartitionValue.EpochPartition {

    /**
     * Convert partition value to Hive-style path segment.
     *
     * Examples:
     * - DatePartition(2024-01-15) -> "date=2024-01-15"
     * - EpochPartition(450) -> "epoch=450"
     *
     * @return Hive-style partition path segment
     */
    String toPathSegment();

    /**
     * Convert partition value to slot range for querying blockchain data.
     *
     * @param eraService Service for era/slot calculations
     * @return SlotRange for this partition
     */
    SlotRange toSlotRange(EraService eraService);

    /**
     * Date-based partition (daily).
     *
     * Example: date=2024-01-15 represents all blocks from
     * 2024-01-15T00:00:00Z to 2024-01-15T23:59:59Z (UTC).
     *
     * @param date The partition date (UTC)
     */
    record DatePartition(LocalDate date) implements PartitionValue {
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        @Override
        public String toPathSegment() {
            return "date=" + date.format(DATE_FORMATTER);
        }

        @Override
        public SlotRange toSlotRange(EraService eraService) {
            // Convert date to epoch seconds (start and end of day in UTC)
            LocalDate nextDay = date.plusDays(1);
            long startEpochSeconds = date.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
            long endEpochSeconds = nextDay.atStartOfDay(ZoneOffset.UTC).toEpochSecond();

            // Convert epoch seconds to slots using EraService
            // This handles both Byron and Shelley/post-Shelley eras correctly
            long startSlot = eraService.slotFromTime(startEpochSeconds);
            long endSlot = eraService.slotFromTime(endEpochSeconds);

            // On genesis day, start-of-day (midnight UTC) can be before the actual genesis time
            // (e.g., mainnet genesis is 2017-09-23T21:44:51Z), producing a negative slot.
            // No blockchain data exists before slot 0, so clamp to 0.
            startSlot = Math.max(0, startSlot);
            endSlot = Math.max(0, endSlot);

            return new SlotRange(startSlot, endSlot);
        }
    }

    /**
     * Epoch-based partition.
     *
     * Example: epoch=450 represents all blocks in epoch 450
     * (432,000 slots = 5 days on mainnet).
     *
     * @param epoch The epoch number
     */
    record EpochPartition(int epoch) implements PartitionValue {
        @Override
        public String toPathSegment() {
            return "epoch=" + epoch;
        }

        @Override
        public SlotRange toSlotRange(EraService eraService) {
            // Get the absolute slots for this epoch
            // Note: Cardano epochs are 432,000 slots (5 days)
            long startSlot = eraService.getShelleyAbsoluteSlot(epoch, 0);
            long endSlot = eraService.getShelleyAbsoluteSlot(epoch + 1, 0);

            return new SlotRange(startSlot, endSlot);
        }
    }

    /**
     * Factory method for creating date partitions.
     *
     * @param date The partition date
     * @return DatePartition instance
     */
    static PartitionValue ofDate(LocalDate date) {
        return new DatePartition(date);
    }

    /**
     * Factory method for creating epoch partitions.
     *
     * @param epoch The epoch number
     * @return EpochPartition instance
     */
    static PartitionValue ofEpoch(int epoch) {
        return new EpochPartition(epoch);
    }
}
