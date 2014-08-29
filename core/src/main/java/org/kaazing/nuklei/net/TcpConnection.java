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

import org.kaazing.nuklei.BitUtil;
import org.kaazing.nuklei.concurrent.AtomicBuffer;
import org.kaazing.nuklei.concurrent.ringbuffer.mpsc.MpscRingBufferWriter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;

/**
 */
public class TcpConnection
{
    public static final int MAX_RECEIVE_LENGTH = 4096;

    private final SocketChannel channel;
    private final MpscRingBufferWriter receiveWriter;
    private final long id;
    private final ByteBuffer receiveByteBuffer;
    private final AtomicBuffer atomicBuffer;

    // TODO: connect version of constructor

    // accepted version
    public TcpConnection(
        final SocketChannel channel,
        final long id,
        final AtomicBuffer receiveBuffer)
    {
        this.channel = channel;
        this.id = id;

        receiveWriter = new MpscRingBufferWriter(receiveBuffer);
        receiveByteBuffer = ByteBuffer.allocateDirect(MAX_RECEIVE_LENGTH).order(ByteOrder.nativeOrder());
        atomicBuffer = new AtomicBuffer(receiveByteBuffer);

        informOfNewConnection();
    }

    public SocketChannel channel()
    {
        return channel;
    }

    public long id()
    {
        return id;
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
            receiveByteBuffer.putLong(id);
            final int length = channel.read(receiveByteBuffer);

            if (!receiveWriter.write(TcpManagerEvents.RECEIVED_DATA_TYPE_ID, atomicBuffer, 0, length + BitUtil.SIZE_OF_LONG))
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

    private void informOfNewConnection()
    {
        atomicBuffer.putLong(0, id);

        if (!receiveWriter.write(TcpManagerEvents.NEW_CONNECTION_TYPE_ID, atomicBuffer, 0, BitUtil.SIZE_OF_LONG))
        {
            throw new IllegalStateException("could not write to receive buffer");
        }
    }
}
