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

import m2tk.assistant.core.presets.RunningStatus;
import m2tk.assistant.ui.builder.DescriptorNodeBuilders;
import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.dvb.decoder.element.ServiceDescriptionDecoder;
import m2tk.dvb.decoder.section.SDTSectionDecoder;
import m2tk.encoding.Encoding;
import m2tk.mpeg2.decoder.DescriptorLoopDecoder;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class SDTNodeBuilder implements TreeNodeBuilder
{
    private final SDTSectionDecoder sdt = new SDTSectionDecoder();
    private final ServiceDescriptionDecoder serviceDecoder = new ServiceDescriptionDecoder();
    private final DescriptorLoopDecoder descriptorLoopDecoder = new DescriptorLoopDecoder();


    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        sdt.attach(encoding);

        String text = String.format("Section[%02X]（传输流号：%d，原始网络号：%d）",
                                    sdt.getSectionNumber(),
                                    sdt.getTransportStreamID(),
                                    sdt.getOriginalNetworkID());
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(text);

        node.add(create(String.format("table_id = 0x%02X", sdt.getTableID())));
        node.add(create(String.format("section_syntax_indicator = %d", sdt.getSyntaxIndicator())));
        node.add(create(String.format("section_length = %d", sdt.getSectionLength())));
        node.add(create(String.format("transport_stream_id = %d", sdt.getTransportStreamID())));
        node.add(create(String.format("version_number = %d", sdt.getVersionNumber())));
        node.add(create(String.format("current_next_indicator = %d", sdt.getCurrentNextIndicator())));
        node.add(create(String.format("section_number = %d", sdt.getSectionNumber())));
        node.add(create(String.format("last_section_number = %d", sdt.getLastSectionNumber())));

        node.add(create(String.format("original_network_id = %d", sdt.getOriginalNetworkID())));

        Encoding[] descriptions = sdt.getServiceDescriptionList();
        for (int i = 0; i < descriptions.length; i++)
        {
            serviceDecoder.attach(descriptions[i]);

            DefaultMutableTreeNode nodeService = new DefaultMutableTreeNode(String.format("业务描述%d", i + 1));

            nodeService.add(create(String.format("service_id = %d", serviceDecoder.getServiceID())));
            nodeService.add(create(String.format("EIT_schedule_flag = %d", serviceDecoder.getEITScheduleFlag())));
            nodeService.add(create(String.format("EIT_present_following_flag = %d", serviceDecoder.getEITPresentFollowingFlag())));
            nodeService.add(create(String.format("running_status = %d（%s）",
                                                 serviceDecoder.getRunningStatus(),
                                                 RunningStatus.name(serviceDecoder.getRunningStatus()))));
            nodeService.add(create(String.format("free_CA_mode = %d", serviceDecoder.getFreeCAMode())));

            DefaultMutableTreeNode nodeDescList = new DefaultMutableTreeNode();
            Encoding descloop = serviceDecoder.getDescriptorLoop();
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
            nodeService.add(nodeDescList);

            node.add(nodeService);
        }

        node.add(create(String.format("CRC_32 = 0x%08X", sdt.getChecksum())));

        return node;
    }
}
