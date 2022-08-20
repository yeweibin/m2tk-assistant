package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.dvb.decoder.descriptor.MultilingualNetworkNameDescriptorDecoder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class MultilingualNetworkNameDescriptorNodeBuilder implements TreeNodeBuilder
{
    private final MultilingualNetworkNameDescriptorDecoder decoder = new MultilingualNetworkNameDescriptorDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode("multilingual_network_name_descriptor");
        node.add(create(String.format("descriptor_tag = 0x%02X", decoder.getTag())));
        node.add(create(String.format("descriptor_length = %d", decoder.getPayloadLength())));

        Encoding[] names = decoder.getMultilingualNames();
        DefaultMutableTreeNode nodeList = new DefaultMutableTreeNode();
        for (int i = 0; i < names.length; i++)
        {
            Encoding name = names[i];
            String langCode = decoder.getLanguageCode(name);
            String netName = decoder.getMultilingualName(name);

            String text = String.format("网络名%d = （%s）'%s'（原始数据：[%s]）",
                                        i + 1,
                                        langCode,
                                        netName,
                                        name.toHexStringPrettyPrint(4, name.size()));
            nodeList.add(create(text));
        }
        nodeList.setUserObject(String.format("多语言名称（%d）", nodeList.getChildCount()));
        node.add(nodeList);

        return node;
    }
}
