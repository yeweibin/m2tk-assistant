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

package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.DescriptorPayloadNodeBuilder;
import m2tk.dvb.DVB;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;

public class CableDeliverySystemDescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        long frequency = payload.readUINT32(0);
        int FECOuter = payload.readUINT8(5) & 0xF;
        int modulation = payload.readUINT8(6);
        int symbolRate = (int) (payload.readUINT32(7) & 0xFFFFFFF);
        int FECInner = payload.readUINT8(10) & 0xF;

        node.add(create("频率 = " + DVB.translateCableFrequencyCode(frequency)));
        node.add(create(String.format("前向纠错外码 = '%s'（%s）",
                                      fourBits(FECOuter),
                                      outerFECScheme(FECOuter))));
        node.add(create(String.format("调制方式 = %s", modulationType(modulation))));
        node.add(create(String.format("符号率 = %s", DVB.translateSymbolRateCode(symbolRate))));
        node.add(create(String.format("前向纠错内码 = '%s'（%s）",
                                      fourBits(FECInner),
                                      innerFECScheme(FECInner))));
    }

    private String fourBits(int value)
    {
        String bits = "0000";
        String binary = Integer.toBinaryString(value);
        return bits.substring(binary.length()) + binary;
    }

    private String modulationType(int code)
    {
        switch (code)
        {
            case 0x00:
                return "未定义";
            case 0x01:
                return "16 QAM";
            case 0x02:
                return "32 QAM";
            case 0x03:
                return "64 QAM";
            case 0x04:
                return "128 QAM";
            case 0x05:
                return "256 QAM";
            default:
                return "预留使用";
        }
    }

    private String outerFECScheme(int scheme)
    {
        switch (scheme)
        {
            case 0b0000:
                return "未定义";
            case 0b0001:
                return "无前向纠错外码";
            case 0b0010:
                return "RS（204，188）";
            default:
                return "预留使用";
        }
    }

    private String innerFECScheme(int scheme)
    {
        switch (scheme)
        {
            case 0b0000:
                return "未定义";
            case 0b0001:
                return "卷积码率 1/2";
            case 0b0010:
                return "卷积码率 2/3";
            case 0b0011:
                return "卷积码率 3/4";
            case 0b0100:
                return "卷积码率 5/6";
            case 0b0101:
                return "卷积码率 7/8";
            case 0b1111:
                return "无卷积编码";
            default:
                return "预留使用";
        }
    }
}
