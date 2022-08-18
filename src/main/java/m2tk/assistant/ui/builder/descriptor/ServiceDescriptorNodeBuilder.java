package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.analyzer.presets.ServiceTypes;
import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.dvb.decoder.descriptor.ServiceDescriptorDecoder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class ServiceDescriptorNodeBuilder implements TreeNodeBuilder
{
    private final ServiceDescriptorDecoder decoder = new ServiceDescriptorDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode("service_descriptor");
        node.add(create("descriptor_tag = 0x48"));
        node.add(create("descriptor_length = " + decoder.getPayloadLength()));
        node.add(create(String.format("service_type = %d（%s）",
                                      decoder.getServiceType(),
                                      ServiceTypes.name(decoder.getServiceType()))));

        int len = encoding.readUINT8(3);
        if (len == 0)
        {
            node.add(create("service_provider_name_length = 0"));
            node.add(create("service_provider_name = []"));
        } else
        {
            node.add(create(String.format("service_provider_name_length = %d", len)));
            node.add(create(String.format("service_provider_name = %s（原始数据：%s）",
                                          decoder.getServiceProviderName(),
                                          encoding.toHexStringPrettyPrint(4, 4 + len))));
        }

        int offset = 4 + len;
        len = encoding.readUINT8(offset);
        if (len == 0)
        {
            node.add(create("service_name_length = 0"));
            node.add(create("service_name = []"));
        } else
        {
            node.add(create(String.format("service_name_length = %d", len)));
            node.add(create(String.format("service_name = %s（原始数据：%s）",
                                          decoder.getServiceName(),
                                          encoding.toHexStringPrettyPrint(offset + 1, offset + 1 + len))));
        }

        return node;
    }
}
