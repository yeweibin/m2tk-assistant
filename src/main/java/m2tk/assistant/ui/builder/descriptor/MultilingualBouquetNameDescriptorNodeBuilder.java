package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.dvb.decoder.descriptor.MultilingualBouquetNameDescriptorDecoder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class MultilingualBouquetNameDescriptorNodeBuilder implements TreeNodeBuilder
{
    private final MultilingualBouquetNameDescriptorDecoder decoder = new MultilingualBouquetNameDescriptorDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode("multilingual_bouquet_name_descriptor");
        node.add(create(String.format("descriptor_tag = 0x%02X", decoder.getTag())));
        node.add(create(String.format("descriptor_length = %d", decoder.getPayloadLength())));

        Encoding[] names = decoder.getMultilingualNames();
        DefaultMutableTreeNode nodeList = new DefaultMutableTreeNode();
        for (int i = 0; i < names.length; i++)
        {
            Encoding name = names[i];
            String langCode = decoder.getLanguageCode(name);
            String bqtName = decoder.getMultilingualName(name);

            String text = String.format("业务群名%d = （%s）'%s'（原始数据：[%s]）",
                                        i + 1,
                                        langCode,
                                        bqtName,
                                        name.toHexStringPrettyPrint(4, name.size()));
            nodeList.add(create(text));
        }
        nodeList.setUserObject(String.format("多语言名称（%d）", nodeList.getChildCount()));
        node.add(nodeList);

        return node;
    }
}
