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

package m2tk.assistant.template.decoder;

import m2tk.assistant.template.definition.*;
import m2tk.encoding.Encoding;

import java.util.*;

public class SectionDecoder
{
    private static final Map<String, TableTemplate> TEMPLATE_MAP = new HashMap<>();
    private static final TableTemplate DEFAULT_TABLE_TEMPLATE;

    static
    {
        TableTemplate template = new TableTemplate();
        template.setName("private_section");
        template.setStandard("mpeg");
        template.setTableIds(Collections.emptyList());
        template.setTableSyntax(List.of(DataFieldDefinition.number("table_id", "uimsbf", "8", FieldPresentation.of("TableId", "0x%02X")),
                                        DataFieldDefinition.number("section_syntax_indicator", "bslbf", "1", null),
                                        DataFieldDefinition.number("private_indicator", "bslbf", "1", null),
                                        DataFieldDefinition.number("reserved", "bslbf", "2", null),
                                        DataFieldDefinition.number("private_section_length", "bslbf", "12", FieldPresentation.of("Section Length", "%d")),
                                        DataFieldDefinition.bytes("private_data", "private_section_length", "0", FieldPresentation.of("Section Payload"))));
        template.setUniqueKey(UniqueKey.of("table_id"));

        DEFAULT_TABLE_TEMPLATE = template;
    }

    public static void registerTemplate(TableTemplate template)
    {
        synchronized (TEMPLATE_MAP)
        {
            Objects.requireNonNull(template);
            for (TableId tableId : template.getTableIds())
            {
                String key = String.format("%02x", tableId.getId());
                TEMPLATE_MAP.put(key, template);
            }
        }
    }

    public SyntaxField decode(Encoding encoding, int position, int limit)
    {
        int tableId = encoding.readUINT8(position);
        int sectionLength = encoding.readUINT16(position + 1) & 0x0FFF;

        if (position + sectionLength + 3 > Math.min(limit, encoding.size()))
            throw new IndexOutOfBoundsException("字段超出可解码范围");

        String key = String.format("%02x", tableId);
        TableTemplate template = TEMPLATE_MAP.getOrDefault(key, DEFAULT_TABLE_TEMPLATE);

        SyntaxField section = SyntaxField.complex(template.getName());

        int bitOffset = 0;
        for (SyntaxFieldDefinition fieldDefinition : template.getTableSyntax())
        {
            SyntaxFieldDecoder decoder = SyntaxFieldDecoder.of(fieldDefinition);
            int decodedBits = decoder.decode(fieldDefinition, encoding, position, bitOffset, limit, section);

            position = position + (bitOffset + decodedBits) / 8;
            bitOffset = (bitOffset + decodedBits) % 8;
        }

        return section;
    }
}
