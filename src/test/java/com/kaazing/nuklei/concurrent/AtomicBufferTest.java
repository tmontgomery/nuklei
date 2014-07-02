package com.kaazing.nuklei.concurrent;

import com.kaazing.nuklei.BitUtil;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static java.lang.Integer.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;

/*
 * Based on SBE tests of DirectBuffer
 */
@RunWith(Theories.class)
public class AtomicBufferTest
{
    private static final int BUFFER_CAPACITY = 4096;
    private static final int INDEX = 8;

    private static final byte BYTE_VALUE = 1;
    private static final short SHORT_VALUE = 2;
    private static final int INT_VALUE = 4;
    private static final float FLOAT_VALUE = 5.0f;
    private static final long LONG_VALUE = 6;
    private static final double DOUBLE_VALUE = 7.0d;

    @Rule
    public final ExpectedException exceptionRule = ExpectedException.none();

    @DataPoint
    public static final ByteOrder NATIVE_BYTE_ORDER = ByteOrder.nativeOrder();

    @DataPoint
    public static final ByteOrder NONNATIVE_BYTE_ORDER =
            (NATIVE_BYTE_ORDER == ByteOrder.BIG_ENDIAN) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;

    @DataPoint
    public static final AtomicBuffer BYTE_ARRAY_BACKED = new AtomicBuffer(new byte[BUFFER_CAPACITY]);

    @DataPoint
    public static final AtomicBuffer HEAP_BYTE_BUFFER = new AtomicBuffer(ByteBuffer.allocate(BUFFER_CAPACITY));

    @DataPoint
    public static final AtomicBuffer DIRECT_BYTE_BUFFER = new AtomicBuffer(ByteBuffer.allocateDirect(BUFFER_CAPACITY));

    @DataPoint
    public static final AtomicBuffer HEAP_BYTE_BUFFER_SLICE =
            new AtomicBuffer(((ByteBuffer)(ByteBuffer.allocate(BUFFER_CAPACITY * 2).position(BUFFER_CAPACITY))).slice());

    // Note this will leak memory and a real world application would need to reclaim the allocated memory!!!
    @DataPoint
    public static final AtomicBuffer OFF_HEAP_BUFFER = new AtomicBuffer(BitUtil.UNSAFE.allocateMemory(BUFFER_CAPACITY), BUFFER_CAPACITY);

    @Theory
    @Test(expected = IndexOutOfBoundsException.class)
    public void shouldThrowExceptionForLimitAboveCapacity(final AtomicBuffer buffer)
    {
        final int position = BUFFER_CAPACITY + 1;
        buffer.checkLimit(position);
    }

    @Theory
    public void shouldNotThrowExceptionForLimitAtCapacity(final AtomicBuffer buffer)
    {
        final int position = BUFFER_CAPACITY;

        buffer.checkLimit(position);
    }

    @Theory
    public void shouldGetLongFromBuffer(final AtomicBuffer buffer, final ByteOrder byteOrder)
    {
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(byteOrder);

        duplicateBuffer.putLong(INDEX, LONG_VALUE);

        assertThat(buffer.getLong(INDEX, byteOrder), is(LONG_VALUE));
    }

    @Theory
    public void shouldPutLongToBuffer(final AtomicBuffer buffer, final ByteOrder byteOrder)
    {
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(byteOrder);

        buffer.putLong(INDEX, LONG_VALUE, byteOrder);

        assertThat(duplicateBuffer.getLong(INDEX), is(LONG_VALUE));
    }

    @Theory
    public void shouldGetLongVolatileFromNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(ByteOrder.nativeOrder());

        duplicateBuffer.putLong(INDEX, LONG_VALUE);

        assertThat(buffer.getLongVolatile(INDEX), is(LONG_VALUE));
    }

    @Theory
    public void shouldPutLongOrderedToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(ByteOrder.nativeOrder());

        buffer.putLongOrdered(INDEX, LONG_VALUE);

        assertThat(duplicateBuffer.getLong(INDEX), is(LONG_VALUE));
    }

    @Theory
    public void shouldCompareAndSwapLongToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(ByteOrder.nativeOrder());

        duplicateBuffer.putLong(INDEX, LONG_VALUE);

        assertTrue(buffer.compareAndSwapLong(INDEX, LONG_VALUE, LONG_VALUE + 1));

        assertThat(duplicateBuffer.getLong(INDEX), CoreMatchers.is(LONG_VALUE + 1));
    }

    @Theory
    public void shouldGetIntFromBuffer(final AtomicBuffer buffer, final ByteOrder byteOrder)
    {
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(byteOrder);

        duplicateBuffer.putInt(INDEX, INT_VALUE);

        assertThat(valueOf(buffer.getInt(INDEX, byteOrder)), is(valueOf(INT_VALUE)));
    }

    @Theory
    public void shouldGetIntFromNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(ByteOrder.nativeOrder());

        duplicateBuffer.putInt(INDEX, INT_VALUE);

        assertThat(buffer.getInt(INDEX), is(INT_VALUE));
    }

    @Theory
    public void shouldPutIntToBuffer(final AtomicBuffer buffer, final ByteOrder byteOrder)
    {
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(byteOrder);

        buffer.putInt(INDEX, INT_VALUE, byteOrder);

        assertThat(valueOf(duplicateBuffer.getInt(INDEX)), is(valueOf(INT_VALUE)));
    }

    @Theory
    public void shouldPutIntToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(ByteOrder.nativeOrder());

        buffer.putInt(INDEX, INT_VALUE);

        assertThat(duplicateBuffer.getInt(INDEX), is(INT_VALUE));
    }

    @Theory
    public void shouldGetIntVolatileFromNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(ByteOrder.nativeOrder());

        duplicateBuffer.putInt(INDEX, INT_VALUE);

        assertThat(buffer.getIntVolatile(INDEX), is(INT_VALUE));
    }

    @Theory
    public void shouldPutIntOrderedToNativeBuffer(final AtomicBuffer buffer)
    {
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(ByteOrder.nativeOrder());

        buffer.putIntOrdered(INDEX, INT_VALUE);

        assertThat(duplicateBuffer.getInt(INDEX), is(INT_VALUE));
    }

    @Theory
    public void shouldGetShortFromBuffer(final AtomicBuffer buffer, final ByteOrder byteOrder)
    {
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(byteOrder);

        duplicateBuffer.putShort(INDEX, SHORT_VALUE);

        assertThat(buffer.getShort(INDEX, byteOrder), is(SHORT_VALUE));
    }

    @Theory
    public void shouldPutShortToBuffer(final AtomicBuffer buffer, final ByteOrder byteOrder)
    {
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(byteOrder);

        buffer.putShort(INDEX, SHORT_VALUE, byteOrder);

        assertThat(duplicateBuffer.getShort(INDEX), is(SHORT_VALUE));
    }

    @Theory
    public void shouldGetDoubleFromBuffer(final AtomicBuffer buffer, final ByteOrder byteOrder)
    {
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(byteOrder);

        duplicateBuffer.putDouble(INDEX, DOUBLE_VALUE);

        assertThat(buffer.getDouble(INDEX, byteOrder), is(DOUBLE_VALUE));
    }

    @Theory
    public void shouldPutDoubleToBuffer(final AtomicBuffer buffer, final ByteOrder byteOrder)
    {
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(byteOrder);

        buffer.putDouble(INDEX, DOUBLE_VALUE, byteOrder);

        assertThat(duplicateBuffer.getDouble(INDEX), is(DOUBLE_VALUE));
    }

    @Theory
    public void shouldGetFloatFromBuffer(final AtomicBuffer buffer, final ByteOrder byteOrder)
    {
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(byteOrder);

        duplicateBuffer.putFloat(INDEX, FLOAT_VALUE);

        assertThat(buffer.getFloat(INDEX, byteOrder), is(FLOAT_VALUE));
    }

    @Theory
    public void shouldPutFloatToBuffer(final AtomicBuffer buffer, final ByteOrder byteOrder)
    {
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(byteOrder);

        buffer.putFloat(INDEX, FLOAT_VALUE, byteOrder);

        assertThat(duplicateBuffer.getFloat(INDEX), is(FLOAT_VALUE));
    }

    @Theory
    public void shouldGetByteFromBuffer(final AtomicBuffer buffer, final ByteOrder byteOrder)
    {
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(byteOrder);

        duplicateBuffer.put(INDEX, BYTE_VALUE);

        assertThat(buffer.getByte(INDEX), is(BYTE_VALUE));
    }

    @Theory
    public void shouldPutByteToBuffer(final AtomicBuffer buffer, final ByteOrder byteOrder)
    {
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(byteOrder);

        buffer.putByte(INDEX, BYTE_VALUE);

        assertThat(duplicateBuffer.get(INDEX), is(BYTE_VALUE));
    }

    @Theory
    public void shouldGetByteArrayFromBuffer(final AtomicBuffer buffer)
    {
        final byte[] testArray = {'H', 'e', 'l', 'l', 'o'};

        int i = INDEX;
        for (final byte v : testArray)
        {
            buffer.putByte(i, v);
            i += BitUtil.SIZE_OF_BYTE;
        }

        final byte[] result = new byte[testArray.length];
        buffer.getBytes(INDEX, result);

        assertThat(result, is(testArray));
    }

    @Theory
    public void shouldGetBytesFromBuffer(final AtomicBuffer buffer, final ByteOrder byteOrder)
    {
        final byte[] testBytes = "Hello World".getBytes();

        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(byteOrder);
        duplicateBuffer.position(INDEX);
        duplicateBuffer.put(testBytes);

        final byte[] buff = new byte[testBytes.length];
        buffer.getBytes(INDEX, buff);

        assertThat(buff, is(testBytes));
    }

    @Theory
    public void shouldGetBytesFromBufferToBuffer(final AtomicBuffer buffer, final ByteOrder byteOrder)
    {
        final byte[] testBytes = "Hello World".getBytes();

        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(byteOrder);
        duplicateBuffer.position(INDEX);
        duplicateBuffer.put(testBytes);

        final ByteBuffer dstBuffer = ByteBuffer.allocate(testBytes.length);
        buffer.getBytes(INDEX, dstBuffer, testBytes.length);

        assertThat(dstBuffer.array(), is(testBytes));
    }
    @Theory
    public void shouldGetBytesFromBufferToAtomicBuffer(final AtomicBuffer buffer, final ByteOrder byteOrder)
    {
        final byte[] testBytes = "Hello World".getBytes();

        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(byteOrder);
        duplicateBuffer.position(INDEX);
        duplicateBuffer.put(testBytes);

        final ByteBuffer dstBuffer = ByteBuffer.allocateDirect(testBytes.length);
        buffer.getBytes(INDEX, dstBuffer, testBytes.length);

        byte[] result = new byte[testBytes.length];
        dstBuffer.flip();
        dstBuffer.get(result);
        assertThat(result, is(testBytes));
    }

    @Theory
    public void shouldGetBytesFromBufferToSlice(final AtomicBuffer buffer, final ByteOrder byteOrder)
    {
        final byte[] testBytes = "Hello World".getBytes();

        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(byteOrder);
        duplicateBuffer.position(INDEX);
        duplicateBuffer.put(testBytes);

        final ByteBuffer dstBuffer =
                ((ByteBuffer) ByteBuffer.allocate(testBytes.length*2).position(testBytes.length)).slice();

        buffer.getBytes(INDEX, dstBuffer, testBytes.length);

        byte[] result = new byte[testBytes.length];
        dstBuffer.flip();
        dstBuffer.get(result);
        assertThat(result, is(testBytes));
    }

    @Theory
    public void shouldGetBytesToDirectBufferFromDirectBuffer(final AtomicBuffer buffer)
    {
        final byte[] testBytes = "Hello World!".getBytes();
        final ByteBuffer testBuff = (ByteBuffer)ByteBuffer.allocate(testBytes.length * 2).position(testBytes.length);
        final AtomicBuffer dstBuffer = new AtomicBuffer(testBuff.slice());

        buffer.putBytes(INDEX, testBytes);
        buffer.getBytes(INDEX, dstBuffer, 0, testBytes.length);

        byte[] result = new byte[testBytes.length];
        dstBuffer.getBytes(0, result);
        assertThat(result, is(testBytes));
    }

    @Theory
    public void shouldPutBytesToBuffer(final AtomicBuffer buffer, final ByteOrder byteOrder)
    {
        final byte[] testBytes = "Hello World".getBytes();
        buffer.putBytes(INDEX, testBytes);

        final byte[] buff = new byte[testBytes.length];
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(byteOrder);
        duplicateBuffer.position(INDEX);
        duplicateBuffer.get(buff);

        assertThat(buff, is(testBytes));
    }

    @Theory
    public void shouldPutBytesToBufferFromBuffer(final AtomicBuffer buffer, final ByteOrder byteOrder)
    {
        final byte[] testBytes = "Hello World".getBytes();
        final ByteBuffer srcBuffer = ByteBuffer.wrap(testBytes);

        buffer.putBytes(INDEX, srcBuffer, testBytes.length);

        final byte[] buff = new byte[testBytes.length];
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(byteOrder);
        duplicateBuffer.position(INDEX);
        duplicateBuffer.get(buff);

        assertThat(buff, is(testBytes));
    }

    @Theory
    public void shouldPutBytesToBufferFromAtomicBuffer(final AtomicBuffer buffer, final ByteOrder byteOrder)
    {
        final byte[] testBytes = "Hello World".getBytes();
        final ByteBuffer srcBuffer = ByteBuffer.allocateDirect(testBytes.length);
        srcBuffer.put(testBytes);
        srcBuffer.flip();

        buffer.putBytes(INDEX, srcBuffer, testBytes.length);

        final byte[] buff = new byte[testBytes.length];
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(byteOrder);
        duplicateBuffer.position(INDEX);
        duplicateBuffer.get(buff);

        assertThat(buff, is(testBytes));
    }

    @Theory
    public void shouldPutBytesToBufferFromSlice(final AtomicBuffer buffer, final ByteOrder byteOrder)
    {
        final byte[] testBytes = "Hello World".getBytes();
        final ByteBuffer srcBuffer = ((ByteBuffer) ByteBuffer.allocate(testBytes.length * 2).position(testBytes.length)).slice();

        srcBuffer.put(testBytes);
        srcBuffer.flip();

        buffer.putBytes(INDEX, srcBuffer, testBytes.length);

        final byte[] buff = new byte[testBytes.length];
        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(byteOrder);
        duplicateBuffer.position(INDEX);
        duplicateBuffer.get(buff);

        assertThat(buff, is(testBytes));
    }

    @Theory
    public void shouldPutBytesToDirectBufferFromDirectBuffer(final AtomicBuffer buffer)
    {
        final byte[] testBytes = "Hello World!".getBytes();
        final AtomicBuffer srcBuffer =
                new AtomicBuffer(((ByteBuffer)ByteBuffer.allocate(testBytes.length * 2).position(testBytes.length)).slice());

        srcBuffer.putBytes(0, testBytes);
        buffer.putBytes(INDEX, srcBuffer, 0, testBytes.length);

        final byte[] buff = new byte[testBytes.length];
        buffer.getBytes(INDEX, buff);

        assertThat(buff, is(testBytes));
    }

    @Theory
    public void shouldGetByteArrayFromBufferNpe(final AtomicBuffer buffer)
    {
        exceptionRule.expect(NullPointerException.class);
        final byte[] testBytes = null;

        buffer.getBytes(INDEX, testBytes, 0, 10);
    }

    @Theory
    public void shouldPutByteArrayToBufferNpe(final AtomicBuffer buffer)
    {
        exceptionRule.expect(NullPointerException.class);
        final byte[] testBytes = null;

        buffer.putBytes(INDEX, testBytes, 0, 10);
    }

    @Theory
    public void shouldGetBytesFromBufferNpe(final AtomicBuffer buffer)
    {
        exceptionRule.expect(NullPointerException.class);
        final ByteBuffer testBuffer = null;

        buffer.getBytes(INDEX, testBuffer, 10);
    }

    @Theory
    public void shouldPutBytesToBufferNpe(final AtomicBuffer buffer)
    {
        exceptionRule.expect(NullPointerException.class);
        final ByteBuffer testBuffer = null;

        buffer.putBytes(INDEX, testBuffer, 10);
    }

    @Theory
    public void shouldGetDirectBytesFromBufferNpe(final AtomicBuffer buffer)
    {
        exceptionRule.expect(NullPointerException.class);
        final AtomicBuffer testBuffer = null;

        buffer.getBytes(INDEX, testBuffer, 0, 10);
    }

    @Theory
    public void shouldPutDirectBytesToBufferNpe(final AtomicBuffer buffer)
    {
        exceptionRule.expect(NullPointerException.class);
        final AtomicBuffer testBuffer = null;

        buffer.putBytes(INDEX, testBuffer, 0, 10);
    }

    @Theory
    public void shouldGetByteArrayFromBufferTruncate(final AtomicBuffer buffer)
    {
        final byte[] testBytes = new byte[10];

        final int result = buffer.getBytes(INDEX, testBytes, 0, 21);

        assertThat(result, is(10));
    }

    @Theory
    public void shouldPutByteArrayToBufferTruncate(final AtomicBuffer buffer)
    {
        final byte[] testBytes = new byte[11];

        final int result = buffer.putBytes(INDEX, testBytes, 0, 20);

        assertThat(result, is(11));
    }

    @Theory
    public void shouldGetBytesFromBufferTruncate(final AtomicBuffer buffer)
    {
        final ByteBuffer testBytes = ByteBuffer.allocate(12);

        final int result = buffer.getBytes(INDEX, testBytes, 20);

        assertThat(result, is(12));
    }

    @Theory
    public void shouldPutBytesToBufferTruncate(final AtomicBuffer buffer)
    {
        final ByteBuffer testBytes = ByteBuffer.allocate(13);

        final int result = buffer.putBytes(INDEX, testBytes, 20);

        assertThat(result, is(13));
    }

    @Theory
    public void shouldGetDirectBytesFromBufferTruncate(final AtomicBuffer buffer)
    {
        final AtomicBuffer testBytes = new AtomicBuffer(new byte[14]);

        final int result = buffer.getBytes(INDEX, testBytes, 0, 20);

        assertThat(result, is(14));
    }

    @Theory
    public void shouldPutDirectBytesToBufferTruncate(final AtomicBuffer buffer)
    {
        final AtomicBuffer testBytes = new AtomicBuffer(new byte[15]);

        final int result = buffer.putBytes(INDEX, testBytes, 0, 20);

        assertThat(result, is(15));
    }

    @Theory
    public void shouldSetMemory(final AtomicBuffer buffer, final ByteOrder byteOrder)
    {
        final byte[] testBytes = "ooooooooooo".getBytes();

        buffer.setMemory(0, testBytes.length, testBytes[0]);

        final ByteBuffer duplicateBuffer = buffer.duplicateByteBuffer().order(byteOrder);
        final byte[] buff = new byte[testBytes.length];
        duplicateBuffer.get(buff);

        assertThat(buff, is(testBytes));
    }
}
