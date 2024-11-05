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

import m2tk.assistant.ui.builder.DescriptorNodeBuilders;
import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.encoding.Encoding;
import m2tk.mpeg2.decoder.DescriptorLoopDecoder;
import m2tk.mpeg2.decoder.section.CATSectionDecoder;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class CATNodeBuilder implements TreeNodeBuilder
{
    private final CATSectionDecoder cat = new CATSectionDecoder();
    private final DescriptorLoopDecoder descriptorLoopDecoder = new DescriptorLoopDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        cat.attach(encoding);

        String text = String.format("Section[%02X]", cat.getSectionNumber());
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(text);

        node.add(create(String.format("table_id = 0x%02X", cat.getTableID())));
        node.add(create(String.format("section_syntax_indicator = %d", cat.getSyntaxIndicator())));
        node.add(create(String.format("section_length = %d", cat.getSectionLength())));
        node.add(create(String.format("version_number = %d", cat.getVersionNumber())));
        node.add(create(String.format("current_next_indicator = %d", cat.getCurrentNextIndicator())));
        node.add(create(String.format("section_number = %d", cat.getSectionNumber())));
        node.add(create(String.format("last_section_number = %d", cat.getLastSectionNumber())));

        DefaultMutableTreeNode nodeDescList = new DefaultMutableTreeNode();
        Encoding descloop = cat.getDescriptorLoop();
        if (descloop.size() > 0)
        {
            descriptorLoopDecoder.attach(descloop);
            descriptorLoopDecoder.forEach(descriptor -> {
                int tag = descriptor.readUINT8(0);
                TreeNodeBuilder builder = DescriptorNodeBuilders.getBuilder(tag);
                nodeDescList.add(builder.build(descriptor));
            });
        }
        nodeDescList.setUserObject(String.format("描述符（%d）", nodeDescList.getChildCount()));
        node.add(nodeDescList);

        node.add(create(String.format("CRC_32 = 0x%08X", cat.getChecksum())));

        return node;
    }
}
