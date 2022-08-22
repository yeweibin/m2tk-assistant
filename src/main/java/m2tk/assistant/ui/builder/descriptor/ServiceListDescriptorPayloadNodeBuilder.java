package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.analyzer.presets.ServiceTypes;
import m2tk.assistant.ui.builder.DescriptorPayloadNodeBuilder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;

public class ServiceListDescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    private static final int BLOCK_SIZE = 3;

    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        int count = payload.size() / BLOCK_SIZE;
        for (int i = 0; i < count; i++)
        {
            int serviceId = payload.readUINT16(i * BLOCK_SIZE);
            int serviceType = payload.readUINT8(i * BLOCK_SIZE + 2);

            String text = String.format("业务说明%d：[业务号：%d，业务类型：（0x%02X）%s]",
                                        i + 1,
                                        serviceId,
                                        serviceType,
                                        ServiceTypes.name(serviceType));
            node.add(create(text));
        }
    }
}
