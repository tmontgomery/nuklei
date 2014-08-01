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
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link java.util.concurrent.ForkJoinPool} based scheduler that does not use a submitter thread
 */
public class FjpFreeStandingNuklei
{
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

        newArray[oldArray.length] = new Wrapper(nukleus).start(pool);

        nukleusArrayRef.lazySet(newArray);
    }

    private static class Wrapper extends ForkJoinTask<Void>
    {
        private final Nukleus nukleus;

        Wrapper(final Nukleus nukleus)
        {
            this.nukleus = nukleus;
        }

        public Wrapper start(final ForkJoinPool pool)
        {
            pool.execute(this);
            return this;
        }

        public Void getRawResult()
        {
            return null;
        }

        protected void setRawResult(final Void value)
        {
        }

        protected boolean exec()
        {
            nukleus.process();
            reinitialize();
            fork();

            return true;
        }
    }
}
