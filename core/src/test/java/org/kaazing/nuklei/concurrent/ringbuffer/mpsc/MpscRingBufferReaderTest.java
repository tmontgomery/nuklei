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
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test MpscRingBufferReader in isolation
 */
public class MpscRingBufferReaderTest
{
    private static final int MSG_TYPE_ID = 100;
    private static final int CAPACITY = 1024;

    private static final int HEAD_COUNTER_INDEX = CAPACITY + MpscRingBuffer.HEAD_RELATIVE_OFFSET;
    private static final int TAIL_COUNTER_INDEX = CAPACITY + MpscRingBuffer.TAIL_RELATIVE_OFFSET;

    private final AtomicBuffer buffer = mock(AtomicBuffer.class);
    private MpscRingBufferReader reader;

    @Before
    public void setUp()
    {
        when(buffer.capacity()).thenReturn(CAPACITY + MpscRingBuffer.STATE_TRAILER_SIZE);

        reader = new MpscRingBufferReader(buffer);
    }

    @Test
    public void shouldReturnCorrectCapacity()
    {
        assertThat(reader.capacity(), is(CAPACITY));
    }

    @Test
    public void shouldReadNothingWhenEmpty()
    {
        final long head = 0L;
        final long tail = head;

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);

        final RingBufferReader.ReadHandler handler = (typeId, buffer, index, length) -> fail("should not be called");

        assertThat(reader.read(handler, 1), is(0));
    }

    @Test
    public void shouldReadNothingWhenEmptyAndNotAtZero()
    {
        final long head = CAPACITY + MpscRingBuffer.MESSAGE_ALIGNMENT;
        final long tail = head;

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);

        final RingBufferReader.ReadHandler handler = (typeId, buffer, index, length) -> fail("should not be called");

        assertThat(reader.read(handler, Integer.MAX_VALUE), is(0));
    }

    @Test
    public void shouldReadSingleMessageWithAllReadInCorrectMemoryOrder()
    {
        final long head = 0L;
        final long tail = MpscRingBuffer.MESSAGE_ALIGNMENT;
        final int headIndex = (int)head;

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);
        // make sure to have volatile return 0 then return real length second time
        when(buffer.getIntVolatile(headIndex + MpscRingBuffer.HEADER_MSG_LENGTH_OFFSET)).thenReturn(0)
                .thenReturn(MpscRingBuffer.MESSAGE_ALIGNMENT);
        when(buffer.getInt(headIndex + MpscRingBuffer.HEADER_MSG_TYPE_OFFSET)).thenReturn(MSG_TYPE_ID);

        final int[] times = new int[1];
        final RingBufferReader.ReadHandler handler = (typeId, buffer, index, length) -> times[0]++;

        assertThat(reader.read(handler, Integer.MAX_VALUE), is(1));
        assertThat(times[0], is(1));

        final InOrder inOrder = Mockito.inOrder(buffer);
        inOrder.verify(buffer, Mockito.times(2)).getIntVolatile(headIndex + MpscRingBuffer.HEADER_MSG_LENGTH_OFFSET);
        inOrder.verify(buffer).getInt(headIndex + MpscRingBuffer.HEADER_MSG_TYPE_OFFSET);
        inOrder.verify(buffer).setMemory(headIndex, MpscRingBuffer.MESSAGE_ALIGNMENT, (byte)0);
        inOrder.verify(buffer).putLongOrdered(HEAD_COUNTER_INDEX, tail);
    }

    @Test
    public void shouldReadTwoMessages()
    {
        final long head = 0L;
        final long tail = 2 * MpscRingBuffer.MESSAGE_ALIGNMENT;
        final int headIndex = (int)head;

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);
        when(buffer.getIntVolatile(headIndex + MpscRingBuffer.HEADER_MSG_LENGTH_OFFSET))
                .thenReturn(MpscRingBuffer.MESSAGE_ALIGNMENT);
        when(buffer.getIntVolatile(headIndex + MpscRingBuffer.MESSAGE_ALIGNMENT + MpscRingBuffer.HEADER_MSG_LENGTH_OFFSET))
                .thenReturn(MpscRingBuffer.MESSAGE_ALIGNMENT);
        when(buffer.getInt(headIndex + MpscRingBuffer.HEADER_MSG_TYPE_OFFSET)).thenReturn(MSG_TYPE_ID);
        when(buffer.getInt(headIndex + MpscRingBuffer.MESSAGE_ALIGNMENT + MpscRingBuffer.HEADER_MSG_TYPE_OFFSET))
                .thenReturn(MSG_TYPE_ID);

        final int[] times = new int[1];
        final RingBufferReader.ReadHandler handler = (typeId, buffer, index, length) -> times[0]++;

        assertThat(reader.read(handler, Integer.MAX_VALUE), is(2));
        assertThat(times[0], is(2));

        final InOrder inOrder = Mockito.inOrder(buffer);
        inOrder.verify(buffer).setMemory(headIndex, 2 * MpscRingBuffer.MESSAGE_ALIGNMENT, (byte)0);
        inOrder.verify(buffer).putLongOrdered(HEAD_COUNTER_INDEX, tail);
    }

    @Test
    public void shouldEnforceReadLimit()
    {
        final long head = 0L;
        final long tail = 2 * MpscRingBuffer.MESSAGE_ALIGNMENT;
        final int headIndex = (int)head;

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);
        when(buffer.getIntVolatile(headIndex + MpscRingBuffer.HEADER_MSG_LENGTH_OFFSET))
                .thenReturn(MpscRingBuffer.MESSAGE_ALIGNMENT);
        when(buffer.getInt(headIndex + MpscRingBuffer.HEADER_MSG_TYPE_OFFSET)).thenReturn(MSG_TYPE_ID);

        final int[] times = new int[1];
        final RingBufferReader.ReadHandler handler = (typeId, buffer, index, length) -> times[0]++;

        assertThat(reader.read(handler, 1), is(1));
        assertThat(times[0], is(1));

        final InOrder inOrder = Mockito.inOrder(buffer);
        inOrder.verify(buffer).setMemory(headIndex, MpscRingBuffer.MESSAGE_ALIGNMENT, (byte)0);
        inOrder.verify(buffer).putLongOrdered(HEAD_COUNTER_INDEX, head + MpscRingBuffer.MESSAGE_ALIGNMENT);
    }

    @Test
    public void shouldReturnCorrectMessageLengthWithSingleMessage()
    {
        final int messageLength = 16;
        final long head = 0L;
        final long tail = MpscRingBuffer.MESSAGE_ALIGNMENT;
        final int headIndex = (int)head;

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);
        when(buffer.getIntVolatile(headIndex + MpscRingBuffer.HEADER_MSG_LENGTH_OFFSET))
                .thenReturn(messageLength + MpscRingBuffer.HEADER_LENGTH);
        when(buffer.getInt(headIndex + MpscRingBuffer.HEADER_MSG_TYPE_OFFSET)).thenReturn(MSG_TYPE_ID);

        final int[] timesAndLength = new int[2];
        final RingBufferReader.ReadHandler handler = (typeId, buffer, index, length) ->
        {
            timesAndLength[0]++;
            timesAndLength[1] = length;
        };

        assertThat(reader.read(handler, Integer.MAX_VALUE), is(1));
        assertThat(timesAndLength[0], is(1));
        assertThat(timesAndLength[1], is(messageLength));

        final InOrder inOrder = Mockito.inOrder(buffer);
        inOrder.verify(buffer).setMemory(headIndex, MpscRingBuffer.MESSAGE_ALIGNMENT, (byte)0);
        inOrder.verify(buffer).putLongOrdered(HEAD_COUNTER_INDEX, tail);
    }

    @Test
    public void shouldReturnCorrectMessageLengthWithMultipleMessages()
    {
        final int messageLength = 16;
        final long head = 0L;
        final long tail = 2 * MpscRingBuffer.MESSAGE_ALIGNMENT;
        final int headIndex = (int)head;

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);
        when(buffer.getIntVolatile(headIndex + MpscRingBuffer.HEADER_MSG_LENGTH_OFFSET))
                .thenReturn(messageLength + MpscRingBuffer.HEADER_LENGTH);
        when(buffer.getIntVolatile(headIndex + MpscRingBuffer.MESSAGE_ALIGNMENT + MpscRingBuffer.HEADER_MSG_LENGTH_OFFSET))
                .thenReturn(messageLength + MpscRingBuffer.HEADER_LENGTH);
        when(buffer.getInt(headIndex + MpscRingBuffer.HEADER_MSG_TYPE_OFFSET)).thenReturn(MSG_TYPE_ID);
        when(buffer.getInt(headIndex + MpscRingBuffer.MESSAGE_ALIGNMENT + MpscRingBuffer.HEADER_MSG_TYPE_OFFSET))
                .thenReturn(MSG_TYPE_ID);

        final int[] timesAndOffsets = new int[3];
        final RingBufferReader.ReadHandler handler = (typeId, buffer, index, length) ->
        {
            timesAndOffsets[0]++;
            timesAndOffsets[timesAndOffsets[0]] = index;
            assertThat(length, is(messageLength));
        };

        assertThat(reader.read(handler, Integer.MAX_VALUE), is(2));
        assertThat(timesAndOffsets[0], is(2));
        assertThat(timesAndOffsets[1], is(MpscRingBuffer.HEADER_LENGTH));
        assertThat(timesAndOffsets[2], is(MpscRingBuffer.MESSAGE_ALIGNMENT + MpscRingBuffer.HEADER_LENGTH));

        final InOrder inOrder = Mockito.inOrder(buffer);
        inOrder.verify(buffer).setMemory(headIndex, 2 * MpscRingBuffer.MESSAGE_ALIGNMENT, (byte)0);
        inOrder.verify(buffer).putLongOrdered(HEAD_COUNTER_INDEX, tail);
    }

    @Test
    public void shouldHandleExceptionFromHandler()
    {
        final long head = 0L;
        final long tail = 2 * MpscRingBuffer.MESSAGE_ALIGNMENT;
        final int headIndex = (int)head;

        when(buffer.getLongVolatile(HEAD_COUNTER_INDEX)).thenReturn(head);
        when(buffer.getLongVolatile(TAIL_COUNTER_INDEX)).thenReturn(tail);
        when(buffer.getIntVolatile(headIndex + MpscRingBuffer.HEADER_MSG_LENGTH_OFFSET))
                .thenReturn(MpscRingBuffer.MESSAGE_ALIGNMENT);
        when(buffer.getIntVolatile(headIndex + MpscRingBuffer.MESSAGE_ALIGNMENT + MpscRingBuffer.HEADER_MSG_LENGTH_OFFSET))
                .thenReturn(MpscRingBuffer.MESSAGE_ALIGNMENT);
        when(buffer.getInt(headIndex + MpscRingBuffer.HEADER_MSG_TYPE_OFFSET)).thenReturn(MSG_TYPE_ID);
        when(buffer.getInt(headIndex + MpscRingBuffer.MESSAGE_ALIGNMENT + MpscRingBuffer.HEADER_MSG_TYPE_OFFSET))
                .thenReturn(MSG_TYPE_ID);

        final int[] times = new int[1];
        final RingBufferReader.ReadHandler handler = (typeId, buffer, index, length) ->
        {
            if (2 == ++times[0])
            {
                throw new RuntimeException();
            }
        };

        try
        {
            reader.read(handler, Integer.MAX_VALUE);
        }
        catch (final RuntimeException ex)
        {
            assertThat(times[0], is(2));

            final InOrder inOrder = Mockito.inOrder(buffer);
            inOrder.verify(buffer).setMemory(headIndex, 2 * MpscRingBuffer.MESSAGE_ALIGNMENT, (byte)0);
            inOrder.verify(buffer).putLongOrdered(HEAD_COUNTER_INDEX, tail);

            return;
        }

        fail("should not reach here");
    }
}
