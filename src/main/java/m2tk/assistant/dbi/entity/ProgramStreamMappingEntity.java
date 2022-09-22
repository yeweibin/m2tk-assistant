package m2tk.assistant.dbi.entity;

import lombok.Data;
import m2tk.assistant.analyzer.presets.StreamTypes;

import java.util.Objects;

@Data
public class ProgramStreamMappingEntity
{
    private long id;
    private long transactionId;
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
