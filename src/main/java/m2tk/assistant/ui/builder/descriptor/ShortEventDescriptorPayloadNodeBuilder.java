package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.DescriptorPayloadNodeBuilder;
import m2tk.dvb.DVB;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;

public class ShortEventDescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        String langCode = DVB.decodeThreeLetterCode(payload.readUINT24(0));
        int eventNameLength = payload.readUINT8(3);
        String eventName = DVB.decodeString(payload.getRange(4, 4 + eventNameLength));
        int eventDescLength = payload.readUINT8(4 + eventNameLength);
        String eventDesc = DVB.decodeString(payload.getRange(4 + eventNameLength + 1,
                                                             4 + eventNameLength + 1 + eventDescLength));

        node.add(create(String.format("ISO_639_language_code = '%s'", langCode)));
        node.add(create(String.format("event_name_length = %d", eventNameLength)));
        node.add(create(String.format("event_name = '%s'（原始数据：[%s]）",
                                      eventName,
                                      payload.toHexStringPrettyPrint(4, 4 + eventNameLength))));
        node.add(create(String.format("text_length = %d", eventDescLength)));
        node.add(create(String.format("text = '%s'（原始数据：[%s]）",
                                      eventDesc,
                                      payload.toHexStringPrettyPrint(4 + eventNameLength + 1,
                                                                     4 + eventNameLength + 1 + eventDescLength))));
    }
}
