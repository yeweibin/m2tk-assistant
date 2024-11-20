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

package m2tk.assistant.app.ui.builder.section;

import m2tk.assistant.api.presets.RunningStatus;
import m2tk.assistant.app.ui.builder.DescriptorNodeBuilders;
import m2tk.assistant.app.ui.builder.TreeNodeBuilder;
import m2tk.dvb.DVB;
import m2tk.dvb.decoder.element.EventDescriptionDecoder;
import m2tk.dvb.decoder.section.EITSectionDecoder;
import m2tk.encoding.Encoding;
import m2tk.mpeg2.decoder.DescriptorLoopDecoder;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import java.time.format.DateTimeFormatter;

public class EITNodeBuilder implements TreeNodeBuilder
{
    private static final DateTimeFormatter startTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private final EITSectionDecoder eit = new EITSectionDecoder();
    private final EventDescriptionDecoder eventDecoder = new EventDescriptionDecoder();
    private final DescriptorLoopDecoder descriptorLoopDecoder = new DescriptorLoopDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        eit.attach(encoding);

        String text = String.format("Section[%02X]（业务号：%d，传输流号：%d，原始网络号：%d）",
                                    eit.getSectionNumber(),
                                    eit.getServiceID(),
                                    eit.getTransportStreamID(),
                                    eit.getOriginalNetworkID());
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(text);

        node.add(create(String.format("table_id = 0x%02X", eit.getTableID())));
        node.add(create(String.format("section_syntax_indicator = %d", eit.getSyntaxIndicator())));
        node.add(create(String.format("section_length = %d", eit.getSectionLength())));
        node.add(create(String.format("service_id = %d", eit.getServiceID())));
        node.add(create(String.format("version_number = %d", eit.getVersionNumber())));
        node.add(create(String.format("current_next_indicator = %d", eit.getCurrentNextIndicator())));
        node.add(create(String.format("section_number = %d", eit.getSectionNumber())));
        node.add(create(String.format("last_section_number = %d", eit.getLastSectionNumber())));

        node.add(create(String.format("transport_stream_id = %d", eit.getTransportStreamID())));
        node.add(create(String.format("original_network_id = %d", eit.getOriginalNetworkID())));

        node.add(create(String.format("segment_last_section_number = %d", eit.getSegmentLastSectionNumber())));
        node.add(create(String.format("last_table_id = %d", eit.getLastTableID())));

        Encoding[] descriptions = eit.getEventDescriptionList();
        for (int i = 0; i < descriptions.length; i++)
        {
            eventDecoder.attach(descriptions[i]);

            DefaultMutableTreeNode nodeEvent = new DefaultMutableTreeNode(String.format("事件描述%d", i + 1));

            nodeEvent.add(create(String.format("event_id = %d", eventDecoder.getEventID())));
            nodeEvent.add(create(String.format("start_time = %s（原始数据：%s）",
                                               translateStartTime(eventDecoder.getStartTime()),
                                               descriptions[i].toHexStringPrettyPrint(2, 7))));
            nodeEvent.add(create(String.format("duration = %s", DVB.printTimeFields(eventDecoder.getDuration()))));
            nodeEvent.add(create(String.format("running_status = %d（%s）",
                                               eventDecoder.getRunningStatus(),
                                               RunningStatus.name(eventDecoder.getRunningStatus()))));
            nodeEvent.add(create(String.format("free_CA_mode = %d", eventDecoder.getFreeCAMode())));

            DefaultMutableTreeNode nodeDescList = new DefaultMutableTreeNode();
            Encoding descloop = eventDecoder.getDescriptorLoop();
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
            nodeEvent.add(nodeDescList);

            node.add(nodeEvent);
        }

        node.add(create(String.format("CRC_32 = 0x%08X", eit.getChecksum())));

        return node;
    }

    private String translateStartTime(long timepoint)
    {
        // NVOD索引事件的起始时间为全1。
        if (timepoint == 0xFFFFFFFFFFL)
            return "未定义";
        return DVB.decodeTimepointIntoLocalDateTime(timepoint)
                  .format(startTimeFormatter);
    }
}
