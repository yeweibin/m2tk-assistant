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
package m2tk.assistant.api.template;

import m2tk.assistant.api.template.definition.Label;
import m2tk.assistant.api.template.definition.SelectorTemplate;
import m2tk.assistant.api.template.definition.SyntaxFieldDefinition;
import m2tk.encoding.Encoding;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class SelectorDecoder
{
    private static final Map<String, SelectorTemplate> TEMPLATE_MAP = new HashMap<>();
    private static final SyntaxDecoder SYNTAX_DECODER = new SyntaxDecoder();

    public static void registerTemplate(SelectorTemplate template)
    {
        Objects.requireNonNull(template);
        synchronized (TEMPLATE_MAP)
        {
            TEMPLATE_MAP.put(template.getName(), template);
        }
    }

    public SyntaxField decode(String name, Encoding encoding, int position, int limit, SyntaxField parent)
    {
        SelectorTemplate template = TEMPLATE_MAP.get(name);
        if (template == null)
            throw new IllegalArgumentException("无效的选择器名称");

        if (position > Math.min(limit, encoding.size()))
            throw new IndexOutOfBoundsException("字段超出可解码范围");

        SyntaxField selector = SyntaxField.selector(template.getName(),
                                                    Optional.ofNullable(template.getDisplayName())
                                                            .map(Label::getText)
                                                            .orElse(template.getName()),
                                                    position);
        if (parent != null)
            parent.appendChild(selector);

        int start = position;
        int bitOffset = 0;
        for (SyntaxFieldDefinition fieldDefinition : template.getSelectorSyntax())
        {
            int decodedBits = SYNTAX_DECODER.decode(fieldDefinition, encoding, position, bitOffset, limit, selector);

            position = position + (bitOffset + decodedBits) / 8;
            bitOffset = (bitOffset + decodedBits) % 8;
        }

        selector.setBitLength((position - start) * 8);
        return selector;
    }
}
