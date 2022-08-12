package m2tk.assistant.dbi.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TR290EventEntity
{
    public static final String TC_TS_SYNC_LOSS = "1.1";
    public static final String TC_SYNC_BYTE_ERROR = "1.2";
    public static final String TC_PAT_ERROR_2 = "1.3.a";
    public static final String TC_CONTINUITY_COUNT_ERROR = "1.4";
    public static final String TC_PMT_ERROR_2 = "1.5.a";
    public static final String TC_PID_ERROR = "1.6";

    public static final String TC_TRANSPORT_ERROR = "2.1";
    public static final String TC_CRC_ERROR = "2.2";
    public static final String TC_PCR_ERROR = "2.3";
    public static final String TC_PCR_REPETITION_ERROR = "2.3.a";
    public static final String TC_PCR_DISCONTINUITY_INDICATOR_ERROR = "2.3.b";
    public static final String TC_PCR_ACCURACY_ERROR = "2.4";
    public static final String TC_PTS_ERROR = "2.5";
    public static final String TC_CAT_ERROR = "2.6";

    public static final String TC_NIT_ERROR = "3.1";
    public static final String TC_NIT_ACTUAL_ERROR = "3.1.a";
    public static final String TC_NIT_OTHER_ERROR = "3.1.b";
    public static final String TC_SI_REPETITION_ERROR = "3.2";
    public static final String TC_BUFFER_ERROR = "3.3";
    public static final String TC_UNREFERENCED_PID = "3.4";
    public static final String TC_UNREFERENCED_PID_2 = "3.4.a";
    public static final String TC_SDT_ERROR = "3.5";
    public static final String TC_SDT_ACTUAL_ERROR = "3.5.a";
    public static final String TC_SDT_OTHER_ERROR = "3.5.b";
    public static final String TC_EIT_ERROR = "3.6";
    public static final String TC_EIT_ACTUAL_ERROR = "3.6.a";
    public static final String TC_EIT_OTHER_ERROR = "3.6.b";
    public static final String TC_EIT_PF_ERROR = "3.6.c";
    public static final String TC_RST_ERROR = "3.7";
    public static final String TC_TDT_ERROR = "3.8";
    public static final String TC_EMPTY_BUFFER_ERROR = "3.9";
    public static final String TC_DATA_DELAY_ERROR = "3.10";

    private long id;
    private String type;
    private String description;
    private int streamPid;
    private long position;
    private LocalDateTime timestamp;
}
