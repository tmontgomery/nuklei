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
import org.kaazing.nuklei.concurrent.ringbuffer.RingBufferReader;

import static org.kaazing.nuklei.BitUtil.align;

/**
 * Multiple Publisher, Single Consumer Ring Buffer Reader
 */
public class MpscRingBufferReader implements RingBufferReader
{
    private final AtomicBuffer buffer;
    private final int mask;
    private final int tailCounterOffset;
    private final int headCounterOffset;
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
        this.tailCounterOffset = capacity + MpscRingBuffer.TAIL_RELATIVE_OFFSET;
        this.headCounterOffset = capacity + MpscRingBuffer.HEAD_RELATIVE_OFFSET;
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
        final long tail = tailVolatile();
        final long head = headVolatile();
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

    private long headVolatile()
    {
        return buffer.getLongVolatile(headCounterOffset);
    }

    private long tailVolatile()
    {
        return buffer.getLongVolatile(tailCounterOffset);
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
        buffer.putLongOrdered(headCounterOffset, value);
    }
}
