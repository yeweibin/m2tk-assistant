package m2tk.assistant.dbi.entity;

import lombok.Data;

@Data
public class SourceEntity
{
    private long id;
    private int bitrate;
    private int frameSize;
    private int transportStreamId;
    private long packetCount;
    private String sourceName;
}
