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

import m2tk.dvb.DVB;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public interface ValueMapping
{
    boolean eval(long value);

    String mapping();

    static ValueMapping mono(long value, String mapping)
    {
        return new MonoValueMapping(value, mapping);
    }

    static ValueMapping range(long min, long max, String mapping)
    {
        return new RangeValueMapping(min, max, mapping);
    }

    static ValueMapping dvbTime()
    {
        return new MJDUTCTimeMapping();
    }

    static ValueMapping duration()
    {
        return new DurationMapping();
    }

    static ValueMapping threeLetterCode()
    {
        return new ThreeLetterCodeMapping();
    }

    record MonoValueMapping(long value, String mapping) implements ValueMapping
    {
        @Override
        public boolean eval(long value)
        {
            return value == this.value;
        }
    }

    record RangeValueMapping(long min, long max, String mapping) implements ValueMapping
    {
        @Override
        public boolean eval(long value)
        {
            return min <= value && value <= max;
        }
    }

    class MJDUTCTimeMapping implements ValueMapping
    {
        private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        private String mapping;

        @Override
        public boolean eval(long value)
        {
            mapping = LocalDateTime.of(DVB.decodeDate((int) (value >> 24)),
                                       DVB.decodeTime((int) (value & 0xFFFFFF)))
                                   .format(timeFormatter);
            return true;
        }

        @Override
        public String mapping()
        {
            return mapping;
        }
    }

    class DurationMapping implements ValueMapping
    {
        private String mapping;

        @Override
        public boolean eval(long value)
        {
            mapping = DVB.printTimeFields((int) value);
            return true;
        }

        @Override
        public String mapping()
        {
            return mapping;
        }
    }

    class ThreeLetterCodeMapping implements ValueMapping
    {
        private String mapping;

        @Override
        public boolean eval(long value)
        {
            mapping = DVB.decodeThreeLetterCode((int) value);
            return true;
        }

        @Override
        public String mapping()
        {
            return mapping;
        }
    }
}
