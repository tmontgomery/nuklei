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
package org.kaazing.nuklei.concurrent.ringbuffer;

import org.kaazing.nuklei.concurrent.AtomicBuffer;

/**
 * Writer Interface for a Ring Buffer
 */
public interface RingBufferWriter
{
    /**
     * Write a given message to the ring buffer.
     *
     * @param typeId for the message
     * @param buffer of the message to write
     * @param offset of the message within the buffer
     * @param length of the message in bytes
     * @return whether write was successful or not. If not successful, should be retried.
     */
    boolean write(final int typeId, final AtomicBuffer buffer, final int offset, final int length);
}
