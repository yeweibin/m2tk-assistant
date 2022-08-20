package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.dvb.decoder.descriptor.MultilingualComponentDescriptorDecoder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class MultilingualComponentDescriptorNodeBuilder implements TreeNodeBuilder
{
    private final MultilingualComponentDescriptorDecoder decoder = new MultilingualComponentDescriptorDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode("multilingual_network_name_descriptor");
        node.add(create(String.format("descriptor_tag = 0x%02X", decoder.getTag())));
        node.add(create(String.format("descriptor_length = %d", decoder.getPayloadLength())));

        node.add(create(String.format("component_tag = 0x%02X", decoder.getComponentTag())));

        Encoding[] descriptions = decoder.getMultilingualDescriptions();
        DefaultMutableTreeNode nodeList = new DefaultMutableTreeNode();
        for (int i = 0; i < descriptions.length; i++)
        {
            Encoding description = descriptions[i];
            String langCode = decoder.getLanguageCode(description);
            String descText = decoder.getDescriptionText(description);
            String text = String.format("描述%d = （%s）'%s'（原始数据：[%s]）",
                                        i + 1,
                                        langCode,
                                        descText,
                                        description.toHexStringPrettyPrint(4, description.size()));
            nodeList.add(create(text));
        }
        nodeList.setUserObject(String.format("多语言描述（%d）", nodeList.getChildCount()));
        node.add(nodeList);

        return node;
    }
}
