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
package m2tk.assistant.api.template.definition;

import m2tk.dvb.DVB;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public interface ValueMapping
{
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    String map(long value);

    static String raw(long value)
    {
        return String.valueOf(value);
    }

    static ValueMapping mono(long reference, String mapping)
    {
        return value -> (value == reference) ? mapping : null;
    }

    static ValueMapping range(long min, long max, String mapping)
    {
        return value -> (min <= value && value <= max) ? mapping : null;
    }

    static ValueMapping dvbTime()
    {
        return value -> LocalDateTime.of(DVB.decodeDate((int) (value >> 24)),
                                         DVB.decodeTime((int) (value & 0xFFFFFF)))
                                     .format(timeFormatter);
    }

    static ValueMapping duration()
    {
        return value -> DVB.printTimeFields((int) value);
    }

    static ValueMapping threeLetterCode()
    {
        return value -> DVB.decodeThreeLetterCode((int) value);
    }

    static ValueMapping ipv4()
    {
        return value -> String.format("%d.%d.%d.%d",
                                      (value >>> 24) & 0xFF,
                                      (value >>> 16) & 0xFF,
                                      (value >>>  8) & 0xFF,
                                      (value         & 0xFF));
    }
}
