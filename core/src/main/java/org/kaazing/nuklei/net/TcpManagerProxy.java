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
import org.kaazing.nuklei.concurrent.MpscArrayBuffer;
import org.kaazing.nuklei.concurrent.ringbuffer.mpsc.MpscRingBufferWriter;
import org.kaazing.nuklei.net.command.TcpDetachCmd;
import org.kaazing.nuklei.net.command.TcpLocalAttachCmd;

import java.net.InetAddress;

/**
 * Interface for sending commands to a {@link TcpManager}
 */
public class TcpManagerProxy
{
    private final MpscArrayBuffer<Object> commandQueue;
    private final AtomicBuffer sendBuffer;
    private final MpscRingBufferWriter sendWriter;

    public TcpManagerProxy(final MpscArrayBuffer<Object> commandQueue, final AtomicBuffer sendBuffer)
    {
        this.commandQueue = commandQueue;
        this.sendBuffer = sendBuffer;
        this.sendWriter = new MpscRingBufferWriter(sendBuffer);
    }

    /**
     * Local Attach
     *
     * @param port to bind to
     * @param addresses to bind to
     * @param receiveBuffer to place received data from connections
     * @return id to use for {@link #detach(long)}
     */
    public long attach(
        final int port,
        final InetAddress[] addresses,
        final AtomicBuffer receiveBuffer)
    {
        final long id = commandQueue.nextId();
        final TcpLocalAttachCmd cmd = new TcpLocalAttachCmd(port, id, addresses, receiveBuffer);

        if (!commandQueue.write(cmd))
        {
            throw new IllegalStateException("could not write command");
        }

        return id;
    }

    /**
     * Detach (local or remote)
     *
     * @param id of the attach
     */
    public void detach(final long id)
    {
        final TcpDetachCmd cmd = new TcpDetachCmd(id);

        if (!commandQueue.write(cmd))
        {
            throw new IllegalStateException("could not write command");
        }
    }

    public void send(final AtomicBuffer buffer, final int offset, final int length)
    {
        if (!sendWriter.write(TcpManagerEvents.SEND_DATA_TYPE_ID, buffer, offset, length))
        {
            throw new IllegalStateException("could not write to send buffer");
        }
    }

    /*
     * variants
     * - remote attach (connect), no local bind, array buffer
     * - remote attach (connect), local bind, array buffer
     * - remote attach (connect), no local bind, ring buffer
     * - remote attach (connect), local bind, ring buffer
     *
     * - local attach (onAcceptable), array buffer
     * - local attach (onAcceptable), ring buffer
     */

}
