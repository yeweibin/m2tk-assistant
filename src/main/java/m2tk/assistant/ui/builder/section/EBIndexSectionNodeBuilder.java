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

package m2tk.assistant.ui.builder.section;

import m2tk.assistant.analyzer.presets.StreamTypes;
import m2tk.assistant.ui.builder.DescriptorNodeBuilders;
import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.dvb.DVB;
import m2tk.encoding.Encoding;
import m2tk.mpeg2.MPEG2;
import m2tk.mpeg2.decoder.DescriptorLoopDecoder;
import m2tk.mpeg2.decoder.ExtendedSectionDecoder;
import m2tk.mpeg2.decoder.element.ProgramElementDecoder;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class EBIndexSectionNodeBuilder implements TreeNodeBuilder
{
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private final ExtendedSectionDecoder decoder = new ExtendedSectionDecoder();
    private final DescriptorLoopDecoder descriptorLoopDecoder = new DescriptorLoopDecoder();
    private final ProgramElementDecoder elementDecoder = new ProgramElementDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        String text = String.format("Section[s:%02X, v:%d]", decoder.getSectionNumber(), decoder.getVersionNumber());
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(text);

        node.add(create(String.format("table_id = 0x%02X", decoder.getTableID())));
        node.add(create(String.format("section_syntax_indicator = %d", decoder.getSyntaxIndicator())));
        node.add(create(String.format("section_length = %d", decoder.getSectionLength())));
        node.add(create(String.format("table_id_extension = %d", decoder.getTableIDExtension())));
        node.add(create(String.format("version_number = %d", decoder.getVersionNumber())));
        node.add(create(String.format("current_next_indicator = %d", decoder.getCurrentNextIndicator())));
        node.add(create(String.format("section_number = %d", decoder.getSectionNumber())));
        node.add(create(String.format("last_section_number = %d", decoder.getLastSectionNumber())));

        Encoding payload = decoder.getPayload();
        int ebmNumber = payload.readUINT8(0);

        DefaultMutableTreeNode nodeEBMList = new DefaultMutableTreeNode(String.format("EBM_List（%d）", ebmNumber));
        node.add(nodeEBMList);

        int offset = 1;
        for (int i = 0; i < ebmNumber; i++)
        {
            int ebmLength = payload.readUINT16(offset);
            int ebmFinish = offset + 2 + ebmLength;
            offset += 2;

            DefaultMutableTreeNode nodeEBM = new DefaultMutableTreeNode(String.format("EBM_%d（%d字节）", i + 1, ebmLength));

            nodeEBM.add(create(String.format("EBM_id = '%s'", payload.toHexString(offset, offset + 18).substring(1))));
            offset += 18;
            nodeEBM.add(create(String.format("EBM_original_network_id = %d", payload.readUINT16(offset))));
            offset += 2;
            long startTime = payload.readUINT40(offset);
            nodeEBM.add(create(String.format("EBM_start_time = %s（%05x）", translateTimepoint(startTime), startTime)));
            offset += 5;
            long endTime = payload.readUINT40(offset);
            nodeEBM.add(create(String.format("EBM_end_time = %s（%05x）", translateTimepoint(endTime), endTime)));
            offset += 5;
            nodeEBM.add(create(String.format("EBM_type = '%s'", new String(payload.getRange(offset, offset + 5)))));
            offset += 5;
            int ebmClass = payload.readUINT8(offset) >> 4;
            int ebmLevel = payload.readUINT8(offset) & 0xF;
            nodeEBM.add(create(String.format("EBM_class = '%s'", translateEBMClass(ebmClass))));
            nodeEBM.add(create(String.format("EBM_level = '%s'", translateEBMLevel(ebmLevel))));
            offset += 1;
            int resourceNumber = payload.readUINT8(offset);
            offset += 1;
            DefaultMutableTreeNode nodeEBResourceList = new DefaultMutableTreeNode(String.format("EB_ResourceList（%d）", resourceNumber));
            for (int j = 0; j < resourceNumber; j ++)
            {
                String resourceCode = payload.toHexString(offset, offset + 12).substring(1);
                nodeEBResourceList.add(create(String.format("EB_resource_code_%d = '%s'", j + 1, resourceCode)));
                offset += 12;
            }
            nodeEBM.add(nodeEBResourceList);

            int detailsChannelIndicate = payload.readUINT8(offset) & 1;
            offset += 1;
            if (detailsChannelIndicate == 1)
            {
                nodeEBM.add(create(String.format("details_channel_network_id = %d", payload.readUINT16(offset))));
                offset += 2;
                nodeEBM.add(create(String.format("details_channel_transport_stream_id = %d", payload.readUINT16(offset))));
                offset += 2;
                nodeEBM.add(create(String.format("details_channel_program_number = %d", payload.readUINT16(offset))));
                offset += 2;
                nodeEBM.add(create(String.format("details_channel_PCR_PID = 0x%X", payload.readUINT16(offset) & MPEG2.PID_MASK)));
                offset += 2;

                int descloopLen = payload.readUINT16(offset) & 0x0FFF;
                offset += 2;

                Encoding descloop = payload.readSelector(offset, descloopLen);
                offset += descloopLen;
                DefaultMutableTreeNode nodeDescList = new DefaultMutableTreeNode();
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
                nodeEBM.add(nodeDescList);

                Encoding[] elements = getProgramElementList(payload, offset);
                DefaultMutableTreeNode nodeElementList = new DefaultMutableTreeNode(String.format("ElementStreamList（%d）", elements.length));
                nodeEBM.add(nodeElementList);

                offset += 2;
                for (int j = 0; j < elements.length; j++)
                {
                    elementDecoder.attach(elements[j]);

                    DefaultMutableTreeNode nodeElement = new DefaultMutableTreeNode(String.format("基本流%d", j + 1));
                    nodeElement.add(create(String.format("stream_type = 0x%02X（%s）",
                                                         elementDecoder.getStreamType(),
                                                         StreamTypes.description(elementDecoder.getStreamType()))));
                    nodeElement.add(create(String.format("elementary_PID = 0x%X", elementDecoder.getElementaryPID())));

                    DefaultMutableTreeNode nodeESInfo = new DefaultMutableTreeNode();
                    Encoding esInfo = elementDecoder.getDescriptorLoop();
                    if (esInfo.size() > 0)
                    {
                        descriptorLoopDecoder.attach(esInfo);
                        descriptorLoopDecoder.forEach(descriptor -> {
                            int tag = descriptor.readUINT8(0);
                            TreeNodeBuilder builder = DescriptorNodeBuilders.getBuilder(tag);
                            nodeESInfo.add(builder.build(descriptor));
                        });
                    }
                    nodeESInfo.setUserObject(String.format("ES描述（%d）", nodeESInfo.getChildCount()));
                    nodeElement.add(nodeESInfo);

                    nodeElementList.add(nodeElement);
                    offset += elements[j].size();
                }
            }

            if (offset != ebmFinish)
            {
                nodeEBM.removeAllChildren();
                nodeEBM.add(new DefaultMutableTreeNode("错误的EBM编码"));
            }

            nodeEBMList.add(nodeEBM);
            offset = ebmFinish;
        }

        int signatureLength = payload.readUINT16(offset);
        node.add(create(String.format("signature = '%s'", payload.toHexString(offset + 2, offset + 2 + signatureLength))));

        node.add(create(String.format("CRC_32 = 0x%08X", decoder.getChecksum())));

        return node;
    }

    private String translateTimepoint(long timepoint)
    {
        if (timepoint == 0xFFFFFFFFFFL)
            return "未定义";

        // timepoint = [MJD:2]+[UTC:3]
        int mjd = (int) ((timepoint >> 24) & 0x0000FFFFL);
        int utc = (int) ((timepoint) & 0x00FFFFFFL);

        LocalDate date = DVB.decodeDate(mjd);
        LocalTime time = DVB.decodeTime(utc);

        // 应急广播没有遵循真正的UTC时间，而是采用当地时间。
        return LocalDateTime.of(date, time).format(timeFormatter);
    }

    private String translateEBMClass(int ebmClass)
    {
        switch (ebmClass)
        {
            case 0b0001: return "发布系统演练";
            case 0b0010: return "模拟演练";
            case 0b0011: return "实际演练";
            case 0b0100: return "应急广播";
            default: return "保留";
        }
    }

    private String translateEBMLevel(int ebmLevel)
    {
        switch (ebmLevel)
        {
            case 1: return "1级（特别重大）";
            case 2: return "2级（重大）";
            case 3: return "3级（较大）";
            case 4: return "4级（一般）";
            default: return "保留";
        }
    }

    private Encoding[] getProgramElementList(Encoding payload, int offset)
    {
        int streamInfoLength = payload.readUINT16(offset);
        List<Encoding> list = new ArrayList<>();
        int limit = offset + 2 + streamInfoLength;
        offset += 2;
        while (offset < limit)
        {
            int esInfoLength = payload.readUINT16(offset + 3) & 0xFFF;
            list.add(payload.readSelector(offset, 5 + esInfoLength));
            offset += 5 + esInfoLength;
        }
        return list.toArray(new Encoding[0]);
    }
}
