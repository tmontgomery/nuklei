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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

/**
 * Nukleus that uses an {@link java.nio.channels.Selector}
 */
public class NioSelectorNukleus implements Nukleus
{
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

    /*
       TODO:
       Behaviors

       each object is an outstanding operation for one of the below? or need to keep a single object per Channel?

       CONNECT -

       ACCEPT -

       READ -

       WRITE -
     */

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

                /*
                 * TODO:
                 * grab the ready set and pass back the selection key attachment as the "message"
                 *    int onNioEvent(key.attachment(), key)
                 */

                iter.remove();
            }
        }

        return handledMessages;

    }
}
