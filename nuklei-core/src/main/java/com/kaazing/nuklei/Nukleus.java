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
package com.kaazing.nuklei;

/**
 * Basic interface for a running service, aka Nukleus.
 */
@FunctionalInterface
public interface Nukleus
{
    /**
     * Process waiting events or do some other single iteration amount of work
     *
     * The value returned should indicate the desire to be rescheduled quickly. A higher value means,
     * more desire. Here are some ways to leverage this knowledge.
     *
     * 1. This method does processing of messages and returns the number of messages processed. If
     * no messages processed, then returning 0 means no immediate needs. A value of > 0 means some messages
     * processed and more might be waiting.
     *
     * 2. This method does a lazy processing and doesn't need to be rescheduled very heavily. Thus the method
     * always returns 0.
     *
     * 3. This method is time critical and should always be rescheduled quickly without letting the runtime put
     * the thread idle for any reason. Thus the method always returns > 0.
     *
     * NOTE: This method makes no guarantees of processing, it is simply hints to the runtime/scheduler.
     *
     * @return indication of weight of rescheduling desire
     */
    int process();
}
