/*
 * Copyright 2014 Kaazing Corporation, All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kaazing.nuklei.concurrent;

import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.util.concurrent.CyclicBarrier;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test MpscArrayBuffer reader and writers in multiple threads
 */
@RunWith(Theories.class)
public class MpscArrayBufferConcurrencyTest
{
    private static final int NUM_MESSAGES_PER_WRITER = 10 * 1000 * 1000;
    private static final int NUM_IDS_PER_GENERATOR = 10 * 1000 * 1000;

    private static final int NUM_WRITERS_INDEX = 0;
    private static final int NUM_GENERATORS_INDEX = 0;
    private static final int CAPACITY_INDEX = 1;

    @DataPoint
    public static final int[] TWO_WRITERS_2 = { 2, 2 };

// Additional data points that can aid in testing. Uncomment if suspected problems
//
//    @DataPoint
//    public static final int[] ONE_WRITER_2 = { 1, 2 };
//
//    @DataPoint
//    public static final int[] TWO_WRITERS_16K = { 2, 16 * 1024};
//
//    @DataPoint
//    public static final int[] ONE_WRITER_16K = { 1, 16 * 1024 };

    @Theory
    @Test(timeout = 1000)
    public void shouldGenerateIds(final int[] params) throws Exception
    {
        final int numIds = NUM_IDS_PER_GENERATOR;
        final int numGenerators = params[NUM_GENERATORS_INDEX];
        final int capacity = params[CAPACITY_INDEX];
        final Thread[] threads = new Thread[numGenerators];

        final CyclicBarrier goBarrier = new CyclicBarrier(numGenerators);

        final MpscArrayBuffer<Integer> buffer = new MpscArrayBuffer<>(capacity);

        final Runnable generatorRun = () ->
        {
            try
            {
                goBarrier.await();
            }
            catch (final Exception ex)
            {
            }

            IntStream.range(0, numIds).forEach((j) -> buffer.nextId());
        };

        IntStream.range(0, numGenerators).forEach((i) ->
        {
            threads[i] = new Thread(generatorRun);
            threads[i].start();
        });

        IntStream.range(0, numGenerators).forEach((i) ->
        {
            try
            {
                threads[i].join();
            }
            catch (final Exception ex)
            {
            }
        });

        assertThat(buffer.nextId(), is((long)(numGenerators * numIds)));
    }

    @Theory
    @Test(timeout = 10000)
    public void shouldExchangeMessages(final int[] params) throws Exception
    {
        final int numMessages = NUM_MESSAGES_PER_WRITER;
        final int numWriters = params[NUM_WRITERS_INDEX];
        final int capacity = params[CAPACITY_INDEX];

        final CyclicBarrier goBarrier = new CyclicBarrier(numWriters);

        final MpscArrayBuffer<int[]> buffer = new MpscArrayBuffer<>(capacity);

        IntStream.range(0, numWriters).forEach((i) ->
                new Thread(new Writer(buffer, goBarrier, i, numMessages)).start());

        final int[] counts = new int[numWriters];

        final MpscArrayBuffer.ReadHandler<int[]> handler = (msg) ->
        {
            assertThat(msg.length, is(2));

            final int id = msg[0];
            final int messageNum = msg[1];

            assertThat(messageNum, is(counts[id]));

            counts[id]++;
        };

        int msgCount = 0;
        while (msgCount < (numMessages * numWriters))
        {
            final int readCount = buffer.read(handler, Integer.MAX_VALUE);

            if (0 == readCount)
            {
                Thread.yield();
            }

            msgCount += readCount;
        }

        assertThat(msgCount, is(numMessages * numWriters));
    }

    private static class Writer implements Runnable
    {
        private final CyclicBarrier goBarrier;
        private final MpscArrayBuffer<int[]> buffer;
        private final int id;
        private final int messages;

        public Writer(final MpscArrayBuffer<int[]> buffer, final CyclicBarrier goBarrier,
                      final int id, final int messages)
        {
            this.goBarrier = goBarrier;
            this.buffer = buffer;
            this.id = id;
            this.messages = messages;
        }

        public void run()
        {
            try
            {
                goBarrier.await();
            }
            catch (final Exception ex)
            {
            }

            for (int i = 0; i < messages; i++)
            {
                final int[] msg = new int[2];

                msg[0] = id;
                msg[1] = i;

                while (!buffer.write(msg))
                {
                    Thread.yield();
                }
            }
        }
    }

}
