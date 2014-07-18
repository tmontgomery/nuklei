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

import org.kaazing.nuklei.concurrent.MpscArrayBuffer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Control;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 2 writer, 1 reader benchmark
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
public class MpscArrayBufferBasic
{
    private static final Integer VALUE = 101;
    private static final ThreadLocal<ReaderMarker> marker = new ThreadLocal<>();

    private final MpscArrayBuffer<Integer> buffer = new MpscArrayBuffer<>(1024*1024);
    private final Consumer<Integer> handler = (i) -> {};

    @State(Scope.Thread)
    public static class ReaderMarker
    {
        public ReaderMarker()
        {
            marker.set(this);
        }
    }

    @TearDown(Level.Iteration)
    public void emptyBuffer()
    {
        // used to indicate reader
        if (null == marker.get())
        {
            return;
        }

        while (buffer.read(handler, Integer.MAX_VALUE) != 0)
        {
            Thread.yield();
        }
    }

    @Benchmark
    @Group("throughput")
    @GroupThreads(2)
    public void write(final Control control)
    {
        while(!buffer.write(VALUE) && !control.stopMeasurement)
        {
            Thread.yield();
        }
    }

    @Benchmark
    @Group("throughput")
    @GroupThreads(1)
    public void read(final Control control, final ReaderMarker marker)
    {
        while(buffer.read(handler, Integer.MAX_VALUE) == 0 && !control.stopMeasurement)
        {
            Thread.yield();
        }
    }
}
