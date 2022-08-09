package m2tk.assistant.analyzer.domain;

import lombok.Getter;

@Getter
public class ElementaryStream
{
    private final int streamPid;
    private final long packetCount;
    private final int continuityErrorCount;
    private final int bitrate;
    private final double ratio;
    private final boolean scrambled;
    private final int streamType;
    private final String category;
    private final String description;
    private final int associatedProgramNumber;

    public ElementaryStream(int streamPid,
                            int streamType,
                            String category,
                            String description,
                            int associatedProgramNumber)
    {
        this(streamPid,
             0,
             0,
             0,
             0,
             false,
             streamType,
             category,
             description,
             associatedProgramNumber);
    }

    public ElementaryStream(int streamPid,
                            long packetCount,
                            int continuityErrorCount,
                            int bitrate,
                            double ratio,
                            boolean scrambled,
                            int streamType,
                            String category,
                            String description,
                            int associatedProgramNumber)
    {
        this.streamPid = streamPid;
        this.packetCount = packetCount;
        this.continuityErrorCount = continuityErrorCount;
        this.bitrate = bitrate;
        this.ratio = ratio;
        this.scrambled = scrambled;
        this.streamType = streamType;
        this.category = category;
        this.description = description;
        this.associatedProgramNumber = associatedProgramNumber;
    }

    public boolean isPresent()
    {
        return packetCount > 0;
    }
}
