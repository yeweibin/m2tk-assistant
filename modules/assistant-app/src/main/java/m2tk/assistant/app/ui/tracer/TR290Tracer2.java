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

import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.api.M2TKDatabase;
import m2tk.assistant.api.Tracer;
import m2tk.assistant.api.domain.StreamSource;
import m2tk.assistant.api.domain.TR290Event;
import m2tk.assistant.api.presets.TR290ErrorTypes;
import m2tk.dvb.decoder.section.*;
import m2tk.mpeg2.ProgramClockReference;
import m2tk.mpeg2.decoder.DescriptorLoopDecoder;
import m2tk.mpeg2.decoder.SectionDecoder;
import m2tk.mpeg2.decoder.TransportPacketDecoder;
import m2tk.mpeg2.decoder.descriptor.CADescriptorDecoder;
import m2tk.mpeg2.decoder.element.AdaptationFieldDecoder;
import m2tk.mpeg2.decoder.element.ProgramClockReferenceDecoder;
import m2tk.mpeg2.decoder.element.ProgramElementDecoder;
import m2tk.mpeg2.decoder.section.CATSectionDecoder;
import m2tk.mpeg2.decoder.section.PATSectionDecoder;
import m2tk.mpeg2.decoder.section.PMTSectionDecoder;
import m2tk.multiplex.TSDemux;
import m2tk.multiplex.TSDemuxPayload;
import org.pf4j.Extension;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.IntStream;

@Slf4j
@Extension
public class TR290Tracer2 implements Tracer
{
    private final TransportPacketDecoder pkt;
    private final AdaptationFieldDecoder adpt;
    private final ProgramClockReferenceDecoder pcr;
    private final SectionDecoder sec;
    private final PATSectionDecoder pat;
    private final CATSectionDecoder cat;
    private final PMTSectionDecoder pmt;
    private final NITSectionDecoder nit;
    private final BATSectionDecoder bat;
    private final SDTSectionDecoder sdt;
    private final EITSectionDecoder eit;
    private final RSTSectionDecoder rst;
    private final TDTSectionDecoder tdt;
    private final DescriptorLoopDecoder descloop;
    private final ProgramElementDecoder element;
    private final CADescriptorDecoder cad;
    private final TSDemux.Channel[] pmtChannels;
    private final long[] pmtOccurPositions;
    private final long[] pmtOccurTimes;
    private final Map<String, Context> sectionContexts;
    private final Set<Integer> programNumbers;
    private final Set<Integer> programPmtPids;
    private final Set<Integer> pmtMappedStreams;

    private final long[] streamOccurTimes;
    private final long[] streamOccurPositions;

    private final int[] scrambledFlags;
    private final int[] streamMarks;
    private final int[] streamCounts;
    private long lastUnreferencedStreamCheckTime;
    private long lastUnreferencedStreamCheckPosition;

    private long lastPATOccurTime;
    private long lastPATOccurPosition;
    private long lastCATOccurTime;
    private long lastCATOccurPosition;
    private long lastNITActOccurTime;
    private long lastNITActOccurPosition;
    private long lastSDTActOccurTime;
    private long lastSDTActOccurPosition;
    private long lastEITActPFOccurTime;
    private long lastEITActPFOccurPosition;
    private long lastEITActPFS0OccurTime;
    private long lastEITActPFS0OccurPosition;
    private long lastEITActPFS1OccurTime;
    private long lastEITActPFS1OccurPosition;
    private long lastRSTOccurTime;
    private long lastRSTOccurPosition;
    private long lastTDTOccurTime;
    private long lastTDTOccurPosition;
    private int pcrPid;
    private long lastPcrValue;
    private long lastPcrPct;
    private int avgBitrate;
    private int pktcnt;

    private M2TKDatabase databaseService;
    private long transactionId;

    static class Context
    {
        long occurTime;
        long occurPosition;
        long checksum;
        boolean checksumCorrect;
    }

    public TR290Tracer2()
    {
        pkt = new TransportPacketDecoder();
        adpt = new AdaptationFieldDecoder();
        pcr = new ProgramClockReferenceDecoder();

        sec = new SectionDecoder();
        pat = new PATSectionDecoder();
        cat = new CATSectionDecoder();
        pmt = new PMTSectionDecoder();
        nit = new NITSectionDecoder();
        bat = new BATSectionDecoder();
        sdt = new SDTSectionDecoder();
        eit = new EITSectionDecoder();
        rst = new RSTSectionDecoder();
        tdt = new TDTSectionDecoder();
        descloop = new DescriptorLoopDecoder();
        cad = new CADescriptorDecoder();
        element = new ProgramElementDecoder();

        pmtChannels = new TSDemux.Channel[8192];
        pmtOccurTimes = new long[8192];
        pmtOccurPositions = new long[8192];

        sectionContexts = new HashMap<>();
        programNumbers = new HashSet<>();
        programPmtPids = new HashSet<>();
        pmtMappedStreams = new HashSet<>();

        streamOccurTimes = new long[8192];
        streamOccurPositions = new long[8192];

        scrambledFlags = new int[8192];
        streamMarks = new int[8192];
        streamCounts = new int[8192];
        lastUnreferencedStreamCheckTime = -1;
        lastUnreferencedStreamCheckPosition = -1;

        lastPATOccurTime = -1;
        lastPATOccurPosition = -1;
        lastCATOccurTime = -1;
        lastCATOccurPosition = -1;

        lastNITActOccurTime = -1;
        lastNITActOccurPosition = -1;
        lastSDTActOccurTime = -1;
        lastSDTActOccurPosition = -1;
        lastEITActPFOccurTime = -1;
        lastEITActPFOccurPosition = -1;
        lastEITActPFS0OccurTime = -1;
        lastEITActPFS0OccurPosition = -1;
        lastEITActPFS1OccurTime = -1;
        lastEITActPFS1OccurPosition = -1;
        lastRSTOccurTime = -1;
        lastRSTOccurPosition = -1;
        lastTDTOccurTime = -1;
        lastTDTOccurPosition = -1;
        pcrPid = -1;
        lastPcrValue = -1;
        avgBitrate = 0;
        pktcnt = 0;
    }

    @Override
    public void configure(StreamSource source, TSDemux demux, M2TKDatabase database)
    {
        databaseService = database;

        demux.registerRawChannel(this::processTransportPacket);
        demux.registerSectionChannel(0x0000, this::processSection);
        demux.registerSectionChannel(0x0001, this::processSection);
        demux.registerSectionChannel(0x0010, this::processSection);
        demux.registerSectionChannel(0x0011, this::processSection);
        demux.registerSectionChannel(0x0012, this::processSection);
        demux.registerSectionChannel(0x0014, this::processSection);
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

    private void processTransportPacket(TSDemuxPayload payload)
    {
        pkt.attach(payload.getEncoding());
        if (pkt.containsTransportError())
            return;

        pktcnt = (pktcnt + 1) % 1000;

        int pid = payload.getStreamPID();
        streamOccurTimes[pid] = System.currentTimeMillis();
        streamOccurPositions[pid] = payload.getStartPacketCounter();

        streamCounts[pid] += 1;
        scrambledFlags[pid] = pkt.isScrambled() ? 1 : 0;

        calculateBitrate(payload);
        checkPATSectionOccurrenceInterval(payload);
        checkCATSectionOccurrenceInterval(payload);
        checkPMTSectionOccurrenceInterval(payload);
        checkNITSectionOccurrenceInterval(payload);
        checkSDTSectionOccurrenceInterval(payload);
        checkEITSectionOccurrenceInterval(payload);
        checkTDTSectionOccurrenceInterval(payload);
        checkPMTMappedStreamOccurrenceInterval(payload);

        checkUnexpectedScrambledPATStream(payload);
        checkUnexpectedScrambledPMTStream(payload);

        checkUnreferencedStream(payload);
    }

    private void calculateBitrate(TSDemuxPayload payload)
    {
        try
        {
            long currPcrValue = readPCR();
            long currPct = payload.getStartPacketCounter();
            if (currPcrValue == -1)
                return; // 无PCR

            if (pcrPid == -1)
            {
                // 遇到的第一个PCR
                pcrPid = payload.getStreamPID();
                lastPcrValue = currPcrValue;
                lastPcrPct = currPct;
                return;
            }

            if (pcrPid == payload.getStreamPID())
            {
                int bitrate = ProgramClockReference.bitrate(lastPcrValue, currPcrValue, currPct - lastPcrPct);
                avgBitrate = (avgBitrate == 0) ? bitrate : (avgBitrate + bitrate) / 2;
                lastPcrValue = currPcrValue;
                lastPcrPct = currPct;
            }
        } catch (Exception ex)
        {
            log.warn("{}", ex.getMessage());
        }
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

    private long calculateInterval(long lastOccurPosition, long currOccurPosition,
                                   long lastOccurTime, long currOccurTime)
    {
        if (avgBitrate != 0)
            return (currOccurPosition - lastOccurPosition) * 188 * 8 * 1000 / avgBitrate;
        return (lastOccurTime > 0) ? currOccurTime - lastOccurTime : 0;
    }

    private void checkPATSectionOccurrenceInterval(TSDemuxPayload payload)
    {
        if (pktcnt != 0)
            return; // pktcnt以固定长度循环（默认设为1000），即这里每1000个包检查一次，避免频率过高影响处理速度。

        long currOccurPosition = payload.getStartPacketCounter();
        long currOccurTime = System.currentTimeMillis();

        long interval = calculateInterval(lastPATOccurPosition, currOccurPosition,
                                          lastPATOccurTime, currOccurTime);
        if (interval > 500)
        {
            reportError(TR290ErrorTypes.PAT_ERROR_2, "超过0.5s未收到PAT分段",
                        payload.getStartPacketCounter(), payload.getStreamPID());

            // 重置位置以待下一轮检查
            lastPATOccurPosition = currOccurPosition;
            lastPATOccurTime = currOccurTime;
        }
    }

    private void checkCATSectionOccurrenceInterval(TSDemuxPayload payload)
    {
        if (pktcnt != 100)
            return;

        long currOccurPosition = payload.getStartPacketCounter();
        long currOccurTime = System.currentTimeMillis();

        long interval = calculateInterval(lastCATOccurPosition, currOccurPosition,
                                          lastCATOccurTime, currOccurTime);
        if (interval < 500)
            return;

        boolean hasScrambledStream = IntStream.of(scrambledFlags).anyMatch(flag -> flag == 1);
        if (hasScrambledStream)
            reportError(TR290ErrorTypes.CAT_ERROR, "存在加扰流，但超过0.5s未收到CAT分段",
                        payload.getStartPacketCounter(), payload.getStreamPID());

        // 重置位置以待下一轮检查
        lastCATOccurPosition = currOccurPosition;
        lastCATOccurTime = currOccurTime;
    }

    private void checkPMTSectionOccurrenceInterval(TSDemuxPayload payload)
    {
        if (pktcnt != 200)
            return; // 错开PAT检测

        long currOccurPosition = payload.getStartPacketCounter();
        long currOccurTime = System.currentTimeMillis();

        for (int pmtpid : programPmtPids)
        {
            long interval = calculateInterval(pmtOccurPositions[pmtpid], currOccurPosition,
                                              pmtOccurTimes[pmtpid], currOccurTime);
            if (interval > 500)
            {
                reportError(TR290ErrorTypes.PMT_ERROR_2,
                            String.format("超过0.5s未收到PMT分段（pid = %d）", pmtpid),
                            payload.getStartPacketCounter(), payload.getStreamPID());

                // 重置位置以待下一轮检查
                pmtOccurPositions[pmtpid] = currOccurPosition;
                pmtOccurTimes[pmtpid] = currOccurTime;
            }
        }
    }

    private void checkNITSectionOccurrenceInterval(TSDemuxPayload payload)
    {
        if (pktcnt != 400)
            return;

        long currOccurPosition = payload.getStartPacketCounter();
        long currOccurTime = System.currentTimeMillis();

        long interval = calculateInterval(lastNITActOccurPosition, currOccurPosition,
                                          lastNITActOccurTime, currOccurTime);
        if (interval > 10000)
        {
            reportError(TR290ErrorTypes.NIT_ACTUAL_ERROR, "超过10s未收到NIT_actual分段",
                        payload.getStartPacketCounter(), payload.getStreamPID());

            // 重置位置以待下一轮检查
            lastNITActOccurPosition = currOccurPosition;
            lastNITActOccurTime = currOccurTime;
        }
    }

    private void checkSDTSectionOccurrenceInterval(TSDemuxPayload payload)
    {
        if (pktcnt != 500)
            return;

        long currOccurPosition = payload.getStartPacketCounter();
        long currOccurTime = System.currentTimeMillis();

        long interval = calculateInterval(lastSDTActOccurPosition, currOccurPosition,
                                          lastSDTActOccurTime, currOccurTime);
        if (interval > 2000)
        {
            reportError(TR290ErrorTypes.SDT_ACTUAL_ERROR, "超过2s未收到SDT_actual分段",
                        payload.getStartPacketCounter(), payload.getStreamPID());

            // 重置位置以待下一轮检查
            lastSDTActOccurPosition = currOccurPosition;
            lastSDTActOccurTime = currOccurTime;
        }
    }

    private void checkEITSectionOccurrenceInterval(TSDemuxPayload payload)
    {
        if (pktcnt != 600)
            return;

        long currOccurPosition = payload.getStartPacketCounter();
        long currOccurTime = System.currentTimeMillis();

        long interval = calculateInterval(lastEITActPFS0OccurPosition, currOccurPosition,
                                          lastEITActPFS0OccurTime, currOccurTime);
        if (interval > 2000)
        {
            reportError(TR290ErrorTypes.EIT_ACTUAL_ERROR, "超过2s未收到EIT_actual P/F Section[0]分段",
                        payload.getStartPacketCounter(), payload.getStreamPID());

            // 重置位置以待下一轮检查
            lastEITActPFS0OccurPosition = currOccurPosition;
            lastEITActPFS0OccurTime = currOccurTime;
        }

        interval = calculateInterval(lastEITActPFS1OccurPosition, currOccurPosition,
                                     lastEITActPFS1OccurTime, currOccurTime);
        if (interval > 2000)
        {
            reportError(TR290ErrorTypes.EIT_ACTUAL_ERROR, "超过2s未收到EIT_actual P/F Section[1]分段",
                        payload.getStartPacketCounter(), payload.getStreamPID());

            // 重置位置以待下一轮检查
            lastEITActPFS1OccurPosition = currOccurPosition;
            lastEITActPFS1OccurTime = currOccurTime;
        }
    }

    private void checkTDTSectionOccurrenceInterval(TSDemuxPayload payload)
    {
        if (pktcnt != 700)
            return;

        long currOccurPosition = payload.getStartPacketCounter();
        long currOccurTime = System.currentTimeMillis();

        long interval = calculateInterval(lastTDTOccurPosition, currOccurPosition,
                                          lastTDTOccurTime, currOccurTime);
        if (interval > 30000)
        {
            reportError(TR290ErrorTypes.SI_REPETITION_ERROR, "超过30s未收到TDT表",
                        payload.getFinishPacketCounter(), payload.getStreamPID());
        }

        lastTDTOccurPosition = currOccurPosition;
        lastTDTOccurTime = currOccurTime;
    }

    private void checkPMTMappedStreamOccurrenceInterval(TSDemuxPayload payload)
    {
        if (pktcnt != 900)
            return;

        long currOccurPosition = payload.getStartPacketCounter();
        long currOccurTime = System.currentTimeMillis();

        for (int pid : pmtMappedStreams)
        {
            long interval = calculateInterval(streamOccurPositions[pid], currOccurPosition,
                                              streamOccurTimes[pid], currOccurTime);
            if (interval > 5000)
            {
                reportError(TR290ErrorTypes.PID_ERROR,
                            String.format("超过5s未收到被PMT映射的流（pid = %d）", pid),
                            payload.getStartPacketCounter(), payload.getStreamPID());

                streamOccurPositions[pid] = currOccurPosition;
                streamOccurTimes[pid] = currOccurTime;
            }
        }
    }

    private void checkUnexpectedScrambledPATStream(TSDemuxPayload payload)
    {
        // 如果持续出现加扰情况，则会持续报警，显著影响执行效率。有风险。
        if (payload.getStreamPID() == 0x0000 && pkt.isScrambled())
        {
            reportError(TR290ErrorTypes.PAT_ERROR_2,
                        String.format("PID=0的TS包加扰指示不等于0（pct = %d）",
                                      payload.getStartPacketCounter()),
                        payload.getStartPacketCounter(), payload.getStreamPID());
        }
    }

    private void checkUnexpectedScrambledPMTStream(TSDemuxPayload payload)
    {
        // 如果持续出现加扰情况，则会持续报警，显著影响执行效率。有风险。
        if (pmtChannels[payload.getStreamPID()] != null && pkt.isScrambled())
        {
            reportError(TR290ErrorTypes.PMT_ERROR_2,
                        String.format("携带PMT的TS包加扰指示不等于0（pid = %d，pct = %d）",
                                      payload.getStreamPID(), payload.getStartPacketCounter()),
                        payload.getStartPacketCounter(), payload.getStreamPID());
        }
    }

    private void checkUnreferencedStream(TSDemuxPayload payload)
    {
        if (pktcnt != 800)
            return;

        long currOccurPosition = payload.getStartPacketCounter();
        long currOccurTime = System.currentTimeMillis();

        long interval = calculateInterval(lastUnreferencedStreamCheckPosition, currOccurPosition,
                                          lastUnreferencedStreamCheckTime, currOccurTime);
        if (interval < 500)
            return;

        for (int i = 0x20; i < 0x1FFF; i++)
        {
            // 非空包，非PMT流，且未被PMT标记（ECM、ES）、未被CAT标记（EMM）的其他出现数据的流
            if (pmtChannels[i] == null && streamMarks[i] == 0 && streamCounts[i] > 0)
            {
                reportError(TR290ErrorTypes.UNREFERENCED_PID,
                            String.format("超过0.5s仍然存在未被PMT、CAT关联的流（pid = %d）", i),
                            payload.getFinishPacketCounter(), payload.getStreamPID());

                streamCounts[i] = 0;
            }
        }

        lastUnreferencedStreamCheckPosition = currOccurPosition;
        lastUnreferencedStreamCheckTime = currOccurTime;
    }

    private void processSection(TSDemuxPayload payload)
    {
        if (payload.getType() != TSDemuxPayload.Type.SECTION || !sec.isAttachable(payload.getEncoding()))
            return;

        sec.attach(payload.getEncoding());
        int tableId = sec.getTableID();

        int pid = payload.getStreamPID();
        if (pid == 0x0000)
        {
            checkUnexpectedSectionOnPATStream(tableId, payload);
            processPAT(payload);
        }

        if (pid == 0x0001)
        {
            checkUnexpectedSectionOnCATStream(tableId, payload);
            processCAT(payload);
        }

        if (pmtChannels[pid] != null && pmt.isAttachable(payload.getEncoding()))
            processPMT(payload);

        if (pid == 0x0010)
        {
            checkUnexpectedSectionOnNITStream(tableId, payload);
            processNIT(payload);
        }

        if (pid == 0x0011)
        {
            checkUnexpectedSectionOnSDTStream(tableId, payload);
            processSDT(payload);
            processBAT(payload);
        }

        if (pid == 0x0012)
        {
            checkUnexpectedSectionOnEITStream(tableId, payload);
            processEIT(payload);
        }

        if (pid == 0x0013)
        {
            checkUnexpectedSectionOnRSTStream(tableId, payload);
            processRST(payload);
        }

        if (pid == 0x0014)
        {
            checkUnexpectedSectionOnTDTStream(tableId, payload);
            processTDT(payload);
        }
    }

    private void checkUnexpectedSectionOnPATStream(int tableId, TSDemuxPayload payload)
    {
        if (tableId != 0x00)
        {
            reportError(TR290ErrorTypes.PAT_ERROR_2,
                        String.format("TableID不为0的段出现在PID=0的流里（table_id = %02x）", tableId),
                        payload.getFinishPacketCounter(), payload.getStreamPID());
        }
    }

    private void checkUnexpectedSectionOnCATStream(int tableId, TSDemuxPayload payload)
    {
        if (tableId != 0x01)
        {
            reportError(TR290ErrorTypes.CAT_ERROR,
                        String.format("TableID不为1的段出现在PID=1的流里（table_id = %02x）", tableId),
                        payload.getFinishPacketCounter(), payload.getStreamPID());
        }
    }

    private void checkUnexpectedSectionOnNITStream(int tableId, TSDemuxPayload payload)
    {
        if (tableId != 0x40 && tableId != 0x41 && tableId != 0x72)
        {
            reportError(TR290ErrorTypes.NIT_ACTUAL_ERROR,
                        String.format("NIT或ST以外的表出现在PID=0x0010的流里（table_id = %02x）", tableId),
                        payload.getFinishPacketCounter(), payload.getStreamPID());
        }
    }

    private void checkUnexpectedSectionOnSDTStream(int tableId, TSDemuxPayload payload)
    {
        if (tableId != 0x42 && tableId != 0x46 && tableId != 0x4A && tableId != 0x72)
        {
            reportError(TR290ErrorTypes.SDT_ACTUAL_ERROR,
                        String.format("BAT或SDT或ST以外的表出现在PID=0x0011的流里（table_id = %02x）", tableId),
                        payload.getFinishPacketCounter(), payload.getStreamPID());
        }
    }

    private void checkUnexpectedSectionOnEITStream(int tableId, TSDemuxPayload payload)
    {
        if ((tableId < 0x4E || tableId > 0x6F) && tableId != 0x72)
        {
            reportError(TR290ErrorTypes.EIT_ACTUAL_ERROR,
                        String.format("EIT或ST以外的表出现在PID=0x0012的流里（table_id = %02x）", tableId),
                        payload.getFinishPacketCounter(), payload.getStreamPID());
        }
    }

    private void checkUnexpectedSectionOnRSTStream(int tableId, TSDemuxPayload payload)
    {
        if (tableId != 0x71 && tableId != 0x72)
        {
            reportError(TR290ErrorTypes.RST_ERROR,
                        String.format("RST或ST以外的表出现在PID=0x0013的流里（table_id = %02x）", tableId),
                        payload.getFinishPacketCounter(), payload.getStreamPID());
        }
    }

    private void checkUnexpectedSectionOnTDTStream(int tableId, TSDemuxPayload payload)
    {
        if (tableId != 0x70 && tableId != 0x72 && tableId != 0x73)
        {
            reportError(TR290ErrorTypes.TDT_ERROR,
                        String.format("TDT或TOT或ST以外的表出现在PID=0x0014的流里（table_id = %02x）", tableId),
                        payload.getFinishPacketCounter(), payload.getStreamPID());
        }
    }

    private void processPAT(TSDemuxPayload payload)
    {
        if (!pat.isAttachable(payload.getEncoding()))
            return;

        lastPATOccurTime = System.currentTimeMillis();
        lastPATOccurPosition = payload.getFinishPacketCounter();

        pat.attach(payload.getEncoding());
        int secnum = pat.getSectionNumber();
        long checksum = pat.getChecksum();

        String uid = String.format("pat.%d", secnum);
        Context ctx = sectionContexts.get(uid);
        if (ctx != null && ctx.checksum == checksum)
        {
            // 严格的相等。
            if (!ctx.checksumCorrect)
                reportError(TR290ErrorTypes.CRC_ERROR, "PAT表CRC32错误",
                            payload.getFinishPacketCounter(), payload.getStreamPID());
            return;
        }

        TSDemux demux = payload.getChannel().getHost();
        if (ctx != null)
        {
            // 如果这里ctx不为空，则肯定checksum有变化（因为相同时前面已经处理了），下同。
            removeTableContexts("pat");

            // 任何一个PAT分段变化，都将当前节目清空，重新解析PMT。
            for (int pid = 0; pid < 8192; pid++)
            {
                TSDemux.Channel channel = pmtChannels[pid];
                if (channel != null)
                {
                    demux.closeChannel(channel);
                    streamMarks[pid] = 0;
                    pmtChannels[pid] = null;
                }
            }
            programNumbers.clear();
            programPmtPids.clear();
            pmtMappedStreams.clear();
        }

        // 更新上下文
        ctx = updateTableContext(uid, checksum);

        pat.forEachProgramAssociation((number, pmtpid) -> {
            TSDemux.Channel channel = demux.registerSectionChannel(pmtpid, this::processPMT);
            pmtChannels[pmtpid] = channel;
            streamMarks[pmtpid] = 1;
            programNumbers.add(number);
            programPmtPids.add(pmtpid);
        });

        if (!pat.isChecksumCorrect())
        {
            ctx.checksumCorrect = false;
            reportError(TR290ErrorTypes.CRC_ERROR, "PAT表CRC32错误",
                        payload.getFinishPacketCounter(), payload.getStreamPID());
        }
    }

    private void processCAT(TSDemuxPayload payload)
    {
        if (!cat.isAttachable(payload.getEncoding()))
            return;

        cat.attach(payload.getEncoding());

        lastCATOccurTime = System.currentTimeMillis();
        lastCATOccurPosition = payload.getFinishPacketCounter();

        int secnum = cat.getSectionNumber();
        long checksum = cat.getChecksum();

        String uid = String.format("cat.%d", secnum);
        Context ctx = sectionContexts.get(uid);
        if (ctx != null && ctx.checksum == checksum)
        {
            // 严格的相等。
            if (!ctx.checksumCorrect)
                reportError(TR290ErrorTypes.CRC_ERROR, "CAT表CRC32错误",
                            payload.getFinishPacketCounter(), payload.getStreamPID());
            return;
        }

        if (ctx != null)
            removeTableContexts("cat");

        ctx = updateTableContext(uid, checksum);

        descloop.attach(cat.getDescriptorLoop());
        descloop.forEach(cad::isAttachable, descriptor -> {
            cad.attach(descriptor);
            streamMarks[cad.getConditionalAccessStreamPID()] = 1; // 标记EMM
        });

        if (!cat.isChecksumCorrect())
        {
            ctx.checksumCorrect = false;
            reportError(TR290ErrorTypes.CRC_ERROR, "CAT表CRC32错误",
                        payload.getFinishPacketCounter(), payload.getStreamPID());
        }
    }

    private void processPMT(TSDemuxPayload payload)
    {
        pmt.attach(payload.getEncoding());
        int number = pmt.getProgramNumber();
        long checksum = pmt.getChecksum();

        if (!programNumbers.contains(number))
            return; // 非注册节目的PMT，不处理。

        String uid = String.format("pmt.%d", number);
        Context ctx = sectionContexts.get(uid);
        if (ctx != null && ctx.checksum == checksum)
        {
            pmtOccurTimes[payload.getStreamPID()] = System.currentTimeMillis();
            pmtOccurPositions[payload.getStreamPID()] = payload.getFinishPacketCounter();

            // 严格的相等。
            if (!ctx.checksumCorrect)
                reportError(TR290ErrorTypes.CRC_ERROR, String.format("PMT表CRC32错误（节目号：%d）", number),
                            payload.getFinishPacketCounter(), payload.getStreamPID());
            return;
        }

        // 更新上下文
        ctx = updateTableContext(uid, checksum);
        pmtOccurTimes[payload.getStreamPID()] = System.currentTimeMillis();
        pmtOccurPositions[payload.getStreamPID()] = payload.getFinishPacketCounter();

        descloop.attach(pmt.getDescriptorLoop());
        descloop.forEach(cad::isAttachable, descriptor -> {
            cad.attach(descriptor);
            streamMarks[cad.getConditionalAccessStreamPID()] = 1; // 标记ECM
        });

        // 所有PMT基本流类型均一视同仁，判定时不作区分。
        pmt.forEachProgramElement(encoding -> {
            element.attach(encoding);
            streamMarks[element.getElementaryPID()] = 1; // 标记ES
            pmtMappedStreams.add(element.getElementaryPID());

            descloop.attach(element.getDescriptorLoop());
            descloop.forEach(cad::isAttachable, descriptor -> {
                cad.attach(descriptor);
                streamMarks[cad.getConditionalAccessStreamPID()] = 1; // 标记ECM
            });
        });

        if (!pmt.isChecksumCorrect())
        {
            ctx.checksumCorrect = false;
            reportError(TR290ErrorTypes.CRC_ERROR, String.format("PMT表CRC32错误（节目号：%d）", number),
                        payload.getFinishPacketCounter(), payload.getStreamPID());
        }
    }

    private void processNIT(TSDemuxPayload payload)
    {
        if (!nit.isAttachable(payload.getEncoding()))
            return;

        nit.attach(payload.getEncoding());
        int tableId = nit.getTableID();
        int networkId = nit.getNetworkID();
        int secnum = nit.getSectionNumber();
        long checksum = nit.getChecksum();

        long currOccurPosition = payload.getFinishPacketCounter();
        long currOccurTime = System.currentTimeMillis();

        if (tableId == 0x40)
        {
            long interval = calculateInterval(lastNITActOccurPosition, currOccurPosition,
                                              lastNITActOccurTime, currOccurTime);
            if (0 < interval && interval < 25)
            {
                reportError(TR290ErrorTypes.NIT_ACTUAL_ERROR,
                            String.format("NIT_actual间隔小于25ms（实际：%dms）", interval),
                            payload.getFinishPacketCounter(), payload.getStreamPID());
            }

            lastNITActOccurPosition = currOccurPosition;
            lastNITActOccurTime = currOccurTime;
        }

        String uid = String.format("nit.%d.%d.%d", tableId, networkId, secnum);
        Context ctx = sectionContexts.get(uid);
        if (ctx != null && ctx.checksum == checksum)
        {
            if (tableId == 0x41)
            {
                long interval = calculateInterval(ctx.occurPosition, currOccurPosition,
                                                  ctx.occurTime, currOccurTime);
                if (interval > 10000)
                {
                    reportError(TR290ErrorTypes.SDT_OTHER_ERROR,
                                String.format("NIT_other间隔大于10s（实际：%.1fs）", 1.0 * interval / 1000),
                                payload.getFinishPacketCounter(), payload.getStreamPID());
                }

                ctx.occurPosition = currOccurPosition;
                ctx.occurTime = currOccurTime;
            }

            if (!ctx.checksumCorrect)
                reportError(TR290ErrorTypes.CRC_ERROR,
                            String.format("NIT表CRC32错误（nid = %d）", networkId),
                            payload.getFinishPacketCounter(), payload.getStreamPID());
            return;
        }

        if (ctx != null)
            removeTableContexts(String.format("nit.%d.%d", tableId, networkId));

        // 更新上下文
        ctx = updateTableContext(uid, checksum);
        ctx.occurPosition = currOccurPosition;
        ctx.occurTime = currOccurTime;

        if (!nit.isChecksumCorrect())
        {
            ctx.checksumCorrect = false;
            reportError(TR290ErrorTypes.CRC_ERROR,
                        String.format("NIT表CRC32错误（nid = %d）", networkId),
                        payload.getFinishPacketCounter(), payload.getStreamPID());
        }
    }

    private void processBAT(TSDemuxPayload payload)
    {
        if (!bat.isAttachable(payload.getEncoding()))
            return;

        bat.attach(payload.getEncoding());
        int bouquetId = bat.getBouquetID();
        int secnum = bat.getSectionNumber();
        long checksum = bat.getChecksum();

        long currOccurPosition = payload.getFinishPacketCounter();
        long currOccurTime = System.currentTimeMillis();

        String uid = String.format("bat.%d.%d", bouquetId, secnum);
        Context ctx = sectionContexts.get(uid);
        if (ctx != null && ctx.checksum == checksum)
        {
            long interval = calculateInterval(ctx.occurPosition, currOccurPosition,
                                              ctx.occurTime, currOccurTime);
            if (interval > 10000)
            {
                reportError(TR290ErrorTypes.SI_REPETITION_ERROR,
                            String.format("BAT分段间隔大于10s（实际：%.1fs，bid = %d）",
                                          1.0 * interval / 1000, bouquetId),
                            payload.getFinishPacketCounter(), payload.getStreamPID());
            }

            ctx.occurPosition = currOccurPosition;
            ctx.occurTime = currOccurTime;

            if (!ctx.checksumCorrect)
                reportError(TR290ErrorTypes.CRC_ERROR,
                            String.format("BAT表CRC32错误（bid = %d）", bouquetId),
                            payload.getFinishPacketCounter(), payload.getStreamPID());
            return;
        }

        if (ctx != null)
            removeTableContexts(String.format("bat.%d", bouquetId));

        // 更新上下文
        ctx = updateTableContext(uid, checksum);
        ctx.occurPosition = currOccurPosition;
        ctx.occurTime = currOccurTime;

        if (!bat.isChecksumCorrect())
        {
            ctx.checksumCorrect = false;
            reportError(TR290ErrorTypes.CRC_ERROR,
                        String.format("BAT表CRC32错误（bid = %d）", bouquetId),
                        payload.getFinishPacketCounter(), payload.getStreamPID());
        }
    }

    private void processSDT(TSDemuxPayload payload)
    {
        if (!sdt.isAttachable(payload.getEncoding()))
            return;

        sdt.attach(payload.getEncoding());
        int tableId = sdt.getTableID();
        int tsid = sdt.getTransportStreamID();
        int onid = sdt.getOriginalNetworkID();
        int secnum = sdt.getSectionNumber();
        long checksum = sdt.getChecksum();

        long currOccurPosition = payload.getFinishPacketCounter();
        long currOccurTime = System.currentTimeMillis();

        if (tableId == 0x42)
        {
            long interval = calculateInterval(lastSDTActOccurPosition, currOccurPosition,
                                              lastSDTActOccurTime, currOccurTime);
            if (0 < interval && interval < 25)
            {
                reportError(TR290ErrorTypes.SDT_ACTUAL_ERROR,
                            String.format("SDT_actual间隔小于25ms（实际：%dms）", interval),
                            payload.getFinishPacketCounter(), payload.getStreamPID());
            }

            lastSDTActOccurPosition = currOccurPosition;
            lastSDTActOccurTime = currOccurTime;
        }

        String uid = String.format("sdt.%d.%d.%d.%d", tableId, onid, tsid, secnum);
        Context ctx = sectionContexts.get(uid);
        if (ctx != null && ctx.checksum == checksum)
        {
            if (tableId == 0x46)
            {
                long interval = calculateInterval(ctx.occurPosition, currOccurPosition,
                                                  ctx.occurTime, currOccurTime);
                if (interval > 10000)
                {
                    reportError(TR290ErrorTypes.SDT_OTHER_ERROR,
                                String.format("SDT_other间隔大于10s（实际：%.1fs）", 1.0 * interval / 1000),
                                payload.getFinishPacketCounter(), payload.getStreamPID());
                }

                ctx.occurPosition = currOccurPosition;
                ctx.occurTime = currOccurTime;
            }

            if (!ctx.checksumCorrect)
                reportError(TR290ErrorTypes.CRC_ERROR,
                            String.format("SDT表CRC32错误（onid = %d，tsid = %d）", onid, tsid),
                            payload.getFinishPacketCounter(), payload.getStreamPID());
            return;
        }

        if (ctx != null)
            removeTableContexts(String.format("sdt.%d.%d.%d", tableId, onid, tsid));

        ctx = updateTableContext(uid, checksum);
        ctx.occurPosition = currOccurPosition;
        ctx.occurTime = currOccurTime;

        if (!sdt.isChecksumCorrect())
        {
            ctx.checksumCorrect = false;
            reportError(TR290ErrorTypes.CRC_ERROR,
                        String.format("SDT表CRC32错误（onid = %d，tsid = %d）", onid, tsid),
                        payload.getFinishPacketCounter(), payload.getStreamPID());
        }
    }

    private void processEIT(TSDemuxPayload payload)
    {
        if (!eit.isAttachable(payload.getEncoding()))
            return;

        eit.attach(payload.getEncoding());
        int tableId = eit.getTableID();
        int sid = eit.getServiceID();
        int tsid = eit.getTransportStreamID();
        int onid = eit.getOriginalNetworkID();
        int secnum = eit.getSectionNumber();
        long checksum = eit.getChecksum();

        long currOccurPosition = payload.getFinishPacketCounter();
        long currOccurTime = System.currentTimeMillis();

        if (tableId == 0x4E)
        {
            long interval = calculateInterval(lastEITActPFOccurPosition, currOccurPosition,
                                              lastEITActPFOccurTime, currOccurTime);
            if (0 < interval && interval < 25)
            {
                reportError(TR290ErrorTypes.EIT_ACTUAL_ERROR,
                            String.format("EIT_actual P/F 间隔小于25ms（实际：%dms）", interval),
                            payload.getFinishPacketCounter(), payload.getStreamPID());
            }

            lastEITActPFOccurPosition = currOccurPosition;
            lastEITActPFOccurTime = currOccurTime;

            if (secnum == 0)
            {
                lastEITActPFS0OccurPosition = currOccurPosition;
                lastEITActPFS0OccurTime = currOccurTime;
            }
            if (secnum == 1)
            {
                lastEITActPFS1OccurPosition = currOccurPosition;
                lastEITActPFS1OccurTime = currOccurTime;
            }
        }

        String uid = String.format("eit.%d.%d.%d.%d.%d", tableId, onid, tsid, sid, secnum);
        Context ctx = sectionContexts.get(uid);
        if (ctx != null && ctx.checksum == checksum)
        {
            if (tableId == 0x4F)
            {
                long interval = calculateInterval(ctx.occurPosition, currOccurPosition,
                                                  ctx.occurTime, currOccurTime);
                if (interval > 10000)
                {
                    reportError(TR290ErrorTypes.EIT_OTHER_ERROR,
                                String.format("EIT_other %s 间隔大于10s（实际：%.1fs）",
                                              secnum == 0 ? "P" : "F",
                                              1.0 * interval / 1000),
                                payload.getFinishPacketCounter(), payload.getStreamPID());
                }

                ctx.occurPosition = currOccurPosition;
                ctx.occurTime = currOccurTime;
            }

            if (!ctx.checksumCorrect)
                reportError(TR290ErrorTypes.CRC_ERROR,
                            String.format("EIT表CRC32错误（onid = %d, tsid = %d, sid = %d）", onid, tsid, sid),
                            payload.getFinishPacketCounter(), payload.getStreamPID());
            return;
        }

        if (ctx != null)
            removeTableContexts(String.format("eit.%d.%d.%d.%d", tableId, onid, tsid, sid));

        ctx = updateTableContext(uid, checksum);
        ctx.occurPosition = currOccurPosition;
        ctx.occurTime = currOccurTime;

        if (!eit.isChecksumCorrect())
        {
            ctx.checksumCorrect = false;
            reportError(TR290ErrorTypes.CRC_ERROR,
                        String.format("EIT表CRC32错误（onid = %d, tsid = %d, sid = %d）", onid, tsid, sid),
                        payload.getFinishPacketCounter(), payload.getStreamPID());
        }
    }

    private void processRST(TSDemuxPayload payload)
    {
        if (!rst.isAttachable(payload.getEncoding()))
            return;

        long currOccurPosition = payload.getFinishPacketCounter();
        long currOccurTime = System.currentTimeMillis();

        long interval = calculateInterval(lastRSTOccurPosition, currOccurPosition,
                                          lastRSTOccurTime, currOccurTime);
        if (0 < interval && interval < 25)
        {
            reportError(TR290ErrorTypes.RST_ERROR,
                        String.format("RST间隔小于25ms（实际：%dms）", interval),
                        payload.getFinishPacketCounter(), payload.getStreamPID());
        }

        lastRSTOccurPosition = currOccurPosition;
        lastRSTOccurTime = currOccurTime;
    }

    private void processTDT(TSDemuxPayload payload)
    {
        if (!tdt.isAttachable(payload.getEncoding()))
            return;

        long currOccurPosition = payload.getFinishPacketCounter();
        long currOccurTime = System.currentTimeMillis();

        long interval = calculateInterval(lastTDTOccurPosition, currOccurPosition,
                                          lastTDTOccurTime, currOccurTime);
        if (0 < interval && interval < 25)
        {
            reportError(TR290ErrorTypes.TDT_ERROR,
                        String.format("TDT间隔小于25ms（实际：%dms）", interval),
                        payload.getFinishPacketCounter(), payload.getStreamPID());
        }

        if (interval > 30000)
        {
            reportError(TR290ErrorTypes.SI_REPETITION_ERROR,
                        String.format("TDT间隔大于30s（实际：%.1fs）", 1.0 * interval / 1000),
                        payload.getFinishPacketCounter(), payload.getStreamPID());
        }

        lastTDTOccurPosition = currOccurPosition;
        lastTDTOccurTime = currOccurTime;
    }

    private void removeTableContexts(String keyPrefix)
    {
        List<String> tableKeys = new ArrayList<>(sectionContexts.keySet());
        for (String key : tableKeys)
        {
            if (key.startsWith(keyPrefix))
                sectionContexts.remove(key);
        }
    }

    private Context updateTableContext(String key, long checksum)
    {
        Context ctx = new Context();
        ctx.checksum = checksum;
        ctx.checksumCorrect = true;
        sectionContexts.put(key, ctx);
        return ctx;
    }
}
