/*
 *  Copyright (c) M2TK Project. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package m2tk.assistant.app.ui.tracer;

import m2tk.assistant.api.M2TKDatabase;
import m2tk.assistant.api.Tracer;
import m2tk.assistant.api.domain.PCR;
import m2tk.assistant.api.domain.PCRCheck;
import m2tk.assistant.api.domain.StreamSource;
import m2tk.assistant.api.domain.TR290Event;
import m2tk.assistant.api.presets.TR290ErrorTypes;
import m2tk.mpeg2.MPEG2;
import m2tk.mpeg2.ProgramClockReference;
import m2tk.mpeg2.decoder.TransportPacketDecoder;
import m2tk.mpeg2.decoder.element.AdaptationFieldDecoder;
import m2tk.mpeg2.decoder.element.ProgramClockReferenceDecoder;
import m2tk.multiplex.*;
import org.pf4j.Extension;

import java.time.OffsetDateTime;
import java.util.Arrays;

/**
 * TR290错误检测有很多项，有些项目相对独立，有些项目比较矛盾，不宜放在同一个处理逻辑里。
 * 因此拆分到不同的检测流程里。
 * TR290Trace1只负责与传输包有关的错误。
 */
@Extension
public class TR290Tracer1 implements Tracer
{
    private final int[] TEFs;
    private final int[] CCTs; // next cct
    private final int[] DupCnts;
    private final int[] TECnts; // trans_err_cnt
    private final int[] CECnts; // cc_err_cnt
    private final long[] PCRs;
    private final long[] PCRPcts;
    private final byte[][] PktStubs;
    private final TransportPacketDecoder pkt;
    private final AdaptationFieldDecoder adpt;
    private final ProgramClockReferenceDecoder pcr;
    private int pktcnt;
    private long avgBitrate;
    private M2TKDatabase databaseService;

    private static final byte[] DUPLICATE_PACKET_MASK = new byte[MPEG2.TS_PACKET_SIZE];

    static
    {
        Arrays.fill(DUPLICATE_PACKET_MASK, (byte) 0xFF);
        Arrays.fill(DUPLICATE_PACKET_MASK, 6, 12, (byte) 0); // PCR field
    }

    public TR290Tracer1()
    {
        TEFs = new int[8192];
        CCTs = new int[8192];
        TECnts = new int[8192];
        CECnts = new int[8192];
        DupCnts = new int[8192];
        PCRs = new long[8192];
        PCRPcts = new long[8192];
        PktStubs = new byte[8192][188];
        pkt = new TransportPacketDecoder();
        adpt = new AdaptationFieldDecoder();
        pcr = new ProgramClockReferenceDecoder();

        for (int i = 0; i < 8192; i++)
        {
            PCRs[i] = -1;
            PCRPcts[i] = -1;
            TEFs[i] = 0;
            TECnts[i] = 0;
            CECnts[i] = 0;
            CCTs[i] = -1;
            DupCnts[i] = 0;
        }

        avgBitrate = 0;
    }

    @Override
    public void configure(StreamSource source, TSDemux demux, M2TKDatabase database)
    {
        databaseService = database;

        demux.registerEventListener(this::processDemuxEvent);
        demux.registerRawChannel(this::processTransportPacket);
    }

    private void reportError(String errorType, String errorMessage, long position, int stream)
    {
        TR290Event event = new TR290Event();
        event.setTimestamp(OffsetDateTime.now());
        event.setType(errorType);
        event.setDescription(errorMessage);
        event.setPosition(position);
        event.setStream(stream);
        databaseService.addTR290Event(event);
    }

    private void processDemuxEvent(TSDemuxEvent event)
    {
        if (event instanceof DemuxStatus status && !status.isRunning())
        {
            updateStreamErrorCounts(true);
        }

        if (event instanceof TransportStatus status)
        {
            TSState currentState = status.getCurrentState();
            if (currentState == TSState.SYNC_BYTE_ERROR)
            {
                for (int i = 0; i < 8192; i++)
                {
                    PCRs[i] = -1;
                    PCRPcts[i] = -1;
                    CCTs[i] = -1;
                    DupCnts[i] = 0;
                }

                reportError(TR290ErrorTypes.SYNC_BYTE_ERROR, "同步字节错误",
                            status.getPosition(), status.getPid());
            }
            if (currentState == TSState.SYNC_LOST)
            {
                for (int i = 0; i < 8192; i++)
                {
                    PCRs[i] = -1;
                    PCRPcts[i] = -1;
                    CCTs[i] = -1;
                    DupCnts[i] = 0;
                }

                reportError(TR290ErrorTypes.TS_SYNC_LOSS, "同步丢失错误",
                            status.getPosition(), status.getPid());
            }
        }
    }

    private void processTransportPacket(TSDemuxPayload payload)
    {
        pktcnt++;
        updateStreamErrorCounts(false);

        int pid = payload.getStreamPID();
        if (pid == 8191)
            return; // 空包不做检查

        pkt.attach(payload.getEncoding());
        if (pkt.containsTransportError())
        {
            reportTransportError(payload);
        } else
        {
            TEFs[pid] = 0; // 传输错误恢复或持续正常，此后允许通报传输错误。
            checkContinuityCount(payload);
            checkPCRMeasurements(payload);
        }
    }

    private void reportTransportError(TSDemuxPayload payload)
    {
        int pid = payload.getStreamPID();
        if (TEFs[pid] == 0)
        {
            reportError(TR290ErrorTypes.TRANSPORT_ERROR,
                        String.format("当前流指示传输错误（pid = %d）", pid),
                        payload.getStartPacketCounter(), payload.getStreamPID());
            TEFs[pid] = 1; // 该流的传输错误已经通报过，就不再重复通报了。
        }

        TECnts[pid] += 1;
        CCTs[pid] = -1;
        DupCnts[pid] = 0;
        PCRs[pid] = -1;
        PCRPcts[pid] = -1;
    }

    private void checkContinuityCount(TSDemuxPayload payload)
    {
        int pid = payload.getStreamPID();

        // Continuity Counter
        int curr_cct = pkt.getContinuityCounter();
        if (CCTs[pid] == -1)
        {
            if (pkt.containsPayload())
            {
                CCTs[pid] = curr_cct;
                DupCnts[pid] = 0;
                payload.getEncoding().copyRange(0, 188, PktStubs[pid]);
            }
            return;
        }

        // 有前一个包的CCT记录
        if (!pkt.containsPayload())
        {
            // 没有负载的情况下，CCT不允许变化，所以当前CCT应该等于前一个包的CCT
            if (CCTs[pid] != curr_cct)
            {
                CECnts[pid] += 1;
                reportError(TR290ErrorTypes.CONTINUITY_COUNT_ERROR,
                            String.format("无负载时连续计数器发生变化（期望：%d，实际：%d，pid = %d）",
                                          CCTs[pid], curr_cct, pid),
                            payload.getStartPacketCounter(), payload.getStreamPID());
            }

            // 没有负载则不算重复包，所以不用留存根。
            return;
        }

        // 有负载，应该判断是否重复。
        int exp_cct = (CCTs[pid] + 1) & 0xF;
        if (exp_cct == curr_cct)
        {
            // CCT正常，保留存根。
            CCTs[pid] = curr_cct;
            DupCnts[pid] = 0;
            payload.getEncoding().copyRange(0, 188, PktStubs[pid]);
        }

        // CCT没有按规律变化

        // 可能是重复的包，要记录重复次数。规范允许相同的包最多重复一次。
        if (CCTs[pid] == curr_cct && isDuplicatePacket())
        {
            DupCnts[pid] += 1;

            if (DupCnts[pid] > 1)
            {
                CCTs[pid] = -1;
                CECnts[pid] += 1;
                reportError(TR290ErrorTypes.CONTINUITY_COUNT_ERROR,
                            String.format("重复包连续出现了两次以上（pid = %d）", pid),
                            payload.getStartPacketCounter(), payload.getStreamPID());
            }
            return;
        }

        // 不是重复的包，则属于CCT错乱。
        CECnts[pid] += 1;
        reportError(TR290ErrorTypes.CONTINUITY_COUNT_ERROR,
                    String.format("连续计数错误（期望：%d，实际：%d，pid = %d）",
                                  exp_cct, curr_cct, pid),
                    payload.getStartPacketCounter(), payload.getStreamPID());

        // 重置计数器，此时需要保留存根
        CCTs[pid] = -1;
        DupCnts[pid] = 0;
        payload.getEncoding().copyRange(0, 188, PktStubs[pid]);
    }

    private boolean isDuplicatePacket()
    {
        int pid = pkt.getPID();
        return pkt.containsUsefulAdaptationField()
               ? pkt.getEncoding().identicalTo(PktStubs[pid], DUPLICATE_PACKET_MASK)
               : pkt.getEncoding().identicalTo(PktStubs[pid]);
    }

    private void checkPCRMeasurements(TSDemuxPayload payload)
    {
        // 注意，传输流错误优先级：同步错误 > 传输错误 > 连续计数错误 > 其他
        // 所以PCR检查要在连续计数检查之后，并根据连续计数检查结果适当调整上下文。
        int pid = payload.getStreamPID();

        if (CCTs[pid] == -1)
        {
            // 此时发生连续计数错误，当前包不可靠。
            PCRs[pid] = -1;
            PCRPcts[pid] = -1;
            return;
        }

        long currPcr = readPCR();
        long currPct = payload.getStartPacketCounter();
        if (currPcr == -1)
            return;

        PCR pcr = new PCR();
        pcr.setPid(pid);
        pcr.setPosition(currPct);
        pcr.setValue(currPcr);
        databaseService.addPCR(pcr);

        if (PCRs[pid] == -1)
        {
            // 受到高优先级错误的影响，PCR计算会被重置。
            PCRs[pid] = currPcr;
            PCRPcts[pid] = currPct;
            return;
        }

        // 可以正常计算了。
        long bitrate = ProgramClockReference.bitrate(PCRs[pid], currPcr, currPct - PCRPcts[pid]);
        avgBitrate = (avgBitrate == 0) ? bitrate : (avgBitrate + bitrate) / 2;

        // 判断PCR是否抖动、是否间断、是否精确
        long deltaTb = (currPct - PCRPcts[pid]) * 188 * 8 * 1000_000_000 / avgBitrate;
        long deltaTpcr = ProgramClockReference.deltaNanos(PCRs[pid], currPcr);
        long pcrAccuracy = deltaTb - deltaTpcr;
        boolean repetitionCheckFailed = deltaTb > 40_000_000;
        boolean discontinuityCheckFailed = adpt.getDiscontinuityIndicator() == 0 && deltaTpcr > 100_000_000;
        boolean accuracyCheckFailed = Math.abs(pcrAccuracy) > 500;

        if (repetitionCheckFailed)
        {
            reportError(TR290ErrorTypes.PCR_REPETITION_ERROR,
                        String.format("PCR间隔超过40ms（当前间隔：%dms）", deltaTb / 1000_000),
                        payload.getStartPacketCounter(), payload.getStreamPID());
        }

        if (discontinuityCheckFailed)
        {
            reportError(TR290ErrorTypes.PCR_DISCONTINUITY_INDICATOR_ERROR,
                        String.format("未设置不连续标志情况下前后PCR差值大于100ms（当前差值：%dms）", deltaTpcr / 1000_000),
                        payload.getStartPacketCounter(), payload.getStreamPID());
        }

        if (accuracyCheckFailed)
        {
            reportError(TR290ErrorTypes.PCR_ACCURACY_ERROR,
                        String.format("PCR精度误差超过正负500ns（当前误差：%dns）", pcrAccuracy),
                        payload.getStartPacketCounter(), payload.getStreamPID());
        }

        PCRCheck check = new PCRCheck();
        check.setPid(pid);
        check.setPrevValue(PCRs[pid]);
        check.setPrevPosition(PCRPcts[pid]);
        check.setCurrValue(currPcr);
        check.setCurrPosition(currPct);
        check.setBitrate(bitrate);
        check.setDiffNanos(deltaTpcr);
        check.setIntervalNanos(deltaTb);
        check.setAccuracyNanos(pcrAccuracy);
        check.setRepetitionCheckFailed(repetitionCheckFailed);
        check.setDiscontinuityCheckFailed(discontinuityCheckFailed);
        check.setAccuracyCheckFailed(accuracyCheckFailed);
        databaseService.addPCRCheck(check);

        PCRs[pid] = currPcr;
        PCRPcts[pid] = currPct;
    }

    private long readPCR()
    {
        if (!pkt.containsUsefulAdaptationField())
            return -1;

        try
        {
            adpt.attach(pkt.getAdaptationField());
            if (adpt.isEmpty() || adpt.getProgramClockReferenceFlag() == 0)
                return -1;

            if (adpt.getDiscontinuityIndicator() == 1)
            {
                PCRs[pkt.getPID()] = -1;
                PCRPcts[pkt.getPID()] = -1;
                return -1;
            }

            pcr.attach(adpt.getProgramClockReference());
            return pcr.getProgramClockReferenceValue();
        } catch (Exception ex)
        {
            return -1;
        }
    }

    private void updateStreamErrorCounts(boolean immediately)
    {
        if (immediately || pktcnt > 2000)
        {
            for (int pid = 0; pid < 8191; pid++)
            {
                if (TECnts[pid] > 0 || CECnts[pid] > 0)
                {
                    databaseService.accumulateElementaryStreamErrors(pid, TECnts[pid], CECnts[pid]);
                    TECnts[pid] = 0;
                    CECnts[pid] = 0;
                }
            }
            pktcnt = 0;
        }
    }
}
