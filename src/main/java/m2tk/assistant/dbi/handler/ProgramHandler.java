package m2tk.assistant.dbi.handler;

import cn.hutool.core.lang.generator.Generator;
import m2tk.assistant.dbi.entity.ProgramEntity;
import m2tk.assistant.dbi.entity.ProgramStreamMappingEntity;
import m2tk.assistant.dbi.mapper.ProgramEntityMapper;
import m2tk.assistant.dbi.mapper.ProgramStreamMappingEntityMapper;
import org.jdbi.v3.core.Handle;

import java.util.*;

public class ProgramHandler
{
    private final Generator<Long> idGenerator;
    private final ProgramEntityMapper programEntityMapper;
    private final ProgramStreamMappingEntityMapper mappingEntityMapper;

    public ProgramHandler(Generator<Long> generator)
    {
        idGenerator = generator;
        programEntityMapper = new ProgramEntityMapper();
        mappingEntityMapper = new ProgramStreamMappingEntityMapper();
    }

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
    }

    public void resetTable(Handle handle)
    {
        handle.execute("TRUNCATE TABLE T_PROGRAM");
        handle.execute("TRUNCATE TABLE T_PROGRAM_STREAM_MAPPING");
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
}
