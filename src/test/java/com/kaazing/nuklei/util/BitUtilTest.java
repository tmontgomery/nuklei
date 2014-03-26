package com.kaazing.nuklei.util;

import org.junit.Test;

import static com.kaazing.nuklei.util.BitUtil.toHex;
import static com.kaazing.nuklei.util.BitUtil.toHexByteArray;
import static java.lang.Integer.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 */
public class BitUtilTest
{
    @Test
    public void shouldConvertToHexCorrectly() throws Exception
    {
        final byte[] buffer = { 0x01, 0x23, 0x45, 0x69, 0x78, (byte)0xBC, (byte)0xDA, (byte)0xEF, 0x5F };
        final byte[] converted = toHexByteArray(buffer);
        final String hexStr = toHex(buffer);

        assertThat(valueOf(converted[0]), is(valueOf('0')));
        assertThat(valueOf(converted[1]), is(valueOf('1')));
        assertThat(valueOf(converted[2]), is(valueOf('2')));
        assertThat(valueOf(converted[3]), is(valueOf('3')));
        assertThat(hexStr, is("0123456978bcdaef5f"));
    }
}
