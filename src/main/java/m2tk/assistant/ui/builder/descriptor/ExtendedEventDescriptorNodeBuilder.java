package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.dvb.decoder.descriptor.ExtendedEventDescriptorDecoder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class ExtendedEventDescriptorNodeBuilder implements TreeNodeBuilder
{
    private final ExtendedEventDescriptorDecoder decoder = new ExtendedEventDescriptorDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode("extended_event_descriptor");
        node.add(create("descriptor_tag = 0x4E"));
        node.add(create("descriptor_length = " + decoder.getPayloadLength()));
        node.add(create("descriptor_number = " + decoder.getDescriptorNumber()));
        node.add(create("last_descriptor_number = " + decoder.getLastDescriptorNumber()));
        node.add(create("ISO_639_language_code = " + decoder.getLanguageCode()));

        Encoding[] items = decoder.getItems();
        for (int i = 0; i < items.length; i++)
        {
            Encoding item = items[i];
            DefaultMutableTreeNode nodeItem = new DefaultMutableTreeNode("条目" + (i + 1));

            int len = decoder.getItemDescriptionLength(item);
            if (len == 0)
            {
                nodeItem.add(create("item_description_length = 0"));
                nodeItem.add(create("item_description = []"));
            } else
            {
                nodeItem.add(create(String.format("item_description_length = %d", len)));
                nodeItem.add(create(String.format("item_description = %s（原始数据：%s）",
                                                  decoder.getItemDescription(item),
                                                  item.toHexStringPrettyPrint(1, 1 + len))));
            }

            int offset = 1 + len;
            len = decoder.getItemTextLength(item);
            if (len == 0)
            {
                nodeItem.add(create("item_length = 0"));
                nodeItem.add(create("item = []"));
            } else
            {
                nodeItem.add(create(String.format("item_length = %d", len)));
                nodeItem.add(create(String.format("item = %s（原始数据：%s）",
                                                  decoder.getItemText(item),
                                                  item.toHexStringPrettyPrint(offset + 1, offset + 1 + len))));
            }

            node.add(nodeItem);
        }

        int offset = 7 + decoder.getLengthOfItems();
        int len = decoder.getTextLength();
        if (len == 0)
        {
            node.add(create("text_length = 0"));
            node.add(create("text = []"));
        } else
        {
            node.add(create(String.format("text_length = %d", len)));
            node.add(create(String.format("text = %s（原始数据：%s）",
                                          decoder.getText(),
                                          encoding.toHexStringPrettyPrint(offset + 1, offset + 1 + len))));
        }

        return node;
    }
}
