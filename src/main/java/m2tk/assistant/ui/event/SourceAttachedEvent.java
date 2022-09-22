package m2tk.assistant.ui.event;

import lombok.Data;
import m2tk.assistant.dbi.entity.SourceEntity;

@Data
public class SourceAttachedEvent
{
    private SourceEntity source;
}
