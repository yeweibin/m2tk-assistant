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

package m2tk.assistant.ui.builder.section;

import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.encoding.Encoding;
import m2tk.mpeg2.decoder.ExtendedSectionDecoder;
import m2tk.mpeg2.decoder.SectionDecoder;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class PrivateSectionNodeBuilder implements TreeNodeBuilder
{
    private final SectionDecoder decoder = new SectionDecoder();
    private final ExtendedSectionDecoder decoderEx = new ExtendedSectionDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode();

        node.add(create(String.format("table_id = 0x%02X", decoder.getTableID())));
        node.add(create(String.format("section_syntax_indicator = %d", decoder.getSyntaxIndicator())));
        node.add(create(String.format("section_length = %d", decoder.getSectionLength())));

        if (decoder.getSyntaxIndicator() == 0)
        {
            node.setUserObject(String.format("Section（table_id = 0x%02X）", decoder.getTableID()));
            node.add(create(String.format("section_payload = %s", decoder.getPayload().toHexStringPrettyPrint())));
        } else
        {
            decoderEx.attach(encoding);

            node.setUserObject(String.format("Section[%02X]（table_id = 0x%02X）",
                                             decoderEx.getSectionNumber(),
                                             decoderEx.getTableID()));

            node.add(create(String.format("table_id_extension = %d", decoderEx.getTableIDExtension())));
            node.add(create(String.format("version_number = %d", decoderEx.getVersionNumber())));
            node.add(create(String.format("current_next_indicator = %d", decoderEx.getCurrentNextIndicator())));
            node.add(create(String.format("section_number = %d", decoderEx.getSectionNumber())));
            node.add(create(String.format("last_section_number = %d", decoderEx.getLastSectionNumber())));
            node.add(create(String.format("section_payload = %s", decoderEx.getPayload().toHexStringPrettyPrint())));
            node.add(create(String.format("CRC_32 = 0x%08X", decoderEx.getChecksum())));
        }

        return node;
    }
}
