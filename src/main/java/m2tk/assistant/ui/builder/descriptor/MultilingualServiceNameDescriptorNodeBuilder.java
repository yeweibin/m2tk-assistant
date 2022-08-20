package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.dvb.decoder.descriptor.MultilingualServiceNameDescriptorDecoder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class MultilingualServiceNameDescriptorNodeBuilder implements TreeNodeBuilder
{
    private final MultilingualServiceNameDescriptorDecoder decoder = new MultilingualServiceNameDescriptorDecoder();

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
            String providerName = decoder.getMultilingualServiceProviderName(name);
            String serviceName = decoder.getMultilingualServiceName(name);

            DefaultMutableTreeNode nodeDesc = new DefaultMutableTreeNode("业务描述" + (i + 1));
            nodeDesc.add(create(String.format("业务提供商 = （%s）'%s'（原始数据：[%s]）",
                                              langCode,
                                              providerName,
                                              name.toHexStringPrettyPrint(4, name.readUINT8(3)))));

            int offset = 4 + name.readUINT8(3);
            nodeDesc.add(create(String.format("业务名 = （%s）'%s'（原始数据：[%s]）",
                                              langCode,
                                              serviceName,
                                              name.toHexStringPrettyPrint(offset + 1, offset + 1 + name.readUINT8(offset)))));

            nodeList.add(nodeDesc);
        }
        nodeList.setUserObject(String.format("多语言名称（%d）", nodeList.getChildCount()));
        node.add(nodeList);

        return node;
    }
}
