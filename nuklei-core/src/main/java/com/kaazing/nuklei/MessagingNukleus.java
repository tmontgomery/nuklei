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
package com.kaazing.nuklei;

import com.kaazing.nuklei.concurrent.AtomicBuffer;
import com.kaazing.nuklei.concurrent.MpscArrayBuffer;
import com.kaazing.nuklei.concurrent.ringbuffer.mpsc.MpscRingBufferReader;

import java.util.function.Consumer;

/**
 * Nukleus that reads and processes messages
 */
public class MessagingNukleus implements Nukleus
{
    private final MpscRingBufferReader ringBufferReader;
    private final MpscRingBufferReader.ReadHandler ringBufferHandler;
    private final MpscArrayBuffer<Object> arrayBuffer;
    private final Consumer<Object> arrayBufferHandler;
    private final NioSelectorNukleus nioSelectorNukleus;
    private final int ringBufferReadLimit;
    private final int arrayBufferReadLimit;

    public MessagingNukleus(final Builder builder)
    {
        if (null == builder.ringBuffer && null == builder.arrayBuffer && null == builder.nioSelectorNukleus)
        {
            throw new IllegalArgumentException("must specify either RingBuffer, ArrayBuffer, and/or NioSelector for Nukleus");
        }

        this.ringBufferReader = (null != builder.ringBuffer) ? new MpscRingBufferReader(builder.ringBuffer) : null;
        this.arrayBuffer = builder.arrayBuffer;
        this.ringBufferHandler = builder.ringBufferHandler;
        this.arrayBufferHandler = builder.arrayBufferHandler;
        this.ringBufferReadLimit = builder.ringBufferReadLimit;
        this.arrayBufferReadLimit = builder.arrayBufferReadLimit;
        this.nioSelectorNukleus = builder.nioSelectorNukleus;
    }

    /** {@inheritDoc} */
    public int process()
    {
        int weight = 0;

        try
        {
            // TODO: instead of checks, use noop objects (return 0 weight) when these are not defined.

            if (null != ringBufferReader)
            {
                weight += ringBufferReader.read(ringBufferHandler, ringBufferReadLimit);
            }

            if (null != arrayBuffer)
            {
                weight += arrayBuffer.read(arrayBufferHandler, arrayBufferReadLimit);
            }

            if (null != nioSelectorNukleus)
            {
                weight += nioSelectorNukleus.process();
            }
        }
        catch (final Exception ex)
        {
            throw new RuntimeException(ex);
        }

        return weight;
    }


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
