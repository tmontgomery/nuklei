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
package org.kaazing.nuklei;

import org.kaazing.nuklei.BitUtil;
import org.kaazing.nuklei.Flyweight;
import org.kaazing.nuklei.concurrent.AtomicBuffer;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FlyweightTest
{
    public final static long UINT32_VALUE = 0xdeadbeefL;
    public final static int UINT16_VALUE = 0xceec;
    public final static short UINT8_VALUE = 0xcc;
    public final static byte[] BIG_ENDIAN_BYTE =    { (byte)0xde, (byte)0xad, (byte)0xbe, (byte)0xef,
                                                      (byte)0xce, (byte)0xec, (byte)0xcc };
    public final static byte[] LITTLE_ENDIAN_BYTE = { (byte)0xef, (byte)0xbe, (byte)0xad, (byte)0xde,
                                                      (byte)0xec, (byte)0xce, (byte)0xcc };
    public final static int UINT32_OFFSET = 0;
    public final static int UINT16_OFFSET = BitUtil.SIZE_OF_INT;
    public final static int UINT8_OFFSET = BitUtil.SIZE_OF_INT + BitUtil.SIZE_OF_SHORT;

    private final ByteBuffer buffer = ByteBuffer.allocateDirect(256);
    private final AtomicBuffer aBuff = new AtomicBuffer(buffer);
    private final ByteBuffer viewBuffer = buffer.duplicate();
    private final Flyweight flyweightBigEndian = new Flyweight(ByteOrder.BIG_ENDIAN);
    private final Flyweight flyweightLittleEndian = new Flyweight(ByteOrder.LITTLE_ENDIAN);

    @Test
    public void shouldEncodeUIntBigEndianCorrectly()
    {
        final Flyweight flyweight = flyweightBigEndian;

        flyweight.reset(aBuff, 0);
        Flyweight.uint32Put(aBuff, UINT32_OFFSET, UINT32_VALUE, ByteOrder.BIG_ENDIAN);
        Flyweight.uint16Put(aBuff, UINT16_OFFSET, UINT16_VALUE, ByteOrder.BIG_ENDIAN);
        Flyweight.uint8Put(aBuff, UINT8_OFFSET, UINT8_VALUE);

        assertThat(Byte.valueOf(viewBuffer.get(0)), is(Byte.valueOf(BIG_ENDIAN_BYTE[0])));
        assertThat(Byte.valueOf(viewBuffer.get(1)), is(Byte.valueOf(BIG_ENDIAN_BYTE[1])));
        assertThat(Byte.valueOf(viewBuffer.get(2)), is(Byte.valueOf(BIG_ENDIAN_BYTE[2])));
        assertThat(Byte.valueOf(viewBuffer.get(3)), is(Byte.valueOf(BIG_ENDIAN_BYTE[3])));
        assertThat(Byte.valueOf(viewBuffer.get(4)), is(Byte.valueOf(BIG_ENDIAN_BYTE[4])));
        assertThat(Byte.valueOf(viewBuffer.get(5)), is(Byte.valueOf(BIG_ENDIAN_BYTE[5])));
        assertThat(Byte.valueOf(viewBuffer.get(6)), is(Byte.valueOf(BIG_ENDIAN_BYTE[6])));
    }

    @Test
    public void shouldEncodeUIntLittleEndianCorrectly()
    {
        final Flyweight flyweight = flyweightLittleEndian;

        flyweight.reset(aBuff, 0);
        Flyweight.uint32Put(aBuff, UINT32_OFFSET, UINT32_VALUE, ByteOrder.LITTLE_ENDIAN);
        Flyweight.uint16Put(aBuff, UINT16_OFFSET, UINT16_VALUE, ByteOrder.LITTLE_ENDIAN);
        Flyweight.uint8Put(aBuff, UINT8_OFFSET, UINT8_VALUE);

        assertThat(Byte.valueOf(viewBuffer.get(0)), is(Byte.valueOf(LITTLE_ENDIAN_BYTE[0])));
        assertThat(Byte.valueOf(viewBuffer.get(1)), is(Byte.valueOf(LITTLE_ENDIAN_BYTE[1])));
        assertThat(Byte.valueOf(viewBuffer.get(2)), is(Byte.valueOf(LITTLE_ENDIAN_BYTE[2])));
        assertThat(Byte.valueOf(viewBuffer.get(3)), is(Byte.valueOf(LITTLE_ENDIAN_BYTE[3])));
        assertThat(Byte.valueOf(viewBuffer.get(4)), is(Byte.valueOf(LITTLE_ENDIAN_BYTE[4])));
        assertThat(Byte.valueOf(viewBuffer.get(5)), is(Byte.valueOf(LITTLE_ENDIAN_BYTE[5])));
        assertThat(Byte.valueOf(viewBuffer.get(6)), is(Byte.valueOf(LITTLE_ENDIAN_BYTE[6])));
    }

    @Test
    public void shouldDecodeUIntBigEndianCorrectly()
    {
        final Flyweight flyweight = flyweightBigEndian;

        IntStream.range(0, 7).forEach(i -> viewBuffer.put(i, BIG_ENDIAN_BYTE[i]));

        flyweight.reset(aBuff, 0);
        assertThat(Long.valueOf(Flyweight.uint32Get(aBuff, UINT32_OFFSET, ByteOrder.BIG_ENDIAN)), is(UINT32_VALUE));
        assertThat(Integer.valueOf(Flyweight.uint16Get(aBuff, UINT16_OFFSET, ByteOrder.BIG_ENDIAN)), is(UINT16_VALUE));
        assertThat(Short.valueOf(Flyweight.uint8Get(aBuff, UINT8_OFFSET)), is(UINT8_VALUE));
    }

    @Test
    public void shouldDecodeUIntLittleEndianCorrectly()
    {
        final Flyweight flyweight = flyweightLittleEndian;

        IntStream.range(0, 7).forEach(i -> viewBuffer.put(i, LITTLE_ENDIAN_BYTE[i]));

        flyweight.reset(aBuff, 0);
        assertThat(Long.valueOf(Flyweight.uint32Get(aBuff, UINT32_OFFSET, ByteOrder.LITTLE_ENDIAN)), is(UINT32_VALUE));
        assertThat(Integer.valueOf(Flyweight.uint16Get(aBuff, UINT16_OFFSET, ByteOrder.LITTLE_ENDIAN)),
                   is(UINT16_VALUE));
        assertThat(Short.valueOf(Flyweight.uint8Get(aBuff, UINT8_OFFSET)), is(UINT8_VALUE));
    }

    @Test
    public void shouldTestBitInByte()
    {
        final byte bits = (byte)0b1000_0000;
        final int bufferIndex = 8;
        final int bitIndex = 7;
        aBuff.putByte(bufferIndex, bits);

        for (int i = 0; i < 8; i++)
        {
            boolean result = Flyweight.bitSet(aBuff, bufferIndex, i);
            if (bitIndex == i)
            {
                assertTrue(result);
            }
            else
            {
                assertFalse("bit set i = " + i, result);
            }
        }
    }

    @Test
    public void shouldSetBitInByte()
    {
        final int bufferIndex = 8;

        short total = 0;
        for (int i = 0; i < 8; i++)
        {
            Flyweight.bitSet(aBuff, bufferIndex, i, true);
            total += (1 << i);
            assertThat(Byte.valueOf(aBuff.getByte(bufferIndex)), is(Byte.valueOf((byte)total)));
        }
    }
}
