package m2tk.assistant.analyzer;

import m2tk.assistant.analyzer.presets.CASystems;
import m2tk.assistant.analyzer.presets.StreamTypes;
import m2tk.assistant.dbi.DatabaseService;
import m2tk.assistant.dbi.entity.ProgramEntity;
import m2tk.assistant.dbi.entity.SourceEntity;
import m2tk.mpeg2.decoder.DescriptorLoopDecoder;
import m2tk.mpeg2.decoder.ProgramElementDecoder;
import m2tk.mpeg2.decoder.descriptor.CADescriptorDecoder;
import m2tk.mpeg2.decoder.section.CATSectionDecoder;
import m2tk.mpeg2.decoder.section.PATSectionDecoder;
import m2tk.mpeg2.decoder.section.PMTSectionDecoder;
import m2tk.multiplex.TSDemux;
import m2tk.multiplex.TSDemuxPayload;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PSIRecorder
{
    private final DatabaseService databaseService;

    private final PATSectionDecoder pat;
    private final CATSectionDecoder cat;
    private final PMTSectionDecoder pmt;
    private final CADescriptorDecoder cad;
    private final DescriptorLoopDecoder descloop;
    private final ProgramElementDecoder element;
    private final Map<Integer, Integer> pmtVersions;
    private final Map<Integer, ProgramContext> programs;

    private int[] patSections;
    private int[] catSections;
    private int tsid;

    static class ProgramContext
    {
        ProgramEntity program;
        TSDemux.Channel channel;
    }

    public PSIRecorder(DatabaseService service)
    {
        databaseService = service;

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

    public void processPAT(TSDemuxPayload payload)
    {
        if (payload.getType() != TSDemuxPayload.Type.SECTION ||
            payload.getStreamPID() != 0x0000 ||
            !pat.isAttachable(payload.getEncoding()))
            return;

        pat.attach(payload.getEncoding());
        int version = pat.getVersionNumber();
        int secnum = pat.getSectionNumber();
        int total = pat.getLastSectionNumber() + 1;
        if (total == patSections.length && patSections[secnum] == version)
            return; // 已经处理过了。

        databaseService.updateStreamUsage(payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "PAT");

        if (total != patSections.length)
        {
            patSections = new int[total];
            Arrays.fill(patSections, -1);

            TSDemux demuxer = payload.getChannel().getHost();
            for (ProgramContext context : programs.values())
            {
                demuxer.closeChannel(context.channel);
                databaseService.updateStreamUsage(context.program.getPmtPid(), StreamTypes.CATEGORY_USER_PRIVATE, "未知流");
            }
            pmtVersions.clear();
            databaseService.clearPrograms();
        }

        tsid = pat.getTransportStreamID();
        patSections[secnum] = version;

        pat.forEachProgramAssociation((number, pmtpid) -> {
            TSDemux demuxer = payload.getChannel().getHost();

            ProgramContext context = new ProgramContext();
            context.program = databaseService.addProgram(tsid, number, pmtpid);
            context.program.setPmtPid(pmtpid);
            context.channel = demuxer.requestChannel(TSDemuxPayload.Type.SECTION);
            context.channel.setStreamPID(pmtpid);
            context.channel.setPayloadHandler(this::processPMT);
            context.channel.setEnabled(true);
            programs.put(number, context);
            pmtVersions.put(pmtpid, -1);
        });

        SourceEntity source = databaseService.getSource();
        source.setTransportStreamId(tsid);
        databaseService.updateSourceTransportId(source);
    }

    public void processCAT(TSDemuxPayload payload)
    {
        if (payload.getType() != TSDemuxPayload.Type.SECTION ||
            payload.getStreamPID() != 0x0001 ||
            !cat.isAttachable(payload.getEncoding()))
            return;

        cat.attach(payload.getEncoding());
        int version = cat.getVersionNumber();
        int secnum = cat.getSectionNumber();
        int total = cat.getLastSectionNumber() + 1;
        if (total == catSections.length && catSections[secnum] == version)
            return; // 已经处理过了。

        databaseService.updateStreamUsage(payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "CAT");

        if (total != catSections.length)
        {
            catSections = new int[total];
            Arrays.fill(catSections, -1);
        }

        catSections[secnum] = version;
        descloop.attach(cat.getDescriptorLoop());
        descloop.forEach(cad::isAttachable, descriptor -> {
            cad.attach(descriptor);
            int emm = cad.getConditionalAccessStreamPID();
            int casid = cad.getConditionalAccessSystemID();
            String vendor = CASystems.vendor(casid);
            String description = String.format("EMM，系统号：%04X", casid);
            if (!vendor.isEmpty())
                description += "，提供商：" + vendor;
            databaseService.updateStreamUsage(emm, StreamTypes.CATEGORY_DATA, description);
        });
    }

    public void processPMT(TSDemuxPayload payload)
    {
        if (payload.getType() != TSDemuxPayload.Type.SECTION ||
            !pmtVersions.containsKey(payload.getStreamPID()) ||
            !pmt.isAttachable(payload.getEncoding()))
            return;

        pmt.attach(payload.getEncoding());
        int version = pmt.getVersionNumber();
        int pmtpid = payload.getStreamPID();

        if (pmtVersions.get(pmtpid) == version)
            return;

        pmtVersions.put(pmtpid, version);
        ProgramEntity program = programs.get(pmt.getProgramNumber()).program;
        program.setPmtVersion(version);
        program.setPmtPid(pmtpid);
        program.setPcrPid(pmt.getProgramClockReferencePID());

        databaseService.updateStreamUsage(pmtpid,
                                          StreamTypes.CATEGORY_DATA,
                                          String.format("PMT（节目号：%d）", program.getProgramNumber()));

        descloop.attach(pmt.getDescriptorLoop());
        descloop.forEach(cad::isAttachable, encoding -> {
            program.setFreeAccess(false);
            cad.attach(encoding);
            int ecm = cad.getConditionalAccessStreamPID();
            int casid = cad.getConditionalAccessSystemID();
            String vendor = CASystems.vendor(casid);
            String ecmdesc = String.format("ECM（节目号：%d），系统号：%04X", program.getProgramNumber(), casid);
            if (!vendor.isEmpty())
                ecmdesc += "，提供商：" + vendor;
            databaseService.updateStreamUsage(ecm, StreamTypes.CATEGORY_DATA, ecmdesc);
            databaseService.addProgramStreamMapping(program.getProgramNumber(), ecm, 0x05, StreamTypes.CATEGORY_DATA, ecmdesc);
        });

        pmt.forEachProgramElement(encoding -> {
            element.attach(encoding);
            int esPid = element.getElementaryPID();
            int esType = element.getStreamType();
            String description = String.format("%s（节目号：%d）", StreamTypes.description(esType), program.getProgramNumber());
            databaseService.updateStreamUsage(esPid, StreamTypes.category(esType), description);
            databaseService.addProgramStreamMapping(program.getProgramNumber(),
                                                    esPid,
                                                    esType,
                                                    StreamTypes.category(esType),
                                                    description);

            descloop.attach(element.getDescriptorLoop());
            descloop.forEach(cad::isAttachable, descriptor -> {
                program.setFreeAccess(false);
                cad.attach(descriptor);
                int ecm = cad.getConditionalAccessStreamPID();
                int casid = cad.getConditionalAccessSystemID();
                String vendor = CASystems.vendor(casid);
                String ecmdesc = String.format("ECM（节目号：%d），系统号：%04X", program.getProgramNumber(), casid);
                if (!vendor.isEmpty())
                    ecmdesc += "，提供商：" + vendor;
                databaseService.updateStreamUsage(ecm, StreamTypes.CATEGORY_DATA, ecmdesc);
                databaseService.addProgramStreamMapping(program.getProgramNumber(), ecm, 0x05, StreamTypes.CATEGORY_DATA, ecmdesc);
            });
        });

        databaseService.updateProgram(program);
    }
}
