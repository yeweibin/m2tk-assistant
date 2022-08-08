package m2tk.assistant.analyzer;

import m2tk.assistant.dbi.DatabaseService;
import m2tk.assistant.dbi.entity.SourceEntity;
import m2tk.assistant.dbi.entity.StreamEntity;
import m2tk.mpeg2.ProgramClockReference;
import m2tk.mpeg2.decoder.TransportPacketDecoder;
import m2tk.mpeg2.decoder.element.AdaptationFieldDecoder;
import m2tk.mpeg2.decoder.element.ProgramClockReferenceDecoder;
import m2tk.multiplex.DemuxStatus;
import m2tk.multiplex.TSDemuxPayload;
import m2tk.multiplex.TSState;
import m2tk.multiplex.TransportStatus;

public class StreamTracer
{
    private final DatabaseService databaseService;
    private final StreamEntity[] streams;
    private final int[] CCTs;
    private final TransportPacketDecoder pkt;
    private final AdaptationFieldDecoder adpt;
    private final ProgramClockReferenceDecoder pcr;
    private int pcrPid;
    private long lastPcrValue;
    private long lastPcrPct;
    private long lastPct;
    private int avgBitrate;
    private int frameSize;
    private long t0;
    private SourceEntity source;

    public StreamTracer(DatabaseService service)
    {
        databaseService = service;
        streams = new StreamEntity[8192];
        CCTs = new int[8192];
        pkt = new TransportPacketDecoder();
        adpt = new AdaptationFieldDecoder();
        pcr = new ProgramClockReferenceDecoder();
        pcrPid = -1;
        lastPcrValue = -1;
        frameSize = -1;
        avgBitrate = 0;
        t0 = System.currentTimeMillis();
    }

    public void processTransportStatus(TransportStatus status)
    {
        System.out.printf("status changed: %s -> %s%n",
                          status.getPreviousState().name(),
                          status.getCurrentState().name());
        if (status.getCurrentState() == TSState.SYNC_BYTE_ERROR ||
            status.getCurrentState() == TSState.SYNC_LOST)
            System.out.println("sync error");
    }

    public void processDemuxStatus(DemuxStatus status)
    {
        if (status.isRunning())
        {
            source = databaseService.getSource();
        } else
        {
            source.setBitrate(avgBitrate);
            source.setFrameSize(frameSize);
            source.setPacketCount(lastPct);
            databaseService.updateSourceStatistics(source);

            for (StreamEntity stream : streams)
            {
                if (stream != null && stream.getPacketCount() > 0)
                {
                    double ratio = 1.0d * stream.getPacketCount() / lastPct;
                    stream.setRatio(ratio);
                    stream.setBitrate((int) (avgBitrate * ratio));
                    databaseService.updateStreamStatistics(stream);
                }
            }
        }
    }

    public void processTransportPacket(TSDemuxPayload payload)
    {
        if (frameSize == -1)
            frameSize = payload.getEncoding().size();

        pkt.attach(payload.getEncoding());
        int pid = pkt.getPID();

        StreamEntity stream = streams[pid];
        if (stream == null)
        {
            stream = databaseService.getStream(pid);
            streams[pid] = stream;
            CCTs[pid] = -1;
        }

        stream.setPacketCount(stream.getPacketCount() + 1);

        if (pkt.isScrambled())
            stream.setScrambled(true);

        int cct = pkt.getContinuityCounter();
        if (pid != 0x1FFF && CCTs[pid] != -1 && cct != nextCCT(CCTs[pid]))
            stream.setContinuityErrorCount(stream.getContinuityErrorCount() + 1);
        CCTs[pid] = cct;

        long currPcrValue = readPCR();
        long currPct = payload.getStartPacketCounter();
        if (currPcrValue != -1)
        {
            if (pcrPid == -1)
            {
                // 遇到的第一个PCR
                pcrPid = pid;
                lastPcrValue = currPcrValue;
                lastPcrPct = currPct;
            } else if (pcrPid == pid)
            {
                int bitrate = ProgramClockReference.bitrate(lastPcrValue, currPcrValue, currPct - lastPcrPct);
                avgBitrate = (avgBitrate + bitrate) / 2;
                lastPcrValue = currPcrValue;
                lastPcrPct = currPct;
            }
        }

        lastPct = currPct;
        saveToDatabase();
    }

    private void saveToDatabase()
    {
        long t1 = System.currentTimeMillis();
        if ((t1 - t0) >= 1000) // 超过1s才更新数据库
        {
            source.setBitrate(avgBitrate);
            source.setFrameSize(frameSize);
            source.setPacketCount(lastPct);
            databaseService.updateSourceStatistics(source);

            for (StreamEntity stream : streams)
                saveStreamStatistics(stream);

            t0 = System.currentTimeMillis();
        }
    }

    private void saveStreamStatistics(StreamEntity stream)
    {
        if (stream == null || stream.getPacketCount() == 0)
            return;

        double ratio = 1.0d * stream.getPacketCount() / lastPct;
        stream.setRatio(ratio);
        stream.setBitrate((int) (avgBitrate * ratio));
        databaseService.updateStreamStatistics(stream);
    }

    private int nextCCT(int cct)
    {
        return (cct + 1) & 0xF;
    }

    private long readPCR()
    {
        if (!pkt.containsUsefulAdaptationField())
            return -1;

        try
        {
            adpt.attach(pkt.getAdaptationField());
            if (adpt.getProgramClockReferenceFlag() == 0)
                return -1;

            pcr.attach(adpt.getProgramClockReference());
            return pcr.getProgramClockReferenceValue();
        } catch (Exception ex)
        {
            return -1;
        }
    }
}
