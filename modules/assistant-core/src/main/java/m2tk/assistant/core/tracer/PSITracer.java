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

import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.core.M2TKDatabase;
import m2tk.assistant.core.domain.*;
import m2tk.assistant.core.presets.CASystems;
import m2tk.assistant.core.presets.StreamTypes;
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
    private final PATSectionDecoder pat;
    private final CATSectionDecoder cat;
    private final PMTSectionDecoder pmt;
    private final CADescriptorDecoder cad;
    private final DescriptorLoopDecoder descloop;
    private final ProgramElementDecoder element;
    private final Map<Integer, Integer> pmtVersions;
    private final Map<Integer, ProgramContext> programs;

    private M2TKDatabase databaseService;
    private long transactionId;
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
        databaseService = database;
        transactionId = source.getTransactionId();

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
                databaseService.updateElementaryStreamUsage(transactionId,
                                                            context.program.getPmtPid(),
                                                            StreamTypes.CATEGORY_USER_PRIVATE,
                                                            "未知流");
            }
            pmtVersions.clear();
            programs.clear();
            databaseService.clearMPEGPrograms(transactionId);
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
            MPEGProgram program = new MPEGProgram();
            program.setRef(-1);
            program.setTransactionId(transactionId);
            program.setTransportStreamId(tsid);
            program.setProgramNumber(number);
            program.setPmtPid(pmtpid);
            program.setName("未知节目");
            databaseService.addMPEGProgram(program);
            context.program = program;
            context.program.setPmtPid(pmtpid);
            context.channel = demux.registerSectionChannel(pmtpid, this::processPMT);
            programs.put(number, context);
            pmtVersions.put(pmtpid, -1);
        });

        StreamSource source = databaseService.getStreamSource(transactionId);
        source.setTransportStreamId(tsid);
        databaseService.updateStreamSource(source);

        patSections[secnum] = version;
        databaseService.updateElementaryStreamUsage(transactionId, payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "PAT");

        recordPSISection("PAT", payload);
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
            int emm = cad.getConditionalAccessStreamPID();
            int systemId = cad.getConditionalAccessSystemID();
            String vendor = CASystems.vendor(systemId);
            String description = String.format("EMM，系统号：%04X", systemId);
            if (!vendor.isEmpty())
                description += "，提供商：" + vendor;
            databaseService.updateElementaryStreamUsage(transactionId, emm, StreamTypes.CATEGORY_DATA, description);
        });

        catSections[secnum] = version;
        databaseService.updateElementaryStreamUsage(transactionId, payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "CAT");

        recordPSISection("CAT", payload);
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

        MPEGProgram program = context.program;
        program.setPmtVersion(version);
        program.setPmtPid(pmtpid);
        program.setPcrPid(pmt.getProgramClockReferencePID());

        descloop.attach(pmt.getDescriptorLoop());
        descloop.forEach(cad::isAttachable, encoding -> {
            program.setScrambled(false);
            cad.attach(encoding);
            int ecm = cad.getConditionalAccessStreamPID();
            int systemId = cad.getConditionalAccessSystemID();
            byte[] privateData = cad.getPrivateDataBytes();
            String vendor = CASystems.vendor(systemId);
            String description = String.format("ECM（节目号：%d），系统号：%04X", program.getProgramNumber(), systemId);
            if (!vendor.isEmpty())
                description += "，提供商：" + vendor;
            databaseService.updateElementaryStreamUsage(transactionId, ecm, StreamTypes.CATEGORY_DATA, description);

            recordECMStream(systemId, ecm, privateData, program.getProgramNumber(), 8191);
        });

        pmt.forEachProgramElement(encoding -> {
            element.attach(encoding);
            int esPid = element.getElementaryPID();
            int esType = element.getStreamType();
            databaseService.updateElementaryStreamUsage(transactionId, esPid, StreamTypes.category(esType),
                                                        StreamTypes.description(esType) +
                                                        String.format("（节目号：%d）", program.getProgramNumber()));

            ProgramStreamMapping mapping = new ProgramStreamMapping();
            mapping.setRef(-1);
            mapping.setTransactionId(transactionId);
            mapping.setProgramNumber(program.getProgramNumber());
            mapping.setElementaryStreamPid(esPid);
            mapping.setElementaryStreamType(esType);
            mapping.setElementaryStreamCategory(StreamTypes.category(esType));
            mapping.setElementaryStreamDescription(StreamTypes.description(esType));
            databaseService.addProgramStreamMapping(mapping);

            descloop.attach(element.getDescriptorLoop());
            descloop.forEach(cad::isAttachable, descriptor -> {
                program.setScrambled(false);
                cad.attach(descriptor);
                int ecm = cad.getConditionalAccessStreamPID();
                int systemId = cad.getConditionalAccessSystemID();
                byte[] privateData = cad.getPrivateDataBytes();
                String vendor = CASystems.vendor(systemId);
                String description = String.format("ECM（节目号：%d，目标ES：0x%X），系统号：%04X",
                                                   program.getProgramNumber(),
                                                   esPid,
                                                   systemId);
                if (!vendor.isEmpty())
                    description += "，提供商：" + vendor;
                databaseService.updateElementaryStreamUsage(transactionId, ecm, StreamTypes.CATEGORY_DATA, description);

                recordECMStream(systemId, ecm, privateData, program.getProgramNumber(), esPid);
            });
        });

        databaseService.updateMPEGProgram(program);

        pmtVersions.put(pmtpid, version);
        databaseService.updateElementaryStreamUsage(transactionId, pmtpid,
                                                    StreamTypes.CATEGORY_DATA,
                                                    String.format("PMT（节目号：%d）", program.getProgramNumber()));

        recordPSISection("PMT", payload);
    }

    private void recordPSISection(String type, TSDemuxPayload payload)
    {
        PrivateSection section = new PrivateSection();
        section.setRef(-1);
        section.setTransactionId(transactionId);
        section.setTag(type);
        section.setPid(payload.getStreamPID());
        section.setPosition(payload.getFinishPacketCounter());
        section.setEncoding(payload.getEncoding().getBytes());
        databaseService.addPrivateSection(section);
    }

    private void recordECMStream(int systemId, int pid, byte[] privateData, int programNumber, int elementaryStreamPid)
    {
        CASystemStream stream = new CASystemStream();
        stream.setRef(-1);
        stream.setTransactionId(transactionId);
        stream.setSystemId(systemId);
        stream.setStreamPid(pid);
        stream.setStreamType(CASystemStream.TYPE_ECM);
        stream.setStreamPrivateData(privateData);
        stream.setProgramNumber(programNumber);
        stream.setElementaryStreamPid(elementaryStreamPid);
        databaseService.addCASystemStream(stream);
    }
}
