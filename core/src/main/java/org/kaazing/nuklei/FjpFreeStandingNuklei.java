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
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link ForkJoinPool} based scheduler that does not use a submitter thread
 * but instead re-executes as last step in run.
 */
public class FjpFreeStandingNuklei
{
    public static final int SPINS = 100;

    private final ForkJoinPool pool;
    private final AtomicReference<Wrapper[]> nukleusArrayRef;

    public FjpFreeStandingNuklei()
    {
        this(ForkJoinPool.commonPool());
    }

    public FjpFreeStandingNuklei(final ForkJoinPool pool)
    {
        this.pool = pool;
        this.nukleusArrayRef = new AtomicReference<>();

        final Wrapper[] initialArray = new Wrapper[0];

        nukleusArrayRef.set(initialArray);
    }

    public void spinUp(final Nukleus nukleus)
    {
        Wrapper[] oldArray = nukleusArrayRef.get();
        Wrapper[] newArray = Arrays.copyOf(oldArray, oldArray.length + 1);

        newArray[oldArray.length] = new Wrapper(nukleus, pool);

        pool.execute(newArray[oldArray.length]);

        nukleusArrayRef.lazySet(newArray);
    }

    private static class Wrapper implements Runnable
    {
        private final Nukleus nukleus;
        private final ForkJoinPool pool;
        private final FjpManagedBlockerIdler idler;

        Wrapper(final Nukleus nukleus, final ForkJoinPool pool)
        {
            this.nukleus = nukleus;
            this.pool = pool;
            this.idler = new FjpManagedBlockerIdler(SPINS);
        }

        public void run()
        {
            final int weight = nukleus.process();

            idler.idle(weight);

            pool.execute(this);
        }
    }
}
