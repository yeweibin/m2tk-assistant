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
import m2tk.dvb.decoder.section.NITSectionDecoder;
import m2tk.encoding.Encoding;
import m2tk.mpeg2.decoder.DescriptorLoopDecoder;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class NITNodeBuilder implements TreeNodeBuilder
{
    private final NITSectionDecoder nit = new NITSectionDecoder();
    private final DescriptorLoopDecoder descriptorLoopDecoder = new DescriptorLoopDecoder();
    private final TransportStreamDescriptionDecoder transportStreamDecoder = new TransportStreamDescriptionDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        nit.attach(encoding);

        String text = String.format("Section[%02X]（网络号：%d）",
                                    nit.getSectionNumber(),
                                    nit.getNetworkID());
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(text);

        node.add(create(String.format("table_id = 0x%02X", nit.getTableID())));
        node.add(create(String.format("section_syntax_indicator = %d", nit.getSyntaxIndicator())));
        node.add(create(String.format("section_length = %d", nit.getSectionLength())));
        node.add(create(String.format("network_id = %d", nit.getNetworkID())));
        node.add(create(String.format("version_number = %d", nit.getVersionNumber())));
        node.add(create(String.format("current_next_indicator = %d", nit.getCurrentNextIndicator())));
        node.add(create(String.format("section_number = %d", nit.getSectionNumber())));
        node.add(create(String.format("last_section_number = %d", nit.getLastSectionNumber())));

        DefaultMutableTreeNode nodeDescList = new DefaultMutableTreeNode();
        Encoding descloop = nit.getDescriptorLoop();
        if (descloop.size() > 0)
        {
            descriptorLoopDecoder.attach(descloop);
            descriptorLoopDecoder.forEach(descriptor -> {
                int tag = descriptor.readUINT8(0);
                TreeNodeBuilder builder = DescriptorNodeBuilders.getBuilder(tag);
                nodeDescList.add(builder.build(descriptor));
            });
        }
        nodeDescList.setUserObject(String.format("网络描述符（%d）", nodeDescList.getChildCount()));
        node.add(nodeDescList);

        Encoding[] descriptions = nit.getTransportStreamDescriptionList();
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

        node.add(create(String.format("CRC_32 = 0x%08X", nit.getChecksum())));

        return node;
    }
}
