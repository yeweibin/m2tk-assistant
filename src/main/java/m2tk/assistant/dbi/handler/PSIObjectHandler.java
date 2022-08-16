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
import m2tk.assistant.dbi.entity.CAStreamEntity;
import m2tk.assistant.dbi.entity.ProgramEntity;
import m2tk.assistant.dbi.entity.ProgramStreamMappingEntity;
import m2tk.assistant.dbi.mapper.CAStreamEntityMapper;
import m2tk.assistant.dbi.mapper.ProgramEntityMapper;
import m2tk.assistant.dbi.mapper.ProgramStreamMappingEntityMapper;
import org.jdbi.v3.core.Handle;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

public class PSIObjectHandler
{
    private final Generator<Long> idGenerator;
    private final ProgramEntityMapper programEntityMapper;
    private final ProgramStreamMappingEntityMapper mappingEntityMapper;
    private final CAStreamEntityMapper streamEntityMapper;

    public PSIObjectHandler(Generator<Long> generator)
    {
        idGenerator = generator;
        programEntityMapper = new ProgramEntityMapper();
        mappingEntityMapper = new ProgramStreamMappingEntityMapper();
        streamEntityMapper = new CAStreamEntityMapper();
    }

    ///////////////////////////////////////////////////////////////
    // 所有数据库操作都是串行的（单线程），所以不用考虑竞争问题

    public void initTable(Handle handle)
    {
        handle.execute("DROP TABLE IF EXISTS `T_PROGRAM`");
        handle.execute("CREATE TABLE `T_PROGRAM` (" +
                       "`id` BIGINT PRIMARY KEY," +
                       "`ts_id` INT NOT NULL," +
                       "`prg_num` INT NOT NULL," +
                       "`pmt_pid` INT NOT NULL," +
                       "`pcr_pid` INT DEFAULT 8191 NOT NULL," +
                       "`pmt_version` INT DEFAULT -1 NOT NULL," +
                       "`free_access` BOOLEAN DEFAULT TRUE NOT NULL," +
                       "`prg_name` VARCHAR(100)" +
                       ")");

        handle.execute("DROP TABLE IF EXISTS `T_PROGRAM_STREAM_MAPPING`");
        handle.execute("CREATE TABLE `T_PROGRAM_STREAM_MAPPING` (" +
                       "`id` BIGINT PRIMARY KEY," +
                       "`prg_num` INT NOT NULL," +
                       "`es_pid` INT NOT NULL," +
                       "`es_type` INT NOT NULL," +
                       "`es_cate` VARCHAR(3) NOT NULL," +
                       "`es_desc` VARCHAR(100) NOT NULL" +
                       ")");

        handle.execute("DROP TABLE IF EXISTS `T_CA_STREAM`");
        handle.execute("CREATE TABLE `T_CA_STREAM` (" +
                       "`id` BIGINT PRIMARY KEY," +
                       "`cas_id` INT NOT NULL," +
                       "`stream_type` INT NOT NULL," +
                       "`stream_pid` INT NOT NULL," +
                       "`stream_private_data` VARBINARY(255)," +
                       "`program_number` INT," +
                       "`es_pid` INT" +
                       ")");
    }

    public void resetTable(Handle handle)
    {
        handle.execute("TRUNCATE TABLE T_PROGRAM");
        handle.execute("TRUNCATE TABLE T_PROGRAM_STREAM_MAPPING");
        handle.execute("TRUNCATE TABLE T_CA_STREAM");
    }

    public ProgramEntity addProgram(Handle handle, int tsid, int number, int pmtpid)
    {
        ProgramEntity entity = new ProgramEntity();
        entity.setId(idGenerator.next());
        entity.setTransportStreamId(tsid);
        entity.setProgramNumber(number);
        entity.setPmtPid(pmtpid);
        entity.setPcrPid(8191);
        entity.setPmtVersion(-1);
        entity.setFreeAccess(true);
        handle.execute("INSERT INTO T_PROGRAM (`id`, `ts_id`, `prg_num`, `pmt_pid`, `pcr_pid`, `pmt_version`, `free_access`) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?)",
                       entity.getId(),
                       entity.getTransportStreamId(),
                       entity.getProgramNumber(),
                       entity.getPmtPid(),
                       entity.getPcrPid(),
                       entity.getPmtVersion(),
                       entity.isFreeAccess());
        return entity;
    }

    public void updateProgram(Handle handle, ProgramEntity entity)
    {
        handle.execute("UPDATE T_PROGRAM " +
                       "SET `pcr_pid` = ?, " +
                       "    `pmt_version` = ?, " +
                       "    `free_access` = ?, " +
                       "    `prg_name` = ? " +
                       "WHERE `id` = ?",
                       entity.getPcrPid(),
                       entity.getPmtVersion(),
                       entity.isFreeAccess(),
                       entity.getProgramName(),
                       entity.getId());
    }

    public void updateProgramName(Handle handle, int programNumber, int transportStreamId, String programName)
    {
        handle.execute("UPDATE T_PROGRAM SET `prg_name` = ? WHERE `prg_num` = ? AND `ts_id` = ?",
                       programName, programNumber, transportStreamId);
    }

    public void addProgramStreamMapping(Handle handle, int program, int pid, int type, String category, String description)
    {
        handle.execute("INSERT INTO T_PROGRAM_STREAM_MAPPING (`id`, `prg_num`, `es_pid`, `es_type`, `es_cate`, `es_desc`) " +
                       "VALUES (?, ?, ?, ?, ?, ?)",
                       idGenerator.next(), program, pid, type, category, description);
    }

    public List<ProgramEntity> listPrograms(Handle handle)
    {
        return handle.select("SELECT * FROM T_PROGRAM ORDER BY `ts_id`, `prg_num`")
                     .map(programEntityMapper)
                     .list();
    }

    public List<ProgramStreamMappingEntity> listProgramStreamMappings(Handle handle, int number)
    {
        return handle.select("SELECT * FROM T_PROGRAM_STREAM_MAPPING WHERE `prg_num` = ?", number)
                     .map(mappingEntityMapper)
                     .list();
    }

    public ProgramEntity getProgram(Handle handle, int number)
    {
        return handle.select("SELECT * FROM T_PROGRAM WHERE `prg_num` = ?", number)
                     .map(programEntityMapper)
                     .one();
    }

    public CAStreamEntity addEMMStream(Handle handle, int systemId, int emmPid, byte[] emmPrivateData)
    {
        CAStreamEntity entity = new CAStreamEntity();
        entity.setId(idGenerator.next());
        entity.setSystemId(systemId);
        entity.setStreamPid(emmPid);
        entity.setStreamPrivateData(emmPrivateData);
        entity.setStreamType(CAStreamEntity.TYPE_EMM);
        entity.setProgramNumber(0);
        entity.setElementaryStreamPid(8191);

        handle.execute("INSERT INTO T_CA_STREAM (`id`, `cas_id`, `stream_type`, `stream_pid`, `stream_private_data`, `program_number`, `es_pid`) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?)",
                       entity.getId(),
                       entity.getSystemId(),
                       entity.getStreamType(),
                       entity.getStreamPid(),
                       entity.getStreamPrivateData(),
                       entity.getProgramNumber(),
                       entity.getElementaryStreamPid());
        return entity;
    }

    public CAStreamEntity addECMStream(Handle handle, int systemId, int ecmPid, byte[] ecmPrivateData,
                                       int programNumber, int esPid)
    {
        CAStreamEntity entity = new CAStreamEntity();
        entity.setId(idGenerator.next());
        entity.setSystemId(systemId);
        entity.setStreamPid(ecmPid);
        entity.setStreamPrivateData(ecmPrivateData);
        entity.setStreamType(CAStreamEntity.TYPE_ECM);
        entity.setProgramNumber(programNumber);
        entity.setElementaryStreamPid(esPid);

        handle.execute("INSERT INTO T_CA_STREAM (`id`, `cas_id`, `stream_type`, `stream_pid`, `stream_private_data`, `program_number`, `es_pid`) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?)",
                       entity.getId(),
                       entity.getSystemId(),
                       entity.getStreamType(),
                       entity.getStreamPid(),
                       entity.getStreamPrivateData(),
                       entity.getProgramNumber(),
                       entity.getElementaryStreamPid());
        return entity;
    }

    public List<CAStreamEntity> listCAStreams(Handle handle)
    {
        return handle.select("SELECT * FROM T_CA_STREAM ORDER BY `cas_id`, `stream_type`, `stream_pid`")
                     .map(streamEntityMapper)
                     .list();
    }

    public List<CAStreamEntity> listProgramECMStreams(Handle handle, int programNumber)
    {
        return handle.select("SELECT * FROM T_CA_STREAM WHERE `program_number` = ? AND `stream_type` = ? ORDER BY `cas_id`, `stream_pid`",
                             programNumber, CAStreamEntity.TYPE_ECM)
                     .map(streamEntityMapper)
                     .list();
    }

    public List<CAStreamEntity> listECMStreams(Handle handle)
    {
        return handle.select("SELECT * FROM T_CA_STREAM WHERE `stream_type` = ? ORDER BY `cas_id`, `stream_pid`",
                             CAStreamEntity.TYPE_ECM)
                     .map(streamEntityMapper)
                     .list();
    }

    public Map<Integer, List<CAStreamEntity>> listECMGroups(Handle handle)
    {
        return handle.select("SELECT * FROM T_CA_STREAM WHERE `stream_type` = ? ORDER BY `cas_id`, `stream_pid`",
                             CAStreamEntity.TYPE_ECM)
                     .map(streamEntityMapper)
                     .collect(groupingBy(CAStreamEntity::getProgramNumber));
    }

    public List<CAStreamEntity> listEMMStreams(Handle handle)
    {
        return handle.select("SELECT * FROM T_CA_STREAM WHERE `stream_type` = ? ORDER BY `cas_id`, `stream_pid`",
                             CAStreamEntity.TYPE_EMM)
                     .map(streamEntityMapper)
                     .list();
    }
}
