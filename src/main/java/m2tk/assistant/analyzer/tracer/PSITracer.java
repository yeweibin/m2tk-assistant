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

import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.Global;
import m2tk.assistant.analyzer.presets.CASystems;
import m2tk.assistant.analyzer.presets.StreamTypes;
import m2tk.assistant.dbi.DatabaseService;
import m2tk.assistant.dbi.entity.ProgramEntity;
import m2tk.assistant.dbi.entity.SourceEntity;
import m2tk.mpeg2.decoder.DescriptorLoopDecoder;
import m2tk.mpeg2.decoder.descriptor.CADescriptorDecoder;
import m2tk.mpeg2.decoder.element.ProgramElementDecoder;
import m2tk.mpeg2.decoder.section.CATSectionDecoder;
import m2tk.mpeg2.decoder.section.PATSectionDecoder;
import m2tk.mpeg2.decoder.section.PMTSectionDecoder;
import m2tk.multiplex.TSDemux;
import m2tk.multiplex.TSDemuxPayload;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class PSITracer implements Tracer
{
    private final DatabaseService databaseService;
    private final long transactionId;

    private final PATSectionDecoder pat;
    private final CATSectionDecoder cat;
    private final PMTSectionDecoder pmt;
    private final CADescriptorDecoder cad;
    private final DescriptorLoopDecoder descloop;
    private final ProgramElementDecoder element;
    private final Map<Integer, Integer> pmtVersions;
    private final Map<Integer, ProgramContext> programs;

    private final int[] emmCounts;

    private int[] patSections;
    private int[] catSections;
    private int tsid;

    static class ProgramContext
    {
        ProgramEntity program;
        TSDemux.Channel channel;
    }

    public PSITracer(DatabaseService service, long transaction)
    {
        databaseService = service;
        transactionId = transaction;

        pat = new PATSectionDecoder();
        cat = new CATSectionDecoder();
        pmt = new PMTSectionDecoder();
        cad = new CADescriptorDecoder();
        descloop = new DescriptorLoopDecoder();
        element = new ProgramElementDecoder();
        programs = new HashMap<>();
        pmtVersions = new HashMap<>();

        emmCounts = new int[8192];
        patSections = new int[0];
        catSections = new int[0];
        tsid = -1;
    }

    @Override
    public void configureDemux(TSDemux demux)
    {
        demux.registerSectionChannel(0x0000, this::processPAT);
        demux.registerSectionChannel(0x0001, this::processCAT);
    }

    private void processPAT(TSDemuxPayload payload)
    {
        if (payload.getType() != TSDemuxPayload.Type.SECTION ||
            payload.getStreamPID() != 0x0000 ||
            !pat.isAttachable(payload.getEncoding()))
            return;

        pat.attach(payload.getEncoding());
        if (!pat.isChecksumCorrect())
        {
            log.warn("PAT校验错误。");
            return;
        }

        int version = pat.getVersionNumber();
        int secnum = pat.getSectionNumber();
        int total = pat.getLastSectionNumber() + 1;
        if (total == patSections.length && patSections[secnum] == version)
            return; // 已经处理过了。

        TSDemux demux = payload.getChannel().getHost();

        if (total != patSections.length)
        {
            patSections = new int[total];
            Arrays.fill(patSections, -1);

            for (ProgramContext context : programs.values())
            {
                demux.closeChannel(context.channel);
                databaseService.updateStreamUsage(transactionId,
                                                  context.program.getPmtPid(),
                                                  StreamTypes.CATEGORY_USER_PRIVATE,
                                                  "未知流");
            }
            pmtVersions.clear();
            programs.clear();
            databaseService.clearPrograms(transactionId);
        }

        tsid = pat.getTransportStreamID();
        pat.forEachProgramAssociation((number, pmtpid) -> {
            if (log.isDebugEnabled())
            {
                log.debug("[PAT] Program {}, PMT PID: {}",
                          number,
                          String.format("0x%04X", pmtpid));
            }

            ProgramContext context = new ProgramContext();
            context.program = databaseService.addProgram(transactionId, tsid, number, pmtpid);
            context.program.setPmtPid(pmtpid);
            context.channel = demux.registerSectionChannel(pmtpid, this::processPMT);
            programs.put(number, context);
            pmtVersions.put(pmtpid, -1);
        });

        SourceEntity source = databaseService.getSource(transactionId);
        source.setTransportStreamId(tsid);
        databaseService.updateSourceTransportId(source);

        patSections[secnum] = version;
        databaseService.updateStreamUsage(transactionId, payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "PAT");
        databaseService.addSection(transactionId, "PAT",
                                   payload.getStreamPID(),
                                   payload.getFinishPacketCounter(),
                                   payload.getEncoding().getBytes());
    }

    private void processCAT(TSDemuxPayload payload)
    {
        if (payload.getType() != TSDemuxPayload.Type.SECTION ||
            payload.getStreamPID() != 0x0001 ||
            !cat.isAttachable(payload.getEncoding()))
            return;

        cat.attach(payload.getEncoding());
        if (!cat.isChecksumCorrect())
        {
            log.warn("CAT校验错误。");
            return;
        }

        int version = cat.getVersionNumber();
        int secnum = cat.getSectionNumber();
        int total = cat.getLastSectionNumber() + 1;
        if (total == catSections.length && catSections[secnum] == version)
            return; // 已经处理过了。

        if (total != catSections.length)
        {
            catSections = new int[total];
            Arrays.fill(catSections, -1);
            Arrays.fill(emmCounts, 0);
        }

        TSDemux demux = payload.getChannel().getHost();
        descloop.attach(cat.getDescriptorLoop());
        descloop.forEach(cad::isAttachable, descriptor -> {
            cad.attach(descriptor);
            int emm = cad.getConditionalAccessStreamPID();
            int casid = cad.getConditionalAccessSystemID();
            byte[] privateData = cad.getPrivateDataBytes();
            String vendor = CASystems.vendor(casid);
            String description = String.format("EMM，系统号：%04X", casid);
            if (!vendor.isEmpty())
                description += "，提供商：" + vendor;
            databaseService.updateStreamUsage(transactionId, emm, StreamTypes.CATEGORY_DATA, description);
            databaseService.addEMMStream(transactionId, casid, emm, privateData);

            demux.registerSectionChannel(emm, this::processEMM);
        });

        catSections[secnum] = version;
        databaseService.updateStreamUsage(transactionId, payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "CAT");
        databaseService.addSection(transactionId, "CAT",
                                   payload.getStreamPID(),
                                   payload.getFinishPacketCounter(),
                                   payload.getEncoding().getBytes());
    }

    private void processPMT(TSDemuxPayload payload)
    {
        if (payload.getType() != TSDemuxPayload.Type.SECTION ||
            !pmtVersions.containsKey(payload.getStreamPID()) ||
            !pmt.isAttachable(payload.getEncoding()))
            return;

        pmt.attach(payload.getEncoding());
        if (!pat.isChecksumCorrect())
        {
            log.warn("PMT校验错误。");
            return;
        }

        int version = pmt.getVersionNumber();
        int pmtpid = payload.getStreamPID();

        if (pmtVersions.get(pmtpid) == version)
            return;

        ProgramContext context = programs.get(pmt.getProgramNumber());
        if (context == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("[PID {}] 收到了节目{}的PMT，但是该节目未在PAT中描述。丢弃。",
                          String.format("0x%04X", payload.getStreamPID()),
                          pmt.getProgramNumber());
            }
            return;
        }

        ProgramEntity program = context.program;
        program.setPmtVersion(version);
        program.setPmtPid(pmtpid);
        program.setPcrPid(pmt.getProgramClockReferencePID());

        descloop.attach(pmt.getDescriptorLoop());
        descloop.forEach(cad::isAttachable, encoding -> {
            program.setFreeAccess(false);
            cad.attach(encoding);
            int ecm = cad.getConditionalAccessStreamPID();
            int casid = cad.getConditionalAccessSystemID();
            byte[] privateData = cad.getPrivateDataBytes();
            String vendor = CASystems.vendor(casid);
            String description = String.format("ECM（节目号：%d），系统号：%04X", program.getProgramNumber(), casid);
            if (!vendor.isEmpty())
                description += "，提供商：" + vendor;
            databaseService.updateStreamUsage(transactionId, ecm, StreamTypes.CATEGORY_DATA, description);
            databaseService.addECMStream(transactionId, casid, ecm, privateData, program.getProgramNumber(), 8191);
        });

        pmt.forEachProgramElement(encoding -> {
            element.attach(encoding);
            int esPid = element.getElementaryPID();
            int esType = element.getStreamType();
            databaseService.updateStreamUsage(transactionId, esPid, StreamTypes.category(esType),
                                              StreamTypes.description(esType) +
                                              String.format("（节目号：%d）", program.getProgramNumber()));
            databaseService.addProgramStreamMapping(transactionId,
                                                    program.getProgramNumber(),
                                                    esPid,
                                                    esType,
                                                    StreamTypes.category(esType),
                                                    StreamTypes.description(esType));

            descloop.attach(element.getDescriptorLoop());
            descloop.forEach(cad::isAttachable, descriptor -> {
                program.setFreeAccess(false);
                cad.attach(descriptor);
                int ecm = cad.getConditionalAccessStreamPID();
                int casid = cad.getConditionalAccessSystemID();
                byte[] privateData = cad.getPrivateDataBytes();
                String vendor = CASystems.vendor(casid);
                String description = String.format("ECM（节目号：%d，目标ES：0x%X），系统号：%04X",
                                                   program.getProgramNumber(),
                                                   esPid,
                                                   casid);
                if (!vendor.isEmpty())
                    description += "，提供商：" + vendor;
                databaseService.updateStreamUsage(transactionId, ecm, StreamTypes.CATEGORY_DATA, description);
                databaseService.addECMStream(transactionId, casid, ecm, privateData, program.getProgramNumber(), esPid);
            });
        });

        databaseService.updateProgram(program);

        pmtVersions.put(pmtpid, version);
        databaseService.updateStreamUsage(transactionId, pmtpid,
                                          StreamTypes.CATEGORY_DATA,
                                          String.format("PMT（节目号：%d）", program.getProgramNumber()));
        databaseService.addSection(transactionId, "PMT",
                                   payload.getStreamPID(),
                                   payload.getFinishPacketCounter(),
                                   payload.getEncoding().getBytes());
    }

    private void processEMM(TSDemuxPayload payload)
    {
        if (payload.getType() != TSDemuxPayload.Type.SECTION)
            return;

        int tableId = payload.getEncoding().readUINT8(0);
        if (tableId != 0x83 && tableId != 0x84 && tableId != 0x87)
            return;

        int pid = payload.getStreamPID();
        if (emmCounts[pid] >= Global.getPrivateSectionFilteringLimit())
        {
            int drops = Global.getPrivateSectionFilteringLimit() / 10;
            drops = databaseService.removeSections(transactionId, "EMM*", payload.getStreamPID(), drops);
            emmCounts[pid] -= drops;
            log.info("丢弃 {} 个EMM分段记录", drops);
        }

        String tag = "EMM.Personal";
        if (tableId == 0x84)
            tag = "EMM.Global";
        if (tableId == 0x87)
            tag = "EMM.Active";

        databaseService.addSection(transactionId, tag,
                                   payload.getStreamPID(),
                                   payload.getFinishPacketCounter(),
                                   payload.getEncoding().getBytes());
        emmCounts[pid] += 1;
    }
}
