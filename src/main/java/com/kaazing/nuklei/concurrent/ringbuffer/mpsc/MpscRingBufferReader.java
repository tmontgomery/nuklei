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
import com.kaazing.nuklei.concurrent.ringbuffer.RingBufferReader;

import static com.kaazing.nuklei.BitUtil.align;

/**
 * Multiple Publisher, Single Consumer Ring Buffer Reader
 */
public class MpscRingBufferReader implements RingBufferReader
{
    private final AtomicBuffer buffer;
    private final int mask;
    private final int tailCounterIndex;
    private final int headCounterIndex;
    private final int capacity;

    /**
     * Initialize ring buffer reader with underling ring buffer in the {@link AtomicBuffer}
     *
     * @param buffer to use as the underlying ring buffer.
     */
    public MpscRingBufferReader(final AtomicBuffer buffer)
    {
        MpscRingBuffer.checkAtomicBufferCapacity(buffer);

        this.buffer = buffer;
        this.capacity = buffer.capacity() - MpscRingBuffer.STATE_TRAILER_SIZE;
        this.mask = capacity - 1;
        this.tailCounterIndex = capacity + MpscRingBuffer.TAIL_RELATIVE_OFFSET;
        this.headCounterIndex = capacity + MpscRingBuffer.HEAD_RELATIVE_OFFSET;
    }

    /**
     * Return capacity of ring buffer in bytes.
     *
     * @return capacity of ring buffer
     */
    public int capacity()
    {
        return capacity;
    }

    /** {@inheritDoc} */
    public int read(final ReadHandler handler, final int limit)
    {
        final long tail = getTailVolatile();
        final long head = getHeadVolatile();
        final int available = (int)(tail - head);
        int messagesRead = 0;

        if (available > 0)
        {
            final int headIndex = (int)head & mask;
            final int contiguousBlockSize = Math.min(available, capacity - headIndex);
            int bytesRead = 0;

            try
            {
                while ((bytesRead < contiguousBlockSize) && (messagesRead < limit))
                {
                    final int messageIndex = headIndex + bytesRead;
                    final int messageLength = waitForMsgLengthVolatile(messageIndex);

                    final int msgTypeId = readMsgTypeId(messageIndex);

                    bytesRead += align(messageLength, MpscRingBuffer.MESSAGE_ALIGNMENT);

                    if (MpscRingBuffer.PADDING_MSG_TYPE_ID != msgTypeId)
                    {
                        ++messagesRead;
                        handler.onMessage(msgTypeId, buffer, messageIndex + MpscRingBuffer.HEADER_LENGTH,
                                messageLength - MpscRingBuffer.HEADER_LENGTH);
                    }
                }
            }
            finally
            {
                buffer.setMemory(headIndex, bytesRead, (byte) 0);
                putHeadOrdered(head + bytesRead);
            }
        }

        return messagesRead;
    }

    private long getHeadVolatile()
    {
        return buffer.getLongVolatile(headCounterIndex);
    }

    private long getTailVolatile()
    {
        return buffer.getLongVolatile(tailCounterIndex);
    }

    private int waitForMsgLengthVolatile(final int messageIndex)
    {
        int length;

        do
        {
            length = buffer.getIntVolatile(messageIndex + MpscRingBuffer.HEADER_MSG_LENGTH_OFFSET);
        }
        while (0 == length);

        return length;
    }

    private int readMsgTypeId(final int messageIndex)
    {
        return buffer.getInt(messageIndex + MpscRingBuffer.HEADER_MSG_TYPE_OFFSET);
    }

    private void putHeadOrdered(final long value)
    {
        buffer.putLongOrdered(headCounterIndex, value);
    }
}
