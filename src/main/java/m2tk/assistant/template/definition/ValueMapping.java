/*
 * Copyright (c) Ye Weibin. All rights reserved.
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

package m2tk.assistant.template.definition;

public interface ValueMapping
{
    boolean eval(long value);

    String mapping();

    static ValueMapping mono(int value, String mapping)
    {
        return new MonoValueMapping(value, mapping);
    }

    static ValueMapping range(int min, int max, String mapping)
    {
        return new RangeValueMapping(min, max, mapping);
    }


    record MonoValueMapping(int value, String mapping) implements ValueMapping
    {
        @Override
        public boolean eval(long value)
        {
            return value == this.value;
        }
    }

    record RangeValueMapping(int min, int max, String mapping) implements ValueMapping
    {
        @Override
        public boolean eval(long value)
        {
            return min <= value && value <= max;
        }
    }
}
