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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Nuklei is a thread that holds a group of Nukleus
 *
 * Used for manual assignment of Nuklei
 */
public class Nuklei implements Runnable
{
    private final Thread thread;
    private final AtomicReference<Nukleus[]> nukleusArrayRef;

    private volatile boolean done;

    public Nuklei(final String name)
    {
        thread = new Thread(this);
        nukleusArrayRef = new AtomicReference<>();

        final Nukleus[] initialArray = new Nukleus[0];

        thread.setName(name);
        nukleusArrayRef.set(initialArray);
        done = false;
        thread.start();
    }

    public void done(final boolean value)
    {
        done = value;
    }

    public void stop()
    {
        done = true;
        thread.interrupt();

        do
        {
            try
            {
                thread.join(100);

                if (!thread.isAlive())
                {
                    break;
                }
            }
            catch (final InterruptedException ex)
            {
                System.err.println("Nuklei <" + thread.getName() + "> interrupted stop. Retrying...");
                thread.interrupt();
            }
        }
        while (true);
    }

    void spinUp(final Nukleus nukleus)
    {
        Nukleus[] oldArray = nukleusArrayRef.get();
        Nukleus[] newArray = Arrays.copyOf(oldArray, oldArray.length + 1);

        newArray[oldArray.length] = nukleus;

        nukleusArrayRef.lazySet(newArray);
    }

    public void run()
    {
        while (true)
        {
            final Nukleus[] nuklei = nukleusArrayRef.get();
            int weight = 0;

            for (final Nukleus nukleus: nuklei)
            {
                weight += nukleus.process();

                if (done)
                {
                    return;
                }
            }

            // TODO: add idle strategy (spin, yield, then park) and pass weight to it
        }
    }

}
