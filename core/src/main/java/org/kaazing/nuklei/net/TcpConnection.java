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

import org.kaazing.nuklei.concurrent.MpscArrayBuffer;

import java.nio.channels.SocketChannel;

/**
 */
public class TcpConnection
{
    private final SocketChannel channel;

    // TODO: connect version of constructor

    // accepted version
    public TcpConnection(final SocketChannel channel,
                         final MpscArrayBuffer<Object> inboundTarget,
                         final MpscArrayBuffer<Object> outboundTarget)
    {
        this.channel = channel;
    }

    public SocketChannel channel()
    {
        return channel;
    }

    public int onReadable()
    {
        return 0;
    }

    public int onWritable()
    {
        return 0;
    }
}
