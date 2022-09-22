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

import m2tk.assistant.ui.builder.DescriptorPayloadNodeBuilder;
import m2tk.dvb.DVB;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;

public class TerrestrialDeliverySystemDescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        long centreFrequency = payload.readUINT32(0);
        int bandwidth = (payload.readUINT8(4) >> 5) & 0b111;
        int priority = (payload.readUINT8(4) >> 4) & 0b1;
        int timeSlicingIndicator = (payload.readUINT8(4) >> 3) & 0b1;
        int MPEGFECIndicator = (payload.readUINT8(4) >> 2) & 0b1;
        int constellation = (payload.readUINT8(5) >> 6) & 0b11;
        int hierarchyInformation = (payload.readUINT8(5) >> 3) & 0b111;
        int codeRateHPStream = payload.readUINT8(5) & 0b111;
        int codeRateLPStream = (payload.readUINT8(6) >> 5) & 0b111;
        int guardInterval = (payload.readUINT8(6) >> 3) & 0b11;
        int transmissionMode = (payload.readUINT8(6) >> 1) & 0b11;
        int otherFrequencyFlag = payload.readUINT8(6) & 0b1;

        node.add(create(String.format("中心频率 = %s", DVB.translateTerrestrialFrequencyCode(centreFrequency))));
        node.add(create(String.format("带宽 = %s", translateBandwidth(bandwidth))));
        node.add(create(String.format("优先级 = %s", (priority == 1) ? "高" : "低")));
        node.add(create(String.format("TimeSlicing标志位 = %d", timeSlicingIndicator)));
        node.add(create(String.format("MPE-FEC标志位 = %d", MPEGFECIndicator)));
        node.add(create(String.format("星座特性 = %s", translateConstellation(constellation))));
        node.add(create(String.format("层次信息 = %s", translateHierarchyInformation(hierarchyInformation))));
        node.add(create(String.format("高优先级流编码率模式 = %s", translateCodeRate(codeRateHPStream))));
        node.add(create(String.format("低优先级流编码率模式 = %s", translateCodeRate(codeRateLPStream))));
        node.add(create(String.format("保护间隙 = %s", translateGuardInterval(guardInterval))));
        node.add(create(String.format("传输模式 = %s", translateTransmissionMode(transmissionMode))));
        node.add(create(String.format("其他频率标志 = %d", otherFrequencyFlag)));
    }

    private String translateBandwidth(int code)
    {
        switch (code)
        {
            case 0b000:
                return "8 MHz";
            case 0b001:
                return "7 MHz";
            case 0b010:
                return "6 MHz";
            case 0b011:
                return "5 MHz";
            default:
                return "预留使用";
        }
    }

    private String translateConstellation(int code)
    {
        switch (code)
        {
            case 0b00:
                return "QPSK";
            case 0b01:
                return "16 QAM";
            case 0b10:
                return "64 QAM";
            default:
                return "预留使用";
        }
    }

    private String translateHierarchyInformation(int code)
    {
        switch (code)
        {
            case 0b000:
                return "非层次化，原始交织";
            case 0b001:
                return "α = 1，原始交织";
            case 0b010:
                return "α = 2，原始交织";
            case 0b011:
                return "α = 4，原始交织";
            case 0b100:
                return "非层次化，深度交织";
            case 0b101:
                return "α = 1，深度交织";
            case 0b110:
                return "α = 2，深度交织";
            case 0b111:
                return "α = 4，深度交织";
            default:
                return "";
        }
    }

    private String translateCodeRate(int rate)
    {
        switch (rate)
        {
            case 0b000:
                return "1/2";
            case 0b001:
                return "2/3";
            case 0b010:
                return "3/4";
            case 0b011:
                return "5/6";
            case 0b100:
                return "7/8";
            default:
                return "预留使用";
        }
    }

    private String translateGuardInterval(int interval)
    {
        switch (interval)
        {
            case 0b00:
                return "1/32";
            case 0b01:
                return "1/16";
            case 0b10:
                return "1/8";
            case 0b11:
                return "1/4";
            default:
                return "";
        }
    }

    private String translateTransmissionMode(int mode)
    {
        switch (mode)
        {
            case 0b00:
                return "2k模式";
            case 0b01:
                return "8k模式";
            case 0b10:
                return "4k模式";
            default:
                return "预留使用";
        }
    }
}
