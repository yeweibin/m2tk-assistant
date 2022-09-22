package m2tk.assistant.dbi.entity;

import lombok.Data;

@Data
public class ProgramEntity
{
    private long id;
    private long transactionId;
    private int programNumber;
    private int transportStreamId;
    private int pmtPid;
    private int pcrPid;
    private int pmtVersion;
    private boolean freeAccess;

    public ProgramEntity()
    {
        pmtPid = 0x1FFF;
        pcrPid = 0x1FFF;
        pmtVersion = -1;
    }
}
