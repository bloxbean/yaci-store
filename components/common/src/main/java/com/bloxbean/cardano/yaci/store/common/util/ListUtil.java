package com.bloxbean.cardano.yaci.store.common.util;

import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ListUtil {

    public static  <T> List<List<T>> partition(List<T> list, int size) {

        List<List<T>> partitions = new ArrayList<>();
        if (list.size() == 0) {
            return partitions;
        }

        int length = list.size();
        int numOfPartitions = length / size + ((length % size == 0) ? 0 : 1);

        for (int i = 0; i < numOfPartitions; i++) {
            int from = i * size;
            int to = Math.min((i * size + size), length);
            partitions.add(list.subList(from, to));
        }
        return partitions;
    }

    public static <T> void partitionAndApply(List<T> list, int batchSize, Consumer<List<T>> applyFunc) {
        if (list == null || list.size() == 0)
            return;

        if (list.size() <= batchSize) {
            applyFunc.accept(list);
        } else {
            List<List<T>> partitions = partition(new ArrayList<>(list), batchSize);
            for (var partition: partitions) {
                applyFunc.accept(partition);
            }
        }
    }

    @SneakyThrows
    public static <T> void partitionAndApplyInParallel(List<T> list, int batchSize, Consumer<List<T>> applyFunc) {
        if (list == null || list.size() == 0)
            return;

        if (list.size() <= batchSize) {
            applyFunc.accept(list);
        } else {
            List<List<T>> partitions = partition(new ArrayList<>(list), batchSize);
            List<CompletableFuture> futures = new ArrayList<>();
            for (var partition: partitions) {
                var completableFuture = CompletableFuture.supplyAsync(() -> {
                    applyFunc.accept(partition);
                    return true;
                });
                futures.add(completableFuture);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            for (var future: futures) {
                future.get();
            }
        }
    }
}
