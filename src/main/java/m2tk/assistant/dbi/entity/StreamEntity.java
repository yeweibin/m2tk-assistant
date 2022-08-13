package m2tk.assistant.dbi.entity;

import lombok.Data;
import m2tk.assistant.analyzer.presets.StreamTypes;

@Data
public class StreamEntity
{
    private long id;
    private int pid;
    private boolean marked;
    private long packetCount;
    private int pcrCount;
    private int continuityErrorCount;
    private int bitrate;
    private double ratio;
    private boolean scrambled;
    private String category;
    private String description;

    public StreamEntity()
    {
        pid = 0x1FFF;
        marked = false;
        category = StreamTypes.CATEGORY_USER_PRIVATE;
        description = "空包";
    }
}
