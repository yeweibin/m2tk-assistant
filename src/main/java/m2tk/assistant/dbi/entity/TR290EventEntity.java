package m2tk.assistant.dbi.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TR290EventEntity
{
    private long id;
    private long transactionId;
    private String type;
    private String description;
    private int streamPid;
    private long position;
    private LocalDateTime timestamp;
}
