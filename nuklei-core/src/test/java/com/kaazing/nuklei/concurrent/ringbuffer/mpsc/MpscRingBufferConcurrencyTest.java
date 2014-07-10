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
package com.kaazing.nuklei.concurrent.ringbuffer.mpsc;

import com.kaazing.nuklei.BitUtil;
import com.kaazing.nuklei.concurrent.AtomicBuffer;
import com.kaazing.nuklei.concurrent.ringbuffer.RingBufferReader;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test MpscRingBuffer reader and writers in multiple threads
 */
@RunWith(Theories.class)
public class MpscRingBufferConcurrencyTest
{
    private static final int NUM_MESSAGES_PER_WRITER = 10 * 1000 * 1000;
    private static final int NUM_IDS_PER_GENERATOR = 10 * 1000 * 1000;

    private static final int MSG_TYPE_ID = 100;

    private static final int NUM_WRITERS_INDEX = 0;
    private static final int NUM_GENERATORS_INDEX = 0;
    private static final int CAPACITY_INDEX = 1;

    private static final int MESSAGE_LENGTH = 2 * BitUtil.SIZE_OF_INT;

    @DataPoint
    public static final int[] TWO_WRITERS_16K = { 2, 16 * 1024 };

// Additional data points that can aid in testing. Uncomment if suspected problems
//
//    @DataPoint
//    public static final int[] ONE_WRITER_16K = { 1, 16 * 1024 };
//
//    @DataPoint
//    public static final int[] TWO_WRITERS_64K = { 2, 64 * 1024 };
//
//    @DataPoint
//    public static final int[] ONE_WRITER_64K = { 1, 64 * 1024 };

    @Theory
    @Test(timeout = 1000)
    public void shouldGenerateIds(final int[] params) throws Exception
    {
        final int numIds = NUM_IDS_PER_GENERATOR;
        final int numGenerators = params[NUM_GENERATORS_INDEX];
        final int capacity = params[CAPACITY_INDEX];
        final Thread[] threads = new Thread[numGenerators];

        final CyclicBarrier goBarrier = new CyclicBarrier(numGenerators);

        final AtomicBuffer atomicBuffer =
                new AtomicBuffer(ByteBuffer.allocateDirect(capacity + MpscRingBuffer.STATE_TRAILER_SIZE));

        final Runnable generatorRun = () ->
        {
            final MpscRingBufferIdGenerator generator = new MpscRingBufferIdGenerator(atomicBuffer);

            try
            {
                goBarrier.await();
            }
            catch (final Exception ex)
            {
            }

            IntStream.range(0, numIds).forEach((j) -> generator.nextId());
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

        final MpscRingBufferIdGenerator generator = new MpscRingBufferIdGenerator(atomicBuffer);

        assertThat(generator.nextId(), is((long)(numGenerators * numIds)));
    }

    @Theory
    @Test(timeout = 10000)
    public void shouldExchangeMessages(final int[] params) throws Exception
    {
        final int numMessages = NUM_MESSAGES_PER_WRITER;
        final int numWriters = params[NUM_WRITERS_INDEX];
        final int capacity = params[CAPACITY_INDEX];

        final CyclicBarrier goBarrier = new CyclicBarrier(numWriters);

        final AtomicBuffer atomicBuffer =
                new AtomicBuffer(ByteBuffer.allocateDirect(capacity + MpscRingBuffer.STATE_TRAILER_SIZE));

        final MpscRingBufferReader reader = new MpscRingBufferReader(atomicBuffer);

        IntStream.range(0, numWriters).forEach((i) ->
                new Thread(new Writer(atomicBuffer, goBarrier, i, numMessages)).start());

        final int[] counts = new int[numWriters];

        final RingBufferReader.ReadHandler handler = (typeId, buffer, index, length) ->
        {
            assertThat(typeId, is(MSG_TYPE_ID));
            assertThat(length, is(MESSAGE_LENGTH));

            final int id = buffer.getInt(index);
            final int messageNum = buffer.getInt(index + BitUtil.SIZE_OF_INT);

            assertThat(messageNum, is(counts[id]));

            counts[id]++;
        };

        int msgCount = 0;
        while (msgCount < (numMessages * numWriters))
        {
            final int readCount = reader.read(handler, Integer.MAX_VALUE);

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
        private final MpscRingBufferWriter writer;
        private final int id;
        private final int messages;
        private final AtomicBuffer srcBuffer;

        public Writer(final AtomicBuffer buffer, final CyclicBarrier goBarrier, final int id, final int messages)
        {
            this.goBarrier = goBarrier;
            this.writer = new MpscRingBufferWriter(buffer);
            this.id = id;
            this.messages = messages;
            this.srcBuffer = new AtomicBuffer(new byte[MESSAGE_LENGTH]);
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

            srcBuffer.putInt(0, id);

            for (int i = 0; i < messages; i++)
            {
                srcBuffer.putInt(BitUtil.SIZE_OF_INT, i);

                while (!writer.write(MSG_TYPE_ID, srcBuffer, 0, MESSAGE_LENGTH))
                {
                    Thread.yield();
                }
            }
        }
    }
}
