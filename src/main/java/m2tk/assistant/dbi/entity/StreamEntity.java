package m2tk.assistant.dbi.entity;

import m2tk.assistant.analyzer.presets.StreamTypes;

public class StreamEntity
{
    private long id;
    private int pid;
    private long packetCount;
    private int continuityErrorCount;
    private int bitrate;
    private double ratio;
    private boolean scrambled;
    private String category;
    private String description;

    public StreamEntity()
    {
        pid = 0x1FFF;
        category = StreamTypes.CATEGORY_USER_PRIVATE;
        description = "空包";
    }

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public int getPid()
    {
        return pid;
    }

    public void setPid(int pid)
    {
        this.pid = pid;
    }

    public long getPacketCount()
    {
        return packetCount;
    }

    public void setPacketCount(long packetCount)
    {
        this.packetCount = packetCount;
    }

    public int getContinuityErrorCount()
    {
        return continuityErrorCount;
    }

    public void setContinuityErrorCount(int continuityErrorCount)
    {
        this.continuityErrorCount = continuityErrorCount;
    }

    public int getBitrate()
    {
        return bitrate;
    }

    public void setBitrate(int bitrate)
    {
        this.bitrate = bitrate;
    }

    public double getRatio()
    {
        return ratio;
    }

    public void setRatio(double ratio)
    {
        this.ratio = ratio;
    }

    public boolean isScrambled()
    {
        return scrambled;
    }

    public void setScrambled(boolean scrambled)
    {
        this.scrambled = scrambled;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }
}
