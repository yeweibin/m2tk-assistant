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

import m2tk.assistant.dbi.DatabaseService;
import m2tk.mpeg2.decoder.SectionDecoder;
import m2tk.multiplex.TSDemux;
import m2tk.multiplex.TSDemuxPayload;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class UserPrivateSectionTracer implements Tracer
{
    private final DatabaseService databaseService;
    private final long transactionId;

    private final Set<Integer> targetStreams;
    private final SectionDecoder sec;
    private final int limitPerStream;
    private final int[] secCounts;

    public UserPrivateSectionTracer(DatabaseService service, long transaction,
                                    Collection<Integer> streams, int maxSectionCountPerStream)
    {
        databaseService = service;
        transactionId = transaction;

        targetStreams = new HashSet<>(streams);
        limitPerStream = maxSectionCountPerStream;
        sec = new SectionDecoder();
        secCounts = new int[8192];
    }

    @Override
    public void configureDemux(TSDemux demux)
    {
        for (Integer pid : targetStreams)
        {
            demux.registerSectionChannel(pid, this::processSection);
        }
    }

    private void processSection(TSDemuxPayload payload)
    {
        if (!targetStreams.contains(payload.getStreamPID()) ||
            payload.getType() != TSDemuxPayload.Type.SECTION ||
            !sec.isAttachable(payload.getEncoding()))
            return;

        if (secCounts[payload.getStreamPID()] < limitPerStream)
        {
            databaseService.addSection(transactionId,
                                         "user-private",
                                       payload.getStreamPID(),
                                       payload.getFinishPacketCounter(),
                                       payload.getEncoding().getBytes());
            secCounts[payload.getStreamPID()] += 1;
        }
    }
}
