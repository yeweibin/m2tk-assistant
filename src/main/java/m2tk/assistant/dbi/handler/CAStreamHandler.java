package m2tk.assistant.dbi.handler;

import cn.hutool.core.lang.generator.Generator;
import m2tk.assistant.dbi.entity.CAStreamEntity;
import m2tk.assistant.dbi.entity.ProgramEntity;
import m2tk.assistant.dbi.entity.ProgramStreamMappingEntity;
import m2tk.assistant.dbi.mapper.CAStreamEntityMapper;
import org.jdbi.v3.core.Handle;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

public class CAStreamHandler
{
    private final Generator<Long> idGenerator;
    private final CAStreamEntityMapper streamEntityMapper;

    public CAStreamHandler(Generator<Long> generator)
    {
        idGenerator = generator;
        streamEntityMapper = new CAStreamEntityMapper();
    }

    public void initTable(Handle handle)
    {
        handle.execute("DROP TABLE IF EXISTS `T_CA_STREAM`");
        handle.execute("CREATE TABLE `T_CA_STREAM` (" +
                       "`id` BIGINT PRIMARY KEY," +
                       "`cas_id` INT NOT NULL," +
                       "`stream_type` INT NOT NULL," +
                       "`stream_pid` INT NOT NULL," +
                       "`stream_private_data` VARBINARY(255)," +
                       "`program_number` INT DEFAULT 0," +
                       "`es_pid` INT DEFAULT 8191" +
                       ")");
    }

    public void resetTable(Handle handle)
    {
        handle.execute("TRUNCATE TABLE T_CA_STREAM");
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
