package m2tk.assistant.analyzer.domain;

import lombok.Getter;
import m2tk.assistant.analyzer.presets.CASystems;
import m2tk.assistant.analyzer.util.ProgramStreamComparator;
import m2tk.assistant.dbi.entity.CAStreamEntity;
import m2tk.assistant.dbi.entity.ProgramEntity;
import m2tk.assistant.dbi.entity.ProgramStreamMappingEntity;
import m2tk.assistant.dbi.entity.StreamEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
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
    private final List<CASystemStream> ecmList;
    private final List<ElementaryStream> elementList;

    public MPEGProgram(String name,
                       ProgramEntity program,
                       List<CAStreamEntity> ecms,
                       List<ProgramStreamMappingEntity> mappings,
                       Map<Integer, StreamEntity> streams)
    {
        Objects.requireNonNull(program);
        Objects.requireNonNull(ecms);
        Objects.requireNonNull(mappings);
        Objects.requireNonNull(streams);

        programName = name;
        programNumber = program.getProgramNumber();
        transportStreamId = program.getTransportStreamId();
        pmtPid = program.getPmtPid();
        pcrPid = program.getPcrPid();
        pmtVersion = program.getPmtVersion();

        ecmList = new ArrayList<>();
        elementList = new ArrayList<>();

        int totalBitrate = 0;
        for (CAStreamEntity ecm : ecms)
        {
            String vendor = CASystems.vendor(ecm.getSystemId());
            int esPid = ecm.getElementaryStreamPid();
            String description = String.format("ECM, 系统号：%04X", ecm.getSystemId());
            if (!vendor.isEmpty())
                description += "（" + vendor + "）";
            if (esPid != 8191)
                description += String.format("，目标ES：0x%X", esPid);

            CASystemStream ecmStream = new CASystemStream(ecm.getSystemId(),
                                                          ecm.getStreamPid(),
                                                          "",
                                                          description);
            ecmList.add(ecmStream);

            StreamEntity stream = streams.get(ecm.getStreamPid());
            totalBitrate += (stream == null) ? 0 : stream.getBitrate();
        }

        boolean hasScrambledElement = false;
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
                                                         stream.getPcrCount(),
                                                         stream.getContinuityErrorCount(),
                                                         stream.getBitrate(),
                                                         stream.getRatio(),
                                                         stream.isScrambled(),
                                                         mapping.getStreamType(),
                                                         mapping.getStreamCategory(),
                                                         mapping.getStreamDescription(),
                                                         programNumber);

            elementList.add(es);
            totalBitrate += es.getBitrate();

            if (es.isScrambled())
                hasScrambledElement = true;
        }

        ProgramStreamComparator comparator = new ProgramStreamComparator();
        elementList.sort(comparator);

        freeAccess = ecmList.isEmpty() && !hasScrambledElement;
        bandwidth = totalBitrate;
    }
}
