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
import m2tk.assistant.api.domain.CASystemStream;
import m2tk.assistant.api.domain.MPEGProgram;
import m2tk.assistant.api.domain.StreamSource;
import m2tk.assistant.api.presets.CASystems;
import m2tk.assistant.api.presets.StreamTypes;
import m2tk.mpeg2.decoder.DescriptorLoopDecoder;
import m2tk.mpeg2.decoder.descriptor.CADescriptorDecoder;
import m2tk.mpeg2.decoder.element.ProgramElementDecoder;
import m2tk.mpeg2.decoder.section.CATSectionDecoder;
import m2tk.mpeg2.decoder.section.PATSectionDecoder;
import m2tk.mpeg2.decoder.section.PMTSectionDecoder;
import m2tk.multiplex.TSDemux;
import m2tk.multiplex.TSDemuxPayload;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Extension
public class PSITracer implements Tracer
{
    private static final Logger log = LoggerFactory.getLogger(PSITracer.class);

    private final PATSectionDecoder pat;
    private final CATSectionDecoder cat;
    private final PMTSectionDecoder pmt;
    private final CADescriptorDecoder cad;
    private final DescriptorLoopDecoder descloop;
    private final ProgramElementDecoder element;
    private final Map<Integer, Integer> pmtVersions;
    private final Map<Integer, ProgramContext> programs;

    private M2TKDatabase databaseService;
    private int sourceId;
    private int[] patSections;
    private int[] catSections;
    private int tsid;

    static class ProgramContext
    {
        MPEGProgram program;
        TSDemux.Channel channel;
    }

    public PSITracer()
    {
        pat = new PATSectionDecoder();
        cat = new CATSectionDecoder();
        pmt = new PMTSectionDecoder();
        cad = new CADescriptorDecoder();
        descloop = new DescriptorLoopDecoder();
        element = new ProgramElementDecoder();
        programs = new HashMap<>();
        pmtVersions = new HashMap<>();

        patSections = new int[0];
        catSections = new int[0];
        tsid = -1;
    }

    @Override
    public void configure(StreamSource source, TSDemux demux, M2TKDatabase database)
    {
        sourceId = source.getId();
        databaseService = database;

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
                databaseService.updateElementaryStreamUsage(context.program.getPmtPid(),
                                                            "",
                                                            "");
            }
            pmtVersions.clear();
            programs.clear();
            databaseService.clearMPEGPrograms();
        }

        tsid = pat.getTransportStreamID();
        pat.forEachProgramAssociation((number, pmtpid) -> {
            log.debug("[PAT] Program {}, PMT PID: {}", number, pmtpid);

            ProgramContext context = new ProgramContext();
            context.program = databaseService.addMPEGProgram(number, tsid, pmtpid);
            context.channel = demux.registerSectionChannel(pmtpid, this::processPMT);
            programs.put(number, context);
            pmtVersions.put(pmtpid, -1);
        });

        databaseService.updateStreamSourceTransportId(sourceId, tsid);

        patSections[secnum] = version;
        databaseService.updateElementaryStreamUsage(payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "PAT");
        databaseService.addPrivateSection("PAT",
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
        }

        descloop.attach(cat.getDescriptorLoop());
        descloop.forEach(cad::isAttachable, descriptor -> {
            cad.attach(descriptor);
            int emmpid = cad.getConditionalAccessStreamPID();
            int systemId = cad.getConditionalAccessSystemID();
            byte[] privateData = cad.getPrivateDataBytes();
            String vendor = CASystems.vendor(systemId);
            String description = String.format("EMM，系统号：%04X", systemId);
            if (!vendor.isEmpty())
                description += "，提供商：" + vendor;
            databaseService.updateElementaryStreamUsage(emmpid, StreamTypes.CATEGORY_DATA, description);
            databaseService.addCASystemStream(emmpid, CASystemStream.TYPE_EMM,
                                              systemId, privateData,
                                              -1, -1);
        });

        catSections[secnum] = version;
        databaseService.updateElementaryStreamUsage(payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "CAT");
        databaseService.addPrivateSection("CAT",
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
            log.debug("[PID {}] 收到了节目号为 {} 的PMT数据，但是该节目未在PAT中定义。丢弃。",
                      payload.getStreamPID(),
                      pmt.getProgramNumber());
            return;
        }

        MPEGProgram program = context.program;
        program.setPmtVersion(version);
        program.setPmtPid(pmtpid);
        program.setPcrPid(pmt.getProgramClockReferencePID());

        descloop.attach(pmt.getDescriptorLoop());
        descloop.forEach(cad::isAttachable, encoding -> {
            program.setFreeAccess(false);
            cad.attach(encoding);
            int ecmpid = cad.getConditionalAccessStreamPID();
            int systemId = cad.getConditionalAccessSystemID();
            byte[] privateData = cad.getPrivateDataBytes();
            String vendor = CASystems.vendor(systemId);
            String description = String.format("ECM（节目号：%d），系统号：%04X", program.getProgramNumber(), systemId);
            if (!vendor.isEmpty())
                description += "，提供商：" + vendor;
            databaseService.updateElementaryStreamUsage(ecmpid, StreamTypes.CATEGORY_DATA, description);
            databaseService.addCASystemStream(ecmpid, CASystemStream.TYPE_ECM,
                                              systemId, privateData,
                                              program.getId(), -1);
        });

        pmt.forEachProgramElement(encoding -> {
            element.attach(encoding);
            int esPid = element.getElementaryPID();
            int esType = element.getStreamType();
            databaseService.updateElementaryStreamUsage(esPid, StreamTypes.category(esType),
                                                        StreamTypes.description(esType) +
                                                        String.format("（节目号：%d）", program.getProgramNumber()));
            databaseService.addProgramElementaryMapping(program.getId(), esPid, esType);

            descloop.attach(element.getDescriptorLoop());
            descloop.forEach(cad::isAttachable, descriptor -> {
                program.setFreeAccess(false);
                cad.attach(descriptor);
                int ecmpid = cad.getConditionalAccessStreamPID();
                int systemId = cad.getConditionalAccessSystemID();
                byte[] privateData = cad.getPrivateDataBytes();
                String vendor = CASystems.vendor(systemId);
                String description = String.format("ECM（节目号：%d，目标ES：0x%X），系统号：%04X",
                                                   program.getProgramNumber(),
                                                   esPid,
                                                   systemId);
                if (!vendor.isEmpty())
                    description += "，提供商：" + vendor;
                databaseService.updateElementaryStreamUsage(ecmpid, StreamTypes.CATEGORY_DATA, description);
                databaseService.addCASystemStream(ecmpid, CASystemStream.TYPE_ECM,
                                                  systemId, privateData,
                                                  program.getId(), esPid);
            });
        });

        databaseService.updateMPEGProgram(program.getId(),
                                          program.getPcrPid(),
                                          program.getPmtVersion(),
                                          program.isFreeAccess());

        pmtVersions.put(pmtpid, version);
        databaseService.updateElementaryStreamUsage(pmtpid,
                                                    StreamTypes.CATEGORY_DATA,
                                                    String.format("PMT（节目号：%d）", program.getProgramNumber()));

        databaseService.addPrivateSection("PMT",
                                          payload.getStreamPID(),
                                          payload.getFinishPacketCounter(),
                                          payload.getEncoding().getBytes());
    }
}
