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
package m2tk.assistant.app.kernel.tracer;

import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.api.M2TKDatabase;
import m2tk.assistant.api.Tracer;
import m2tk.assistant.api.domain.*;
import m2tk.assistant.api.presets.StreamTypes;
import m2tk.dvb.DVB;
import m2tk.dvb.decoder.descriptor.*;
import m2tk.dvb.decoder.element.EventDescriptionDecoder;
import m2tk.dvb.decoder.element.ServiceDescriptionDecoder;
import m2tk.dvb.decoder.element.TransportStreamDescriptionDecoder;
import m2tk.dvb.decoder.section.*;
import m2tk.mpeg2.decoder.DescriptorLoopDecoder;
import m2tk.mpeg2.decoder.SectionDecoder;
import m2tk.multiplex.DemuxStatus;
import m2tk.multiplex.TSDemux;
import m2tk.multiplex.TSDemuxEvent;
import m2tk.multiplex.TSDemuxPayload;
import org.pf4j.Extension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Extension
public class SITracer implements Tracer
{
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
    private final Map<String, Integer> tableVersions;

    private int sourceId;
    private M2TKDatabase databaseService;

    public SITracer()
    {
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
        tableVersions = new HashMap<>();
    }

    @Override
    public void configure(StreamSource source, TSDemux demux, M2TKDatabase database)
    {
        sourceId = source.getId();
        databaseService = database;

        demux.registerEventListener(this::processDemuxStatus);
        demux.registerSectionChannel(0x0010, this::processSection);
        demux.registerSectionChannel(0x0011, this::processSection);
        demux.registerSectionChannel(0x0012, this::processSection);
        demux.registerSectionChannel(0x0014, this::processSection);
    }

    private void processDemuxStatus(TSDemuxEvent event)
    {
        if (event instanceof DemuxStatus status)
        {
            if (status.isRunning())
            {
                tableVersions.clear();
            }
        }
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

        SINetwork network = databaseService.addSINetwork(nit.getNetworkID(), tableId == 0x40);
        databaseService.updateStreamSourceComponentPresence(sourceId, network.isActualNetwork() ? "NIT_Actual" : "NIT_Other", true);
        databaseService.updateElementaryStreamUsage(payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "NIT");
        databaseService.addPrivateSection(network.isActualNetwork() ? "NIT_Actual" : "NIT_Other",
                                          payload.getStreamPID(),
                                          payload.getFinishPacketCounter(),
                                          payload.getEncoding().getBytes());

        tableVersions.put(uid, version);

        descloop.attach(nit.getDescriptorLoop());
        descloop.findFirstDescriptor(nnd::isAttachable)
                .ifPresent(encoding -> {
                    nnd.attach(encoding);
                    network.setName(nnd.getNetworkName());
                });
        databaseService.updateSINetwork(network);

        nit.forEachTransportStreamDescription(encoding -> {
            tsd.attach(encoding);
            SIMultiplex multiplex = databaseService.addSIMultiplex(network.getId(),
                                                                   tsd.getTransportStreamID(),
                                                                   tsd.getOriginalNetworkID());

            descloop.attach(tsd.getDescriptorLoop());
            descloop.forEach(descriptor -> {
                if (cdsd.isAttachable(descriptor))
                {
                    cdsd.attach(descriptor);
                    multiplex.setDeliverySystemType("有线数字电视系统");
                    multiplex.setTransmitFrequency(DVB.translateCableFrequencyCode(cdsd.getFrequencyCode()));
                }
                if (sdsd.isAttachable(descriptor))
                {
                    sdsd.attach(descriptor);
                    multiplex.setDeliverySystemType("卫星数字电视系统");
                    multiplex.setTransmitFrequency(DVB.translateSatelliteFrequencyCode(sdsd.getFrequencyCode()));
                }
                if (tdsd.isAttachable(descriptor))
                {
                    tdsd.attach(descriptor);
                    multiplex.setDeliverySystemType("地面数字电视系统");
                    multiplex.setTransmitFrequency(DVB.translateTerrestrialFrequencyCode(tdsd.getCentreFrequencyCode()));
                }
            });
            databaseService.updateSIMultiplex(multiplex);
        });
    }

    private void processBAT(TSDemuxPayload payload)
    {
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

        SIBouquet bouquet = databaseService.addSIBouquet(bouquetId);
        databaseService.updateStreamSourceComponentPresence(sourceId, "BAT", true);
        databaseService.updateElementaryStreamUsage(payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "SDT/BAT");
        databaseService.addPrivateSection("BAT",
                                          payload.getStreamPID(),
                                          payload.getFinishPacketCounter(),
                                          payload.getEncoding().getBytes());

        tableVersions.put(uid, version);

        descloop.attach(bat.getDescriptorLoop());
        descloop.findFirstDescriptor(bnd::isAttachable)
                .ifPresent(encoding ->
                           {
                               bnd.attach(encoding);
                               bouquet.setName(bnd.getBouquetName());
                           });
        databaseService.updateSIBouquet(bouquet);

        bat.forEachTransportStreamDescription(encoding -> {
            tsd.attach(encoding);
            descloop.attach(tsd.getDescriptorLoop());
            descloop.findFirstDescriptor(sld::isAttachable)
                    .ifPresent(descriptor -> {
                        sld.attach(descriptor);
                        int[] serviceIds = sld.getServiceIDList();
                        for (int serviceId : serviceIds)
                        {
                            databaseService.addBouquetServiceMapping(bouquet.getId(),
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

        boolean actualTS = (tableId == 0x42);
        databaseService.updateStreamSourceComponentPresence(sourceId, actualTS ? "SDT_Actual" : "SDT_Other", true);
        databaseService.updateElementaryStreamUsage(payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "SDT/BAT");
        databaseService.addPrivateSection(actualTS ? "SDT_Actual" : "SDT_Other",
                                          payload.getStreamPID(),
                                          payload.getFinishPacketCounter(),
                                          payload.getEncoding().getBytes());
        tableVersions.put(uid, version);

        sdt.forEachServiceDescription(encoding -> {
            sdd.attach(encoding);

            SIService service = databaseService.addSIService(sdd.getServiceID(), transportStreamId, originalNetworkId, actualTS);
            service.setRunningStatus(sdd.getRunningStatus());
            service.setFreeAccess(sdd.getFreeCAMode() == 0);
            service.setPresentFollowingEITEnabled(sdd.getEITPresentFollowingFlag() == 1);
            service.setScheduleEITEnabled(sdd.getEITScheduleFlag() == 1);

            descloop.attach(sdd.getDescriptorLoop());
            descloop.forEach(descriptor -> {
                if (sd.isAttachable(descriptor))
                {
                    sd.attach(descriptor);
                    service.setServiceType(sd.getServiceType());
                    service.setName(sd.getServiceName());
                    service.setProvider(sd.getServiceProviderName());
                }
                if (tssd.isAttachable(descriptor))
                {
                    tssd.attach(descriptor);
                    service.setServiceType(0x05);
                    service.setName(String.format("NVOD时移业务（引用业务号：%d）", tssd.getReferenceServiceID()));
                    service.setReferenceServiceId(tssd.getReferenceServiceID());
                }
            });
            databaseService.updateSIService(service);
        });
    }

    private void processEIT(TSDemuxPayload payload)
    {
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

        databaseService.updateStreamSourceComponentPresence(sourceId, getEITTag(tableId), true);
        databaseService.updateElementaryStreamUsage(payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "EIT");
        databaseService.addPrivateSection(getEITTag(tableId),
                                          payload.getStreamPID(),
                                          payload.getFinishPacketCounter(),
                                          payload.getEncoding().getBytes());

        tableVersions.put(uid, version);

        eit.forEachEventDescription(encoding -> {
            edd.attach(encoding);

            SIEvent event = databaseService.addSIEvent(edd.getEventID(), transportStreamId, originalNetworkId, serviceId);
            event.setRunningStatus(edd.getRunningStatus());
            event.setFreeAccess(edd.getFreeCAMode() == 0);
            event.setStartTime(translateStartTime(edd.getStartTime()));
            event.setDuration(DVB.decodeDuration(edd.getDuration()));
            event.setPresentEvent((tableId == 0x4E || tableId == 0x4F) && (secnum == 0));
            event.setScheduleEvent(tableId >= 0x50 && tableId <= 0x5F);

            descloop.attach(edd.getDescriptorLoop());
            descloop.forEach(descriptor -> {
                if (sed.isAttachable(descriptor))
                {
                    sed.attach(descriptor);
                    event.setLanguageCode(sed.getLanguageCode());
                    event.setTitle(sed.getEventName());
                    event.setDescription(sed.getEventDescription());
                }
                if (tsed.isAttachable(descriptor))
                {
                    tsed.attach(descriptor);
                    event.setNvodTimeShiftedEvent(true);
                    event.setReferenceServiceId(tsed.getReferenceServiceID());
                    event.setReferenceEventId(tsed.getReferenceEventID());
                }
            });
            databaseService.updateSIEvent(event);
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

        databaseService.addTimestamp(DVB.decodeTimepointIntoOffsetDateTime(tdt.getUTCTime()));
        databaseService.updateElementaryStreamUsage(payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "TDT/TOT");
        databaseService.addPrivateSection("TDT",
                                          payload.getStreamPID(),
                                          payload.getFinishPacketCounter(),
                                          payload.getEncoding().getBytes());
    }

    private void processTOT(TSDemuxPayload payload)
    {
        tot.attach(payload.getEncoding());

        databaseService.addTimestamp(DVB.decodeTimepointIntoOffsetDateTime(tot.getUTCTime()));
        databaseService.updateElementaryStreamUsage(payload.getStreamPID(), StreamTypes.CATEGORY_DATA, "TDT/TOT");
        databaseService.addPrivateSection("TOT",
                                          payload.getStreamPID(),
                                          payload.getFinishPacketCounter(),
                                          payload.getEncoding().getBytes());
    }

    private OffsetDateTime translateStartTime(long timepoint)
    {
        // NVOD索引事件的起始时间为全1。
        return (timepoint == 0xFFFFFFFFFFL)
               ? null
               : DVB.decodeTimepointIntoOffsetDateTime(timepoint);
    }
}
