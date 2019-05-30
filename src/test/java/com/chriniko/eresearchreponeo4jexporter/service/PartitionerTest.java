package com.chriniko.eresearchreponeo4jexporter.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PartitionerTest {

    private Partitioner partitioner;

    @Before
    public void setUp() {
        partitioner = new Partitioner();
    }

    @Test
    public void empty_data_case() {

        // given
        int batchSize = 100;
        int n = 0;


        List<Integer> datum = Collections.emptyList();

        // when
        List<List<Integer>> splitter = partitioner.process(datum, batchSize);

        // then
        long count = splitter.stream().map(List::size).reduce(0, Integer::sum);
        Assert.assertEquals(n, count);

        Assert.assertEquals(1, splitter.size());
        Assert.assertEquals(0, splitter.get(0).size());

    }

    @Test(expected = IllegalArgumentException.class) // then
    public void null_data_case() {

        // given
        int batchSize = 100;

        // when
        partitioner.process(null, batchSize);
    }

    @Test
    public void equal_distribution_case() {

        // given
        int batchSize = 100;
        int n = 1_000_000;


        List<Integer> datum = IntStream.rangeClosed(1, n).boxed().collect(Collectors.toList());

        // when
        List<List<Integer>> splitter = partitioner.process(datum, batchSize);

        // then
        long count = splitter.stream().map(List::size).reduce(0, Integer::sum);
        Assert.assertEquals(n, count);

        Assert.assertEquals(n / batchSize, splitter.size());

        int from = 1;
        int to = batchSize;
        for (List<Integer> work : splitter) {

            Assert.assertEquals(batchSize, work.size());

            List<Integer> expected = IntStream.rangeClosed(from, to).boxed().collect(Collectors.toList());
            Assert.assertEquals(expected, work);

            from += batchSize;
            to = from + batchSize - 1;
        }

    }

    @Test
    public void less_than_batch_size_case() {

        // given
        int batchSize = 100;
        int n = 87;

        List<Integer> datum = IntStream.rangeClosed(1, n).boxed().collect(Collectors.toList());

        // when
        List<List<Integer>> splitter = partitioner.process(datum, batchSize);

        // then
        long count = splitter.stream().map(List::size).reduce(0, Integer::sum);
        Assert.assertEquals(count, n);

        Assert.assertEquals(1, splitter.size());
        Assert.assertEquals(n, splitter.get(0).size());
        Assert.assertEquals(datum, splitter.get(0));
    }

    @Test
    public void not_equal_distribution_case_1() {

        // given
        int batchSize = 100;
        int n = 1_000_001;

        List<Integer> datum = IntStream.rangeClosed(1, n).boxed().collect(Collectors.toList());

        // when
        List<List<Integer>> splitter = partitioner.process(datum, batchSize);

        // then
        int remained = n % batchSize;
        int totalSize = n / batchSize + 1 /*last entry has all the remained records*/;

        Assert.assertEquals(totalSize, splitter.size());

        long count = splitter.stream().map(List::size).reduce(0, Integer::sum);
        Assert.assertEquals(n, count);

        int idx = 0;

        int from = 1;
        int to = batchSize;

        for (List<Integer> work : splitter) {

            idx++;

            if (idx == totalSize) { //remained work
                Assert.assertEquals(remained, work.size());

                List<Integer> expected = datum.subList(from - 1, datum.size());
                Assert.assertEquals(expected, work);

            } else {
                Assert.assertEquals(batchSize, work.size());

                List<Integer> expected = IntStream.rangeClosed(from, to).boxed().collect(Collectors.toList());
                Assert.assertEquals(expected, work);

            }
            from += batchSize;
            to = from + batchSize - 1;
        }
    }

    @Test
    public void not_equal_distribution_case_2() {

        // given
        int batchSize = 100;
        int n = 1_312_451;

        List<Integer> datum = IntStream.rangeClosed(1, n).boxed().collect(Collectors.toList());

        // when
        List<List<Integer>> splitter = partitioner.process(datum, batchSize);

        // then
        long count = splitter.stream().map(List::size).reduce(0, Integer::sum);
        Assert.assertEquals(n, count);

        int remained = n % batchSize;
        int totalSize = n / batchSize + 1 /*last entry has all the remained records*/;

        Assert.assertEquals(totalSize, splitter.size());

        int idx = 0;

        int from = 1;
        int to = batchSize;

        for (List<Integer> work : splitter) {

            idx++;

            if (idx == totalSize) { //remained work
                Assert.assertEquals(remained, work.size());

                List<Integer> expected = datum.subList(from - 1, datum.size());
                Assert.assertEquals(expected, work);

            } else {
                Assert.assertEquals(batchSize, work.size());

                List<Integer> expected = IntStream.rangeClosed(from, to).boxed().collect(Collectors.toList());
                Assert.assertEquals(expected, work);

            }
            from += batchSize;
            to = from + batchSize - 1;
        }

    }

    @Test
    public void not_equal_distribution_remainedAsLastRecord_false_case_1() {

        // given
        int batchSize = 100;
        int n = 1_000_015;

        List<Integer> datum = IntStream.rangeClosed(1, n).boxed().collect(Collectors.toList());

        // when
        List<List<Integer>> splitter = partitioner.process(datum, batchSize, false);

        // then
        int remained = n % batchSize;
        int totalSize = n / batchSize /*all remained records have equally distributed*/;

        Assert.assertEquals(totalSize, splitter.size());

        long count = splitter.stream().map(List::size).reduce(0, Integer::sum);
        Assert.assertEquals(n, count);

        Map<Integer, Integer> remainedPerSplit = IntStream.range(0, splitter.size())
                .boxed()
                .collect(Collectors.toMap(i -> i, i -> 0));


        for (int i = 0; i < remained; i++) {
            int splitIdx = i % remainedPerSplit.size();
            remainedPerSplit.computeIfAbsent(splitIdx, k -> 0);
            remainedPerSplit.computeIfPresent(splitIdx, (k, v) -> v + 1);
        }
        System.out.println(remainedPerSplit);


        int idx = 0;
        for (List<Integer> work : splitter) {


            Assert.assertEquals(
                    batchSize + remainedPerSplit.get(idx),
                    work.size()
            );

            idx++;

            //TODO assert contents...
        }
    }


}