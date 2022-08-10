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

import m2tk.assistant.dbi.DatabaseService;
import m2tk.assistant.dbi.entity.TR290EventEntity;
import m2tk.mpeg2.decoder.TransportPacketDecoder;
import m2tk.mpeg2.decoder.element.AdaptationFieldDecoder;
import m2tk.mpeg2.decoder.element.ProgramClockReferenceDecoder;
import m2tk.multiplex.TSDemuxPayload;
import m2tk.multiplex.TSState;
import m2tk.multiplex.TransportStatus;

public class TR290Tracer
{
    private final DatabaseService databaseService;
    private final int[] CCTs;
    private final TransportPacketDecoder pkt;
    private final AdaptationFieldDecoder adpt;
    private final ProgramClockReferenceDecoder pcr;

    public TR290Tracer(DatabaseService service)
    {
        databaseService = service;
        CCTs = new int[8192];
        pkt = new TransportPacketDecoder();
        adpt = new AdaptationFieldDecoder();
        pcr = new ProgramClockReferenceDecoder();
    }

    public void processTransportStatus(TransportStatus status)
    {
        if (status.getCurrentState() == TSState.SYNC_BYTE_ERROR)
            databaseService.addTR290Event(1,
                                          TR290EventEntity.TC_SYNC_BYTE_ERROR,
                                          "同步字节错误",
                                          status.getPosition(),
                                          status.getPid());
        if (status.getCurrentState() == TSState.SYNC_LOST)
            databaseService.addTR290Event(1,
                                          TR290EventEntity.TC_TS_SYNC_LOSS,
                                          "同步丢失错误",
                                          status.getPosition(),
                                          status.getPid());
    }

    public void processTransportPacket(TSDemuxPayload payload)
    {
        pkt.attach(payload.getEncoding());
        if (pkt.containsTransportError())
            databaseService.addTR290Event(2,
                                          TR290EventEntity.TC_TRANSPORT_ERROR,
                                          "当前流指示传输错误",
                                          payload.getStartPacketCounter(),
                                          payload.getStreamPID());
    }
}
