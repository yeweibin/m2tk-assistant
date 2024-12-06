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

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.api.M2TKDatabase;
import m2tk.assistant.api.Tracer;
import m2tk.assistant.api.domain.FilteringHook;
import m2tk.assistant.api.domain.StreamSource;
import m2tk.mpeg2.decoder.SectionDecoder;
import m2tk.multiplex.TSDemux;
import m2tk.multiplex.TSDemuxPayload;
import org.pf4j.Extension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Extension
public class UserPrivateSectionTracer implements Tracer
{
    private final List<FilteringHook> hooks;
    private final Set<Integer> targetStreams;
    private final SectionDecoder sec;
    private final int[] secCounts;
    private int limitPerStream;

    private M2TKDatabase databaseService;

    public UserPrivateSectionTracer()
    {
        hooks = new ArrayList<>();
        targetStreams = new HashSet<>();
        sec = new SectionDecoder();
        secCounts = new int[8192];
    }

    @Override
    public void configure(StreamSource source, TSDemux demux, M2TKDatabase database)
    {
        databaseService = database;

        hooks.clear();
        targetStreams.clear();

        List<FilteringHook> allHooks = database.listFilteringHooks(source.getUri());
        for (FilteringHook hook : allHooks)
        {
            if (StrUtil.equalsIgnoreCase(hook.getSubjectType(), "section"))
            {
                hooks.add(hook);
                targetStreams.add(hook.getSubjectPid());
            }
        }
        for (Integer pid : targetStreams)
            demux.registerSectionChannel(pid, this::processSection);

        String limit = database.getPreference("filtering.section.limit-per-stream", "1000");
        limitPerStream = Math.max(Math.min(Integer.parseInt(limit), 1000), 0);
    }

    private void processSection(TSDemuxPayload payload)
    {
        if (payload.getType() != TSDemuxPayload.Type.SECTION ||
            !sec.isAttachable(payload.getEncoding()))
            return;

        sec.attach(payload.getEncoding());
        int pid = payload.getStreamPID();
        int tableId = sec.getTableID();

        boolean filterable = false;
        for (FilteringHook hook : hooks)
        {
            if ((hook.getSubjectPid() == pid) &&
                (hook.getSubjectTableId() == tableId || hook.getSubjectTableId() == -1))
            {
                filterable = true;
                break;
            }
        }

        if (filterable)
        {
            if (secCounts[pid] >= limitPerStream)
            {
                databaseService.removePrivateSections("UserPrivate", pid, limitPerStream / 2);
                secCounts[pid] = 0;
                log.info("过滤记录达到上限，丢弃位于流 {} 上的私有分段记录：{} 条", pid, limitPerStream / 2);
            }

            databaseService.addPrivateSection("UserPrivate",
                                              payload.getStreamPID(),
                                              payload.getFinishPacketCounter(),
                                              payload.getEncoding().getBytes());
            secCounts[pid] += 1;
        }
    }
}
