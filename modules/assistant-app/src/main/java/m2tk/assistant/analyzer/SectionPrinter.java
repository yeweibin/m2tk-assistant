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

package m2tk.assistant.analyzer;

import m2tk.assistant.dbi.DatabaseService;
import m2tk.assistant.dbi.entity.SectionEntity;
import m2tk.multiplex.DemuxStatus;
import m2tk.util.Bytes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class SectionPrinter implements Consumer<DemuxStatus>
{
    private final DatabaseService databaseService;
    private final long transactionId;

    public SectionPrinter(DatabaseService service, long transaction)
    {
        databaseService = Objects.requireNonNull(service);
        transactionId = transaction;
    }

    @Override
    public void accept(DemuxStatus status)
    {
        if (status.isRunning())
            return;

        printSections("PAT");
        printSections("CAT");
        printSections("PMT");
        printSections("NIT_Actual");
        printSections("NIT_Other");
        printSections("BAT");
        printSections("SDT_Actual");
        printSections("SDT_Other");
        printSections("EIT_PF_Actual");
        printSections("EIT_PF_Other");
        printSections("EIT_Schedule_Actual");
        printSections("EIT_Schedule_Other");
        printSections("TDT");
        printSections("EMM");
    }

    private void printSections(String name)
    {
        Map<Integer, List<SectionEntity>> sectionGroups = databaseService.getSectionGroups(transactionId, name)
                                                                         .stream()
                                                                         .collect(groupingBy(SectionEntity::getStream,
                                                                                             toList()));
        List<Integer> pids = new ArrayList<>(sectionGroups.keySet());
        pids.sort(Integer::compare);
        for (Integer pid : pids)
        {
            List<SectionEntity> sections = sectionGroups.get(pid);
            System.out.printf("%s(PID: %d)> Total: %d%n", name, pid, sections.size());
            for (SectionEntity section : sections)
                System.out.println("  => " + Bytes.toHexString(section.getEncoding()));
            System.out.println();
        }
    }
}
