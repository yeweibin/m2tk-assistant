/*
 * Copyright (c) M2TK Project. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package m2tk.assistant.core.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TR290Stats stats = (TR290Stats) o;
        if (errorCounts.size() != stats.errorCounts.size()) return false;
        if (!Objects.equals(errorCounts.keySet(), stats.errorCounts.keySet())) return false;

        for (String key : errorCounts.keySet())
        {
            if (!Objects.equals(errorCounts.get(key), stats.errorCounts.get(key)))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(errorCounts, errorLastEvents);
    }
}