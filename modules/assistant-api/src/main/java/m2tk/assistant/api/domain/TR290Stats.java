/*
 *  Copyright (c) M2TK Project. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package m2tk.assistant.api.domain;

import java.util.HashMap;
import java.util.Map;

public class TR290Stats
{
    private final Map<String, Long> errorCounts;
    private final Map<String, TR290Event> errorLastEvents;

    public TR290Stats()
    {
        errorCounts = new HashMap<>();
        errorLastEvents = new HashMap<>();
    }

    public void setStat(String errorType, long count, TR290Event lastEvent)
    {
        errorCounts.put(errorType, count);
        errorLastEvents.put(errorType, lastEvent);
    }

    public long getErrorCount(String errorType)
    {
        return errorCounts.getOrDefault(errorType, 0L);
    }

    public TR290Event getErrorLastEvent(String errorType)
    {
        return errorLastEvents.get(errorType);
    }
}