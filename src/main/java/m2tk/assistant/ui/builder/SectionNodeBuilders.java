package m2tk.assistant.ui.builder;

import m2tk.assistant.ui.builder.section.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class SectionNodeBuilders
{
    private static final Map<Integer, Class<? extends TreeNodeBuilder>> builderClasses;
    static
    {
        builderClasses = new HashMap<>();
        builderClasses.put(0x00, PATNodeBuilder.class);
        builderClasses.put(0x01, CATNodeBuilder.class);
        builderClasses.put(0x02, PMTNodeBuilder.class);
        builderClasses.put(0x40, NITNodeBuilder.class);
        builderClasses.put(0x41, NITNodeBuilder.class);
        builderClasses.put(0x42, SDTNodeBuilder.class);
        builderClasses.put(0x46, SDTNodeBuilder.class);
        builderClasses.put(0x4A, BATNodeBuilder.class);
        builderClasses.put(0x70, TDTNodeBuilder.class);
        builderClasses.put(0x73, TOTNodeBuilder.class);
    }

    private SectionNodeBuilders()
    {}

    public static void registerBuilder(int tag, Class<? extends TreeNodeBuilder> builderClass)
    {
        Objects.requireNonNull(builderClass);
        builderClasses.put(tag, builderClass);
    }

    public static TreeNodeBuilder getBuilder(int tableId)
    {
        try
        {
            Class<? extends TreeNodeBuilder> cls = builderClasses.get(tableId);
            if (cls != null)
                return cls.getDeclaredConstructor().newInstance();

            if (0x4E <= tableId && tableId <= 0x6F)
                return new EITNodeBuilder();

            return new PrivateSectionNodeBuilder();
        } catch (Exception ex)
        {
            return new PrivateSectionNodeBuilder();
        }
    }
}
