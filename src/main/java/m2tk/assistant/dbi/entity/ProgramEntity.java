package m2tk.assistant.dbi.entity;

public class ProgramEntity
{
    private long id;
    private int programNumber;
    private int transportStreamId;
    private int pmtPid;
    private int pcrPid;
    private int pmtVersion;
    private boolean freeAccess;
    private String programName;

    public ProgramEntity()
    {
        pmtPid = 0x1FFF;
        pcrPid = 0x1FFF;
        pmtVersion = -1;
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

    public int getTransportStreamId()
    {
        return transportStreamId;
    }

    public void setTransportStreamId(int transportStreamId)
    {
        this.transportStreamId = transportStreamId;
    }

    public int getPmtPid()
    {
        return pmtPid;
    }

    public void setPmtPid(int pmtPid)
    {
        this.pmtPid = pmtPid;
    }

    public int getPcrPid()
    {
        return pcrPid;
    }

    public void setPcrPid(int pcrPid)
    {
        this.pcrPid = pcrPid;
    }

    public int getPmtVersion()
    {
        return pmtVersion;
    }

    public void setPmtVersion(int pmtVersion)
    {
        this.pmtVersion = pmtVersion;
    }

    public boolean isFreeAccess()
    {
        return freeAccess;
    }

    public void setFreeAccess(boolean freeAccess)
    {
        this.freeAccess = freeAccess;
    }

    public String getProgramName()
    {
        return programName;
    }

    public void setProgramName(String programName)
    {
        this.programName = programName;
    }
}
