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

import org.junit.Test;

import static org.kaazing.nuklei.BitUtil.toHex;
import static org.kaazing.nuklei.BitUtil.toHexByteArray;
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
