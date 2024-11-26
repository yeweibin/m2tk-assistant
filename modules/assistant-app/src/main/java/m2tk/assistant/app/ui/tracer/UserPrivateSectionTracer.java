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

package m2tk.assistant.app.ui.tracer;

import m2tk.assistant.api.M2TKDatabase;
import m2tk.assistant.api.Tracer;
import m2tk.assistant.api.domain.StreamSource;
import m2tk.mpeg2.decoder.SectionDecoder;
import m2tk.multiplex.TSDemux;
import m2tk.multiplex.TSDemuxPayload;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

//@Extension
public class UserPrivateSectionTracer implements Tracer
{
    private static final Logger log = LoggerFactory.getLogger(UserPrivateSectionTracer.class);

    private final Set<Integer> targetStreams;
    private final SectionDecoder sec;
    private final int limitPerStream;
    private final int[] secCounts;

    private M2TKDatabase databaseService;

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
            databaseService.removePrivateSections("UserPrivate", pid);
            secCounts[pid] = 0;
            log.info("丢弃位于流 {} 上的 {} 个私有分段记录", pid, drops);
        }

        databaseService.addPrivateSection("UserPrivate",
                                          payload.getStreamPID(),
                                          payload.getFinishPacketCounter(),
                                          payload.getEncoding().getBytes());

        secCounts[pid] += 1;
    }
}
