package m2tk.assistant.ui.event;

import lombok.Data;

@Data
public class SourceChangedEvent
{
    private String sourceName;
    private long transactionId;
}
