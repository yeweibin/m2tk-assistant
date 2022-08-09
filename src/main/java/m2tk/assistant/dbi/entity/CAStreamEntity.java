package m2tk.assistant.dbi.entity;

import lombok.Data;

@Data
public class CAStreamEntity
{
    public static final int TYPE_EMM = 0;
    public static final int TYPE_ECM = 1;

    private long id;
    private int systemId;
    private int streamType;
    private int streamPid;
    private byte[] streamPrivateData;
    private int programNumber;
    private int elementaryStreamPid;
}
