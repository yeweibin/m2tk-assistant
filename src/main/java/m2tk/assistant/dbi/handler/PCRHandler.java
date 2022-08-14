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

package m2tk.assistant.dbi.handler;

import cn.hutool.core.lang.generator.Generator;
import m2tk.assistant.dbi.entity.PCRCheckEntity;
import m2tk.assistant.dbi.entity.PCREntity;
import m2tk.assistant.dbi.entity.PCRStatEntity;
import m2tk.assistant.dbi.mapper.PCRCheckEntityMapper;
import m2tk.assistant.dbi.mapper.PCREntityMapper;
import m2tk.assistant.dbi.mapper.PCRStatEntityMapper;
import org.jdbi.v3.core.Handle;

import java.util.List;

public class PCRHandler
{
    private final Generator<Long> idGenerator;
    private final PCREntityMapper pcrEntityMapper;
    private final PCRCheckEntityMapper checkEntityMapper;
    private final PCRStatEntityMapper statEntityMapper;

    public PCRHandler(Generator<Long> generator)
    {
        idGenerator = generator;
        pcrEntityMapper = new PCREntityMapper();
        checkEntityMapper = new PCRCheckEntityMapper();
        statEntityMapper = new PCRStatEntityMapper();
    }

    public void initTable(Handle handle)
    {
        handle.execute("DROP TABLE IF EXISTS `T_PCR`");
        handle.execute("CREATE TABLE `T_PCR` (" +
                       "`id` BIGINT PRIMARY KEY," +
                       "`pid` INT NOT NULL," +
                       "`pct` BIGINT DEFAULT 0," +
                       "`value` BIGINT DEFAULT 0" +
                       ")");

        handle.execute("DROP TABLE IF EXISTS `T_PCR_CHECK`");
        handle.execute("CREATE TABLE `T_PCR_CHECK` (" +
                       "`id` BIGINT PRIMARY KEY," +
                       "`pid` INT NOT NULL," +
                       "`pct` BIGINT DEFAULT 0," +
                       "`interval_ns` BIGINT DEFAULT 0," +
                       "`diff_ns` BIGINT DEFAULT 0," +
                       "`accuracy_ns` BIGINT DEFAULT 0," +
                       "`repetition_check_failed` BOOLEAN DEFAULT FALSE," +
                       "`discontinuity_check_failed` BOOLEAN DEFAULT FALSE," +
                       "`accuracy_check_failed` BOOLEAN DEFAULT FALSE" +
                       ")");
    }

    public void resetTable(Handle handle)
    {
        handle.execute("TRUNCATE TABLE T_PCR");
        handle.execute("TRUNCATE TABLE T_PCR_CHECK");
    }

    public void addPCR(Handle handle, int pid, long position, long value)
    {
        handle.execute("INSERT INTO T_PCR(`id`, `pid`, `pct`, `value`) VALUES (?,?,?,?)",
                       idGenerator.next(), pid, position, value);
    }

    public void addPCRCheck(Handle handle, int pid, long position,
                            long interval, long diff, long accuracy,
                            boolean repetitionCheckFailed,
                            boolean discontinuityCheckFailed,
                            boolean accuracyCheckFailed)
    {
        handle.execute("INSERT INTO T_PCR_CHECK(`id`, `pid`, `pct`, " +
                       "`interval_ns`, `diff_ns`, `accuracy_ns`, " +
                       "`repetition_check_failed`, `discontinuity_check_failed`, `accuracy_check_failed`) " +
                       "VALUES (?,?,?,?,?,?,?,?,?)",
                       idGenerator.next(), pid, position,
                       interval, diff, accuracy,
                       repetitionCheckFailed,
                       discontinuityCheckFailed,
                       accuracyCheckFailed);
    }

    public List<PCREntity> listPCRs(Handle handle, int pid)
    {
        return handle.select("SELECT * FROM T_PCR WHERE `pid` = ? ORDER BY `pid`", pid)
                     .map(pcrEntityMapper)
                     .list();
    }

    public List<PCRCheckEntity> listPCRChecks(Handle handle, int pid)
    {
        return handle.select("SELECT * FROM T_PCR_CHECK WHERE `pid` = ? ORDER BY `pid`", pid)
                     .map(checkEntityMapper)
                     .list();
    }

    public List<PCRStatEntity> listPCRStats(Handle handle)
    {
        return handle.select("SELECT A.*, B.* FROM " +
                             "  (SELECT `pid`, COUNT(`id`) AS `pcr_count` FROM T_PCR GROUP BY `pid`) A " +
                             "LEFT JOIN " +
                             "  (SELECT `pid`, " +
                             "      MAX(`interval_ns`) AS `max_interval`, " +
                             "      MIN(`interval_ns`) AS `min_interval`, " +
                             "      AVG(`interval_ns`) AS `avg_interval`, " +
                             "      MAX(`accuracy_ns`) AS `max_accuracy`, " +
                             "      MIN(`accuracy_ns`) AS `min_accuracy`, " +
                             "      AVG(`accuracy_ns`) AS `avg_accuracy`, " +
                             "      SUM(CASE `repetition_check_failed` WHEN TRUE THEN 1 ELSE 0 END) AS `repetition_errors`, " +
                             "      SUM(CASE `discontinuity_check_failed` WHEN TRUE THEN 1 ELSE 0 END) AS `discontinuity_errors`, " +
                             "      SUM(CASE `accuracy_check_failed` WHEN TRUE THEN 1 ELSE 0 END) AS `accuracy_errors` " +
                             "  FROM T_PCR_CHECK " +
                             "  GROUP BY `pid`) B " +
                             "ON A.pid = B.pid " +
                             "ORDER BY A.pid")
                     .map(statEntityMapper)
                     .list();
    }
}
