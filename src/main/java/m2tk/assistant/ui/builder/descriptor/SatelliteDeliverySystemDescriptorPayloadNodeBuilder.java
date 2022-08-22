package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.DescriptorPayloadNodeBuilder;
import m2tk.dvb.DVB;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;

public class SatelliteDeliverySystemDescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        long frequency = payload.readUINT32(0);
        int orbitalPosition = payload.readUINT16(4);
        int westEastFlag = (payload.readUINT8(6) >> 7) & 0b1;
        int polarization = (payload.readUINT8(6) >> 5) & 0b11;
        int modulation = (payload.readUINT8(6) & 0b11111);
        int symbolRate = (int) (payload.readUINT32(7) & 0xFFFFFFF);
        int FECInner = (payload.readUINT8(10) & 0xF);

        node.add(create(String.format("下行频率 = %s", DVB.translateSatelliteFrequencyCode(frequency))));
        node.add(create(String.format("轨道位置 = %s", translateOrbitalPosition(polarization, westEastFlag))));
        node.add(create(String.format("极化方式 = %s", polarizationType(polarization))));
        node.add(create(String.format("调制方式 = %s", modulationType(modulation))));
        node.add(create(String.format("符号率 = %s", DVB.translateSymbolRateCode(symbolRate))));
        node.add(create(String.format("前向纠错内码 = '%s'（%s）",
                                      fourBits(FECInner),
                                      innerFECScheme(FECInner))));
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
        return bits.substring(binary.length()) + binary;
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
