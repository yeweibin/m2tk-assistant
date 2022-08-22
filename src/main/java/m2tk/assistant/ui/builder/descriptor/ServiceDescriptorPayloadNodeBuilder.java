package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.analyzer.presets.ServiceTypes;
import m2tk.assistant.ui.builder.DescriptorPayloadNodeBuilder;
import m2tk.dvb.DVB;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;

public class ServiceDescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        int serviceType = payload.readUINT8(0);
        int serviceProviderNameLength = payload.readUINT8(1);
        String serviceProviderName = DVB.decodeString(payload.getRange(2, 2 + serviceProviderNameLength));
        int serviceNameLength = payload.readUINT8(2 + serviceProviderNameLength);
        String serviceName = DVB.decodeString(payload.getRange(2 + serviceProviderNameLength + 1,
                                                               2 + serviceProviderNameLength + 1 + serviceNameLength));

        node.add(create(String.format("service_type = 0x%02X（%s）",
                                      serviceType,
                                      ServiceTypes.name(serviceType))));
        node.add(create(String.format("service_provider_name_length = %d", serviceProviderNameLength)));
        node.add(create(String.format("service_provider_name = '%s'（原始数据：[%s]）",
                                      serviceProviderName,
                                      payload.toHexStringPrettyPrint(2, 2 + serviceProviderNameLength))));
        node.add(create(String.format("service_name_length = %d", serviceNameLength)));
        node.add(create(String.format("service_name = '%s'（原始数据：[%s]）",
                                      serviceName,
                                      payload.toHexStringPrettyPrint(2 + serviceProviderNameLength + 1,
                                                                     2 + serviceProviderNameLength + 1 + serviceNameLength))));
    }
}
