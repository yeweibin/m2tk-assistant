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

package m2tk.assistant.dbi.handler;

import cn.hutool.core.lang.generator.Generator;
import m2tk.assistant.dbi.entity.TR290EventEntity;
import m2tk.assistant.dbi.entity.TR290StatEntity;
import m2tk.assistant.dbi.mapper.TR290EventEntityMapper;
import m2tk.assistant.dbi.mapper.TR290StatEntityMapper;
import org.jdbi.v3.core.Handle;

import java.time.LocalDateTime;
import java.util.List;

public class TR290EventHandler
{
    private final Generator<Long> idGenerator;
    private final TR290EventEntityMapper eventEntityMapper;
    private final TR290StatEntityMapper statsEntityMapper;

    public TR290EventHandler(Generator<Long> generator)
    {
        idGenerator = generator;
        eventEntityMapper = new TR290EventEntityMapper();
        statsEntityMapper = new TR290StatEntityMapper();
    }

    public void initTable(Handle handle)
    {
        handle.execute("DROP TABLE IF EXISTS `T_TR290_EVENT`");
        handle.execute("CREATE TABLE `T_TR290_EVENT` (" +
                       "`id` BIGINT PRIMARY KEY," +
                       "`transaction_id` BIGINT NOT NULL," +
                       "`type` VARCHAR(20) NOT NULL," +
                       "`description` VARCHAR(1000) NOT NULL," +
                       "`stream_pid` INT NOT NULL," +
                       "`position` BIGINT NOT NULL," +
                       "`timestamp` DATETIME NOT NULL" +
                       ")");
    }

    public void addTR290Event(Handle handle,
                              long transactionId,
                              LocalDateTime timestamp,
                              String type,
                              String description,
                              long position,
                              int pid)
    {
        handle.execute("INSERT INTO T_TR290_EVENT (`id`, `transaction_id`, `timestamp`, `type`, `description`, " +
                       "`position`, `stream_pid`) VALUES (?,?,?,?,?,?,?)",
                       idGenerator.next(), transactionId, timestamp, type, description, position, pid);
    }

    public List<TR290EventEntity> listEvents(Handle handle, long transactionId, long start, int count)
    {
        return handle.select("SELECT * FROM T_TR290_EVENT WHERE `transaction_id`= ? AND `id` > ? ORDER BY `id` " +
                             "FETCH FIRST ? ROWS ONLY",
                             transactionId, start, count)
                     .map(eventEntityMapper)
                     .list();
    }

    public List<TR290EventEntity> listEvents(Handle handle, long transactionId, String type, int count)
    {
        return handle.select("SELECT * FROM T_TR290_EVENT WHERE `transaction_id`= ? AND `type` = ? ORDER BY `id` " +
                             "FETCH FIRST ? ROWS ONLY",
                             transactionId, type, count)
                     .map(eventEntityMapper)
                     .list();
    }

    public List<TR290StatEntity> listStats(Handle handle, long transactionId)
    {
        return handle.select("SELECT A.`transaction_id`, " +
                             "           A.`id` AS `id`, " +
                             "           A.`type` AS `indicator`, " +
                             "           A.`timestamp` AS `timestamp`, " +
                             "           A.`description` AS `description`, " +
                             "           B.`count` AS `count` " +
                             "FROM T_TR290_EVENT A " +
                             "INNER JOIN " +
                             "  (SELECT MAX(`id`) AS `id`, COUNT(`id`) AS `count` FROM T_TR290_EVENT WHERE `transaction_id`= ? " +
                             "   GROUP BY `type`) B " +
                             "ON A.`id` = B.`id` " +
                             "ORDER BY A.`type`",
                             transactionId)
                .map(statsEntityMapper).list();
    }
}
