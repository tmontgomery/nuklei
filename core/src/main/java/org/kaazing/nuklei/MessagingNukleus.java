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
package org.kaazing.nuklei;

import org.kaazing.nuklei.concurrent.ArrayBufferReader;
import org.kaazing.nuklei.concurrent.AtomicBuffer;
import org.kaazing.nuklei.concurrent.MpscArrayBuffer;
import org.kaazing.nuklei.concurrent.ringbuffer.RingBufferReader;
import org.kaazing.nuklei.concurrent.ringbuffer.mpsc.MpscRingBufferReader;

import java.util.function.Consumer;

/**
 * Nukleus that reads and processes messages
 */
public class MessagingNukleus implements Nukleus
{
    private static final RingBufferReader NULL_RING_BUFFER_READER = (handler, limit) -> 0;
    private static final ArrayBufferReader<Object> NULL_ARRAY_BUFFER_READER = (handler, limit) -> 0;
    private static final Nukleus NULL_NUKLEUS = () -> 0;

    private final RingBufferReader ringBufferReader;
    private final MpscRingBufferReader mpscRingBufferReader;
    private final MpscRingBufferReader.ReadHandler ringBufferHandler;

    private final ArrayBufferReader<Object> arrayBufferReader;
    private final MpscArrayBuffer<Object> mpscArrayBuffer;
    private final Consumer<Object> arrayBufferHandler;

    private final Nukleus nioSelectorProcess;
    private final NioSelectorNukleus nioSelectorNukleus;

    private final int ringBufferReadLimit;
    private final int arrayBufferReadLimit;

    /**
     * Construct a messaging-based {@link Nukleus} that reads and processes messages in various forms.
     *
     * @param builder for the nukleus
     */
    public MessagingNukleus(final Builder builder)
    {
        if (null == builder.ringBuffer && null == builder.arrayBuffer && null == builder.nioSelectorNukleus)
        {
            throw new IllegalArgumentException("must specify either RingBuffer, ArrayBuffer, and/or NioSelector for Nukleus");
        }

        if (null != builder.ringBuffer)
        {
            this.mpscRingBufferReader = new MpscRingBufferReader(builder.ringBuffer);
            this.ringBufferReader = mpscRingBufferReader;
        }
        else
        {
            this.mpscRingBufferReader = null;
            this.ringBufferReader = NULL_RING_BUFFER_READER;
        }

        if (null != builder.arrayBuffer)
        {
            this.mpscArrayBuffer = builder.arrayBuffer;
            this.arrayBufferReader = builder.arrayBuffer;
        }
        else
        {
            this.mpscArrayBuffer = null;
            this.arrayBufferReader = NULL_ARRAY_BUFFER_READER;
        }

        if (null != builder.nioSelectorNukleus)
        {
            this.nioSelectorNukleus = builder.nioSelectorNukleus;
            this.nioSelectorProcess = builder.nioSelectorNukleus;
        }
        else
        {
            this.nioSelectorNukleus = null;
            this.nioSelectorProcess = NULL_NUKLEUS;
        }

        this.ringBufferHandler = builder.ringBufferHandler;
        this.arrayBufferHandler = builder.arrayBufferHandler;
        this.ringBufferReadLimit = builder.ringBufferReadLimit;
        this.arrayBufferReadLimit = builder.arrayBufferReadLimit;
    }

    /** {@inheritDoc} */
    public int process()
    {
        int weight = 0;

        try
        {
            // some of these might be noop lambdas that return 0, but should be no branching
            weight += ringBufferReader.read(ringBufferHandler, ringBufferReadLimit);
            weight += arrayBufferReader.read(arrayBufferHandler, arrayBufferReadLimit);
            weight += nioSelectorProcess.process();
        }
        catch (final Exception ex)
        {
            throw new RuntimeException(ex);
        }

        return weight;
    }

    /**
     * Builder interface for nukleus
     */
    public static class Builder
    {
        private AtomicBuffer ringBuffer;
        private MpscRingBufferReader.ReadHandler ringBufferHandler;
        private MpscArrayBuffer<Object> arrayBuffer;
        private Consumer<Object> arrayBufferHandler;
        private NioSelectorNukleus nioSelectorNukleus;
        private int ringBufferReadLimit;
        private int arrayBufferReadLimit;

        public Builder mpscRingBuffer(final AtomicBuffer buffer,
                                      final MpscRingBufferReader.ReadHandler handler,
                                      final int limit)
        {
            if (null == buffer || null == handler || limit < 1)
            {
                throw new IllegalArgumentException("MpscRingBuffer must not be null and limit must be positive");
            }

            ringBuffer = buffer;
            ringBufferHandler = handler;
            ringBufferReadLimit = limit;
            return this;
        }

        public Builder mpscArrayBuffer(final MpscArrayBuffer<Object> buffer,
                                       final Consumer<Object> handler,
                                       final int limit)
        {
            if (null == buffer || null == handler || limit < 1)
            {
                throw new IllegalArgumentException("MpscArrayBuffer must not be null and limit must be positive");
            }

            arrayBuffer = buffer;
            arrayBufferHandler = handler;
            arrayBufferReadLimit = limit;
            return this;
        }

        public Builder nioSelector(final NioSelectorNukleus nukleus)
        {
            nioSelectorNukleus = nukleus;
            return this;
        }
    }
}
