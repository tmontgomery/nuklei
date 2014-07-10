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
package com.kaazing.nuklei.concurrent.ringbuffer;

import com.kaazing.nuklei.concurrent.AtomicBuffer;

/**
 * Reader Interface for a Ring Buffer
 */
public interface RingBufferReader
{
    /**
     * Handler for reading messages out of a ring buffer
     */
    @FunctionalInterface
    public interface ReadHandler
    {
        /**
         * Message read from a ring buffer.
         *
         * @param typeId of the message
         * @param buffer of the message
         * @param offset within the buffer where the message starts
         * @param length of the message in bytes
         */
        void onMessage(final int typeId, final AtomicBuffer buffer, final int offset, final int length);
    }

    /**
     * Read pending messages from ring buffer up to a limit of number of messages. Does not block.
     *
     * @param handler to call for all read messages
     * @param limit to impose on the number of read messages
     * @return number of messages read
     */
    int read(final ReadHandler handler, final int limit);
}
