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

import java.time.LocalDateTime;
import java.util.*;

public class TR290Tracer2 implements Tracer
{
    private final DatabaseService databaseService;
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
    private final int[] SPRFs; // Scrambled Packet Reported Flags
    private final Map<String, Context> tableContexts;
    private final Set<Integer> programNumbers;

    private long lastNITActOccurTime;
    private long lastNITActOccurPosition;
    private long lastSDTActOccurTime;
    private long lastSDTActOccurPosition;
    private long lastEITActOccurTime;
    private long lastEITActOccurPosition;
    private long lastRSTOccurTime;
    private long lastRSTOccurPosition;
    private long lastTDTOccurTime;
    private long lastTDTOccurPosition;
    private int pcrPid;
    private long lastPcrValue;
    private long lastPcrPct;
    private int avgBitrate;

    static class Context
    {
        long checksum;
        boolean checksumCorrect;
    }

    public TR290Tracer2(DatabaseService service)
    {
        databaseService = service;
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
        tableContexts = new HashMap<>();
        programNumbers = new HashSet<>();

        SPRFs = new int[8192];

        lastNITActOccurTime = -1;
        lastNITActOccurPosition = -1;
        lastSDTActOccurTime = -1;
        lastSDTActOccurPosition = -1;
        lastEITActOccurTime = -1;
        lastEITActOccurPosition = -1;
        lastRSTOccurTime = -1;
        lastRSTOccurPosition = -1;
        lastTDTOccurTime = -1;
        lastTDTOccurPosition = -1;
        pcrPid = -1;
        lastPcrValue = -1;
        avgBitrate = 0;
    }

    @Override
    public void configureDemux(TSDemux demux)
    {
        demux.registerRawChannel(this::processTransportPacket);
        demux.registerSectionChannel(0x0000, this::processSection);
        demux.registerSectionChannel(0x0001, this::processSection);
        demux.registerSectionChannel(0x0010, this::processSection);
        demux.registerSectionChannel(0x0011, this::processSection);
        demux.registerSectionChannel(0x0012, this::processSection);
        demux.registerSectionChannel(0x0014, this::processSection);
    }

    private void processTransportPacket(TSDemuxPayload payload)
    {
        pkt.attach(payload.getEncoding());
        if (pkt.containsTransportError())
            return;

        int pid = payload.getStreamPID();

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

        // Scrambling Control
        if (pid == 0x0000 && pkt.isScrambled() && SPRFs[pid] == 0)
        {
            databaseService.addTR290Event(LocalDateTime.now(),
                                          TR290ErrorTypes.PAT_ERROR_2,
                                          "PID=0的TS包加扰指示不等于0",
                                          payload.getStartPacketCounter(),
                                          payload.getStreamPID());
            SPRFs[pid] = 1;
        }

        if (pmtChannels[pid] != null && pkt.isScrambled() && SPRFs[pid] == 0)
        {
            databaseService.addTR290Event(LocalDateTime.now(),
                                          TR290ErrorTypes.PMT_ERROR_2,
                                          "携带PMT的TS包加扰指示不等于0",
                                          payload.getStartPacketCounter(),
                                          payload.getStreamPID());
            SPRFs[pid] = 1;
        }
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
            if (pat.isAttachable(payload.getEncoding()))
                processPAT(payload);

            if (tableId != 0x00)
            {
                databaseService.addTR290Event(LocalDateTime.now(),
                                              TR290ErrorTypes.PAT_ERROR_2,
                                              String.format("TableID不为0的段出现在PID=0的流里（table_id = %02x）",
                                                            tableId),
                                              payload.getStartPacketCounter(),
                                              payload.getStreamPID());
            }
        }

        if (pid == 0x0001)
        {
            if (cat.isAttachable(payload.getEncoding()))
                processCAT(payload);

            if (tableId != 0x01)
            {
                databaseService.addTR290Event(LocalDateTime.now(),
                                              TR290ErrorTypes.CAT_ERROR,
                                              String.format("TableID不为1的段出现在PID=1的流里（table_id = %02x）",
                                                            tableId),
                                              payload.getStartPacketCounter(),
                                              payload.getStreamPID());
            }
        }

        if (pmtChannels[pid] != null && pmt.isAttachable(payload.getEncoding()))
            processPMT(payload);

        if (pid == 0x0010)
        {
            if (nit.isAttachable(payload.getEncoding()))
                processNIT(payload);

            if (tableId != 0x40 && tableId != 0x41 && tableId != 0x72)
            {
                databaseService.addTR290Event(LocalDateTime.now(),
                                              TR290ErrorTypes.NIT_ACTUAL_ERROR,
                                              String.format("NIT或ST以外的表出现在PID=0x0010的流里（table_id = %02x）",
                                                            tableId),
                                              payload.getStartPacketCounter(),
                                              payload.getStreamPID());
            }
        }

        if (pid == 0x0011)
        {
            if (bat.isAttachable(payload.getEncoding()))
                processBAT(payload);

            if (sdt.isAttachable(payload.getEncoding()))
                processSDT(payload);

            if (tableId != 0x42 && tableId != 0x46 && tableId != 0x4A && tableId != 0x72)
            {
                databaseService.addTR290Event(LocalDateTime.now(),
                                              TR290ErrorTypes.SDT_ACTUAL_ERROR,
                                              String.format("BAT或SDT或ST以外的表出现在PID=0x0011的流里（table_id = %02x）",
                                                            tableId),
                                              payload.getStartPacketCounter(),
                                              payload.getStreamPID());
            }
        }

        if (pid == 0x0012)
        {
            if (eit.isAttachable(payload.getEncoding()))
                processEIT(payload);

            if ((tableId < 0x4E || tableId > 0x6F) && tableId != 0x72)
            {
                databaseService.addTR290Event(LocalDateTime.now(),
                                              TR290ErrorTypes.EIT_ACTUAL_ERROR,
                                              String.format("EIT或ST以外的表出现在PID=0x0012的流里（table_id = %02x）",
                                                            tableId),
                                              payload.getStartPacketCounter(),
                                              payload.getStreamPID());
            }
        }

        if (pid == 0x0013)
        {
            if (rst.isAttachable(payload.getEncoding()))
                processRST(payload);

            if (tableId != 0x71 && tableId != 0x72)
            {
                databaseService.addTR290Event(LocalDateTime.now(),
                                              TR290ErrorTypes.RST_ERROR,
                                              String.format("RST或ST以外的表出现在PID=0x0013的流里（table_id = %02x）",
                                                            tableId),
                                              payload.getStartPacketCounter(),
                                              payload.getStreamPID());
            }
        }

        if (pid == 0x0014)
        {
            if (tdt.isAttachable(payload.getEncoding()))
                processTDT(payload);

            if (tableId != 0x70 && tableId != 0x72 && tableId != 0x73)
            {
                databaseService.addTR290Event(LocalDateTime.now(),
                                              TR290ErrorTypes.TDT_ERROR,
                                              String.format("TDT或TOT或ST以外的表出现在PID=0x0014的流里（table_id = %02x）",
                                                            tableId),
                                              payload.getStartPacketCounter(),
                                              payload.getStreamPID());
            }
        }
    }

    private void processPAT(TSDemuxPayload payload)
    {
        pat.attach(payload.getEncoding());
        int secnum = pat.getSectionNumber();
        long checksum = pat.getChecksum();

        String uid = String.format("pat.%d", secnum);
        Context ctx = tableContexts.get(uid);
        if (ctx != null && ctx.checksum == checksum)
        {
            // 严格的相等。
            if (!ctx.checksumCorrect)
                databaseService.addTR290Event(LocalDateTime.now(),
                                              TR290ErrorTypes.CRC_ERROR,
                                              "PAT表CRC32错误",
                                              payload.getStartPacketCounter(),
                                              payload.getStreamPID());
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
                    databaseService.setStreamMarked(pid, false);
                    pmtChannels[pid] = null;
                }
            }
            programNumbers.clear();
        }

        // 更新上下文
        ctx = updateTableContext(uid, checksum);

        pat.forEachProgramAssociation((number, pmtpid) -> {
            TSDemux.Channel channel = demux.registerSectionChannel(pmtpid, this::processPMT);
            pmtChannels[pmtpid] = channel;
            programNumbers.add(number);
            databaseService.setStreamMarked(pmtpid, true);
        });

        if (!pat.isChecksumCorrect())
        {
            ctx.checksumCorrect = false;
            databaseService.addTR290Event(LocalDateTime.now(),
                                          TR290ErrorTypes.CRC_ERROR,
                                          "PAT表CRC32错误",
                                          payload.getStartPacketCounter(),
                                          payload.getStreamPID());
        }
    }

    private void processCAT(TSDemuxPayload payload)
    {
        cat.attach(payload.getEncoding());
        int secnum = cat.getSectionNumber();
        long checksum = cat.getChecksum();

        String uid = String.format("cat.%d", secnum);
        Context ctx = tableContexts.get(uid);
        if (ctx != null && ctx.checksum == checksum)
        {
            // 严格的相等。
            if (!ctx.checksumCorrect)
                databaseService.addTR290Event(LocalDateTime.now(),
                                              TR290ErrorTypes.CRC_ERROR,
                                              "CAT表CRC32错误",
                                              payload.getStartPacketCounter(),
                                              payload.getStreamPID());
            return;
        }

        if (ctx != null)
            removeTableContexts("cat");

        ctx = updateTableContext(uid, checksum);

        descloop.attach(cat.getDescriptorLoop());
        descloop.forEach(cad::isAttachable, descriptor -> {
            cad.attach(descriptor);
            databaseService.setStreamMarked(cad.getConditionalAccessStreamPID(), true);
        });

        if (!cat.isChecksumCorrect())
        {
            ctx.checksumCorrect = false;
            databaseService.addTR290Event(LocalDateTime.now(),
                                          TR290ErrorTypes.CRC_ERROR,
                                          "CAT表CRC32错误",
                                          payload.getStartPacketCounter(),
                                          payload.getStreamPID());
        }

        databaseService.setStreamMarked(0x0001, true);
    }

    private void processPMT(TSDemuxPayload payload)
    {
        pmt.attach(payload.getEncoding());
        int number = pmt.getProgramNumber();
        long checksum = pmt.getChecksum();

        if (!programNumbers.contains(number))
            return; // 非注册节目的PMT，不处理。

        String uid = String.format("pmt.%d", number);
        Context ctx = tableContexts.get(uid);
        if (ctx != null && ctx.checksum == checksum)
        {
            // 严格的相等。
            if (!ctx.checksumCorrect)
                databaseService.addTR290Event(LocalDateTime.now(),
                                              TR290ErrorTypes.CRC_ERROR,
                                              "PMT表CRC32错误",
                                              payload.getStartPacketCounter(),
                                              payload.getStreamPID());
            return;
        }

        // 更新上下文
        ctx = updateTableContext(uid, checksum);

        descloop.attach(pmt.getDescriptorLoop());
        descloop.forEach(cad::isAttachable, encoding -> {
            cad.attach(encoding);
            databaseService.setStreamMarked(cad.getConditionalAccessStreamPID(), true);
        });

        pmt.forEachProgramElement(encoding -> {
            element.attach(encoding);
            databaseService.setStreamMarked(element.getElementaryPID(), true);

            descloop.attach(element.getDescriptorLoop());
            descloop.forEach(cad::isAttachable, descriptor -> {
                cad.attach(descriptor);
                databaseService.setStreamMarked(cad.getConditionalAccessStreamPID(), true);
            });
        });

        if (!pmt.isChecksumCorrect())
        {
            ctx.checksumCorrect = false;
            databaseService.addTR290Event(LocalDateTime.now(),
                                          TR290ErrorTypes.CRC_ERROR,
                                          "PMT表CRC32错误",
                                          payload.getStartPacketCounter(),
                                          payload.getStreamPID());
        }
    }

    private void processNIT(TSDemuxPayload payload)
    {
        nit.attach(payload.getEncoding());
        int tableId = nit.getTableID();
        int netId = nit.getNetworkID();
        int secnum = nit.getSectionNumber();
        long checksum = nit.getChecksum();

        if (tableId == 0x40)
        {
            long currOccurPosition = payload.getStartPacketCounter();
            long currOccurTime = System.currentTimeMillis();

            long interval = computeInterval(lastNITActOccurPosition, currOccurPosition,
                                            lastNITActOccurTime, currOccurTime);
            if (interval < 25)
            {
                databaseService.addTR290Event(LocalDateTime.now(),
                                              TR290ErrorTypes.NIT_ACTUAL_ERROR,
                                              String.format("NIT_actual间隔小于25ms（实际：%dms）", interval),
                                              payload.getStartPacketCounter(),
                                              payload.getStreamPID());
            }

            lastNITActOccurPosition = currOccurPosition;
            lastNITActOccurTime = currOccurTime;
        }

        String uid = String.format("nit.%d.%d.%d", tableId, netId, secnum);
        Context ctx = tableContexts.get(uid);
        if (ctx != null && ctx.checksum == checksum)
        {
            if (!ctx.checksumCorrect)
                databaseService.addTR290Event(LocalDateTime.now(),
                                              TR290ErrorTypes.CRC_ERROR,
                                              "NIT表CRC32错误",
                                              payload.getStartPacketCounter(),
                                              payload.getStreamPID());
            return;
        }

        if (ctx != null)
            removeTableContexts(String.format("nit.%d.%d", tableId, netId));

        // 更新上下文
        ctx = updateTableContext(uid, checksum);

        if (!nit.isChecksumCorrect())
        {
            ctx.checksumCorrect = false;
            databaseService.addTR290Event(LocalDateTime.now(),
                                          TR290ErrorTypes.CRC_ERROR,
                                          "NIT表CRC32错误",
                                          payload.getStartPacketCounter(),
                                          payload.getStreamPID());
        }
    }

    private void processBAT(TSDemuxPayload payload)
    {
        bat.attach(payload.getEncoding());
        int bouquetId = bat.getBouquetID();
        int secnum = bat.getSectionNumber();
        long checksum = bat.getChecksum();

        String uid = String.format("bat.%d.%d", bouquetId, secnum);
        Context ctx = tableContexts.get(uid);
        if (ctx != null && ctx.checksum == checksum)
        {
            if (!ctx.checksumCorrect)
                databaseService.addTR290Event(LocalDateTime.now(),
                                              TR290ErrorTypes.CRC_ERROR,
                                              "BAT表CRC32错误",
                                              payload.getStartPacketCounter(),
                                              payload.getStreamPID());
            return;
        }

        if (ctx != null)
            removeTableContexts(String.format("bat.%d", bouquetId));

        // 更新上下文
        ctx = updateTableContext(uid, checksum);

        if (!bat.isChecksumCorrect())
        {
            ctx.checksumCorrect = false;
            databaseService.addTR290Event(LocalDateTime.now(),
                                          TR290ErrorTypes.CRC_ERROR,
                                          "BAT表CRC32错误",
                                          payload.getStartPacketCounter(),
                                          payload.getStreamPID());
        }
    }

    private void processSDT(TSDemuxPayload payload)
    {
        sdt.attach(payload.getEncoding());
        int tableId = sdt.getTableID();
        int tsid = sdt.getTransportStreamID();
        int onid = sdt.getOriginalNetworkID();
        int secnum = sdt.getSectionNumber();
        long checksum = sdt.getChecksum();

        if (tableId == 0x42)
        {
            long currOccurPosition = payload.getStartPacketCounter();
            long currOccurTime = System.currentTimeMillis();

            long interval = computeInterval(lastSDTActOccurPosition, currOccurPosition,
                                            lastSDTActOccurTime, currOccurTime);
            if (interval < 25)
            {
                databaseService.addTR290Event(LocalDateTime.now(),
                                              TR290ErrorTypes.SDT_ACTUAL_ERROR,
                                              String.format("SDT_actual间隔小于25ms（实际：%dms）", interval),
                                              payload.getStartPacketCounter(),
                                              payload.getStreamPID());
            }

            lastSDTActOccurPosition = currOccurPosition;
            lastSDTActOccurTime = currOccurTime;
        }

        String uid = String.format("sdt.%d.%d.%d.%d", tableId, onid, tsid, secnum);
        Context ctx = tableContexts.get(uid);
        if (ctx != null && ctx.checksum == checksum)
        {
            if (!ctx.checksumCorrect)
                databaseService.addTR290Event(LocalDateTime.now(),
                                              TR290ErrorTypes.CRC_ERROR,
                                              "SDT表CRC32错误",
                                              payload.getStartPacketCounter(),
                                              payload.getStreamPID());
            return;
        }

        if (ctx != null)
            removeTableContexts(String.format("sdt.%d.%d.%d", tableId, onid, tsid));

        ctx = updateTableContext(uid, checksum);

        if (!sdt.isChecksumCorrect())
        {
            ctx.checksumCorrect = false;
            databaseService.addTR290Event(LocalDateTime.now(),
                                          TR290ErrorTypes.CRC_ERROR,
                                          "SDT表CRC32错误",
                                          payload.getStartPacketCounter(),
                                          payload.getStreamPID());
        }
    }

    private void processEIT(TSDemuxPayload payload)
    {
        eit.attach(payload.getEncoding());
        int tableId = eit.getTableID();
        int sid = eit.getServiceID();
        int tsid = eit.getTransportStreamID();
        int onid = eit.getOriginalNetworkID();
        int secnum = eit.getSectionNumber();
        long checksum = eit.getChecksum();

        if (tableId == 0x4E)
        {
            long currOccurPosition = payload.getStartPacketCounter();
            long currOccurTime = System.currentTimeMillis();

            long interval = computeInterval(lastEITActOccurPosition, currOccurPosition,
                                            lastEITActOccurTime, currOccurTime);
            if (interval < 25)
            {
                databaseService.addTR290Event(LocalDateTime.now(),
                                              TR290ErrorTypes.EIT_ACTUAL_ERROR,
                                              String.format("EIT_actual间隔小于25ms（实际：%dms）", interval),
                                              payload.getStartPacketCounter(),
                                              payload.getStreamPID());
            }

            lastEITActOccurPosition = currOccurPosition;
            lastEITActOccurTime = currOccurTime;
        }

        String uid = String.format("eit.%d.%d.%d.%d.%d", tableId, onid, tsid, sid, secnum);
        Context ctx = tableContexts.get(uid);
        if (ctx != null && ctx.checksum == checksum)
        {
            if (!ctx.checksumCorrect)
                databaseService.addTR290Event(LocalDateTime.now(),
                                              TR290ErrorTypes.CRC_ERROR,
                                              "EIT表CRC32错误",
                                              payload.getStartPacketCounter(),
                                              payload.getStreamPID());
            return;
        }

        if (ctx != null)
            removeTableContexts(String.format("eit.%d.%d.%d.%d", tableId, onid, tsid, sid));

        ctx = updateTableContext(uid, checksum);

        if (!eit.isChecksumCorrect())
        {
            ctx.checksumCorrect = false;
            databaseService.addTR290Event(LocalDateTime.now(),
                                          TR290ErrorTypes.CRC_ERROR,
                                          "EIT表CRC32错误",
                                          payload.getStartPacketCounter(),
                                          payload.getStreamPID());
        }
    }

    private void processRST(TSDemuxPayload payload)
    {
        long currOccurPosition = payload.getStartPacketCounter();
        long currOccurTime = System.currentTimeMillis();

        long interval = computeInterval(lastRSTOccurPosition, currOccurPosition,
                                        lastRSTOccurTime, currOccurTime);
        if (interval < 25)
        {
            databaseService.addTR290Event(LocalDateTime.now(),
                                          TR290ErrorTypes.RST_ERROR,
                                          String.format("RST间隔小于25ms（实际：%dms）", interval),
                                          payload.getStartPacketCounter(),
                                          payload.getStreamPID());
        }

        lastRSTOccurPosition = currOccurPosition;
        lastRSTOccurTime = currOccurTime;
    }

    private void processTDT(TSDemuxPayload payload)
    {
        long currOccurPosition = payload.getStartPacketCounter();
        long currOccurTime = System.currentTimeMillis();

        long interval = computeInterval(lastTDTOccurPosition, currOccurPosition,
                                        lastTDTOccurTime, currOccurTime);

        if (interval < 25)
        {
            databaseService.addTR290Event(LocalDateTime.now(),
                                          TR290ErrorTypes.TDT_ERROR,
                                          String.format("TDT间隔小于25ms（实际：%dms）", interval),
                                          payload.getStartPacketCounter(),
                                          payload.getStreamPID());
        }

        lastTDTOccurPosition = currOccurPosition;
        lastTDTOccurTime = currOccurTime;
    }

    private long computeInterval(long lastOccurPosition, long currOccurPosition,
                                 long lastOccurTime, long currOccurTime)
    {
        if (avgBitrate > 0)
           return (currOccurPosition - lastOccurPosition) * 188 * 8 * 1000 / avgBitrate;

        return (lastOccurTime > 0) ? currOccurTime - lastOccurTime : 25;
    }

    private void removeTableContexts(String keyPrefix)
    {
        List<String> tableKeys = new ArrayList<>(tableContexts.keySet());
        for (String key : tableKeys)
        {
            if (key.startsWith(keyPrefix))
                tableContexts.remove(key);
        }
    }

    private Context updateTableContext(String key, long checksum)
    {
        Context ctx = new Context();
        ctx.checksum = checksum;
        ctx.checksumCorrect = true;
        tableContexts.put(key, ctx);
        return ctx;
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
