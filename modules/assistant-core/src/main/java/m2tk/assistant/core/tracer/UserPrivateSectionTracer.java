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
import m2tk.assistant.core.domain.PrivateSection;
import m2tk.assistant.core.domain.StreamSource;
import m2tk.mpeg2.decoder.SectionDecoder;
import m2tk.multiplex.TSDemux;
import m2tk.multiplex.TSDemuxPayload;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class UserPrivateSectionTracer implements Tracer
{
    private final Set<Integer> targetStreams;
    private final SectionDecoder sec;
    private final int limitPerStream;
    private final int[] secCounts;

    private M2TKDatabase databaseService;
    private long transactionId;

    public UserPrivateSectionTracer(Collection<Integer> streams, int maxSectionCountPerStream)
    {
        targetStreams = new HashSet<>(streams);
        limitPerStream = maxSectionCountPerStream;
        sec = new SectionDecoder();
        secCounts = new int[8192];
    }

    @Override
    public void configure(StreamSource source, TSDemux demux, M2TKDatabase database)
    {
        databaseService = database;
        transactionId = source.getTransactionId();

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

        int pid = payload.getStreamPID();
        if (secCounts[pid] >= limitPerStream)
        {
            int drops = (int) (limitPerStream * 0.25);
            drops = databaseService.removePrivateSections(transactionId, pid, "UserPrivate", drops);
            secCounts[pid] -= drops;
            log.info("丢弃位于流 {} 上的 {} 个私有分段记录", pid, drops);
        }

        PrivateSection section = new PrivateSection();
        section.setRef(-1);
        section.setTransactionId(transactionId);
        section.setPid(pid);
        section.setTag("UserPrivate");
        section.setPosition(payload.getFinishPacketCounter());
        section.setEncoding(payload.getEncoding().getBytes());
        databaseService.addPrivateSection(section);

        secCounts[pid] += 1;
    }
}
