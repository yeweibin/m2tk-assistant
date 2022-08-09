package m2tk.assistant.dbi.entity;

import lombok.Data;

@Data
public class SectionEntity
{
    private long id;
    private int streamPid;
    private long position;
    private int tableId;
    private String name;
    private byte[] encoding;
}
