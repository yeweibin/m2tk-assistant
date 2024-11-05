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
import m2tk.dvb.decoder.element.TransportStreamDescriptionDecoder;
import m2tk.dvb.decoder.section.BATSectionDecoder;
import m2tk.encoding.Encoding;
import m2tk.mpeg2.decoder.DescriptorLoopDecoder;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class BATNodeBuilder implements TreeNodeBuilder
{
    private final BATSectionDecoder bat = new BATSectionDecoder();
    private final DescriptorLoopDecoder descriptorLoopDecoder = new DescriptorLoopDecoder();
    private final TransportStreamDescriptionDecoder transportStreamDecoder = new TransportStreamDescriptionDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        bat.attach(encoding);

        String text = String.format("Section[%02X]（业务群号：%d）",
                                    bat.getSectionNumber(),
                                    bat.getBouquetID());
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(text);

        node.add(create(String.format("table_id = 0x%02X", bat.getTableID())));
        node.add(create(String.format("section_syntax_indicator = %d", bat.getSyntaxIndicator())));
        node.add(create(String.format("section_length = %d", bat.getSectionLength())));
        node.add(create(String.format("bouquet_id = %d", bat.getBouquetID())));
        node.add(create(String.format("version_number = %d", bat.getVersionNumber())));
        node.add(create(String.format("current_next_indicator = %d", bat.getCurrentNextIndicator())));
        node.add(create(String.format("section_number = %d", bat.getSectionNumber())));
        node.add(create(String.format("last_section_number = %d", bat.getLastSectionNumber())));

        DefaultMutableTreeNode nodeDescList = new DefaultMutableTreeNode();
        Encoding descloop = bat.getDescriptorLoop();
        if (descloop.size() > 0)
        {
            descriptorLoopDecoder.attach(descloop);
            descriptorLoopDecoder.forEach(descriptor -> {
                int tag = descriptor.readUINT8(0);
                TreeNodeBuilder builder = DescriptorNodeBuilders.getBuilder(tag);
                nodeDescList.add(builder.build(descriptor));
            });
        }
        nodeDescList.setUserObject(String.format("业务群描述符（%d）", nodeDescList.getChildCount()));
        node.add(nodeDescList);

        Encoding[] descriptions = bat.getTransportStreamDescriptionList();
        for (int i = 0; i < descriptions.length; i++)
        {
            transportStreamDecoder.attach(descriptions[i]);

            DefaultMutableTreeNode nodeTS = new DefaultMutableTreeNode(String.format("传输流描述%d", i + 1));

            nodeTS.add(create(String.format("transport_stream_id = %d", transportStreamDecoder.getTransportStreamID())));
            nodeTS.add(create(String.format("original_network_id = %d", transportStreamDecoder.getOriginalNetworkID())));

            DefaultMutableTreeNode nodeTSDescList = new DefaultMutableTreeNode();
            descloop = transportStreamDecoder.getDescriptorLoop();
            if (descloop.size() > 0)
            {
                descriptorLoopDecoder.attach(descloop);
                descriptorLoopDecoder.forEach(descriptor -> {
                    int tag = descriptor.readUINT8(0);
                    TreeNodeBuilder builder = DescriptorNodeBuilders.getBuilder(tag);
                    nodeTSDescList.add(builder.build(descriptor));
                });
            }
            nodeTSDescList.setUserObject(String.format("描述符（%d）", nodeTSDescList.getChildCount()));
            nodeTS.add(nodeTSDescList);

            node.add(nodeTS);
        }

        node.add(create(String.format("CRC_32 = 0x%08X", bat.getChecksum())));

        return node;
    }
}
