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

import org.kaazing.nuklei.NioSelectorNukleus;
import org.kaazing.nuklei.concurrent.AtomicBuffer;
import org.kaazing.nuklei.concurrent.MpscArrayBuffer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.IntSupplier;

/**
 */
public class TcpAcceptor
{
    private final long id;
    private final TcpInterfaceAcceptor[] acceptors;
    private final AtomicBuffer receiveBuffer;
    private final NioSelectorNukleus selectorNukleus;
    private final MpscArrayBuffer<Object> tcpReaderCommandQueue;
    private final MpscArrayBuffer<Object> tcpSenderCommandQueue;

    public TcpAcceptor(
        final int port,
        final InetAddress[] interfaces,
        final long id,
        final AtomicBuffer receiveBuffer,
        final NioSelectorNukleus selectorNukleus,
        final MpscArrayBuffer<Object> tcpReaderCommandQueue,
        final MpscArrayBuffer<Object> tcpSenderCommandQueue)
    {
        this.id = id;
        this.receiveBuffer = receiveBuffer;
        this.selectorNukleus = selectorNukleus;
        this.tcpReaderCommandQueue = tcpReaderCommandQueue;
        this.tcpSenderCommandQueue = tcpSenderCommandQueue;

        try
        {
            if (0 == interfaces.length)
            {
                acceptors = new TcpInterfaceAcceptor[1];

                final ServerSocketChannel acceptor = ServerSocketChannel.open();
                acceptor.bind(new InetSocketAddress(port));

                acceptors[0] = new TcpInterfaceAcceptor(acceptor);
                selectorNukleus.register(acceptors[0].acceptor(), SelectionKey.OP_ACCEPT, composeAcceptor(acceptors[0]));
            }
            else
            {
                acceptors = new TcpInterfaceAcceptor[interfaces.length];

                for (int i = 0; i < acceptors.length; i++)
                {
                    final ServerSocketChannel acceptor = ServerSocketChannel.open();
                    acceptor.bind(new InetSocketAddress(interfaces[i], port));

                    acceptors[i] = new TcpInterfaceAcceptor(acceptor);
                    selectorNukleus.register(acceptors[i].acceptor(), SelectionKey.OP_ACCEPT, composeAcceptor(acceptors[i]));
                }
            }
        }
        catch (final Exception ex)
        {
            throw new IllegalStateException(ex);
        }
    }

    public long id()
    {
        return id;
    }

    public void close()
    {
        for (final TcpInterfaceAcceptor acceptor : acceptors)
        {
            selectorNukleus.cancel(acceptor.acceptor(), SelectionKey.OP_ACCEPT);
            acceptor.close();
        }
    }

    private int onAcceptable(final SocketChannel channel)
    {
        final long senderId = tcpSenderCommandQueue.nextId();
        final long receiverId = tcpReaderCommandQueue.nextId();
        final TcpConnection transport = new TcpConnection(channel, receiverId, senderId, receiveBuffer);

        // pass transport off to other nukleus' to process
        tcpReaderCommandQueue.write(transport);
        tcpSenderCommandQueue.write(transport);

        return 1;
    }

    private IntSupplier composeAcceptor(final TcpInterfaceAcceptor acceptor)
    {
        return () ->
        {
            try
            {
                return onAcceptable(acceptor.acceptor().accept());
            }
            catch (final Exception ex)
            {
                ex.printStackTrace();  // TODO: temporary
            }

            return 0;
        };
    }

    private static class TcpInterfaceAcceptor
    {
        final ServerSocketChannel acceptor;

        TcpInterfaceAcceptor(final ServerSocketChannel acceptor)
        {
            this.acceptor = acceptor;
        }

        public void close()
        {
            try
            {
                acceptor.close();
            }
            catch (final Exception ex)
            {
                throw new IllegalStateException(ex);
            }
        }

        public ServerSocketChannel acceptor()
        {
            return acceptor;
        }
    }
}
