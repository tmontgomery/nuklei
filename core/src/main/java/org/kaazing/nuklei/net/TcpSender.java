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
import org.kaazing.nuklei.MessagingNukleus;
import org.kaazing.nuklei.NioSelectorNukleus;
import org.kaazing.nuklei.Nuklei;
import org.kaazing.nuklei.concurrent.AtomicBuffer;
import org.kaazing.nuklei.concurrent.MpscArrayBuffer;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class TcpSender
{
    private static final int MPSC_READ_LIMIT = 10;

    private final MessagingNukleus messagingNukleus;
    private final NioSelectorNukleus selectorNukleus;
    private final Map<Long, TcpConnection> connectionsByIdMap;
    private final ByteBuffer sendByteBuffer;

    public TcpSender(
        final MpscArrayBuffer<Object> commandQueue,
        final AtomicBuffer sendBuffer,
        final NioSelectorNukleus selectorNukleus)
    {
        final MessagingNukleus.Builder builder = new MessagingNukleus.Builder()
            .nioSelector(selectorNukleus)
            .mpscRingBuffer(sendBuffer, this::sendHandler, MPSC_READ_LIMIT)
            .mpscArrayBuffer(commandQueue, this::commandHandler, MPSC_READ_LIMIT);

        this.selectorNukleus = selectorNukleus;

        messagingNukleus = new MessagingNukleus(builder);
        connectionsByIdMap = new HashMap<>();
        sendByteBuffer = sendBuffer.duplicateByteBuffer();
        sendByteBuffer.clear();
    }

    public void launch(final Nuklei nuklei)
    {
        nuklei.spinUp(messagingNukleus);
    }

    private void commandHandler(final Object obj)
    {
        if (obj instanceof TcpConnection)
        {
            final TcpConnection connection = (TcpConnection)obj;

            connectionsByIdMap.put(connection.id(), connection);
        }
    }

    private void sendHandler(final int typeId, final AtomicBuffer buffer, final int offset, final int length)
    {
        if (TcpManagerEvents.SEND_DATA_TYPE_ID == typeId)
        {
            // use the typeId to hold the connectionId
            final TcpConnection connection = connectionsByIdMap.get(buffer.getLong(offset));

            final int messageOffset = offset + BitUtil.SIZE_OF_LONG;
            sendByteBuffer.limit(messageOffset + length - BitUtil.SIZE_OF_LONG);
            sendByteBuffer.position(messageOffset);

            if (null != connection)
            {
                connection.send(sendByteBuffer);
            }
        }
    }
}
