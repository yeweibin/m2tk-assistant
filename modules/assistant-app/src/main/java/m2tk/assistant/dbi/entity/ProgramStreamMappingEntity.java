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

package m2tk.assistant.dbi.entity;

import lombok.Data;
import m2tk.assistant.analyzer.presets.StreamTypes;

import java.util.Objects;

@Data
public class ProgramStreamMappingEntity
{
    private long id;
    private long transactionId;
    private int programNumber;
    private int streamPid;
    private int streamType;
    private String streamCategory;
    private String streamDescription;

    public ProgramStreamMappingEntity()
    {
        streamCategory = StreamTypes.CATEGORY_USER_PRIVATE;
        streamDescription = StreamTypes.description(0xFF);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProgramStreamMappingEntity entity = (ProgramStreamMappingEntity) o;
        return programNumber == entity.programNumber && streamPid == entity.streamPid;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(programNumber, streamPid);
    }
}
