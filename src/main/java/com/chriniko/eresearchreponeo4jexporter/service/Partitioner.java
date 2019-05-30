package com.chriniko.eresearchreponeo4jexporter.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Partitioner {

    public <T> List<List<T>> process(List<T> data, int batchSize) {
        return process(data, batchSize, true);
    }

    public <T> List<List<T>> process(List<T> data, int batchSize, boolean remainedAsLastRecord) {
        if (data == null) {
            throw new IllegalArgumentException("provided data should not be null");
        }

        int dataSize = data.size();

        if (dataSize < batchSize) {
            List<List<T>> result = new ArrayList<>();
            List<T> head = new ArrayList<>(data);
            result.add(head);
            return result;
        }

        if (dataSize % batchSize == 0) { // equal distribution
            int steps = dataSize / batchSize;
            return getLists(data, batchSize, steps);
        }

        // not equal distribution case
        int remained = dataSize % batchSize;
        int steps = dataSize / batchSize;
        List<List<T>> result = getLists(data, batchSize, steps);

        int from = dataSize - remained;
        List<T> lastWork = new ArrayList<>(data.subList(from, data.size()));
        if (remainedAsLastRecord) {
            result.add(lastWork);
        } else {

            for (int i = 0; i < lastWork.size(); i++) {
                T t = lastWork.get(i);
                result.get(i % result.size()).add(t);
            }
        }

        return result;
    }

    private <T> List<List<T>> getLists(List<T> data, int batchSize, int steps) {
        List<List<T>> result = new ArrayList<>(steps);
        int from = 0;
        int to = batchSize;

        for (int i = 1; i <= steps; i++) {
            List<T> integers = data.subList(from, to);
            List<T> work = new ArrayList<>(integers);

            result.add(work);

            from += batchSize;
            to = from + batchSize;
        }

        return result;
    }

}
