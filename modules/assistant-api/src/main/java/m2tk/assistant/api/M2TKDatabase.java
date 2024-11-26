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
package m2tk.assistant.api;

import m2tk.assistant.api.domain.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public interface M2TKDatabase
{
    StreamSource beginDiagnosis(String source, String uri);

    void updateStreamSourceStats(int sourceRef, int bitrate, int frameSize, boolean scrambled, long packetCount, int streamCount);

    void updateStreamSourceTransportId(int sourceRef, int transportStreamId);

    void updateStreamSourceComponentPresence(int sourceRef, String component, boolean present);

    StreamSource getCurrentStreamSource();

    List<StreamSource> listStreamSources();

    void updateElementaryStreamStats(int pid, long pktCount, long pcrCount, int bitrate, double ratio, boolean scrambled);

    void updateElementaryStreamStats(ElementaryStream stream);

    void updateElementaryStreamUsage(int pid, String category, String description);

    void updateElementaryStreamUsage(ElementaryStream stream);

    void accumulateElementaryStreamErrors(int pid, int transportErrors, int continuityErrors);

    ElementaryStream getElementaryStream(int pid);

    List<ElementaryStream> listElementaryStreams(boolean presentOnly);

    MPEGProgram addMPEGProgram(int programNumber, int transportStreamId, int pmtPid);

    void updateMPEGProgram(int programRef, int pcrPid, int pmtVersion, boolean freeAccess);

    void clearMPEGPrograms();

    void addProgramElementaryMapping(int programRef, int streamPid, int streamType);

    List<MPEGProgram> listMPEGPrograms();

    void addCASystemStream(int pid, int type, int systemId, byte[] privateData, int programRef, int programNumber, int elementaryStreamPid);

    List<CASystemStream> listCASystemStreams();

    SIBouquet addSIBouquet(int bouquetId);

    void updateSIBouquet(SIBouquet bouquet);

    void addBouquetServiceMapping(int bouquetRef, int transportStreamId, int originalNetworkId, int serviceId);

    List<SIBouquet> listSIBouquets();

    SINetwork addSINetwork(int networkId, boolean actualNetwork);

    void updateSINetwork(SINetwork network);

    List<SINetwork> listSINetworks();

    SIMultiplex addSIMultiplex(int networkRef, int transportStreamId, int originalNetworkId);

    void updateSIMultiplex(SIMultiplex multiplex);

    void addMultiplexServiceMapping(int bouquetRef, int serviceId);

    List<SIMultiplex> listSIMultiplexes();

    SIService addSIService(int serviceId, int transportStreamId, int originalNetworkId);

    void updateSIService(SIService service);

    List<SIService> listSIServices();

    SIEvent addSIEvent(int eventId, int transportStreamId, int originalNetworkId, int serviceId);

    void updateSIEvent(SIEvent event);

    List<SIEvent> listSIEvents(int transportStreamId, int originalNetworkId, int serviceId,
                               boolean presentOnly, boolean scheduleOnly,
                               OffsetDateTime timeFilterBegin, OffsetDateTime timeFilterEnd);

    void addTimestamp(OffsetDateTime timestamp);

    OffsetDateTime getLastTimestamp();

    void addTR290Event(TR290Event event);

    List<TR290Event> listTR290Events();

    List<TR290Stats> listTR290Stats();

    void addPCR(PCR pcr);

    void addPCRCheck(PCRCheck check);

    List<PCRStats> listPCRStats();

    List<PCRCheck> getRecentPCRChecks(int pid, int limit);

    void addPrivateSection(String tag, int pid, long position, byte[] encoding);

    void removePrivateSections(String tag, int pid);

    List<PrivateSection> getPrivateSections(int pid, int count);

    List<PrivateSection> getPrivateSections(String tag, int pid, int count);

    Map<Integer, List<PrivateSection>> getPrivateSectionGroups(String tag);

    void addTransportPacket(String tag, int pid, long position, byte[] encoding);

    List<TransportPacket> getTransportPackets(String tag, int pid, int count);

    void addTableVersion(TableVersion version);

    List<TableVersion> listTableVersions();

    void addFilteringHook(FilteringHook hook);

    void removeFilteringHook(long hookRef);

    List<FilteringHook> listFilteringHooks();
}
