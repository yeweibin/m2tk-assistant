package m2tk.assistant.dbi.entity;

import lombok.Data;

@Data
public class SectionEntity
{
    private long id;
    private long transactionId;
    private String tag;
    private int stream;
    private long position;
    private byte[] encoding;
}
