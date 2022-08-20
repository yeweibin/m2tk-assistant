package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.dvb.decoder.descriptor.BouquetNameDescriptorDecoder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class BouquetNameDescriptorNodeBuilder implements TreeNodeBuilder
{
    private final BouquetNameDescriptorDecoder decoder = new BouquetNameDescriptorDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode("bouquet_name_descriptor");
        node.add(create(String.format("descriptor_tag = 0x%02X", decoder.getTag())));
        node.add(create(String.format("descriptor_length = %d", decoder.getPayloadLength())));

        String name = decoder.getBouquetName();
        node.add(create(String.format("bouquet name = '%s'（原始数据：%s）",
                                      name.isEmpty() ? "" : name,
                                      name.isEmpty() ? "[]" : decoder.getPayload().toHexStringPrettyPrint())));

        return node;
    }
}
