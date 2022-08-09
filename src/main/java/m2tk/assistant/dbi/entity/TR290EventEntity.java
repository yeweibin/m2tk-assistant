package m2tk.assistant.dbi.entity;

import lombok.Data;

@Data
public class TR290EventEntity
{
    private long id;
    private String typeCode;
    private int streamPid;
    private long position;
}
