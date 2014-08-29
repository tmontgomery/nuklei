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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.nuklei.BitUtil;
import org.kaazing.nuklei.DedicatedNuklei;
import org.kaazing.nuklei.concurrent.AtomicBuffer;
import org.kaazing.nuklei.concurrent.MpscArrayBuffer;
import org.kaazing.nuklei.concurrent.ringbuffer.mpsc.MpscRingBuffer;
import org.kaazing.nuklei.concurrent.ringbuffer.mpsc.MpscRingBufferReader;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Basic tests for TcpManager
 */
public class TcpManagerTest
{
    private static final int MANAGER_COMMAND_QUEUE_SIZE = 1024;
    private static final int MANAGER_SEND_BUFFER_SIZE = 64*1024 + MpscRingBuffer.STATE_TRAILER_SIZE;
    private static final int RECEIVE_BUFFER_SIZE = 64*1024 + MpscRingBuffer.STATE_TRAILER_SIZE;
    private static final int PORT = 40134;
    private static final int SEND_BUFFER_SIZE = 1024;
    private static final int MAGIC_PAYLOAD_INT = 8;

    private final MpscArrayBuffer<Object> managerCommandQueue = new MpscArrayBuffer<>(MANAGER_COMMAND_QUEUE_SIZE);
    private final AtomicBuffer managerSendBuffer = new AtomicBuffer(ByteBuffer.allocate(MANAGER_SEND_BUFFER_SIZE));

    private final AtomicBuffer receiveBuffer = new AtomicBuffer(ByteBuffer.allocate(RECEIVE_BUFFER_SIZE));
    private final MpscRingBufferReader receiver = new MpscRingBufferReader(receiveBuffer);
    private final ByteBuffer sendChannelBuffer = ByteBuffer.allocate(SEND_BUFFER_SIZE).order(ByteOrder.nativeOrder());
    private final AtomicBuffer sendAtomicBuffer = new AtomicBuffer(ByteBuffer.allocate(SEND_BUFFER_SIZE));
    private final ByteBuffer receiveChannelBuffer = ByteBuffer.allocate(RECEIVE_BUFFER_SIZE).order(ByteOrder.nativeOrder());

    private TcpManager tcpManager;
    private TcpManagerProxy tcpManagerProxy;
    private DedicatedNuklei dedicatedNuklei;
    private SocketChannel senderChannel;
    private SocketChannel receiverChannel;

    @Before
    public void setUp() throws Exception
    {
        tcpManager = new TcpManager(managerCommandQueue, managerSendBuffer);
        tcpManagerProxy = new TcpManagerProxy(managerCommandQueue, managerSendBuffer);
        dedicatedNuklei = new DedicatedNuklei("TCP-manager-dedicated");
    }

    @After
    public void closeEverything() throws Exception
    {
        if (null != dedicatedNuklei)
        {
            dedicatedNuklei.stop();
        }

        if (null != tcpManager)
        {
            tcpManager.close();
        }

        if (null != senderChannel)
        {
            senderChannel.close();
        }

        if (null != receiverChannel)
        {
            receiverChannel.close();
        }
    }

    @Test(timeout = 1000)
    public void shouldBeAbleToSpinUpAndShutdownTcpManagerWithSingleNuklei() throws Exception
    {
        tcpManager.launch(dedicatedNuklei);
    }

    @Test(timeout = 1000)
    public void shouldHandleConnectAndSendReceiveOfDataWithSingleNuklei() throws Exception
    {
        tcpManager.launch(dedicatedNuklei);

        tcpManagerProxy.attach(PORT, new InetAddress[0], receiveBuffer);

        // TODO: this could race with the attach completing and doing the bind
        Thread.sleep(100);

        senderChannel = SocketChannel.open();
        senderChannel.connect(new InetSocketAddress("localhost", PORT));

        int messages = receiveSingleMessage((typeId, buffer, offset, length) ->
        {
            assertThat(typeId, is(TcpManagerEvents.NEW_CONNECTION_TYPE_ID));
            assertThat(buffer.getLong(offset), is(0L));
        });
        assertThat(messages, is(1));

        sendChannelBuffer.clear();
        sendChannelBuffer.putInt(MAGIC_PAYLOAD_INT);
        sendChannelBuffer.flip();
        senderChannel.write(sendChannelBuffer);

        messages = receiveSingleMessage((typeId, buffer, offset, length) ->
        {
            assertThat(typeId, is(TcpManagerEvents.RECEIVED_DATA_TYPE_ID));
            assertThat(buffer.getLong(offset), is(0L));
            assertThat(length, is(BitUtil.SIZE_OF_INT + BitUtil.SIZE_OF_LONG));
            assertThat(buffer.getInt(offset + BitUtil.SIZE_OF_LONG), is(MAGIC_PAYLOAD_INT));
        });
        assertThat(messages, is(1));
    }

    @Test(timeout = 1000)
    public void shouldHandleConnectAndReceiveOfDataWithSingleNuklei() throws Exception
    {
        tcpManager.launch(dedicatedNuklei);

        tcpManagerProxy.attach(PORT, new InetAddress[0], receiveBuffer);

        // TODO: this could race with the attach completing and doing the bind
        Thread.sleep(100);

        receiverChannel = SocketChannel.open();
        receiverChannel.connect(new InetSocketAddress("localhost", PORT));
//        receiverChannel.socket().setSoTimeout(100);
        receiverChannel.configureBlocking(false);

        int messages = receiveSingleMessage((typeId, buffer, offset, length) ->
        {
            assertThat(typeId, is(TcpManagerEvents.NEW_CONNECTION_TYPE_ID));
            assertThat(buffer.getLong(offset), is(0L));
        });
        assertThat(messages, is(1));

        sendAtomicBuffer.putLong(0, 0);  // set connection ID
        sendAtomicBuffer.putInt(BitUtil.SIZE_OF_LONG, MAGIC_PAYLOAD_INT);

        tcpManagerProxy.send(sendAtomicBuffer, 0, BitUtil.SIZE_OF_LONG + BitUtil.SIZE_OF_INT);

        receiveChannelBuffer.clear();

        messages = receiveSingleMessage(receiverChannel, (buffer) ->
        {
            assertThat(buffer.position(), is(BitUtil.SIZE_OF_INT));
            assertThat(buffer.getInt(0), is(MAGIC_PAYLOAD_INT));
        });
        assertThat(messages, is(1));
    }

    private int receiveSingleMessage(final MpscRingBufferReader.ReadHandler handler)
    {
        int messages;

        while (0 == (messages = receiver.read(handler, 1)))
        {
            Thread.yield();
        }

        return messages;
    }

    private int receiveSingleMessage(final SocketChannel channel, final Consumer<ByteBuffer> handler) throws Exception
    {
        int len;

        while (0 == (len = channel.read(receiveChannelBuffer)))
        {
            Thread.yield();
        }

        if (0 < len)
        {
            handler.accept(receiveChannelBuffer);
        }

        return 1;
    }
}
