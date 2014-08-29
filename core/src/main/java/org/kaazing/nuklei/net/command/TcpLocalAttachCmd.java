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

package org.kaazing.nuklei.net.command;

import org.kaazing.nuklei.concurrent.AtomicBuffer;

import java.net.InetAddress;

public class TcpLocalAttachCmd
{
    private final int port;
    private final long id;
    private final InetAddress[] addresses;
    private final AtomicBuffer receiveBuffer;

    public TcpLocalAttachCmd(
        final int port, final long id, final InetAddress[] addresses, final AtomicBuffer receiveBuffer)
    {
        this.port = port;
        this.id = id;
        this.addresses = addresses;
        this.receiveBuffer = receiveBuffer;
    }

    public int port()
    {
        return port;
    }

    public long id()
    {
        return id;
    }

    public InetAddress[] addresses()
    {
        return addresses;
    }

    public AtomicBuffer receiveBuffer()
    {
        return receiveBuffer;
    }
}
