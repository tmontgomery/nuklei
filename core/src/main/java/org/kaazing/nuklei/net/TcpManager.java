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

import org.kaazing.nuklei.MessagingNukleus;
import org.kaazing.nuklei.NioSelectorNukleus;
import org.kaazing.nuklei.concurrent.AtomicBuffer;
import org.kaazing.nuklei.concurrent.MpscArrayBuffer;
import org.kaazing.nuklei.net.command.TcpDetachCmd;
import org.kaazing.nuklei.net.command.TcpLocalAttachCmd;

import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class TcpManager
{
    private static final int MPSC_READ_LIMIT = 10;
    private static final int TCP_READER_COMMAND_QUEUE_SIZE = 1024;
    private static final int TCP_SENDER_COMMAND_QUEUE_SIZE = 1024;

    private final MessagingNukleus messagingNukleus;
    private final NioSelectorNukleus acceptNioSelectorNukleus;
    private final NioSelectorNukleus receiveNioSelectorNukleus;
    private final NioSelectorNukleus sendNioSelectorNukleus;
    private final MpscArrayBuffer<Object> tcpReaderCommandQueue;
    private final MpscArrayBuffer<Object> tcpSenderCommandQueue;
    private final TcpReceiver tcpReceiver;
    private final TcpSender tcpSender;
    private final Map<Long, TcpAcceptor> localAttachesByIdMap;

    public TcpManager(final MpscArrayBuffer<Object> commandQueue, final AtomicBuffer sendBuffer)
        throws Exception
    {
        acceptNioSelectorNukleus = new NioSelectorNukleus(Selector.open());
        receiveNioSelectorNukleus = new NioSelectorNukleus(Selector.open());
        sendNioSelectorNukleus = new NioSelectorNukleus(Selector.open());
        tcpReaderCommandQueue = new MpscArrayBuffer<>(TCP_READER_COMMAND_QUEUE_SIZE);
        tcpSenderCommandQueue = new MpscArrayBuffer<>(TCP_SENDER_COMMAND_QUEUE_SIZE);

        final MessagingNukleus.Builder builder = new MessagingNukleus.Builder()
            .mpscArrayBuffer(commandQueue, this::commandHandler, MPSC_READ_LIMIT)
            .nioSelector(acceptNioSelectorNukleus);

        messagingNukleus = new MessagingNukleus(builder);
        tcpReceiver = new TcpReceiver(tcpReaderCommandQueue, receiveNioSelectorNukleus);
        tcpSender = new TcpSender(tcpSenderCommandQueue, sendBuffer, sendNioSelectorNukleus);
        localAttachesByIdMap = new HashMap<>();
    }

    private void commandHandler(final Object obj)
    {
        if (obj instanceof TcpLocalAttachCmd)
        {
            final TcpLocalAttachCmd cmd = (TcpLocalAttachCmd) obj;
            final TcpAcceptor acceptor =
                new TcpAcceptor(
                    cmd.port(),
                    cmd.addresses(),
                    cmd.id(),
                    cmd.receiveBuffer(),
                    acceptNioSelectorNukleus,
                    tcpReaderCommandQueue,
                    tcpSenderCommandQueue);

            localAttachesByIdMap.put(cmd.id(), acceptor);
        }
        else if (obj instanceof TcpDetachCmd)
        {
            final TcpDetachCmd cmd = (TcpDetachCmd) obj;
            final TcpAcceptor acceptor = localAttachesByIdMap.remove(cmd.id());

            acceptor.close();
        }
    }
}
