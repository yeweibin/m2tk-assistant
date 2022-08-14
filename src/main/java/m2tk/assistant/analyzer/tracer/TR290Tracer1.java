/*
 * Copyright (c) Ye Weibin. All rights reserved.
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

package m2tk.assistant.analyzer.tracer;

import m2tk.assistant.analyzer.presets.TR290ErrorTypes;
import m2tk.assistant.dbi.DatabaseService;
import m2tk.mpeg2.MPEG2;
import m2tk.mpeg2.ProgramClockReference;
import m2tk.mpeg2.decoder.TransportPacketDecoder;
import m2tk.mpeg2.decoder.element.AdaptationFieldDecoder;
import m2tk.mpeg2.decoder.element.ProgramClockReferenceDecoder;
import m2tk.multiplex.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 290错误检测有很多项，有些项目相对独立，有些项目比较矛盾，不宜放在同一个处理逻辑里。
 * 因此拆分到不同的检测流程里。
 * TR290Trace1只负责与传输包有关的错误。
 */
public class TR290Tracer1 implements Tracer
{
    private final DatabaseService databaseService;
    private final Map<Integer, PCRContext> PCRContexts;
    private final int[] TEFs;
    private final int[] CCTs; // next cct
    private final int[] DupCnts;
    private final int[] TECnts; // trans_err_cnt
    private final int[] CECnts; // cc_err_cnt
    private final byte[][] PktStubs;
    private final TransportPacketDecoder pkt;
    private final AdaptationFieldDecoder adpt;
    private final ProgramClockReferenceDecoder pcr;
    private int pktcnt;
    private long avgBitrate;
    private static final byte[] DUPLICATE_PACKET_MASK = new byte[MPEG2.TS_PACKET_SIZE];


    static class PCRContext
    {
        long pcrCount;
        long lastValue;
        long lastPosition;
        long maxAccuracy;
        long minAccuracy;
        long sumAccuracy;
        long cntAccuracy;
        long maxInterval;
        long minInterval;
        long sumInterval;
        long cntInterval;
        long insBitrate;
        long sumBitrate;
        long cntBitrate;

        long cntRepetitionViolation;
        long cntDiscontinuityViolation;
        long cntAccuracyViolation;
    }

    static
    {
        Arrays.fill(DUPLICATE_PACKET_MASK, (byte) 0xFF);
        Arrays.fill(DUPLICATE_PACKET_MASK, 6, 12, (byte) 0); // PCR field
    }

    public TR290Tracer1(DatabaseService service)
    {
        databaseService = service;

        PCRContexts = new HashMap<>();

        TEFs = new int[8192];
        CCTs = new int[8192];
        TECnts = new int[8192];
        CECnts = new int[8192];
        DupCnts = new int[8192];
        PktStubs = new byte[8192][188];
        pkt = new TransportPacketDecoder();
        adpt = new AdaptationFieldDecoder();
        pcr = new ProgramClockReferenceDecoder();

        for (int i = 0; i < 8192; i++)
        {
            TEFs[i] = 0;
            TECnts[i] = 0;
            CECnts[i] = 0;
            CCTs[i] = -1;
            DupCnts[i] = 0;
        }

        avgBitrate = 0;
    }

    @Override
    public void configureDemux(TSDemux demux)
    {
        demux.registerEventListener(this::processDemuxEvent);
        demux.registerRawChannel(this::processTransportPacket);
    }

    private void reportError(String errorType, String errorMessage, long position, int stream)
    {
        databaseService.addTR290Event(LocalDateTime.now(),
                                      errorType, errorMessage,
                                      position, stream);
    }

    private void processDemuxEvent(TSDemuxEvent event)
    {
        if (event instanceof DemuxStatus)
        {
            DemuxStatus status = (DemuxStatus) event;
            if (!status.isRunning())
            {
                updateStreamErrorCounts(true);

                PCRContexts.forEach((pid, ctx) -> {
                    System.out.printf("[PID %04x] Bitrate: %,d bps, PCR Cnt: %,d%n",
                                      pid, ctx.sumBitrate / ctx.cntBitrate, ctx.pcrCount);
                    System.out.printf("    [Repeat  ] Errors: %,d%n", ctx.cntRepetitionViolation);
                    System.out.printf("    [Accuracy] Avg: %d ns, Max: %d ns, Min: %d ns, Errors: %,d%n",
                                      ctx.sumAccuracy / ctx.cntAccuracy, ctx.maxAccuracy, ctx.minAccuracy, ctx.cntAccuracyViolation);
                    System.out.printf("    [Interval] Avg: %d ms, Max: %d ms, Min: %d ms, Errors: %,d%n",
                                      ctx.sumInterval / ctx.cntInterval, ctx.maxInterval, ctx.minInterval, ctx.cntDiscontinuityViolation);
                    System.out.println();
                });
            }
        }

        if (event instanceof TransportStatus)
        {
            TransportStatus status = (TransportStatus) event;
            switch (status.getCurrentState())
            {
                case SYNC_BYTE_ERROR:
                {
                    Arrays.fill(CCTs, -1);
                    Arrays.fill(DupCnts, 0);

                    for (PCRContext ctx : PCRContexts.values())
                    {
                        ctx.lastValue = -1;
                        ctx.lastPosition = -1;
                    }

                    reportError(TR290ErrorTypes.SYNC_BYTE_ERROR, "同步字节错误",
                                status.getPosition(), status.getPid());
                    break;
                }

                case SYNC_LOST:
                {
                    Arrays.fill(CCTs, -1);
                    Arrays.fill(DupCnts, 0);

                    for (PCRContext ctx : PCRContexts.values())
                    {
                        ctx.lastValue = -1;
                        ctx.lastPosition = -1;
                    }

                    reportError(TR290ErrorTypes.TS_SYNC_LOSS, "同步丢失错误",
                                status.getPosition(), status.getPid());
                    break;
                }
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
        PCRContext ctx = PCRContexts.get(pid);
        if (ctx != null)
        {
            ctx.lastValue = -1;
            ctx.lastPosition = -1;
        }
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
            PCRContext ctx = PCRContexts.get(pid);
            if (ctx != null)
            {
                ctx.lastValue = -1;
                ctx.lastPosition = -1;
            }
            return;
        }

        long currPcr = readPCR();
        long currPct = payload.getStartPacketCounter();
        if (currPcr == -1)
            return;

        if (!PCRContexts.containsKey(pid))
        {
            PCRContext ctx = new PCRContext();
            ctx.pcrCount = 1;
            ctx.lastValue = currPcr;
            ctx.lastPosition = currPct;
            ctx.minAccuracy = Long.MAX_VALUE;
            ctx.maxAccuracy = Long.MIN_VALUE;
            ctx.sumAccuracy = 0;
            ctx.cntAccuracy = 0;
            ctx.minInterval = Long.MAX_VALUE;
            ctx.maxInterval = 0;
            ctx.sumInterval = 0;
            ctx.cntInterval = 0;
            ctx.insBitrate = 0;
            ctx.sumBitrate = 0;
            ctx.cntBitrate = 0;
            ctx.cntRepetitionViolation = 0;
            ctx.cntDiscontinuityViolation = 0;
            ctx.cntAccuracyViolation = 0;
            PCRContexts.put(pid, ctx);
            return;
        }

        PCRContext ctx = PCRContexts.get(pid);
        if (ctx.lastValue == -1)
        {
            // 受到高优先级错误的影响，PCR计算会被重置。
            ctx.lastValue = currPcr;
            ctx.lastPosition = currPct;
            ctx.pcrCount += 1;
            return;
        }

        // 可以正常计算了。
        ctx.insBitrate = ProgramClockReference.bitrate(ctx.lastValue, currPcr, currPct - ctx.lastPosition);
        ctx.sumBitrate += ctx.insBitrate;
        ctx.cntBitrate += 1;
        ctx.pcrCount += 1;

//        long[] sum = new long[2];
//        PCRContexts.forEach((k, v) -> {
//            if (v.insBitrate > 0)
//            {
//                sum[0] += v.insBitrate;
//                sum[1] += 1;
//            }
//        });
//        avgBitrate = sum[0] / sum[1];

        avgBitrate = (avgBitrate == 0) ? ctx.insBitrate : (avgBitrate + ctx.insBitrate) / 2;

        // 判断PCR是否抖动、是否间断、是否精确
        long deltaTb = (currPct - ctx.lastPosition) * 188 * 8 * 1000_000_000 / avgBitrate;
        if (deltaTb / 1000_000 > 40)
        {
            ctx.cntRepetitionViolation += 1;
            reportError(TR290ErrorTypes.PCR_REPETITION_ERROR,
                        String.format("PCR间隔超过40ms（当前间隔：%dms）", deltaTb / 1000_000),
                        payload.getStartPacketCounter(), payload.getStreamPID());
        }

        long deltaTpcr = ProgramClockReference.deltaNanos(ctx.lastValue, currPcr);
        if (adpt.getDiscontinuityIndicator() == 0 && deltaTpcr / 1000_000 > 100)
        {
            ctx.cntDiscontinuityViolation += 1;
            reportError(TR290ErrorTypes.PCR_DISCONTINUITY_INDICATOR_ERROR,
                        String.format("未设置不连续标志情况下前后PCR差值大于100ms（当前差值：%dms）", deltaTpcr / 1000_000),
                        payload.getStartPacketCounter(), payload.getStreamPID());
        }

        long pcrAccuracy = deltaTb - deltaTpcr;
        if (Math.abs(pcrAccuracy) > 500)
        {
            ctx.cntAccuracyViolation += 1;
            reportError(TR290ErrorTypes.PCR_ACCURACY_ERROR,
                        String.format("PCR精度误差超过正负500ns（当前误差：%dns）", pcrAccuracy),
                        payload.getStartPacketCounter(), payload.getStreamPID());
        }

        deltaTpcr /= 1000_000; // ns -> ms
        ctx.lastValue = currPcr;
        ctx.lastPosition = currPct;
        ctx.maxAccuracy = Math.max(ctx.maxAccuracy, pcrAccuracy);
        ctx.minAccuracy = Math.min(ctx.minAccuracy, pcrAccuracy);
        ctx.sumAccuracy = ctx.sumAccuracy + pcrAccuracy;
        ctx.cntAccuracy = ctx.cntAccuracy + 1;
        ctx.maxInterval = Math.max(ctx.maxInterval, deltaTpcr);
        ctx.minInterval = Math.min(ctx.minInterval, deltaTpcr);
        ctx.sumInterval = ctx.sumInterval + deltaTpcr;
        ctx.cntInterval = ctx.cntInterval + 1;
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
                PCRContext ctx = PCRContexts.get(pkt.getPID());
                if (ctx != null)
                {
                    ctx.lastValue = -1;
                    ctx.lastPosition = -1;
                }
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
            for (int i = 0; i < 8191; i++)
            {
                if (TECnts[i] > 0 || CECnts[i] > 0)
                {
                    databaseService.cumsumStreamErrorCounts(i, TECnts[i], CECnts[i]);
                    TECnts[i] = 0;
                    CECnts[i] = 0;
                }
            }
            pktcnt = 0;
        }
    }
}
