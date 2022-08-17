package m2tk.assistant.analyzer.tracer;

import m2tk.assistant.dbi.DatabaseService;
import m2tk.mpeg2.decoder.SectionDecoder;
import m2tk.multiplex.TSDemux;
import m2tk.multiplex.TSDemuxPayload;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class UserPrivateSectionTracer implements Tracer
{
    private final DatabaseService databaseService;
    private final Set<Integer> targetStreams;
    private final SectionDecoder sec;
    private final int limitPerStream;
    private final int[] secCounts;

    public UserPrivateSectionTracer(DatabaseService service, Collection<Integer> streams, int maxSectionCountPerStream)
    {
        databaseService = service;
        targetStreams = new HashSet<>(streams);
        limitPerStream = maxSectionCountPerStream;
        sec = new SectionDecoder();
        secCounts = new int[8192];
    }

    @Override
    public void configureDemux(TSDemux demux)
    {
        for (Integer pid : targetStreams)
        {
            demux.registerSectionChannel(pid, this::processSection);
        }
    }

    private void processSection(TSDemuxPayload payload)
    {
        if (!targetStreams.contains(payload.getStreamPID()) ||
            payload.getType() != TSDemuxPayload.Type.SECTION ||
            !sec.isAttachable(payload.getEncoding()))
            return;

        if (secCounts[payload.getStreamPID()] < limitPerStream)
        {
            databaseService.addSection("user-private",
                                       payload.getStreamPID(),
                                       payload.getFinishPacketCounter(),
                                       payload.getEncoding().getBytes());
            secCounts[payload.getStreamPID()] += 1;
        }
    }
}
