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

package m2tk.assistant.core;

import m2tk.assistant.core.domain.*;

import java.util.List;
import java.util.Map;

public interface M2TKDatabase
{
    void beginTransaction(StreamSource source);

    void updateStreamSource(StreamSource source);
    StreamSource getTransactionStreamSource(long transactionId);
    StreamSource getStreamSource(long sourceRef);
    List<StreamSource> listRecentSources();

    void updateElementaryStream(ElementaryStream stream);
    void updateElementaryStreamUsage(long transactionId, int pid, String category, String description);
    void accumulateElementaryStreamErrors(long transactionId, int pid, int transportErrors, int continuityErrors);
    ElementaryStream getElementaryStream(long transactionId, int pid);
    ElementaryStream getElementaryStream(long streamRef);
    List<ElementaryStream> listElementaryStreams(long transactionId);

    void addMPEGProgram(MPEGProgram program);
    void updateMPEGProgram(MPEGProgram program);
    void clearMPEGPrograms(long transactionId);
    MPEGProgram getMPEGProgram(long programRef);
    List<MPEGProgram> listMPEGPrograms(long transactionId);

    void addProgramStreamMapping(ProgramStreamMapping mapping);
    List<ProgramStreamMapping> getProgramStreamMappings(long transactionId, int programNumber);

    void addCASystemStream(CASystemStream stream);
    void updateCASystemStream(CASystemStream stream);
    CASystemStream getCASystemStream(long streamRef);
    List<CASystemStream> listCASystemStreams(long transactionId);

    void addSIBouquet(SIBouquet bouquet);
    void updateSIBouquet(SIBouquet bouquet);
    SIBouquet getSIBouquet(long bouquetRef);
    List<SIBouquet> listSIBouquets(long transactionId);

    void addBouquetServiceMapping(BouquetServiceMapping mapping);
    List<BouquetServiceMapping> getBouquetServiceMappings(long transactionId, int bouquetId);

    void addSINetwork(SINetwork network);
    void updateSINetwork(SINetwork network);
    SINetwork getSINetwork(long networkRef);
    List<SINetwork> listSINetworks(long transactionId);

    void addSIMultiplex(SIMultiplex multiplex);
    void updateSIMultiplex(SIMultiplex multiplex);
    SIMultiplex getSIMultiplex(long multiplexRef);
    List<SIMultiplex> listSIMultiplexes(long transactionId);

    void addSIService(SIService service);
    void updateSIService(SIService service);
    SIService getSIService(long serviceRef);
    List<SIService> listSIServices(long transactionId);

    void addSIEvent(SIEvent event);
    void updateSIEvent(SIEvent event);
    SIEvent getSIEvent(long eventRef);
    List<SIEvent> listSIEvents(long transactionId);

    void addTimestamp(Timestamp timestamp);
    Timestamp getLastTimestamp(long transactionId);

    void addTR290Event(TR290Event event);
    List<TR290Event> listTR290Events(long transactionId);
    List<TR290Stats> listTR290Stats(long transactionId);

    void addPCR(PCR pcr);
    void addPCRCheck(PCRCheck check);
    List<PCRStats> listPCRStats(long transactionId);
    List<PCRCheck> getRecentPCRChecks(long transactionId, int pid, int limit);

    void addPrivateSection(PrivateSection section);
    int removePrivateSections(long transactionId, int pid, String tag, int count);
    List<PrivateSection> getPrivateSections(long transactionId, int pid, int count);
    List<PrivateSection> getPrivateSections(long transactionId, int pid, String tag, int count);
    Map<Integer, List<PrivateSection>> getPrivateSectionGroups(long transactionId, String tag);

    void addTransportPacket(TransportPacket packet);
    List<TransportPacket> getTransportPackets(long transactionId, int pid, int count);

    void addFilteringHook(FilteringHook hook);
    void removeFilteringHook(long hookRef);
    List<FilteringHook> listFilteringHooks();
}
