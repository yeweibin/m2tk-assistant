package m2tk.assistant.dbi.entity;

public class CAStreamEntity
{
    public static final int TYPE_EMM = 0;
    public static final int TYPE_ECM = 1;

    private long id;
    private int systemId;
    private int streamType;
    private int streamPid;
    private byte[] streamPrivateData;
    private int programNumber;
    private int elementaryStreamPid;

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public int getSystemId()
    {
        return systemId;
    }

    public void setSystemId(int systemId)
    {
        this.systemId = systemId;
    }

    public int getStreamType()
    {
        return streamType;
    }

    public void setStreamType(int streamType)
    {
        this.streamType = streamType;
    }

    public int getStreamPid()
    {
        return streamPid;
    }

    public void setStreamPid(int streamPid)
    {
        this.streamPid = streamPid;
    }

    public byte[] getStreamPrivateData()
    {
        return streamPrivateData;
    }

    public void setStreamPrivateData(byte[] streamPrivateData)
    {
        this.streamPrivateData = streamPrivateData;
    }

    public int getProgramNumber()
    {
        return programNumber;
    }

    public void setProgramNumber(int programNumber)
    {
        this.programNumber = programNumber;
    }

    public int getElementaryStreamPid()
    {
        return elementaryStreamPid;
    }

    public void setElementaryStreamPid(int elementaryStreamPid)
    {
        this.elementaryStreamPid = elementaryStreamPid;
    }
}
