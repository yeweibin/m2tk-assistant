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
        node.add(create("descriptor_tag = 0x47"));
        node.add(create("descriptor_length = " + decoder.getPayloadLength()));
        node.add(create(String.format("bouquet name = %s（原始数据：%s）",
                                      decoder.getBouquetName(),
                                      decoder.getPayload().toHexStringPrettyPrint())));

        return node;
    }
}
