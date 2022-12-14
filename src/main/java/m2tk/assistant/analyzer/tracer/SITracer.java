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
import m2tk.assistant.analyzer.presets.RunningStatus;
import m2tk.assistant.analyzer.presets.ServiceTypes;
import m2tk.assistant.analyzer.presets.StreamTypes;
import m2tk.assistant.dbi.DatabaseService;
import m2tk.assistant.dbi.entity.*;
import m2tk.dvb.DVB;
import m2tk.dvb.decoder.descriptor.*;
import m2tk.dvb.decoder.element.EventDescriptionDecoder;
import m2tk.dvb.decoder.element.ServiceDescriptionDecoder;
import m2tk.dvb.decoder.element.TransportStreamDescriptionDecoder;
import m2tk.dvb.decoder.section.*;
import m2tk.mpeg2.decoder.DescriptorLoopDecoder;
import m2tk.mpeg2.decoder.SectionDecoder;
import m2tk.multiplex.TSDemux;
import m2tk.multiplex.TSDemuxPayload;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SITracer implements Tracer
{
    private final DatabaseService databaseService;
    private final long transactionId;

    private final SectionDecoder sec;
    private final NITSectionDecoder nit;
    private final BATSectionDecoder bat;
    private final SDTSectionDecoder sdt;
    private final EITSectionDecoder eit;
    private final TDTSectionDecoder tdt;
    private final TOTSectionDecoder tot;
    private final DescriptorLoopDecoder descloop;
    private final TransportStreamDescriptionDecoder tsd;
    private final ServiceDescriptionDecoder sdd;
    private final EventDescriptionDecoder edd;
    private final NetworkNameDescriptorDecoder nnd;
    private final ServiceDescriptorDecoder sd;
    private final TimeShiftedServiceDescriptorDecoder tssd;
    private final TimeShiftedEventDescriptorDecoder tsed;
    private final ShortEventDescriptorDecoder sed;
    private final CableDeliverySystemDescriptorDecoder cdsd;
    private final SatelliteDeliverySystemDescriptorDecoder sdsd;
    private final TerrestrialDeliverySystemDescriptorDecoder tdsd;
    private final BouquetNameDescriptorDecoder bnd;
    private final ServiceListDescriptorDecoder sld;
    private final DateTimeFormatter startTimeFormatter;
    private final Map<String, Integer> tableVersions;

    public SITracer(DatabaseService service, long transaction)
    {
        databaseService = service;
        transactionId = transaction;

        sec = new SectionDecoder();
        nit = new NITSectionDecoder();
        bat = new BATSectionDecoder();
        sdt = new SDTSectionDecoder();
        eit = new EITSectionDecoder();
        tdt = new TDTSectionDecoder();
        tot = new TOTSectionDecoder();
        descloop = new DescriptorLoopDecoder();
        nnd = new NetworkNameDescriptorDecoder();
        bnd = new BouquetNameDescriptorDecoder();
        sd = new ServiceDescriptorDecoder();
        tssd = new TimeShiftedServiceDescriptorDecoder();
        tsed = new TimeShiftedEventDescriptorDecoder();
        sed = new ShortEventDescriptorDecoder();
        cdsd = new CableDeliverySystemDescriptorDecoder();
        sdsd = new SatelliteDeliverySystemDescriptorDecoder();
        tdsd = new TerrestrialDeliverySystemDescriptorDecoder();
        tsd = new TransportStreamDescriptionDecoder();
        sdd = new ServiceDescriptionDecoder();
        edd = new EventDescriptionDecoder();
        sld = new ServiceListDescriptorDecoder();
        startTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        tableVersions = new HashMap<>();
    }

    @Override
    public void configureDemux(TSDemux demux)
    {
        demux.registerSectionChannel(0x0010, this::processSection);
        demux.registerSectionChannel(0x0011, this::processSection);
        demux.registerSectionChannel(0x0012, this::processSection);
        demux.registerSectionChannel(0x0014, this::processSection);
    }

    private void processSection(TSDemuxPayload payload)
    {
        if (payload.getType() != TSDemuxPayload.Type.SECTION || !sec.isAttachable(payload.getEncoding()))
            return;

        if (payload.getStreamPID() == 0x0010 && nit.isAttachable(payload.getEncoding()))
            processNIT(payload);
        if (payload.getStreamPID() == 0x0011 && bat.isAttachable(payload.getEncoding()))
            processBAT(payload);
        if (payload.getStreamPID() == 0x0011 && sdt.isAttachable(payload.getEncoding()))
            processSDT(payload);
        if (payload.getStreamPID() == 0x0012 && eit.isAttachable(payload.getEncoding()))
            processEIT(payload);
        if (payload.getStreamPID() == 0x0014 && tdt.isAttachable(payload.getEncoding()))
            processTDT(payload);
        if (payload.getStreamPID() == 0x0014 && tot.isAttachable(payload.getEncoding()))
            processTOT(payload);
    }

    private void processNIT(TSDemuxPayload payload)
    {
        nit.attach(payload.getEncoding());
        if (!nit.isChecksumCorrect())
        {
            log.warn("NIT???????????????");
            return;
        }

        int tableId = nit.getTableID();
        int version = nit.getVersionNumber();
        int secnum = nit.getSectionNumber();

        String uid = String.format("nit.%02x", tableId);
        if (!tableVersions.containsKey(uid) || tableVersions.get(uid) != version)
        {
            // ????????????
            List<String> keys = new ArrayList<>(tableVersions.keySet());
            for (String key : keys)
                if (key.startsWith(uid))
                    tableVersions.remove(uid);
            tableVersions.put(uid, version);
        }

        uid = String.format("nit.%02x.%d", tableId, secnum);
        if (tableVersions.containsKey(uid) && tableVersions.get(uid) == version)
            return; // ?????????????????????

        databaseService.updateStreamUsage(transactionId, payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "NIT");
        databaseService.addSection(transactionId,
                                   tableId == 0x40 ? "NIT_Actual" : "NIT_Other",
                                   payload.getStreamPID(),
                                   payload.getFinishPacketCounter(),
                                   payload.getEncoding().getBytes());
        tableVersions.put(uid, version);

        SINetworkEntity network = databaseService.addNetwork(transactionId, nit.getNetworkID(), tableId == 0x40);
        descloop.attach(nit.getDescriptorLoop());
        descloop.findFirstDescriptor(nnd::isAttachable)
                .ifPresent(encoding ->
                           {
                               nnd.attach(encoding);
                               String name = nnd.getNetworkName();
                               network.setNetworkName(name);
                           });
        databaseService.updateNetworkName(network);

        nit.forEachTransportStreamDescription(encoding -> {
            tsd.attach(encoding);
            SIMultiplexEntity multiplex = databaseService.addMultiplex(transactionId,
                                                                       nit.getNetworkID(),
                                                                       tsd.getTransportStreamID(),
                                                                       tsd.getOriginalNetworkID());
            descloop.attach(tsd.getDescriptorLoop());
            descloop.forEach(descriptor -> {
                if (cdsd.isAttachable(descriptor))
                {
                    cdsd.attach(descriptor);
                    multiplex.setDeliverySystemType("??????");
                    multiplex.setTransmitFrequency(DVB.translateCableFrequencyCode(cdsd.getFrequencyCode()));
                }
                if (sdsd.isAttachable(descriptor))
                {
                    sdsd.attach(descriptor);
                    multiplex.setDeliverySystemType("??????");
                    multiplex.setTransmitFrequency(DVB.translateSatelliteFrequencyCode(sdsd.getFrequencyCode()));
                }
                if (tdsd.isAttachable(descriptor))
                {
                    tdsd.attach(descriptor);
                    multiplex.setDeliverySystemType("??????");
                    multiplex.setTransmitFrequency(DVB.translateTerrestrialFrequencyCode(tdsd.getCentreFrequencyCode()));
                }
            });
            databaseService.updateMultiplexDeliverySystemConfigure(multiplex);
        });
    }

    private void processBAT(TSDemuxPayload payload)
    {
        bat.attach(payload.getEncoding());
        if (!bat.isChecksumCorrect())
        {
            log.warn("BAT???????????????");
            return;
        }

        int bouquetId = bat.getBouquetID();
        int version = bat.getVersionNumber();
        int secnum = bat.getSectionNumber();

        String uid = String.format("bat.%d", bouquetId);
        if (!tableVersions.containsKey(uid) || tableVersions.get(uid) != version)
        {
            // ????????????
            List<String> keys = new ArrayList<>(tableVersions.keySet());
            for (String key : keys)
                if (key.startsWith(uid))
                    tableVersions.remove(uid);
            tableVersions.put(uid, version);
        }

        uid = String.format("bat.%d.%d", bouquetId, secnum);
        if (tableVersions.containsKey(uid) && tableVersions.get(uid) == version)
            return; // ?????????????????????

        databaseService.updateStreamUsage(transactionId, payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "SDT/BAT");
        databaseService.addSection(transactionId,
                                   "BAT",
                                   payload.getStreamPID(),
                                   payload.getFinishPacketCounter(),
                                   payload.getEncoding().getBytes());
        tableVersions.put(uid, version);

        SIBouquetEntity bouquet = databaseService.addBouquet(transactionId, bouquetId);
        descloop.attach(bat.getDescriptorLoop());
        descloop.findFirstDescriptor(bnd::isAttachable)
                .ifPresent(encoding ->
                           {
                               bnd.attach(encoding);
                               bouquet.setBouquetName(bnd.getBouquetName());
                           });
        databaseService.updateBouquetName(bouquet);

        bat.forEachTransportStreamDescription(encoding -> {
            tsd.attach(encoding);
            descloop.attach(tsd.getDescriptorLoop());
            descloop.findFirstDescriptor(sld::isAttachable)
                    .ifPresent(descriptor -> {
                        sld.attach(descriptor);
                        int[] serviceIds = sld.getServiceIDList();
                        for (int serviceId : serviceIds)
                        {
                            databaseService.addBouquetServiceMapping(transactionId,
                                                                     bouquetId,
                                                                     tsd.getTransportStreamID(),
                                                                     tsd.getOriginalNetworkID(),
                                                                     serviceId);
                        }
                    });
        });
    }

    private void processSDT(TSDemuxPayload payload)
    {
        sdt.attach(payload.getEncoding());
        if (!sdt.isChecksumCorrect())
        {
            log.warn("SDT???????????????");
            return;
        }

        int tableId = sdt.getTableID();
        int transportStreamId = sdt.getTransportStreamID();
        int originalNetworkId = sdt.getOriginalNetworkID();
        int version = sdt.getVersionNumber();
        int secnum = sdt.getSectionNumber();

        String uid = String.format("sdt.%02x.%d.%d", tableId, transportStreamId, originalNetworkId);
        if (!tableVersions.containsKey(uid) || tableVersions.get(uid) != version)
        {
            // ????????????
            List<String> keys = new ArrayList<>(tableVersions.keySet());
            for (String key : keys)
                if (key.startsWith(uid))
                    tableVersions.remove(uid);
            tableVersions.put(uid, version);
        }

        uid = String.format("sdt.%02x.%d.%d.%d", tableId, transportStreamId, originalNetworkId, secnum);
        if (tableVersions.containsKey(uid) && tableVersions.get(uid) == version)
            return; // ?????????????????????

        databaseService.updateStreamUsage(transactionId, payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "SDT/BAT");
        databaseService.addSection(transactionId,
                                   tableId == 0x42 ? "SDT_Actual" : "SDT_Other",
                                   payload.getStreamPID(),
                                   payload.getFinishPacketCounter(),
                                   payload.getEncoding().getBytes());
        tableVersions.put(uid, version);

        sdt.forEachServiceDescription(encoding -> {
            sdd.attach(encoding);
            SIServiceEntity service = databaseService.addService(transactionId,
                                                                 transportStreamId,
                                                                 originalNetworkId,
                                                                 sdd.getServiceID(),
                                                                 RunningStatus.name(sdd.getRunningStatus()),
                                                                 sdd.getFreeCAMode() == 0,
                                                                 sdd.getEITPresentFollowingFlag() == 1,
                                                                 sdd.getEITScheduleFlag() == 1,
                                                                 tableId == 0x42);

            descloop.attach(sdd.getDescriptorLoop());
            descloop.forEach(descriptor -> {
                if (sd.isAttachable(descriptor))
                {
                    sd.attach(descriptor);
                    service.setServiceType(sd.getServiceType());
                    service.setServiceTypeName(ServiceTypes.name(sd.getServiceType()));
                    service.setServiceName(sd.getServiceName());
                    service.setServiceProvider(sd.getServiceProviderName());
                    databaseService.updateServiceDetails(service);
                }
                if (tssd.isAttachable(descriptor))
                {
                    tssd.attach(descriptor);
                    service.setReferenceServiceId(tssd.getReferenceServiceID());
                    service.setServiceType(0x05);
                    service.setServiceTypeName(ServiceTypes.name(0x05));
                    service.setServiceName(String.format("NVOD?????????????????????????????????%d???",
                                                         service.getReferenceServiceId()));
                    databaseService.updateServiceReference(service);
                    databaseService.updateServiceDetails(service);
                }
            });
        });
    }

    private void processEIT(TSDemuxPayload payload)
    {
        eit.attach(payload.getEncoding());
        if (!eit.isChecksumCorrect())
        {
            log.warn("EIT???????????????");
            return;
        }

        int tableId = eit.getTableID();
        int serviceId = eit.getServiceID();
        int transportStreamId = eit.getTransportStreamID();
        int originalNetworkId = eit.getOriginalNetworkID();
        int version = eit.getVersionNumber();
        int secnum = eit.getSectionNumber();

        String uid = String.format("eit.%02x.%d.%d.%d",
                                   tableId, transportStreamId, originalNetworkId, serviceId);
        if (!tableVersions.containsKey(uid) || tableVersions.get(uid) != version)
        {
            // ????????????
            List<String> keys = new ArrayList<>(tableVersions.keySet());
            for (String key : keys)
                if (key.startsWith(uid))
                    tableVersions.remove(uid);
            tableVersions.put(uid, version);
        }

        uid = String.format("eit.%02x.%d.%d.%d.%d",
                            tableId, transportStreamId, originalNetworkId, serviceId, secnum);
        if (tableVersions.containsKey(uid) && tableVersions.get(uid) == version)
            return; // ?????????????????????

        databaseService.updateStreamUsage(transactionId, payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "EIT");
        databaseService.addSection(transactionId,
                                   getEITTag(tableId),
                                   payload.getStreamPID(),
                                   payload.getFinishPacketCounter(),
                                   payload.getEncoding().getBytes());
        tableVersions.put(uid, version);

        eit.forEachEventDescription(encoding -> {
            edd.attach(encoding);
            int eventId = edd.getEventID();
            String startTime = translateStartTime(edd.getStartTime());
            String duration = DVB.printTimeFields(edd.getDuration());
            String runningStatus = RunningStatus.name(edd.getRunningStatus());
            boolean isFreeCAMode = (edd.getFreeCAMode() == 0);
            SIEventEntity event;
            if (tableId == 0x4E || tableId == 0x4F)
            {
                event = databaseService.addPresentFollowingEvent(transactionId,
                                                                 transportStreamId,
                                                                 originalNetworkId,
                                                                 serviceId,
                                                                 eventId,
                                                                 startTime,
                                                                 duration,
                                                                 runningStatus,
                                                                 isFreeCAMode,
                                                                 secnum == 0);
            } else
            {
                event = databaseService.addScheduleEvent(transactionId,
                                                         transportStreamId,
                                                         originalNetworkId,
                                                         serviceId,
                                                         eventId,
                                                         startTime,
                                                         duration,
                                                         runningStatus,
                                                         isFreeCAMode);
            }

            descloop.attach(edd.getDescriptorLoop());
            descloop.forEach(descriptor -> {
                if (sed.isAttachable(descriptor))
                {
                    sed.attach(descriptor);
                    event.setEventName(sed.getEventName());
                    event.setEventDescription(sed.getEventDescription());
                    event.setLanguageCode(sed.getLanguageCode());
                    databaseService.updateEventDescription(event);
                }
                if (tsed.isAttachable(descriptor))
                {
                    tsed.attach(descriptor);
                    event.setReferenceServiceId(tsed.getReferenceServiceID());
                    event.setReferenceEventId(tsed.getReferenceEventID());
                    event.setNvodTimeShiftedEvent(true);
                    databaseService.updateEventReference(event);
                }
            });
        });
    }

    private String getEITTag(int tableId)
    {
        if (tableId == 0x4E)
            return "EIT_PF_Actual";
        else if (tableId == 0x4F)
            return "EIT_PF_Other";
        else if (tableId >= 0x50 && tableId <= 0x5F)
            return "EIT_Schedule_Actual";
        else
            return "EIT_Schedule_Other";
    }

    private void processTDT(TSDemuxPayload payload)
    {
        tdt.attach(payload.getEncoding());
        databaseService.addDateTime(transactionId, tdt.getUTCTime());
        databaseService.updateStreamUsage(transactionId, payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "TDT/TOT");
        databaseService.addSection(transactionId, "TDT",
                                   payload.getStreamPID(),
                                   payload.getFinishPacketCounter(),
                                   payload.getEncoding().getBytes());
    }

    private void processTOT(TSDemuxPayload payload)
    {
        tot.attach(payload.getEncoding());
        databaseService.addDateTime(transactionId, tot.getUTCTime());
        databaseService.updateStreamUsage(transactionId, payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "TDT/TOT");
        databaseService.addSection(transactionId, "TOT",
                                   payload.getStreamPID(),
                                   payload.getFinishPacketCounter(),
                                   payload.getEncoding().getBytes());
    }

    private String translateStartTime(long timepoint)
    {
        // NVOD?????????????????????????????????1???
        if (timepoint == 0xFFFFFFFFFFL)
            return "?????????";
        return DVB.decodeTimepointIntoLocalDateTime(timepoint)
                  .format(startTimeFormatter);
    }
}
