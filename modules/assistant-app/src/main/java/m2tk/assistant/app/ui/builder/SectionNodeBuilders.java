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

package m2tk.assistant.app.ui.builder;

import m2tk.assistant.app.ui.builder.section.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class SectionNodeBuilders
{
    private static final Map<Integer, Class<? extends TreeNodeBuilder>> builderClasses;
    static
    {
        builderClasses = new HashMap<>();
        builderClasses.put(0x00, PATNodeBuilder.class);
        builderClasses.put(0x01, CATNodeBuilder.class);
        builderClasses.put(0x02, PMTNodeBuilder.class);
        builderClasses.put(0x40, NITNodeBuilder.class);
        builderClasses.put(0x41, NITNodeBuilder.class);
        builderClasses.put(0x42, SDTNodeBuilder.class);
        builderClasses.put(0x46, SDTNodeBuilder.class);
        builderClasses.put(0x4A, BATNodeBuilder.class);
        builderClasses.put(0x70, TDTNodeBuilder.class);
        builderClasses.put(0x73, TOTNodeBuilder.class);
    }

    private SectionNodeBuilders()
    {}

    public static void registerBuilder(int tag, Class<? extends TreeNodeBuilder> builderClass)
    {
        Objects.requireNonNull(builderClass);
        builderClasses.put(tag, builderClass);
    }

    public static TreeNodeBuilder getBuilder(int tableId)
    {
        try
        {
            Class<? extends TreeNodeBuilder> cls = builderClasses.get(tableId);
            if (cls != null)
                return cls.getDeclaredConstructor().newInstance();

            if (0x4E <= tableId && tableId <= 0x6F)
                return new EITNodeBuilder();

            return new PrivateSectionNodeBuilder();
        } catch (Exception ex)
        {
            return new PrivateSectionNodeBuilder();
        }
    }
}
