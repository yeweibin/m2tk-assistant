package m2tk.assistant.dbi.entity;

import m2tk.assistant.analyzer.presets.StreamTypes;

import java.util.Objects;

public class ProgramStreamMappingEntity
{
    private long id;
    private int programNumber;
    private int streamPid;
    private int streamType;
    private String streamCategory;
    private String streamDescription;

    public ProgramStreamMappingEntity()
    {
        streamCategory = StreamTypes.CATEGORY_USER_PRIVATE;
        streamDescription = StreamTypes.description(0xFF);
    }

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public int getProgramNumber()
    {
        return programNumber;
    }

    public void setProgramNumber(int programNumber)
    {
        this.programNumber = programNumber;
    }

    public int getStreamPid()
    {
        return streamPid;
    }

    public void setStreamPid(int streamPid)
    {
        this.streamPid = streamPid;
    }

    public int getStreamType()
    {
        return streamType;
    }

    public void setStreamType(int streamType)
    {
        this.streamType = streamType;
    }

    public String getStreamCategory()
    {
        return streamCategory;
    }

    public void setStreamCategory(String streamCategory)
    {
        this.streamCategory = streamCategory;
    }

    public String getStreamDescription()
    {
        return streamDescription;
    }

    public void setStreamDescription(String streamDescription)
    {
        this.streamDescription = streamDescription;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProgramStreamMappingEntity entity = (ProgramStreamMappingEntity) o;
        return programNumber == entity.programNumber && streamPid == entity.streamPid;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(programNumber, streamPid);
    }
}
