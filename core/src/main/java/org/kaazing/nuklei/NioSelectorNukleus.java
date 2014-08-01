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
package org.kaazing.nuklei;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.function.IntSupplier;

/**
 * Nukleus that uses an {@link java.nio.channels.Selector}
 */
public class NioSelectorNukleus implements Nukleus
{
    private static final int DISPATCH_CONNECT = 0;
    private static final int DISPATCH_ACCEPT = 1;
    private static final int DISPATCH_READ = 2;
    private static final int DISPATCH_WRITE = 3;

    public final Selector selector;

    public NioSelectorNukleus(final Selector selector) throws IOException
    {
        this.selector = selector;
    }

    /** {@inheritDoc} */
    public int process()
    {
        int weight = 0;

        try
        {
            selector.selectNow();
            weight += processKeys();
        }
        catch (final Exception ex)
        {
            throw new RuntimeException(ex);
        }

        return weight;
    }

    /**
     * Register a {@link SelectableChannel} for interest and use supplied handler when ready.
     *
     * @param channel for interest
     * @param ops interested in
     * @param handler to call when ready
     * @throws Exception
     */
    public void register(final SelectableChannel channel, final int ops, final IntSupplier handler) throws Exception
    {
        SelectionKey key = channel.keyFor(selector);
        DispatchHandler dispatchHandler;

        if (null == key)
        {
            dispatchHandler = new DispatchHandler();
            key = channel.register(selector, ops);
            key.attach(dispatchHandler);
        }
        else
        {
            dispatchHandler = (DispatchHandler)key.attachment();
        }

        key.interestOps(key.interestOps() | ops);

        if ((ops & SelectionKey.OP_CONNECT) != 0)
        {
            dispatchHandler.dispatcher(DISPATCH_CONNECT, handler);
        }
        else if ((ops & SelectionKey.OP_ACCEPT) != 0)
        {
            dispatchHandler.dispatcher(DISPATCH_ACCEPT, handler);
        }
        else if ((ops & SelectionKey.OP_READ) != 0)
        {
            dispatchHandler.dispatcher(DISPATCH_READ, handler);
        }
        else if ((ops & SelectionKey.OP_WRITE) != 0)
        {
            dispatchHandler.dispatcher(DISPATCH_WRITE, handler);
        }
    }

    /**
     * Cancel interest for a given {@link SelectableChannel}
     *
     * @param channel to cancel on
     * @param ops to cancel
     */
    public void cancel(final SelectableChannel channel, final int ops)
    {
        final SelectionKey key = channel.keyFor(selector);

        if (null != key)
        {
            final int newOps = key.interestOps() & ~ops;

            if (0 == newOps)
            {
                key.cancel();
            }
            else
            {
                key.interestOps(key.interestOps() & ~ops);
            }
        }
    }

    private int processKeys()
    {
        int handledMessages = 0;
        final Set<SelectionKey> selectedKeys = selector.selectedKeys();

        if (!selectedKeys.isEmpty())
        {
            final Iterator<SelectionKey> iter = selectedKeys.iterator();
            while (iter.hasNext())
            {
                final SelectionKey key = iter.next();
                final int readyOps = key.readyOps();
                final DispatchHandler dispatchHandler = (DispatchHandler)key.attachment();

                if ((readyOps & SelectionKey.OP_CONNECT) != 0)
                {
                    handledMessages += dispatchHandler.dispatch(DISPATCH_CONNECT);
                }
                else if ((readyOps & SelectionKey.OP_ACCEPT) != 0)
                {
                    handledMessages += dispatchHandler.dispatch(DISPATCH_ACCEPT);
                }
                else if ((readyOps & SelectionKey.OP_READ) != 0)
                {
                    handledMessages += dispatchHandler.dispatch(DISPATCH_READ);
                }
                else if ((readyOps & SelectionKey.OP_WRITE) != 0)
                {
                    handledMessages += dispatchHandler.dispatch(DISPATCH_WRITE);
                }

                iter.remove();
            }
        }

        return handledMessages;
    }

    private static class DispatchHandler
    {
        private IntSupplier[] dispatchers = new IntSupplier[4];

        public void dispatcher(final int index, final IntSupplier handler)
        {
            dispatchers[index] = handler;
        }

        public int dispatch(final int index)
        {
            return dispatchers[index].getAsInt();
        }
    }
}
