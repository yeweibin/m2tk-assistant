package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.DescriptorPayloadNodeBuilder;
import m2tk.dvb.DVB;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;

public class BouquetNameDescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        String name = DVB.decodeString(payload.getBytes());
        String text = String.format("业务群名称 = '%s'（原始数据：[%s]）",
                                    name,
                                    payload.toHexStringPrettyPrint());
        node.add(create(text));
    }
}
