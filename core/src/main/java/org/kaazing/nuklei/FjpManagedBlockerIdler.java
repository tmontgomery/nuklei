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

import java.util.concurrent.ForkJoinPool;

/**
 * An idler for {@link ForkJoinPool} that uses {@link ForkJoinPool.ManagedBlocker}
 */
public class FjpManagedBlockerIdler implements Idler, ForkJoinPool.ManagedBlocker
{
    public enum State
    {
        NOT_IDLE,
        SPINNING,
        BLOCKING
    }

    private final int maxSpins;

    private State state;
    private int spins;

    private volatile boolean blockDone;

    public FjpManagedBlockerIdler(final int maxSpins)
    {
        this.state = State.NOT_IDLE;
        this.maxSpins = maxSpins;
        this.spins = 0;
    }

    public void idle(final int weight)
    {
        if (weight > 0)
        {
            spins = 0;
            state = State.NOT_IDLE;
            return;
        }

        switch (state)
        {
            case NOT_IDLE:
                state = State.SPINNING;
                spins++;
                break;

            case SPINNING:
                if (++spins > maxSpins)
                {
                    state = State.BLOCKING;
                    blockDone = false;

                    try
                    {
                        ForkJoinPool.managedBlock(this);
                    }
                    catch (final Exception ex)
                    {
                        ex.printStackTrace();  // TODO: temporary
                    }
                }
                break;

            case BLOCKING:
                break;

        }
    }

    public boolean block() throws InterruptedException
    {
        if (!blockDone)
        {
            Thread.yield();  // TODO: might be exponentially increasing parkNanos instead
        }
        blockDone = true;

        return true;
    }

    public boolean isReleasable()
    {
        if (!blockDone)
        {
            return false;
        }

        return true;
    }
}
