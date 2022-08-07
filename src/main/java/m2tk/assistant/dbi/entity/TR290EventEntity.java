package m2tk.assistant.dbi.entity;

public class TR290EventEntity
{
    private long id;
    private String typeCode;
    private int streamPid;
    private long position;

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public String getTypeCode()
    {
        return typeCode;
    }

    public void setTypeCode(String typeCode)
    {
        this.typeCode = typeCode;
    }

    public int getStreamPid()
    {
        return streamPid;
    }

    public void setStreamPid(int streamPid)
    {
        this.streamPid = streamPid;
    }

    public long getPosition()
    {
        return position;
    }

    public void setPosition(long position)
    {
        this.position = position;
    }
}
