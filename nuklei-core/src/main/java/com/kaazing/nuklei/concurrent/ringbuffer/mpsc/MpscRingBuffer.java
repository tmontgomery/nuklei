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

/**
 * Multiple Publisher, Single Consumer (MPSC) Ring Buffer constants and values shared by Readers, Writers, and Spies
 */
public class MpscRingBuffer
{
    /*
     * Trailer houses head and tail for the ring buffer, padded to cache lines to avoid false sharing
     *
     * Layout
     * HEAD (long) = 8 bytes (padded to CACHE_LINE_SIZE)
     * TAIL (long) = 8 bytes (padded to CACHE_LINE_SIZE)
     * ID (long) = 8 bytes (padded to CACHE_LINE_SIZE)
     */
    public static final int TAIL_RELATIVE_OFFSET = 0;
    public static final int HEAD_RELATIVE_OFFSET = BitUtil.CACHE_LINE_SIZE;
    public static final int ID_RELATIVE_OFFSET = 2 * BitUtil.CACHE_LINE_SIZE;
    public static final int STATE_TRAILER_SIZE = 3 * BitUtil.CACHE_LINE_SIZE;

    /* padding message */
    public static final int PADDING_MSG_TYPE_ID = -1;

    /* alignment for each message */
    public static final int MESSAGE_ALIGNMENT = BitUtil.CACHE_LINE_SIZE;

    /*
     * Message Header
     *
     * Message Length (int) = 4 bytes (includes Header Length)
     * Message Type (int) = 4 bytes
     * Sequence Number (long) = 8 bytes
     */
    public static final int HEADER_MSG_LENGTH_OFFSET = 0;
    public static final int HEADER_MSG_TYPE_OFFSET = BitUtil.SIZE_OF_INT;
    public static final int HEADER_MSG_SEQNUM_OFFSET = BitUtil.SIZE_OF_INT;
    public static final int HEADER_LENGTH = HEADER_MSG_SEQNUM_OFFSET + BitUtil.SIZE_OF_LONG;

    public static void checkAtomicBufferCapacity(final AtomicBuffer buffer)
    {
        final int capacity = buffer.capacity() - STATE_TRAILER_SIZE;

        if (capacity < 2 || Integer.bitCount(capacity) > 1)
        {
            final String msg = String.format("buffer capacity is %d, but must be power of 2 + STATE_TRAILER_SIZE",
                    capacity);

            throw new IllegalArgumentException(msg);
        }
    }

    public static void checkMessageTypeId(final int typeId)
    {
        if (MpscRingBuffer.PADDING_MSG_TYPE_ID == typeId)
        {
            final String msg = String.format("typeId of %d is not allowed", typeId);

            throw new IllegalArgumentException(msg);
        }
    }
}
