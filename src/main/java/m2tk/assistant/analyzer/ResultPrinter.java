package m2tk.assistant.analyzer;

import m2tk.assistant.analyzer.domain.CASystemStream;
import m2tk.assistant.analyzer.domain.ElementaryStream;
import m2tk.assistant.analyzer.domain.MPEGProgram;
import m2tk.assistant.dbi.DatabaseService;
import m2tk.assistant.dbi.entity.*;
import m2tk.multiplex.DemuxStatus;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public class ResultPrinter implements Consumer<DemuxStatus>
{
    private final DatabaseService databaseService;

    public ResultPrinter(DatabaseService service)
    {
        databaseService = service;
    }

    @Override
    public void accept(DemuxStatus status)
    {
        if (status.isRunning())
            return;

        SourceEntity source = databaseService.getSource();
        System.out.println("===================================================");
        System.out.println("Source: " + source.getSourceName());
        System.out.println("  bitrate    : " + formatBitrate(source.getBitrate()));
        System.out.println("  frame size : " + source.getFrameSize());
        System.out.println("  packets    : " + String.format("%,d", source.getPacketCount()));
        System.out.println("  ts id      : " + source.getTransportStreamId());
        System.out.println("===================================================");

        List<StreamEntity> streams = databaseService.listStreams();

        for (StreamEntity stream : streams)
        {
            System.out.printf("Stream[%4d] [%s]%n", stream.getPid(), stream.getCategory());
            System.out.printf("  type      : %s%n", stream.getDescription());
            System.out.printf("  packets   : %,d (%.2f%%)%n", stream.getPacketCount(), 100 * stream.getRatio());
            System.out.printf("  bitrate   : %s%n", formatBitrate(stream.getBitrate()));
            System.out.printf("  cct error : %,d%n", stream.getContinuityErrorCount());
            System.out.printf("  scrambled : %s%n", stream.isScrambled() ? "Yes" : "No");
        }

        System.out.println("===================================================");

        Map<Integer, StreamEntity> streamRegistry = streams.stream()
                                                           .collect(toMap(StreamEntity::getPid,
                                                                          Function.identity()));

        List<ProgramEntity> programs = databaseService.listPrograms();
        for (ProgramEntity program : programs)
        {
            List<CAStreamEntity> ecms = databaseService.getProgramECMStreams(program.getProgramNumber());
            List<ProgramStreamMappingEntity> mappings = databaseService.getProgramStreamMappings(program.getProgramNumber());
            MPEGProgram p = new MPEGProgram(program, ecms, mappings, streamRegistry);

            System.out.printf("Program[%4d] %s%n", p.getProgramNumber(), p.getProgramName() == null ? "" : p.getProgramName());
            System.out.printf("   pmt pid : %4Xh (%4d)%n", p.getPmtPid(), p.getPmtPid());
            System.out.printf("   pcr pid : %4Xh (%4d)%n", p.getPcrPid(), p.getPcrPid());
            System.out.printf("   with ca : %s%n", p.isFreeAccess() ? "No" : "Yes");
            for (CASystemStream ecm : p.getEcmList())
                System.out.printf(" %s ECM     : 0x%04X (%4d)%n",
                                  streamRegistry.containsKey(ecm.getStreamPid()) ? " " : "*",
                                  ecm.getStreamPid(),
                                  ecm.getStreamPid());
            for (ElementaryStream es : p.getElementList())
                System.out.printf(" %s ES [%s]  : 0x%04X (%4d) %s%n",
                                  es.isPresent() ? " " : "*",
                                  es.getCategory(),
                                  es.getStreamPid(), es.getStreamPid(),
                                  es.getDescription());
        }
    }

    private String formatBitrate(int bps)
    {
        if (bps > 1000_000)
            return String.format("%.2f Mbps", 1.0d * bps / 1000000);
        if (bps > 1000)
            return String.format("%.2f Kbps", 1.0d * bps / 1000);
        else
            return bps + " bps";
    }
}
