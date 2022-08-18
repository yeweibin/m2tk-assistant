package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.dvb.DVB;
import m2tk.dvb.decoder.descriptor.CableDeliverySystemDescriptorDecoder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class CableDeliverySystemDescriptorNodeBuilder implements TreeNodeBuilder
{
    private final CableDeliverySystemDescriptorDecoder decoder = new CableDeliverySystemDescriptorDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode("cable_delivery_system_descriptor");

        node.add(create("descriptor_tag = 0x44"));
        node.add(create("descriptor_length = " + decoder.getPayloadLength()));
        node.add(create("频率 = " + DVB.decodeFrequencyCode(decoder.getFrequencyCode()) + " MHz"));
        node.add(create("FEC外码 = " + fourBits(decoder.getOuterFECScheme()) + "（" + outerFECScheme(decoder.getInnerFECScheme()) + "）"));
        node.add(create("调制方式 = " + modulationType(decoder.getModulationType())));
        node.add(create("符号率 = " + DVB.decodeSymbolRateCode(decoder.getSymbolRateCode()) + " Msymbol/s"));
        node.add(create("FEC内码 = " + fourBits(decoder.getInnerFECScheme()) + "（" + innerFECScheme(decoder.getInnerFECScheme()) + "）"));

        return node;
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
                return "无FEC外码";
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
