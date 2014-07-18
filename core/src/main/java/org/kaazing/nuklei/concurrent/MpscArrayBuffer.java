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
package org.kaazing.nuklei.concurrent;

import org.kaazing.nuklei.BitUtil;
import sun.misc.Unsafe;

import java.util.function.Consumer;

/*
 * Padding is to 64-bit cache lines, but might need to be 128-bit (uncomment additional padding)
 */
class Padding1
{
    protected long p1, p2, p3, p4, p5, p6, p7;
    //protected long p101, p102, p103, p104, p105, p106, p107;
}

class Tail extends Padding1
{
    protected volatile long tailCounter;
}

class Padding2 extends Tail
{
    protected long p8, p9, p10, p11, p12, p13, p14;
    //protected long p108, p109, p110, p111, p112, p113, p114;
}

class Head extends Padding2
{
    protected volatile long headCounter;
}

class Padding3 extends Head
{
    protected long p15, p16, p17, p18, p19, p20, p21;
    //protected long p115, p116, p117, p118, p119, p120, p121;
}

class IdCounter extends Padding3
{
    protected volatile long idCounter;
}

class Padding4 extends IdCounter
{
    protected long p22, p23, p24, p25, p26, p27, p28;
    //protected long p122, p123, p124, p125, p126, p127, p128;
}

class HeadCache extends Padding4
{
    protected volatile long headCacheCounter;
}

class Padding5 extends HeadCache
{
    protected long p29, p30, p31, p32, p33, p34, p35;
    //protected long p129, p130, p131, p132, p133, p134, p135;
}

/**
 * Multi-Producer, Single Consumer array buffer providing message passing semantics of types.
 *
 * Approach inspired by JCTools (https://github.com/JCTools/JCTools) MpscArrayQueue
 */
public class MpscArrayBuffer<E> extends Padding5
{
    private static final Unsafe UNSAFE = BitUtil.UNSAFE;
    private static final long TAIL_COUNTER_OFFSET;
    private static final long HEAD_COUNTER_OFFSET;
    private static final long ID_COUNTER_OFFSET;
    private static final long HEAD_CACHE_COUNTER_OFFSET;
    private static final int ARRAY_BASE;
    private static final int MESSAGE_SHIFT;

    private static final int INSUFFICIENT_CAPACITY = -1;

    private final E[] messages;
    private final int mask;
    private final int capacity;

    static
    {
        try
        {
            TAIL_COUNTER_OFFSET = UNSAFE.objectFieldOffset(Tail.class.getDeclaredField("tailCounter"));
            HEAD_COUNTER_OFFSET = UNSAFE.objectFieldOffset(Head.class.getDeclaredField("headCounter"));
            ID_COUNTER_OFFSET = UNSAFE.objectFieldOffset(IdCounter.class.getDeclaredField("idCounter"));
            HEAD_CACHE_COUNTER_OFFSET = UNSAFE.objectFieldOffset(HeadCache.class.getDeclaredField("headCacheCounter"));
            ARRAY_BASE = UNSAFE.arrayBaseOffset(Object[].class);
            MESSAGE_SHIFT = calculateAndCheckShiftForScale(UNSAFE.arrayIndexScale(Object[].class));
        }
        catch (final Exception ex)
        {
            throw new RuntimeException(ex);
        }

    }

    /**
     * Initialize buffer with the given capacity in messages.
     *
     * @param capacity of the buffer in messages
     */
    @SuppressWarnings("unchecked")
    public MpscArrayBuffer(final int capacity)
    {
        checkCapacity(capacity);

        this.messages = (E[])new Object[capacity];
        this.mask = capacity - 1;
        this.capacity = capacity;
    }

    /**
     * Return capacity of the buffer in messages.
     *
     * @return capacity of buffer
     */
    public int capacity()
    {
        return capacity;
    }

    /**
     * Write a given message to the buffer.
     *
     * @param message to write into the buffer
     * @return whether write was successful or not. If not successful, should be retried.
     */
    public boolean write(final E message)
    {
        checkMessage(message);

        final int messageIndex = claim();  // claim slot

        if (INSUFFICIENT_CAPACITY == messageIndex)
        {
            return false;
        }

        final long offset = calculateMessageOffset(messageIndex);
        putMessageOrdered(offset, message);

        return true;
    }

    /**
     * Read pending messages from the buffer up to a limit of number of messages. Does not block.
     *
     * @param handler to call for all read messages
     * @param limit to impose on the number of read messages
     * @return number of messages read
     */
    public int read(final Consumer<E> handler, final int limit)
    {
        final long tail = tailVolatile();
        final long head = headVolatile();
        final E[] buffer = messages;
        long currentHead = head;
        int messagesRead = 0;

        try
        {
            while ((currentHead < tail) && (messagesRead < limit))
            {
                final long offset = calculateMessageOffset((int)currentHead & mask);
                final E message = objectVolatile(buffer, offset);

                // tail could have been visible before object is, so treat it as the end
                if (null == message)
                {
                    break;
                }

                ++messagesRead;
                putMessageOrdered(buffer, offset, null);
                handler.accept(message);
                ++currentHead;
            }
        }
        finally
        {
            putHeadOrdered(currentHead);
        }

        return messagesRead;
    }

    /**
     * Generate and return an ID that is unique between participants.
     *
     * @return id
     */
    public long nextId()
    {
        return UNSAFE.getAndAddLong(this, ID_COUNTER_OFFSET, 1);
    }

    private int claim()
    {
        long head = headCacheVolatile();
        long tail;
        do
        {
            tail = tailVolatile();

            final long wrapPoint = tail - capacity;

            if (head <= wrapPoint)
            {
                final long currentHead = headVolatile();

                if (currentHead <= wrapPoint)
                {
                    return INSUFFICIENT_CAPACITY;
                }

                putHeadCacheOrdered(currentHead);
                head = currentHead;
            }
        }
        while (!UNSAFE.compareAndSwapLong(this, TAIL_COUNTER_OFFSET, tail, tail + 1));

        return ((int)tail & mask);
    }

    private long headVolatile()
    {
        return headCounter;
    }

    private long headCacheVolatile()
    {
        return headCacheCounter;
    }

    private long tailVolatile()
    {
        return tailCounter;
    }

    private void putHeadCacheOrdered(final long value)
    {
        UNSAFE.putOrderedLong(this, HEAD_CACHE_COUNTER_OFFSET, value);
    }

    private void putHeadOrdered(final long value)
    {
        UNSAFE.putOrderedLong(this, HEAD_COUNTER_OFFSET, value);
    }

    private void putMessageOrdered(final long offset, final E message)
    {
        UNSAFE.putOrderedObject(messages, offset, message);
    }

    private void putMessageOrdered(final E[] buffer, final long offset, final E message)
    {
        UNSAFE.putOrderedObject(buffer, offset, message);
    }

    private long calculateMessageOffset(final int index)
    {
        return ARRAY_BASE + ((long)index << MESSAGE_SHIFT);
    }

    @SuppressWarnings("unchecked")
    private E objectVolatile(final E[] buffer, final long offset)
    {
        return (E)UNSAFE.getObjectVolatile(buffer, offset);
    }

    private static int calculateAndCheckShiftForScale(final int scale)
    {
        if (4 == scale)
        {
            return 2;
        }
        else if (8 == scale)
        {
            return 3;
        }
        else
        {
            throw new IllegalStateException("unknown pointer size");
        }
    }

    private static void checkCapacity(final int capacity)
    {
        if (capacity < 2 || Integer.bitCount(capacity) > 1)
        {
            final String msg = String.format("buffer capacity is %d, but must be power of 2", capacity);

            throw new IllegalArgumentException(msg);
        }
    }

    private static<E> void checkMessage(final E message)
    {
        if (null == message)
        {
            throw new IllegalArgumentException("message must not be null");
        }
    }
}
