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

package m2tk.assistant.analyzer;

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
import m2tk.multiplex.TSDemuxPayload;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SITracer
{
    private final DatabaseService databaseService;

    private final NITSectionDecoder nit;
    private final BATSectionDecoder bat;
    private final SDTSectionDecoder sdt;
    private final EITSectionDecoder eit;
    private final TDTSectionDecoder tdt;
    private final DescriptorLoopDecoder descloop;
    private final TransportStreamDescriptionDecoder tsd;
    private final ServiceDescriptionDecoder sdd;
    private final EventDescriptionDecoder edd;
    private final NetworkNameDescriptorDecoder nnd;
    private final ServiceDescriptorDecoder sd;
    private final ShortEventDescriptorDecoder sed;
    private final CableDeliverySystemDescriptorDecoder cdsd;
    private final BouquetNameDescriptorDecoder bnd;
    private final ServiceListDescriptorDecoder sld;
    private final DateTimeFormatter startTimeFormatter;
    private final Map<String, Integer> tableVersions;

    public SITracer(DatabaseService service)
    {
        databaseService = service;
        nit = new NITSectionDecoder();
        bat = new BATSectionDecoder();
        sdt = new SDTSectionDecoder();
        eit = new EITSectionDecoder();
        tdt = new TDTSectionDecoder();
        descloop = new DescriptorLoopDecoder();
        nnd = new NetworkNameDescriptorDecoder();
        bnd = new BouquetNameDescriptorDecoder();
        sd = new ServiceDescriptorDecoder();
        sed = new ShortEventDescriptorDecoder();
        cdsd = new CableDeliverySystemDescriptorDecoder();
        tsd = new TransportStreamDescriptionDecoder();
        sdd = new ServiceDescriptionDecoder();
        edd = new EventDescriptionDecoder();
        sld = new ServiceListDescriptorDecoder();
        startTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        tableVersions = new HashMap<>();
    }

    public void processNIT(TSDemuxPayload payload)
    {
        if (payload.getType() != TSDemuxPayload.Type.SECTION ||
            payload.getStreamPID() != 0x0010 ||
            !nit.isAttachable(payload.getEncoding()))
            return;

        nit.attach(payload.getEncoding());
        if (!nit.isChecksumCorrect())
        {
            log.warn("NIT校验错误。");
            return;
        }

        int tableId = nit.getTableID();
        int version = nit.getVersionNumber();
        int secnum = nit.getSectionNumber();

        String uid = String.format("nit.%02x", tableId);
        if (!tableVersions.containsKey(uid) || tableVersions.get(uid) != version)
        {
            // 版本变化
            List<String> keys = new ArrayList<>(tableVersions.keySet());
            for (String key : keys)
                if (key.startsWith(uid))
                    tableVersions.remove(uid);
            tableVersions.put(uid, version);
        }

        uid = String.format("nit.%02x.%d", tableId, secnum);
        if (tableVersions.containsKey(uid) && tableVersions.get(uid) == version)
            return; // 已经处理过了。

        databaseService.updateStreamUsage(payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "NIT");
        tableVersions.put(uid, version);

        SINetworkEntity network = databaseService.addNetwork(nit.getNetworkID(), tableId == 0x40);
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
            SIMultiplexEntity multiplex = databaseService.addMultiplex(nit.getNetworkID(),
                                                                       tsd.getTransportStreamID(),
                                                                       tsd.getOriginalNetworkID());
            descloop.attach(tsd.getDescriptorLoop());
            descloop.findFirstDescriptor(cdsd::isAttachable)
                    .ifPresent(descriptor -> {
                        cdsd.attach(descriptor);
                        multiplex.setDeliverySystemType("Cable");
                        multiplex.setTransmitFrequency(DVB.decodeFrequencyCode(cdsd.getFrequencyCode()) + " MHz");
                    });
            databaseService.updateMultiplexDeliverySystemConfigure(multiplex);
        });
    }

    public void processBAT(TSDemuxPayload payload)
    {
        if (payload.getType() != TSDemuxPayload.Type.SECTION ||
            payload.getStreamPID() != 0x0011 ||
            !bat.isAttachable(payload.getEncoding()))
            return;

        bat.attach(payload.getEncoding());
        if (!bat.isChecksumCorrect())
        {
            log.warn("BAT校验错误。");
            return;
        }

        int bouquetId = bat.getBouquetID();
        int version = bat.getVersionNumber();
        int secnum = bat.getSectionNumber();

        String uid = String.format("bat.%d", bouquetId);
        if (!tableVersions.containsKey(uid) || tableVersions.get(uid) != version)
        {
            // 版本变化
            List<String> keys = new ArrayList<>(tableVersions.keySet());
            for (String key : keys)
                if (key.startsWith(uid))
                    tableVersions.remove(uid);
            tableVersions.put(uid, version);
        }

        uid = String.format("bat.%d.%d", bouquetId, secnum);
        if (tableVersions.containsKey(uid) && tableVersions.get(uid) == version)
            return; // 已经处理过了。

        databaseService.updateStreamUsage(payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "SDT/BAT");
        tableVersions.put(uid, version);

        SIBouquetEntity bouquet = databaseService.addBouquet(bouquetId);
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
                            databaseService.addBouquetServiceMapping(bouquetId,
                                                                     tsd.getTransportStreamID(),
                                                                     tsd.getOriginalNetworkID(),
                                                                     serviceId);
                        }
                    });
        });
    }

    public void processSDT(TSDemuxPayload payload)
    {
        if (payload.getType() != TSDemuxPayload.Type.SECTION ||
            payload.getStreamPID() != 0x0011 ||
            !sdt.isAttachable(payload.getEncoding()))
            return;

        sdt.attach(payload.getEncoding());
        if (!sdt.isChecksumCorrect())
        {
            log.warn("SDT校验错误。");
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
            // 版本变化
            List<String> keys = new ArrayList<>(tableVersions.keySet());
            for (String key : keys)
                if (key.startsWith(uid))
                    tableVersions.remove(uid);
            tableVersions.put(uid, version);
        }

        uid = String.format("sdt.%02x.%d.%d.%d", tableId, transportStreamId, originalNetworkId, secnum);
        if (tableVersions.containsKey(uid) && tableVersions.get(uid) == version)
            return; // 已经处理过了。

        databaseService.updateStreamUsage(payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "SDT/BAT");
        tableVersions.put(uid, version);

        sdt.forEachServiceDescription(encoding -> {
            sdd.attach(encoding);
            SIServiceEntity service = databaseService.addService(transportStreamId,
                                                                 originalNetworkId,
                                                                 sdd.getServiceID(),
                                                                 RunningStatus.name(sdd.getRunningStatus()),
                                                                 sdd.getFreeCAMode() == 0,
                                                                 sdd.getEITPresentFollowingFlag() == 1,
                                                                 sdd.getEITScheduleFlag() == 1,
                                                                 tableId == 0x42);

            descloop.attach(sdd.getDescriptorLoop());
            descloop.findFirstDescriptor(sd::isAttachable)
                    .ifPresent(descriptor -> {
                        sd.attach(descriptor);
                        service.setServiceType(sd.getServiceType());
                        service.setServiceTypeName(ServiceTypes.name(sd.getServiceType()));
                        service.setServiceName(sd.getServiceName());
                        service.setServiceProvider(sd.getServiceProviderName());
                        databaseService.updateServiceDetails(service);
                    });
        });
    }

    public void processEIT(TSDemuxPayload payload)
    {
        if (payload.getType() != TSDemuxPayload.Type.SECTION ||
            payload.getStreamPID() != 0x0012 ||
            !eit.isAttachable(payload.getEncoding()))
            return;

        eit.attach(payload.getEncoding());
        if (!eit.isChecksumCorrect())
        {
            log.warn("EIT校验错误。");
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
            // 版本变化
            List<String> keys = new ArrayList<>(tableVersions.keySet());
            for (String key : keys)
                if (key.startsWith(uid))
                    tableVersions.remove(uid);
            tableVersions.put(uid, version);
        }

        uid = String.format("eit.%02x.%d.%d.%d.%d",
                            tableId, transportStreamId, originalNetworkId, serviceId, secnum);
        if (tableVersions.containsKey(uid) && tableVersions.get(uid) == version)
            return; // 已经处理过了。

        databaseService.updateStreamUsage(payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "EIT");
        tableVersions.put(uid, version);

        eit.forEachEventDescription(encoding -> {
            edd.attach(encoding);
            int eventId = edd.getEventID();
            String startTime = translateStartTime(edd.getStartTime());
            String duration = translateDuration(edd.getDuration());
            String runningStatus = RunningStatus.name(edd.getRunningStatus());
            boolean isFreeCAMode = (edd.getFreeCAMode() == 0);
            SIEventEntity event;
            if (tableId == 0x4E || tableId == 0x4F)
            {
                event = databaseService.addPresentFollowingEvent(transportStreamId,
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
                event = databaseService.addScheduleEvent(transportStreamId,
                                                         originalNetworkId,
                                                         serviceId,
                                                         eventId,
                                                         startTime,
                                                         duration,
                                                         runningStatus,
                                                         isFreeCAMode);
            }

            descloop.attach(edd.getDescriptorLoop());
            descloop.findFirstDescriptor(sed::isAttachable)
                    .ifPresent(descriptor -> {
                        sed.attach(descriptor);
                        event.setEventName(sed.getEventName());
                        event.setEventDescription(sed.getEventDescription());
                        event.setLanguageCode(sed.getLanguageCode());
                        databaseService.updateEventDescription(event);
                    });
        });
    }

    public void processTDT(TSDemuxPayload payload)
    {
        if (payload.getType() != TSDemuxPayload.Type.SECTION ||
            payload.getStreamPID() != 0x0014 ||
            !tdt.isAttachable(payload.getEncoding()))
            return;

        tdt.attach(payload.getEncoding());
        databaseService.addDateTime(tdt.getUTCTime());
        databaseService.updateStreamUsage(payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "TDT/TOT");
    }

    private String translateStartTime(long startTime)
    {
        LocalDateTime localDateTime = DVB.decodeTimepointIntoLocalDateTime(startTime);
        return localDateTime.format(startTimeFormatter);
    }

    private String translateDuration(int duration)
    {
        int seconds = DVB.decodeDuration(duration);
        int hrs = seconds / 3600;
        seconds = seconds % 3600;
        int min = seconds / 60;
        int sec = seconds % 60;

        return String.format("%02d:%02d:%02d", hrs, min, sec);
    }
}
