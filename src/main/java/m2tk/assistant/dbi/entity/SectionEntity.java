package m2tk.assistant.dbi.entity;

public class SectionEntity
{
    private long id;
    private int streamPid;
    private long position;
    private int tableId;
    private String name;
    private byte[] encoding;

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
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

    public int getTableId()
    {
        return tableId;
    }

    public void setTableId(int tableId)
    {
        this.tableId = tableId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public byte[] getEncoding()
    {
        return encoding;
    }

    public void setEncoding(byte[] encoding)
    {
        this.encoding = encoding;
    }
}
