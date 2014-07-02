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

import com.kaazing.nuklei.concurrent.AtomicBuffer;
import com.kaazing.nuklei.concurrent.ringbuffer.RingBufferSpy;

/**
 * Multiple Publisher, Single Consumer Ring Buffer Spy
 */
public class MpscRingBufferSpy implements RingBufferSpy
{
    private final AtomicBuffer buffer;
    private final long head;
    private final int headCounterIndex;
    private final int capacity;
    private final int mask;

    /**
     * Initialize ring buffer spy with underling ring buffer in the {@link AtomicBuffer}
     *
     * @param buffer to use as the underlying buffer.
     */
    public MpscRingBufferSpy(final AtomicBuffer buffer)
    {
        MpscRingBuffer.checkAtomicBufferCapacity(buffer);

        this.buffer = buffer;
        this.capacity = buffer.capacity() - MpscRingBuffer.STATE_TRAILER_SIZE;
        this.mask = capacity - 1;
        this.headCounterIndex = capacity + MpscRingBuffer.HEAD_RELATIVE_OFFSET;

        this.head = getHeadVolatile();
    }

    /** {@inheritDoc} */
    public int spy(final SpyHandler handler, final AtomicBuffer buffer, final int limit)
    {
        return 0;
    }

    private long getHeadVolatile()
    {
        return buffer.getLongVolatile(headCounterIndex);
    }
}
