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

package m2tk.assistant.template;

import m2tk.assistant.template.definition.*;
import m2tk.encoding.Encoding;

import java.util.*;

public class DescriptorDecoder
{
    private static final Map<String, DescriptorTemplate> TEMPLATE_MAP = new HashMap<>();
    private static final DescriptorTemplate DEFAULT_DESCRIPTOR_TEMPLATE;
    private static final SyntaxDecoder SYNTAX_DECODER = new SyntaxDecoder();

    static
    {
        DescriptorTemplate template = new DescriptorTemplate();
        template.setTag(0);
        template.setTagExtension(0);
        template.setName("private_descriptor");
        template.setMayOccurIns(Collections.emptyList());
        template.setDescriptorSyntax(List.of(DataFieldDefinition.number("descriptor_tag", "uimsbf", "8", FieldPresentation.of("描述符标签", "0x%02X")),
                                             DataFieldDefinition.number("descriptor_length", "uimsbf", "8", FieldPresentation.of("描述符长度")),
                                             DataFieldDefinition.bytes("descriptor_payload", "descriptor_length", null, FieldPresentation.of("负载数据"))));

        DEFAULT_DESCRIPTOR_TEMPLATE = template;
    }

    public static void registerTemplate(DescriptorTemplate template)
    {
        synchronized (TEMPLATE_MAP)
        {
            Objects.requireNonNull(template);
            String key = (template.getTag() == 0x7F)
                         ? String.format("%02x.%02x", template.getTag(), template.getTagExtension())
                         : String.format("%02x", template.getTag());
            TEMPLATE_MAP.put(key, template);
        }
    }

    public SyntaxField decode(Encoding encoding, int position, int limit)
    {
        int tag = encoding.readUINT8(position);
        int length = encoding.readUINT8(position + 1);

        if (position + length + 2 > Math.min(limit, encoding.size()))
            throw new IndexOutOfBoundsException("字段超出可解码范围");

        int tagExt = encoding.readUINT8(position + 2);
        String key = (tag == 0x7F) ? String.format("%02x.%02x", tag, tagExt) : String.format("%02x", tag);
        DescriptorTemplate template = TEMPLATE_MAP.getOrDefault(key, DEFAULT_DESCRIPTOR_TEMPLATE);

        String displayName = String.format("私有描述符（Tag: 0x%02X）", tag);
        SyntaxField descriptor = SyntaxField.descriptor(template.getName(),
                                                        Optional.ofNullable(template.getDisplayName())
                                                                .map(Label::getText)
                                                                .orElse(displayName));

        int bitOffset = 0;
        for (SyntaxFieldDefinition fieldDefinition : template.getDescriptorSyntax())
        {
            int decodedBits = SYNTAX_DECODER.decode(fieldDefinition, encoding, position, bitOffset, limit, descriptor);

            position = position + (bitOffset + decodedBits) / 8;
            bitOffset = (bitOffset + decodedBits) % 8;
        }

        return descriptor;
    }
}
