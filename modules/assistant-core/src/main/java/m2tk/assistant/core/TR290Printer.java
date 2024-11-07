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

package m2tk.assistant.core;

import m2tk.multiplex.DemuxStatus;

import java.util.function.Consumer;

public class TR290Printer implements Consumer<DemuxStatus>
{
    private final M2TKDatabase databaseService;
    private final long transactionId;

    public TR290Printer(M2TKDatabase database, long transaction)
    {
        databaseService = database;
        transactionId = transaction;
    }


    @Override
    public void accept(DemuxStatus status)
    {
//        if (status.isRunning())
//            return;
//
//        List<TR290Event> events = databaseService.listTR290Events(transactionId, TR290ErrorTypes.PCR_ACCURACY_ERROR, 10000);
//        System.out.println("error count: " + events.size());
//
//        for (TR290EventEntity event : events)
//        {
//            System.out.printf("%s: %s  发生于：%d, PID = %d%n",
//                              event.getType(),
//                              event.getDescription(),
//                              event.getPosition(),
//                              event.getStreamPid());
//        }
//
//        Map<String, Long> groupStat = events.stream()
//                .collect(groupingBy(e -> e.getType(), summingLong(e -> 1L)));
//
//        System.out.println();
//        System.out.println("===========================================");
//        System.out.println();
//        for (String type : groupStat.keySet())
//        {
//            System.out.printf("%s: 总计 %d%n", type, groupStat.get(type));
//        }
//
//        System.out.println();
//        System.out.println("===========================================");
//        System.out.println();
//        List<PCRStatEntity> pcrStats = databaseService.listPCRStats(transactionId);
//        for (PCRStatEntity stat : pcrStats)
//        {
//            System.out.printf("[PID %04x] PCR Cnt: %d%n", stat.getPid(), stat.getPcrCount());
//            System.out.printf("     Intervals: avg %d ms, min %d ms, max %d ms, errors %d%n",
//                              stat.getAvgInterval() / 1000000,
//                              stat.getMinInterval() / 1000000,
//                              stat.getMaxInterval() / 1000000,
//                              stat.getRepetitionErrors());
//            System.out.printf("     Accuracies: avg %d ns, min %d ns, max %d ns, errors %d%n",
//                              stat.getAvgAccuracy(),
//                              stat.getMinAccuracy(),
//                              stat.getMaxAccuracy(),
//                              stat.getAccuracyErrors());
//        }
    }
}
