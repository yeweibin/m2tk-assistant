package m2tk.assistant.dbi.entity;

public class SourceEntity
{
    private long id;
    private int bitrate;
    private int frameSize;
    private int transportStreamId;
    private long packetCount;
    private String sourceName;

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public int getBitrate()
    {
        return bitrate;
    }

    public void setBitrate(int bitrate)
    {
        this.bitrate = bitrate;
    }

    public int getFrameSize()
    {
        return frameSize;
    }

    public void setFrameSize(int frameSize)
    {
        this.frameSize = frameSize;
    }

    public int getTransportStreamId()
    {
        return transportStreamId;
    }

    public void setTransportStreamId(int transportStreamId)
    {
        this.transportStreamId = transportStreamId;
    }

    public long getPacketCount()
    {
        return packetCount;
    }

    public void setPacketCount(long packetCount)
    {
        this.packetCount = packetCount;
    }

    public String getSourceName()
    {
        return sourceName;
    }

    public void setSourceName(String sourceName)
    {
        this.sourceName = sourceName;
    }
}
