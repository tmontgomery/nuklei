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

import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Test MpscArrayBuffer in isolation
 */
@RunWith(Theories.class)
public class MpscArrayBufferTest
{
    @DataPoint
    public static final int CAPACITY_2 = 2;

    @DataPoint
    public static final int CAPACITY_4 = 4;

    @DataPoint
    public static final int CAPACITY_8 = 8;

    private MpscArrayBuffer<Integer> buffer;

    @Theory
    public void shouldReturnCorrectCapacity(final int capacity)
    {
        buffer = new MpscArrayBuffer<>(capacity);

        assertThat(buffer.capacity(), is(capacity));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenCapacityNotPowerOfTwo()
    {
        buffer = new MpscArrayBuffer<>(7);
    }

    @Theory
    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenMessageIsNull(final int capacity)
    {
        buffer = new MpscArrayBuffer<>(capacity);

        buffer.write(null);
    }

    @Theory
    public void shouldNotWriteWhenAlreadyFull(final int capacity)
    {
        buffer = new MpscArrayBuffer<>(capacity);

        IntStream.range(0, capacity).forEach((i) -> assertTrue(buffer.write(i)));

        assertFalse(buffer.write(capacity));
    }

    @Theory
    public void shouldNotWriteWhenAlreadyFullAndNotAtZero(final int capacity)
    {
        buffer = new MpscArrayBuffer<>(capacity);

        assertTrue(buffer.write(-1));

        IntStream.range(0, capacity - 1).forEach((i) -> assertTrue(buffer.write(i)));

        assertFalse(buffer.write(capacity - 1));
    }

    @Theory
    public void shouldNotWriteWhenCapacityNotAvailableAfterWrap(final int capacity)
    {
        buffer = new MpscArrayBuffer<>(capacity);

        final Consumer<Integer> handler = (i) -> {};

        IntStream.range(0, capacity).forEach((i) -> assertTrue(buffer.write(i)));

        assertThat(buffer.read(handler, Integer.MAX_VALUE), is(capacity));

        IntStream.range(0, capacity).forEach((i) -> assertTrue(buffer.write(i)));

        assertFalse(buffer.write(capacity));
    }

    @Theory
    public void shouldWriteAndReadToEmptyBuffer(final int capacity)
    {
        buffer = new MpscArrayBuffer<>(capacity);

        assertTrue(buffer.write(1));

        final Consumer<Integer> handler = (i) -> assertThat(i, is(1));

        assertThat(buffer.read(handler, Integer.MAX_VALUE), is(1));
    }

    @Theory
    public void shouldWriteAndReadMultipleMessages(final int capacity)
    {
        buffer = new MpscArrayBuffer<>(capacity);

        IntStream.range(0, 2).forEach((i) -> assertTrue(buffer.write(i)));

        final int[] times = new int[1];
        final Consumer<Integer> handler = (i) ->
        {
            assertThat(times[0], is(i));
            ++times[0];
        };

        assertThat(buffer.read(handler, Integer.MAX_VALUE), is(2));
        assertThat(times[0], is(2));
    }

    @Theory
    public void shouldWriteAndReadMultipleMessagesOnWrap(final int capacity)
    {
        buffer = new MpscArrayBuffer<>(capacity);

        final Consumer<Integer> handler = (i) -> {};

        IntStream.range(0, capacity).forEach((i) -> assertTrue(buffer.write(i)));

        assertThat(buffer.read(handler, Integer.MAX_VALUE), is(capacity));

        IntStream.range(0, capacity).forEach((i) -> assertTrue(buffer.write(i)));

        assertThat(buffer.read(handler, Integer.MAX_VALUE), is(capacity));
    }

    @Theory
    public void shouldReadNothingWhenEmpty(final int capacity)
    {
        buffer = new MpscArrayBuffer<>(capacity);

        final Consumer<Integer> handler = (i) -> fail("should not be called");

        assertThat(buffer.read(handler, 1), is(0));
    }

    @Theory
    public void shouldReadNothingWhenEmptyAndNotAtZero(final int capacity)
    {
        buffer = new MpscArrayBuffer<>(capacity);

        IntStream.range(0, 2).forEach((i) -> assertTrue(buffer.write(i)));

        final Consumer<Integer> noOp = (i) -> {};

        assertThat(buffer.read(noOp, Integer.MAX_VALUE), is(2));

        final Consumer<Integer> handler = (i) -> fail("should not be called");

        assertThat(buffer.read(handler, Integer.MAX_VALUE), is(0));
    }

    @Theory
    public void shouldEnforceReadLimit(final int capacity)
    {
        buffer = new MpscArrayBuffer<>(capacity);

        IntStream.range(0, capacity).forEach((i) -> assertTrue(buffer.write(i)));

        final int[] times = new int[1];
        final Consumer<Integer> handler = (i) -> ++times[0];

        assertThat(buffer.read(handler, capacity - 1), is(capacity - 1));
        assertThat(times[0], is(capacity - 1));
    }

    @Theory
    public void shouldHandleExceptionFromHandler(final int capacity)
    {
        buffer = new MpscArrayBuffer<>(capacity);

        assertTrue(buffer.write(1));
        assertTrue(buffer.write(2));

        final int[] times = new int[1];
        final Consumer<Integer> handler = (i) ->
        {
            if (2 == ++times[0])
            {
                throw new RuntimeException();
            }
        };

        try
        {
            buffer.read(handler, Integer.MAX_VALUE);
        }
        catch (final RuntimeException ex)
        {
            assertThat(times[0], is(2));
            return;
        }

        fail("should not reach here");
    }
}
