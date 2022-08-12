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
import m2tk.mpeg2.decoder.TransportPacketDecoder;
import m2tk.multiplex.*;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 290错误检测有很多项，有些项目相对独立，有些项目比较矛盾，不宜放在同一个处理逻辑里。
 * 因此拆分到不同的检测流程里。
 * TR290Trace1只负责与传输包有关的错误。
 */
public class TR290Tracer1 implements Tracer
{
    private final DatabaseService databaseService;
    private final int[] TEFs;
    private final int[] CCTs; // next cct
    private final int[] DupCnts;
    private final int[] CETs; // cc_error_cnt
    private final byte[][] PktStubs;
    private final TransportPacketDecoder pkt;
    private int pktcnt;
    private static final byte[] DUPLICATE_PACKET_MASK = new byte[MPEG2.TS_PACKET_SIZE];

    static
    {
        Arrays.fill(DUPLICATE_PACKET_MASK, (byte) 0xFF);
        Arrays.fill(DUPLICATE_PACKET_MASK, 6, 12, (byte) 0); // PCR field
    }

    public TR290Tracer1(DatabaseService service)
    {
        databaseService = service;
        TEFs = new int[8192];
        CCTs = new int[8192];
        CETs = new int[8192];
        DupCnts = new int[8192];
        PktStubs = new byte[8192][188];
        pkt = new TransportPacketDecoder();

        for (int i = 0; i < 8192; i++)
        {
            TEFs[i] = 0;
            CETs[i] = 0;
            CCTs[i] = -1;
            DupCnts[i] = 0;
        }
    }

    @Override
    public void configureDemux(TSDemux demux)
    {
        demux.registerEventListener(this::processDemuxEvent);
        demux.registerRawChannel(this::processTransportPacket);
    }

    private void processDemuxEvent(TSDemuxEvent event)
    {
        if (event instanceof DemuxStatus)
        {
            DemuxStatus status = (DemuxStatus) event;
            if (!status.isRunning())
                updateContinuityErrorCounts(true);
        }

        if (event instanceof TransportStatus)
        {
            TransportStatus status = (TransportStatus) event;
            switch (status.getCurrentState())
            {
                case SYNC_BYTE_ERROR:
                {
                    databaseService.addTR290Event(LocalDateTime.now(),
                                                  TR290ErrorTypes.SYNC_BYTE_ERROR,
                                                  "同步字节错误",
                                                  status.getPosition(),
                                                  status.getPid());
                    break;
                }
                case SYNC_LOST:
                {
                    databaseService.addTR290Event(LocalDateTime.now(),
                                                  TR290ErrorTypes.TS_SYNC_LOSS,
                                                  "同步丢失错误",
                                                  status.getPosition(),
                                                  status.getPid());
                    break;
                }
            }
        }
    }

    private void processTransportPacket(TSDemuxPayload payload)
    {
        pktcnt++;
        updateContinuityErrorCounts(false);

        int pid = payload.getStreamPID();
        if (pid == 8191)
            return; // 空包不做检查

        pkt.attach(payload.getEncoding());
        if (pkt.containsTransportError())
        {
            if (TEFs[pid] == 0)
            {
                databaseService.addTR290Event(LocalDateTime.now(),
                                              TR290ErrorTypes.TRANSPORT_ERROR,
                                              String.format("当前流指示传输错误（pid = %d）", pid),
                                              payload.getStartPacketCounter(),
                                              payload.getStreamPID());
                TEFs[pid] = 1; // 该流的传输错误已经通报过，就不再重复通报了。
            }

            CCTs[pid] = -1;
            DupCnts[pid] = 0;
            return;
        }

        TEFs[pid] = 0; // 传输错误恢复或持续正常，此后允许通报传输错误。

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
                CETs[pid] += 1;
                databaseService.addTR290Event(LocalDateTime.now(),
                                              TR290ErrorTypes.CONTINUITY_COUNT_ERROR,
                                              String.format("无负载时连续计数器发生变化（期望：%d，实际：%d，pid = %d）",
                                                            CCTs[pid], curr_cct, pid),
                                              payload.getStartPacketCounter(),
                                              payload.getStreamPID());
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
                CETs[pid] += 1;
                databaseService.addTR290Event(LocalDateTime.now(),
                                              TR290ErrorTypes.CONTINUITY_COUNT_ERROR,
                                              String.format("重复包连续出现了两次以上（pid = %d）", pid),
                                              payload.getStartPacketCounter(),
                                              payload.getStreamPID());
            }
            return;
        }

        // 不是重复的包，则属于CCT错乱。
        CETs[pid] += 1;
        databaseService.addTR290Event(LocalDateTime.now(),
                                      TR290ErrorTypes.CONTINUITY_COUNT_ERROR,
                                      String.format("连续计数错误（期望：%d，实际：%d，pid = %d）",
                                                    exp_cct, curr_cct, pid),
                                      payload.getStartPacketCounter(),
                                      payload.getStreamPID());

        // 重置计数器，此时需要保留存根
        CCTs[pid] = curr_cct;
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

    private void updateContinuityErrorCounts(boolean immediately)
    {
        if (immediately || pktcnt > 4000)
        {
            for (int i = 0; i < 8191; i++)
            {
                if (CETs[i] > 0)
                {
                    databaseService.addStreamContinuityErrorCount(i, CETs[i]);
                    CETs[i] = 0;
                }
            }
            pktcnt = 0;
        }
    }
}
