package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.dvb.DVB;
import m2tk.dvb.decoder.descriptor.TerrestrialDeliverySystemDescriptorDecoder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class TerrestrialDeliverySystemDescriptorNodeBuilder implements TreeNodeBuilder
{
    private final TerrestrialDeliverySystemDescriptorDecoder decoder = new TerrestrialDeliverySystemDescriptorDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode("terrestrial_delivery_system_descriptor");

        node.add(create("descriptor_tag = 0x5A"));
        node.add(create("descriptor_length = " + decoder.getPayloadLength()));
        node.add(create("中心频率 = " + DVB.translateTerrestrialFrequencyCode(decoder.getCentreFrequencyCode())));
        node.add(create("带宽 = " + translateBandwidth(decoder.getBandwidth())));
        node.add(create("优先级 = " + (decoder.getPriority() == 1 ? "高" : "低")));
        node.add(create("TimeSlicing标志位 = " + decoder.getTimeSlicingIndicator()));
        node.add(create("MPE-FEC标志位 = " + decoder.getMPEFECIndicator()));
        node.add(create("星座特性 = " + translateConstellation(decoder.getConstellation())));
        node.add(create("层次信息 = " + translateHierarchyInformation(decoder.getHierarchyInformation())));
        node.add(create("高优先级流编码率模式 = " + translateCodeRate(decoder.getCodeRateHPStream())));
        node.add(create("低优先级流编码率模式 = " + translateCodeRate(decoder.getCodeRateLPStream())));
        node.add(create("保护间隙 = " + translateGuardInterval(decoder.getGuardInterval())));
        node.add(create("传输模式 = " + translateTransmissionMode(decoder.getTransmissionMode())));
        node.add(create("其他频率标志 = " + decoder.getOtherFrequencyFlag()));

        return node;
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
