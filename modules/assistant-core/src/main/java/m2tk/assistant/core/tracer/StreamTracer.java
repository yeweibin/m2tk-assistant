/*
 * Copyright (c) M2TK Project. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package m2tk.assistant.core.tracer;

import m2tk.assistant.core.M2TKDatabase;
import m2tk.assistant.core.domain.ElementaryStream;
import m2tk.assistant.core.domain.StreamSource;
import m2tk.mpeg2.ProgramClockReference;
import m2tk.mpeg2.decoder.TransportPacketDecoder;
import m2tk.mpeg2.decoder.element.AdaptationFieldDecoder;
import m2tk.mpeg2.decoder.element.ProgramClockReferenceDecoder;
import m2tk.multiplex.DemuxStatus;
import m2tk.multiplex.TSDemux;
import m2tk.multiplex.TSDemuxEvent;
import m2tk.multiplex.TSDemuxPayload;

public class StreamTracer implements Tracer
{
    private final ElementaryStream[] streams;
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

    private M2TKDatabase databaseService;
    private StreamSource streamSource;
    private long transactionId;

    public StreamTracer()
    {
        streams = new ElementaryStream[8192];
        pkt = new TransportPacketDecoder();
        adpt = new AdaptationFieldDecoder();
        pcr = new ProgramClockReferenceDecoder();
        pcrPid = -1;
        lastPcrValue = -1;
        frameSize = -1;
        avgBitrate = 0;
        t0 = System.currentTimeMillis();
    }

    @Override
    public void configure(StreamSource source, TSDemux demux, M2TKDatabase database)
    {
        streamSource = source;
        databaseService = database;
        transactionId = source.getTransactionId();

        demux.registerEventListener(this::processDemuxStatus);
        demux.registerRawChannel(this::processTransportPacket);
    }

    private void processDemuxStatus(TSDemuxEvent event)
    {
        if (!(event instanceof DemuxStatus status))
            return;

        if (!status.isRunning())
        {
            streamSource.setBitrate(avgBitrate);
            streamSource.setFrameSize(frameSize);
            streamSource.setPacketCount(lastPct);
            databaseService.updateStreamSource(streamSource);

            for (ElementaryStream stream : streams)
            {
                if (stream != null && stream.getPktCount() > 0)
                {
                    double ratio = 1.0d * stream.getPktCount() / lastPct;
                    stream.setRatio(ratio);
                    stream.setBitrate((int) (avgBitrate * ratio));
                    databaseService.updateElementaryStream(stream);
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

        ElementaryStream stream = streams[pid];
        if (stream == null)
        {
            stream = databaseService.getElementaryStream(transactionId, pid);
            streams[pid] = stream;
        }

        stream.setPktCount(stream.getPktCount() + 1);

        if (pkt.isScrambled())
            stream.setScrambled(true);

        long currPcrValue = readPCR();
        long currPct = payload.getStartPacketCounter();
        if (currPcrValue != -1)
        {
            stream.setPcrCount(stream.getPcrCount() + 1);

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
        if ((t1 - t0) >= 200) // 超过200ms才更新数据库
        {
            streamSource.setBitrate(avgBitrate);
            streamSource.setFrameSize(frameSize);
            streamSource.setPacketCount(lastPct);
            databaseService.updateStreamSource(streamSource);

            for (ElementaryStream stream : streams)
                saveStreamStatistics(stream);

            t0 = System.currentTimeMillis();
        }
    }

    private void saveStreamStatistics(ElementaryStream stream)
    {
        if (stream == null || stream.getPktCount() == 0)
            return;

        double ratio = 1.0d * stream.getPktCount() / lastPct;
        stream.setRatio(ratio);
        stream.setBitrate((int) (avgBitrate * ratio));
        databaseService.updateElementaryStream(stream);
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
