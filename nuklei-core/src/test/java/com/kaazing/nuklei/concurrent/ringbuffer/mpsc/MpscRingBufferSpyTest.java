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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test MpscRingBufferSpy in isolation
 */
public class MpscRingBufferSpyTest
{
    private static final int MSG_TYPE_ID = 100;
    private static final int CAPACITY = 1024;

    private static final int HEAD_COUNTER_INDEX = CAPACITY + MpscRingBuffer.HEAD_RELATIVE_OFFSET;
    private static final int TAIL_COUNTER_INDEX = CAPACITY + MpscRingBuffer.TAIL_RELATIVE_OFFSET;

    private final AtomicBuffer buffer = mock(AtomicBuffer.class);
    private MpscRingBufferSpy spy;

    @Before
    public void setUp()
    {
        when(buffer.capacity()).thenReturn(CAPACITY + MpscRingBuffer.STATE_TRAILER_SIZE);

        spy = new MpscRingBufferSpy(buffer);
    }


}
