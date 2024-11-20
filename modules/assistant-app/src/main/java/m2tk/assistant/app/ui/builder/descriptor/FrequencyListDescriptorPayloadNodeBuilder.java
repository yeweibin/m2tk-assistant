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

package m2tk.assistant.app.ui.builder.descriptor;

import m2tk.assistant.app.ui.builder.DescriptorPayloadNodeBuilder;
import m2tk.dvb.DVB;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;

public class FrequencyListDescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        int codingType = payload.readUINT8(0) & 0b11;
        int count = (payload.size() - 1) / 4;

        node.add(create(String.format("coding_type = %s", codingType(codingType))));

        for (int i = 0; i < count; i++)
        {
            long frequency = payload.readUINT32(1 + i * 4);

            String freqText;
            if (codingType == 0b01)
                freqText = DVB.translateSatelliteFrequencyCode(frequency);
            else if (codingType == 0b10)
                freqText = DVB.translateCableFrequencyCode(frequency);
            else if (codingType == 0b11)
                freqText = DVB.translateTerrestrialFrequencyCode(frequency);
            else
                freqText = "未定义";

            node.add(create(String.format("频率%d = %s", i + 1, freqText)));
        }
    }

    private String codingType(int type)
    {
        switch (type)
        {
            case 0b00:
                return "未定义";
            case 0b01:
                return "卫星";
            case 0b10:
                return "有线";
            case 0b11:
                return "地面";
            default:
                return "";
        }
    }
}
