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

package org.kaazing.nuklei.net;

import org.kaazing.nuklei.concurrent.AtomicBuffer;
import org.kaazing.nuklei.concurrent.ringbuffer.mpsc.MpscRingBufferWriter;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 */
public class TcpConnection
{
    public static final int RECEIVE_TYPE_ID = 0;

    public static final int MAX_RECEIVE_LENGTH = 4096;

    private final SocketChannel channel;
    private final MpscRingBufferWriter receiveWriter;
    private final long receiverId;
    private final long senderId;
    private final ByteBuffer receiveByteBuffer;
    private final AtomicBuffer atomicBuffer;

    // TODO: connect version of constructor

    // accepted version
    public TcpConnection(
        final SocketChannel channel,
        final long receiverId,
        final long senderId,
        final AtomicBuffer receiveBuffer)
    {
        this.channel = channel;
        this.receiverId = receiverId;
        this.senderId = senderId;

        receiveWriter = new MpscRingBufferWriter(receiveBuffer);
        receiveByteBuffer = ByteBuffer.allocateDirect(MAX_RECEIVE_LENGTH);
        atomicBuffer = new AtomicBuffer(receiveByteBuffer);
    }

    public SocketChannel channel()
    {
        return channel;
    }

    public long receiverId()
    {
        return receiverId;
    }

    public long senderId()
    {
        return senderId;
    }

    public void send(final ByteBuffer buffer)
    {
        try
        {
            final int length = buffer.remaining();
            final int sent = channel.write(buffer);

            if (sent < length)
            {
                // TODO: finish by handling appropriately with
                throw new IllegalStateException("could not send all of buffer: " + sent + "/" + length); // temporary
            }
        }
        catch (final Exception ex)
        {
            ex.printStackTrace(); // TODO: temp
        }
    }

    public int onReadable()
    {
        try
        {
            receiveByteBuffer.clear();
            final int length = channel.read(receiveByteBuffer);

            if (!receiveWriter.write(RECEIVE_TYPE_ID, atomicBuffer, 0, length))
            {
                throw new IllegalStateException("could not write to receive buffer");
            }
        }
        catch (final Exception ex)
        {
            ex.printStackTrace(); // TODO: temp
        }

        return 0;
    }

    public int onWritable()
    {
        return 0;
    }
}
