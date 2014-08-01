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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link java.util.concurrent.ForkJoinPool} based scheduler that uses a dedicated submitter thread
 */
public class FjpDedicatedNuklei implements Nuklei, Runnable
{
    private final ForkJoinPool pool;
    private final AtomicReference<Wrapper[]> nukleusArrayRef;
    private final Thread schedulerThread;

    private volatile boolean done;

    public FjpDedicatedNuklei()
    {
        this(ForkJoinPool.commonPool());
    }

    public FjpDedicatedNuklei(final ForkJoinPool pool)
    {
        this.pool = pool;
        this.schedulerThread = new Thread(this);
        this.nukleusArrayRef = new AtomicReference<>();

        final Wrapper[] initialArray = new Wrapper[0];

        nukleusArrayRef.set(initialArray);
        done = false;
        schedulerThread.start();
    }

    public void spinUp(final Nukleus nukleus)
    {
        Wrapper[] oldArray = nukleusArrayRef.get();
        Wrapper[] newArray = Arrays.copyOf(oldArray, oldArray.length + 1);

        newArray[oldArray.length] = new Wrapper(nukleus);

        nukleusArrayRef.lazySet(newArray);
    }

    public void run()
    {
        // scheduler/submitter thread
        while (!done)
        {
            final Wrapper[] nuklei = nukleusArrayRef.get();
            int weight = 0;

            for (final Wrapper nukleus: nuklei)
            {
                if (!nukleus.isInProcess())
                {
                    nukleus.inProcess(true);
                    pool.execute(nukleus);
                    weight++;
                }

                if (done)
                {
                    return;  // let submissions finish asynchronously
                }
            }

            // TODO: add idle strategy (spin, yield) and pass weight to it
        }
    }

    private static class Wrapper implements Runnable
    {
        private final AtomicBoolean inProcess = new AtomicBoolean(false);
        private final Nukleus nukleus;

        Wrapper(final Nukleus nukleus)
        {
            this.nukleus = nukleus;
        }

        public boolean isInProcess()
        {
            return inProcess.get();
        }

        public void inProcess(final boolean value)
        {
            inProcess.lazySet(value);
        }

        public void run()
        {
            nukleus.process();
            inProcess.lazySet(false);
        }
    }
}
