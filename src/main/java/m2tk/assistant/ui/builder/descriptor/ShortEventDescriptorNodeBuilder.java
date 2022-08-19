package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.dvb.decoder.descriptor.ShortEventDescriptorDecoder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class ShortEventDescriptorNodeBuilder implements TreeNodeBuilder
{
    private final ShortEventDescriptorDecoder decoder = new ShortEventDescriptorDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode("short_event_descriptor");
        node.add(create("descriptor_tag = 0x4D"));
        node.add(create("descriptor_length = " + decoder.getPayloadLength()));
        node.add(create("ISO_639_language_code = " + decoder.getLanguageCode()));

        int len = encoding.readUINT8(5);
        node.add(create(String.format("event_name_length = %d", len)));
        node.add(create(String.format("event_name = '%s'（原始数据：%s）",
                                      (len == 0) ? "" : decoder.getEventName(),
                                      (len == 0) ? "[]" : encoding.toHexStringPrettyPrint(6, 6 + len))));

        int offset = 6 + len;
        len = encoding.readUINT8(offset);
        node.add(create(String.format("text_length = %d", len)));
        node.add(create(String.format("text = '%s'（原始数据：%s）",
                                      (len == 0) ? "" : decoder.getEventDescription(),
                                      (len == 0) ? "[]" : encoding.toHexStringPrettyPrint(offset + 1, offset + 1 + len))));

        return node;
    }
}
