package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.dvb.DVB;
import m2tk.dvb.decoder.descriptor.SatelliteDeliverySystemDescriptorDecoder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class SatelliteDeliverySystemDescriptorNodeBuilder implements TreeNodeBuilder
{
    private final SatelliteDeliverySystemDescriptorDecoder decoder = new SatelliteDeliverySystemDescriptorDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode("satellite_delivery_system_descriptor");

        node.add(create("descriptor_tag = 0x43"));
        node.add(create("descriptor_length = " + decoder.getPayloadLength()));
        node.add(create("下行频率 = " + DVB.translateSatelliteFrequencyCode(decoder.getFrequencyCode())));
        node.add(create("轨道位置 = " + translateOrbitalPosition(decoder.getOrbitalPositionCode(), decoder.getWestEastFlag())));
        node.add(create("极化方式 = " + polarizationType(decoder.getPolarizationCode())));
        node.add(create("调制方式 = " + modulationType(decoder.getModulationType())));
        node.add(create("符号率 = " + DVB.translateSymbolRateCode(decoder.getSymbolRateCode())));
        node.add(create("前向纠错内码 = " + fourBits(decoder.getInnerFECScheme()) + "（" + innerFECScheme(decoder.getInnerFECScheme()) + "）"));

        return node;
    }

    private String translateOrbitalPosition(int position, int flag)
    {
        int d1 = (position >> 12) & 0xF;
        int d2 = (position >> 8) & 0xF;
        int d3 = (position >> 4) & 0xF;
        int d4 = position & 0xF;
        int p = d1 * 100 + d2 * 10 + d3;

        return String.format("%03d.%d°%s", p, d4, (flag == 0) ? "W" : "E");
    }

    private String polarizationType(int code)
    {
        switch (code)
        {
            case 0b00:
                return "水平极化";
            case 0b01:
                return "垂直极化";
            case 0b10:
                return "左旋圆极化";
            case 0b11:
                return "右旋圆极化";
            default:
                return "";
        }
    }

    private String modulationType(int code)
    {
        switch (code)
        {
            case 0b00000:
                return "未定义";
            case 0b00001:
                return "QPSK";
            default:
                return "预留使用";
        }
    }

    private String fourBits(int value)
    {
        String bits = "0000";
        String binary = Integer.toBinaryString(value);
        return '\'' + bits.substring(binary.length()) + binary + '\'';
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
