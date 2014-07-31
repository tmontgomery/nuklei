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
 * {@link java.util.concurrent.ForkJoinPool} based scheduler
 */
public class FjpNuklei implements Nuklei
{
    public ForkJoinPool pool;
    private final AtomicReference<Nukleus[]> nukleusArrayRef;

    public FjpNuklei()
    {
        this(ForkJoinPool.commonPool());
    }

    public FjpNuklei(final ForkJoinPool pool)
    {
        this.pool = pool;
        this.nukleusArrayRef = new AtomicReference<>();

        final Nukleus[] initialArray = new Nukleus[0];

        nukleusArrayRef.set(initialArray);
    }

    public void spinUp(final Nukleus nukleus)
    {
        Nukleus[] oldArray = nukleusArrayRef.get();
        Nukleus[] newArray = Arrays.copyOf(oldArray, oldArray.length + 1);

        newArray[oldArray.length] = nukleus;

        nukleusArrayRef.lazySet(newArray);
    }
}
