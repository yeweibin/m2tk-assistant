package m2tk.assistant.dbi.handler;

import cn.hutool.core.lang.generator.Generator;
import m2tk.assistant.dbi.entity.SourceEntity;
import m2tk.assistant.dbi.mapper.SourceEntityMapper;
import org.jdbi.v3.core.Handle;

public class SourceHandler
{
    private final Generator<Long> idGenerator;
    private final SourceEntityMapper sourceEntityMapper;

    public SourceHandler(Generator<Long> generator)
    {
        idGenerator = generator;
        sourceEntityMapper = new SourceEntityMapper();
    }

    public void initTable(Handle handle)
    {
        handle.execute("DROP TABLE IF EXISTS `T_SOURCE`");
        handle.execute("CREATE TABLE `T_SOURCE` (" +
                       "`id` BIGINT PRIMARY KEY," +
                       "`transaction_id` BIGINT NOT NULL," +
                       "`bitrate` INT DEFAULT 0," +
                       "`frame_size` INT DEFAULT 188," +
                       "`transport_stream_id` INT DEFAULT 0," +
                       "`packet_count` BIGINT DEFAULT 0," +
                       "`source_name` VARCHAR(500)" +
                       ")");
    }

    public void addSource(Handle handle, long transactionId, String name)
    {
        SourceEntity entity = new SourceEntity();
        entity.setId(idGenerator.next());
        entity.setTransactionId(transactionId);
        entity.setBitrate(0);
        entity.setFrameSize(188);
        entity.setTransportStreamId(0);
        entity.setPacketCount(0);
        entity.setSourceName(name);
        handle.execute("INSERT INTO T_SOURCE (`id`, `transaction_id`, `bitrate`, `frame_size`, " +
                       "`transport_stream_id`, `packet_count`, `source_name`) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?)",
                       entity.getId(),
                       entity.getTransactionId(),
                       entity.getBitrate(),
                       entity.getFrameSize(),
                       entity.getTransportStreamId(),
                       entity.getPacketCount(),
                       entity.getSourceName());
    }

    public SourceEntity getSource(Handle handle, long transactionId)
    {
        return handle.select("SELECT * FROM T_SOURCE WHERE `transaction_id` = ? ORDER BY `id` DESC LIMIT 1",
                             transactionId)
                     .map(sourceEntityMapper)
                     .findFirst()
                     .orElse(null);
    }

    public SourceEntity getLatestSource(Handle handle)
    {
        return handle.select("SELECT * FROM T_SOURCE ORDER BY `id` DESC LIMIT 1")
                     .map(sourceEntityMapper)
                     .findFirst()
                     .orElse(null);
    }

    public void updateSourceStatistics(Handle handle, SourceEntity entity)
    {
        handle.execute("UPDATE T_SOURCE " +
                       "SET `bitrate` = ?, " +
                       "    `frame_size` = ?," +
                       "    `packet_count` = ? " +
                       "WHERE `id` = ?",
                       entity.getBitrate(),
                       entity.getFrameSize(),
                       entity.getPacketCount(),
                       entity.getId());
    }

    public void updateSourceTransportId(Handle handle, SourceEntity entity)
    {
        handle.execute("UPDATE T_SOURCE " +
                       "SET `transport_stream_id` = ? " +
                       "WHERE `id` = ?",
                       entity.getTransportStreamId(),
                       entity.getId());
    }
}
