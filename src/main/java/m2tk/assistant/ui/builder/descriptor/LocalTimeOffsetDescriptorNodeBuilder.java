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

package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.dvb.DVB;
import m2tk.dvb.decoder.descriptor.LocalTimeOffsetDescriptorDecoder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalTimeOffsetDescriptorNodeBuilder implements TreeNodeBuilder
{
    private final LocalTimeOffsetDescriptorDecoder decoder = new LocalTimeOffsetDescriptorDecoder();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode("local_time_offset_descriptor");
        node.add(create(String.format("descriptor_tag = 0x%02X", decoder.getTag())));
        node.add(create(String.format("descriptor_length = %d", decoder.getPayloadLength())));

        int count = decoder.getDescriptionCount();
        for (int i = 0; i < count; i++)
        {
            DefaultMutableTreeNode nodeDesc = new DefaultMutableTreeNode(String.format("本地时间偏移描述%d", i + 1));
            nodeDesc.add(create(String.format("country_code = '%s'", decoder.getCountryCode(i))));
            nodeDesc.add(create(String.format("country_region_id = '%s'（%s）",
                                              sixBits(decoder.getCountryRegionID(i)),
                                              translateCountryRegionID(decoder.getCountryRegionID(i)))));
            nodeDesc.add(create(String.format("local_time_offset_polarity = %d", decoder.getLocalTimeOffsetPolarity(i))));
            nodeDesc.add(create(String.format("local_time_offset = %04X", decoder.getLocalTimeOffset(i))));
            nodeDesc.add(create(String.format("time_of_change = (UTC) %s", translateTimepoint2UTC(decoder.getTimeOfChange(i)))));
            nodeDesc.add(create(String.format("next_time_offset = %04X", decoder.getNextTimeOffset(i))));
            node.add(nodeDesc);
        }

        return node;
    }

    private String sixBits(int value)
    {
        String bits = "000000";
        String binary= Integer.toBinaryString(value);
        return bits.substring(binary.length()) + binary;
    }

    private String translateCountryRegionID(int regionID)
    {
        if (regionID == 0b000000)
            return "未使用扩展时区";
        if (0b000001 <= regionID && regionID <= 0b111100)
            return "（东起）时区" + regionID;
        return "预留使用";
    }

    private String translateTimepoint2UTC(long timepoint)
    {
        return LocalDateTime.of(DVB.decodeDate((int) (timepoint >> 24)),
                                DVB.decodeTime((int) (timepoint & 0xFFFFFF)))
                            .format(timeFormatter);
    }
}
