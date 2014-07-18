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
package org.kaazing.nuklei.jmh;

import org.kaazing.nuklei.BitUtil;
import org.kaazing.nuklei.concurrent.AtomicBuffer;
import org.kaazing.nuklei.concurrent.ringbuffer.mpsc.MpscRingBuffer;
import org.kaazing.nuklei.concurrent.ringbuffer.mpsc.MpscRingBufferReader;
import org.kaazing.nuklei.concurrent.ringbuffer.mpsc.MpscRingBufferWriter;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Control;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * 2 writers, 1 reader
 *
 * Approach taken from JCTools
 */
@State(Scope.Group)
@BenchmarkMode(Mode.Throughput)
@Fork(3)
@Threads(3)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class MpscRingBufferBasic
{
    private static final int MSG_TYPE_ID = 101;
    private static final Integer VALUE = 102;
    private static final ThreadLocal<ReaderMarker> marker = new ThreadLocal<>();

    private final AtomicBuffer buffer = new AtomicBuffer(ByteBuffer.allocateDirect(64*1024 + MpscRingBuffer.STATE_TRAILER_SIZE));
    private final MpscRingBufferWriter writer = new MpscRingBufferWriter(buffer);
    private final MpscRingBufferReader reader = new MpscRingBufferReader(buffer);
    private final MpscRingBufferReader.ReadHandler handler = (typeId, buffer, index, length) -> {};

    private final AtomicBuffer srcBuffer = new AtomicBuffer(ByteBuffer.allocateDirect(BitUtil.SIZE_OF_INT));

    @State(Scope.Thread)
    public static class ReaderMarker
    {
        public ReaderMarker()
        {
            marker.set(this);
        }
    }

    @Setup
    public void initSrcBuffer()
    {
        srcBuffer.putInt(0, VALUE);
    }

    @TearDown(Level.Iteration)
    public void emptyBuffer()
    {
        // used to indicate reader
        if (null == marker.get())
        {
            return;
        }

        while (reader.read(handler, Integer.MAX_VALUE) != 0)
        {
            Thread.yield();
        }
    }

    @Benchmark
    @Group("throughput")
    @GroupThreads(2)
    public void write(final Control control)
    {
        while(!writer.write(MSG_TYPE_ID, srcBuffer, 0, BitUtil.SIZE_OF_INT) && !control.stopMeasurement)
        {
            Thread.yield();
        }
    }

    @Benchmark
    @Group("throughput")
    @GroupThreads(1)
    public void read(final Control control, final ReaderMarker marker)
    {
        while(reader.read(handler, Integer.MAX_VALUE) == 0 && !control.stopMeasurement)
        {
            Thread.yield();
        }
    }


}
