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
package org.kaazing.nuklei.concurrent.ringbuffer.mpsc;

import org.kaazing.nuklei.concurrent.AtomicBuffer;
import org.kaazing.nuklei.concurrent.ringbuffer.RingBufferIdGenerator;

/**
 * MPSC Ring Buffer ID generator
 */
public class MpscRingBufferIdGenerator implements RingBufferIdGenerator
{
    private final AtomicBuffer buffer;
    private final int idCounterIndex;
    private final int capacity;

    /**
     * Initialize ring buffer id generator with underling ring buffer in the {@link AtomicBuffer}
     *
     * @param buffer to use as the underlying ring buffer.
     */
    public MpscRingBufferIdGenerator(final AtomicBuffer buffer)
    {
        MpscRingBuffer.checkAtomicBufferCapacity(buffer);

        this.buffer = buffer;
        this.capacity = buffer.capacity() - MpscRingBuffer.STATE_TRAILER_SIZE;
        this.idCounterIndex = capacity + MpscRingBuffer.ID_RELATIVE_OFFSET;
    }

    /** {@inheritDoc} */
    public long nextId()
    {
        return buffer.getAndAddLong(idCounterIndex, 1);
    }
}
