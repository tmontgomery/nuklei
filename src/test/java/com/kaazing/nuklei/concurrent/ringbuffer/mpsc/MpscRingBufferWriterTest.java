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
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import static com.kaazing.nuklei.BitUtil.align;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Test MpscRingBufferWriter in isolation
 */
public class MpscRingBufferWriterTest
{
    private static final int MSG_TYPE_ID = 100;
    private static final int CAPACITY = 1024;

    private static final int HEAD_COUNTER_INDEX = CAPACITY + MpscRingBuffer.HEAD_RELATIVE_OFFSET;
    private static final int TAIL_COUNTER_INDEX = CAPACITY + MpscRingBuffer.TAIL_RELATIVE_OFFSET;

    private final AtomicBuffer srcBuffer = new AtomicBuffer(new byte[1024]);

    private final AtomicBuffer buffer = mock(AtomicBuffer.class);
    private MpscRingBufferWriter writer;

    @Before
    public void setUp()
    {
        when(buffer.capacity()).thenReturn(CAPACITY + MpscRingBuffer.STATE_TRAILER_SIZE);

        writer = new MpscRingBufferWriter(buffer);
    }

    @Test
    public void shouldReturnCorrectCapacity()
    {
        assertThat(writer.capacity(), is(CAPACITY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenCapacityNotPowerOfTwo()
    {
        when(buffer.capacity()).thenReturn(CAPACITY - 1 + MpscRingBuffer.STATE_TRAILER_SIZE);

        new MpscRingBufferWriter(buffer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenWritingWithWrongMessageTypeId()
    {
        writer.write(MpscRingBuffer.PADDING_MSG_TYPE_ID, srcBuffer, 0, srcBuffer.capacity());
    }

    @Test
    public void shouldNotWriteWhenAlreadyFull()
    {
        final int lengthToWrite = 16;
        final long head = 0L;
        final long tail = head + CAPACITY;

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);

        assertFalse(writer.write(MSG_TYPE_ID, srcBuffer, 0, lengthToWrite));

        verify(buffer, never()).putInt(anyInt(), anyInt());
        verify(buffer, never()).compareAndSwapLong(anyInt(), anyLong(), anyLong());
        verify(buffer, never()).putIntOrdered(anyInt(), anyInt());
    }

    @Test
    public void shouldNotWriteWhenCapacityNotAvailable()
    {
        final int lengthToWrite = 65;
        final long head = 0L;
        final long tail = head + (CAPACITY - align(lengthToWrite - MpscRingBuffer.MESSAGE_ALIGNMENT,
                MpscRingBuffer.MESSAGE_ALIGNMENT));

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);

        assertFalse(writer.write(MSG_TYPE_ID, srcBuffer, 0, lengthToWrite));

        verify(buffer, never()).putInt(anyInt(), anyInt());
        verify(buffer, never()).compareAndSwapLong(anyInt(), anyLong(), anyLong());
        verify(buffer, never()).putIntOrdered(anyInt(), anyInt());
    }

    @Test
    public void shouldNotWriteWhenCapacityNotAvailableAfterWrap()
    {
        final int lengthToWrite = 65;
        final long head = CAPACITY + 1;
        final long tail = head + (CAPACITY - MpscRingBuffer.MESSAGE_ALIGNMENT);

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);

        assertFalse(writer.write(MSG_TYPE_ID, srcBuffer, 0, lengthToWrite));

        verify(buffer, never()).putInt(anyInt(), anyInt());
        verify(buffer, never()).compareAndSwapLong(anyInt(), anyLong(), anyLong());
        verify(buffer, never()).putIntOrdered(anyInt(), anyInt());
    }

    @Test
    public void shouldWriteToEmptyBufferCorrectly()
    {
        final long tail = 0L;
        final long head = 0L;
        final int lengthToWrite = 16;
        final int alignedMessageLength = align(lengthToWrite + MpscRingBuffer.HEADER_LENGTH,
                MpscRingBuffer.MESSAGE_ALIGNMENT);

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);
        when(buffer.compareAndSwapLong(TAIL_COUNTER_INDEX, tail, tail + alignedMessageLength)).thenReturn(true);

        assertTrue(writer.write(MSG_TYPE_ID, srcBuffer, 0, lengthToWrite));

        final InOrder inOrder = Mockito.inOrder(buffer);

        inOrder.verify(buffer).compareAndSwapLong(TAIL_COUNTER_INDEX, tail, tail + alignedMessageLength);
        inOrder.verify(buffer).putInt((int)tail + MpscRingBuffer.HEADER_MSG_TYPE_OFFSET, MSG_TYPE_ID);
        inOrder.verify(buffer).putBytes((int)tail + MpscRingBuffer.HEADER_LENGTH, srcBuffer, 0, lengthToWrite);
        inOrder.verify(buffer).putIntOrdered((int)tail + MpscRingBuffer.HEADER_MSG_LENGTH_OFFSET,
                lengthToWrite + MpscRingBuffer.HEADER_LENGTH);
    }

    @Test
    public void shouldWritePaddingThenMessageOnWrap()
    {
        final int lengthToWrite = 65;
        final int alignedMessageLength = align(lengthToWrite + MpscRingBuffer.HEADER_LENGTH,
                MpscRingBuffer.MESSAGE_ALIGNMENT);
        final long tail = CAPACITY - MpscRingBuffer.MESSAGE_ALIGNMENT;
        final long head = tail - (MpscRingBuffer.MESSAGE_ALIGNMENT);

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);
        when(buffer.compareAndSwapLong(TAIL_COUNTER_INDEX, tail, tail + alignedMessageLength +
                MpscRingBuffer.MESSAGE_ALIGNMENT)).thenReturn(true);

        assertTrue(writer.write(MSG_TYPE_ID, srcBuffer, 0, lengthToWrite));

        final InOrder inOrder = Mockito.inOrder(buffer);

        inOrder.verify(buffer).compareAndSwapLong(TAIL_COUNTER_INDEX, tail,
                tail + alignedMessageLength + MpscRingBuffer.MESSAGE_ALIGNMENT);

        inOrder.verify(buffer).putInt((int)tail + MpscRingBuffer.HEADER_MSG_TYPE_OFFSET,
                MpscRingBuffer.PADDING_MSG_TYPE_ID);
        inOrder.verify(buffer).putIntOrdered((int) tail + MpscRingBuffer.HEADER_MSG_LENGTH_OFFSET,
                MpscRingBuffer.MESSAGE_ALIGNMENT);

        inOrder.verify(buffer).putInt(0 + MpscRingBuffer.HEADER_MSG_TYPE_OFFSET, MSG_TYPE_ID);
        inOrder.verify(buffer).putBytes(0 + MpscRingBuffer.HEADER_LENGTH, srcBuffer, 0, lengthToWrite);
        inOrder.verify(buffer).putIntOrdered(0 + MpscRingBuffer.HEADER_MSG_LENGTH_OFFSET,
                lengthToWrite + MpscRingBuffer.HEADER_LENGTH);
    }

    @Test
    public void shouldWritePaddingThenMessageOnWrapEvenIfEmpty()
    {
        final int lengthToWrite = 65;
        final int alignedMessageLength = align(lengthToWrite + MpscRingBuffer.HEADER_LENGTH,
                MpscRingBuffer.MESSAGE_ALIGNMENT);
        final long tail = CAPACITY - MpscRingBuffer.MESSAGE_ALIGNMENT;
        final long head = tail;

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);
        when(buffer.compareAndSwapLong(TAIL_COUNTER_INDEX, tail, tail + alignedMessageLength +
                MpscRingBuffer.MESSAGE_ALIGNMENT)).thenReturn(true);

        assertTrue(writer.write(MSG_TYPE_ID, srcBuffer, 0, lengthToWrite));

        final InOrder inOrder = Mockito.inOrder(buffer);

        inOrder.verify(buffer).compareAndSwapLong(TAIL_COUNTER_INDEX, tail,
                tail + alignedMessageLength + MpscRingBuffer.MESSAGE_ALIGNMENT);

        inOrder.verify(buffer).putInt((int)tail + MpscRingBuffer.HEADER_MSG_TYPE_OFFSET,
                MpscRingBuffer.PADDING_MSG_TYPE_ID);
        inOrder.verify(buffer).putIntOrdered((int)tail + MpscRingBuffer.HEADER_MSG_LENGTH_OFFSET,
                MpscRingBuffer.MESSAGE_ALIGNMENT);

        inOrder.verify(buffer).putInt(0 + MpscRingBuffer.HEADER_MSG_TYPE_OFFSET, MSG_TYPE_ID);
        inOrder.verify(buffer).putBytes(0 + MpscRingBuffer.HEADER_LENGTH, srcBuffer, 0, lengthToWrite);
        inOrder.verify(buffer).putIntOrdered(0 + MpscRingBuffer.HEADER_MSG_LENGTH_OFFSET,
                lengthToWrite + MpscRingBuffer.HEADER_LENGTH);
    }

}
