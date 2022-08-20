package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.dvb.DVB;
import m2tk.dvb.decoder.descriptor.FrequencyListDescriptorDecoder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class FrequencyListDescriptorNodeBuilder implements TreeNodeBuilder
{
    private final FrequencyListDescriptorDecoder decoder = new FrequencyListDescriptorDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode("frequency_list_descriptor");
        node.add(create(String.format("descriptor_tag = 0x%02X", decoder.getTag())));
        node.add(create(String.format("descriptor_length = %d", decoder.getPayloadLength())));

        node.add(create(String.format("coding_type = %s", codingType(decoder.getCodingType()))));

        long[] frequencyList = decoder.getFrequencyList();
        DefaultMutableTreeNode nodeList = new DefaultMutableTreeNode();
        for (int i = 0;i < frequencyList.length; i++)
        {
            long frequencyCode = frequencyList[i];

            String freqText;
            if (decoder.getCodingType() == 0b01)
                freqText = DVB.translateSatelliteFrequencyCode(frequencyCode);
            else if (decoder.getCodingType() == 0b10)
                freqText = DVB.translateCableFrequencyCode(frequencyCode);
            else if (decoder.getCodingType() == 0b11)
                freqText = DVB.translateTerrestrialFrequencyCode(frequencyCode);
            else
                freqText = "未定义";

            nodeList.add(create(String.format("中心频率%d = %s", i + 1, freqText)));
        }
        nodeList.setUserObject(String.format("频率列表（%d）", nodeList.getChildCount()));
        node.add(nodeList);

        return node;
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
