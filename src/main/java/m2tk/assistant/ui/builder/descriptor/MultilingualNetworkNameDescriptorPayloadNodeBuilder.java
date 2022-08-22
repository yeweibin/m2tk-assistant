package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.DescriptorPayloadNodeBuilder;
import m2tk.dvb.DVB;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;

public class MultilingualNetworkNameDescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        int offset = 0;
        int i = 0;
        while (offset < payload.size())
        {
            i += 1;
            String langCode = DVB.decodeThreeLetterCode(payload.readUINT24(offset));
            int nameLen = payload.readUINT8(offset + 3);
            String name = DVB.decodeString(payload.getRange(offset + 4, offset + 4 + nameLen));

            String text = String.format("网络名%d = （%s）'%s'（原始数据：[%s]）",
                                        i,
                                        langCode,
                                        name,
                                        payload.toHexStringPrettyPrint(offset + 4, offset + 4 + nameLen));
            node.add(create(text));
            offset += 4 + nameLen;
        }
    }
}
