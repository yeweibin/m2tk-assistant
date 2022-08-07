package m2tk.assistant.analyzer.domain;

import m2tk.assistant.analyzer.util.ProgramStreamComparator;
import m2tk.assistant.dbi.entity.ProgramEntity;
import m2tk.assistant.dbi.entity.ProgramStreamMappingEntity;
import m2tk.assistant.dbi.entity.StreamEntity;

import java.util.*;

public class MPEGProgram
{
    private final String programName;
    private final int programNumber;
    private final int transportStreamId;
    private final boolean freeAccess;
    private final int bandwidth;
    private final int pmtPid;
    private final int pcrPid;
    private final int pmtVersion;
    private final List<ElementaryStream> ecmList;
    private final List<ElementaryStream> elementList;

    public MPEGProgram(ProgramEntity program,
                       List<ProgramStreamMappingEntity> mappings,
                       Map<Integer, StreamEntity> streams)
    {
        Objects.requireNonNull(program);
        Objects.requireNonNull(mappings);
        Objects.requireNonNull(streams);

        programName = program.getProgramName();
        programNumber = program.getProgramNumber();
        transportStreamId = program.getTransportStreamId();
        pmtPid = program.getPmtPid();
        pcrPid = program.getPcrPid();
        pmtVersion = program.getPmtVersion();

        ecmList = new ArrayList<>();
        elementList = new ArrayList<>();

        int totalBitrate = 0;
        for (ProgramStreamMappingEntity mapping : mappings)
        {
            StreamEntity stream = streams.get(mapping.getStreamPid());

            // 会有不存在的映射吗？会（这也是TR290错误之一），
            // 说明PMT内容有误，或部分ES流中断。
            ElementaryStream es = (stream == null)
                                  ? new ElementaryStream(mapping.getStreamPid(),
                                                         mapping.getStreamType(),
                                                         mapping.getStreamCategory(),
                                                         mapping.getStreamDescription(),
                                                         programNumber)
                                  : new ElementaryStream(mapping.getStreamPid(),
                                                         stream.getPacketCount(),
                                                         stream.getContinuityErrorCount(),
                                                         stream.getBitrate(),
                                                         stream.getRatio(),
                                                         stream.isScrambled(),
                                                         mapping.getStreamType(),
                                                         mapping.getStreamCategory(),
                                                         mapping.getStreamDescription(),
                                                         programNumber);

            if (mapping.getStreamDescription().startsWith("ECM"))
                ecmList.add(es);
            else
                elementList.add(es);

            totalBitrate += es.getBitrate();
        }

        ProgramStreamComparator comparator = new ProgramStreamComparator();
        ecmList.sort(comparator);
        elementList.sort(comparator);

        freeAccess = ecmList.isEmpty();
        bandwidth = totalBitrate;
    }

    public String getProgramName()
    {
        return programName;
    }

    public int getProgramNumber()
    {
        return programNumber;
    }

    public int getTransportStreamId()
    {
        return transportStreamId;
    }

    public boolean isFreeAccess()
    {
        return freeAccess;
    }

    public int getBandwidth()
    {
        return bandwidth;
    }

    public int getPmtVersion()
    {
        return pmtVersion;
    }

    public int getPmtPid()
    {
        return pmtPid;
    }

    public int getPcrPid()
    {
        return pcrPid;
    }

    public List<ElementaryStream> getEcmList()
    {
        return Collections.unmodifiableList(ecmList);
    }

    public List<ElementaryStream> getElementList()
    {
        return Collections.unmodifiableList(elementList);
    }
}
