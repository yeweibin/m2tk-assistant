package m2tk.assistant.ui.builder;

import m2tk.assistant.ui.builder.descriptor.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class DescriptorNodeBuilders
{
    private static final Map<Integer, Class<? extends TreeNodeBuilder>> builderClasses;
    static
    {
        builderClasses = new HashMap<>();
        builderClasses.put(0x09, CADescriptorNodeBuilder.class);
        builderClasses.put(0x40, NetworkNameDescriptorNodeBuilder.class);
        builderClasses.put(0x41, ServiceListDescriptorNodeBuilder.class);
        builderClasses.put(0x42, StuffingDescriptorNodeBuilder.class);
        builderClasses.put(0x43, SatelliteDeliverySystemDescriptorNodeBuilder.class);
        builderClasses.put(0x44, CableDeliverySystemDescriptorNodeBuilder.class);
        builderClasses.put(0x47, BouquetNameDescriptorNodeBuilder.class);
        builderClasses.put(0x48, ServiceDescriptorNodeBuilder.class);
        builderClasses.put(0x4A, LinkageDescriptorNodeBuilder.class);
        builderClasses.put(0x4D, ShortEventDescriptorNodeBuilder.class);
        builderClasses.put(0x4E, ExtendedEventDescriptorNodeBuilder.class);
        builderClasses.put(0x53, CAIdentifierDescriptorNodeBuilder.class);
        builderClasses.put(0x54, ContentDescriptorNodeBuilder.class);
        builderClasses.put(0x55, ParentalRatingDescriptorNodeBuilder.class);
        builderClasses.put(0x5A, TerrestrialDeliverySystemDescriptorNodeBuilder.class);
        builderClasses.put(0x5B, MultilingualNetworkNameDescriptorNodeBuilder.class);
        builderClasses.put(0x5C, MultilingualBouquetNameDescriptorNodeBuilder.class);
        builderClasses.put(0x5D, MultilingualServiceNameDescriptorNodeBuilder.class);
        builderClasses.put(0x5E, MultilingualComponentDescriptorNodeBuilder.class);
        builderClasses.put(0x62, FrequencyListDescriptorNodeBuilder.class);
        builderClasses.put(0x68, DSNGDescriptorNodeBuilder.class);
        builderClasses.put(0x6B, AncillaryDataDescriptorNodeBuilder.class);
        builderClasses.put(0x70, AdaptationFieldDataDescriptorNodeBuilder.class);
    }

    private DescriptorNodeBuilders()
    {}

    public static void registerBuilder(int tag, Class<? extends TreeNodeBuilder> builderClass)
    {
        Objects.requireNonNull(builderClass);
        builderClasses.put(tag, builderClass);
    }

    public static TreeNodeBuilder getBuilder(int tag)
    {
        try
        {
            Class<? extends TreeNodeBuilder> cls = builderClasses.getOrDefault(tag, GenericDescriptorNodeBuilder.class);
            return cls.getDeclaredConstructor().newInstance();
        } catch (Exception ex)
        {
            return new GenericDescriptorNodeBuilder();
        }
    }
}
