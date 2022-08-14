package m2tk.assistant.dbi.handler;


import cn.hutool.core.lang.generator.Generator;
import m2tk.assistant.analyzer.presets.StreamTypes;
import m2tk.assistant.dbi.entity.StreamEntity;
import m2tk.assistant.dbi.mapper.StreamEntityMapper;
import org.jdbi.v3.core.Handle;

import java.util.List;

public class StreamHandler
{
    private final Generator<Long> idGenerator;
    private final StreamEntityMapper streamEntityMapper;

    public StreamHandler(Generator<Long> generator)
    {
        idGenerator = generator;
        streamEntityMapper = new StreamEntityMapper();
    }

    public void initTable(Handle handle)
    {
        handle.execute("DROP TABLE IF EXISTS `T_STREAM`");
        handle.execute("CREATE TABLE `T_STREAM` (" +
                       "`id` BIGINT PRIMARY KEY," +
                       "`pid` INT NOT NULL," +
                       "`pkt_cnt` BIGINT DEFAULT 0," +
                       "`pcr_cnt` INT DEFAULT 0," +
                       "`trans_err_cnt` INT DEFAULT 0," +
                       "`cc_err_cnt` INT DEFAULT 0," +
                       "`bitrate` INT DEFAULT 0," +
                       "`ratio` DOUBLE PRECISION DEFAULT 0.0," +
                       "`category` VARCHAR(3)," +
                       "`description` VARCHAR(100)," +
                       "`marked` BOOLEAN DEFAULT FALSE," +
                       "`scrambled` BOOLEAN DEFAULT FALSE" +
                       ")");

        for (int pid = 0; pid < 8191; pid++)
            handle.execute("INSERT INTO T_STREAM (`id`, `pid`, `category`, `description`) VALUES (?, ?, ?, ?)",
                           idGenerator.next(), pid, StreamTypes.CATEGORY_USER_PRIVATE, "私有数据");
        handle.execute("INSERT INTO T_STREAM (`id`, `pid`, `category`, `description`) VALUES (?, ?, ?, ?)",
                       idGenerator.next(), 8191, StreamTypes.CATEGORY_USER_PRIVATE, "空包");
    }

    public void resetTable(Handle handle)
    {
        handle.execute("TRUNCATE TABLE T_STREAM");
        for (int pid = 0; pid < 8191; pid++)
            handle.execute("INSERT INTO T_STREAM (`id`, `pid`, `category`, `description`) VALUES (?, ?, ?, ?)",
                           idGenerator.next(), pid, StreamTypes.CATEGORY_USER_PRIVATE, "私有数据");
        handle.execute("INSERT INTO T_STREAM (`id`, `pid`, `category`, `description`) VALUES (?, ?, ?, ?)",
                       idGenerator.next(), 8191, StreamTypes.CATEGORY_USER_PRIVATE, "空包");

        handle.execute("UPDATE T_STREAM SET `marked` = TRUE WHERE `pid` = 0");
    }

    public StreamEntity getStream(Handle handle, int pid)
    {
        return handle.select("SELECT * FROM T_STREAM WHERE `pid` = ?", pid)
                     .map(streamEntityMapper)
                     .one();
    }

    public void updateStreamStatistics(Handle handle, StreamEntity entity)
    {
        handle.execute("UPDATE T_STREAM " +
                       "SET `ratio` = ?, " +
                       "    `bitrate` = ?, " +
                       "    `pkt_cnt` = ?, " +
                       "    `pcr_cnt` = ?, " +
                       "    `scrambled` = ? " +
                       "WHERE `pid` = ?",
                       entity.getRatio(),
                       entity.getBitrate(),
                       entity.getPacketCount(),
                       entity.getPcrCount(),
                       entity.isScrambled(),
                       entity.getPid());
    }

    public void updateStreamUsage(Handle handle, int pid, String category, String description)
    {
        handle.execute("UPDATE T_STREAM " +
                       "SET `category` = ?, " +
                       "    `description` = ? " +
                       "WHERE `pid` = ?",
                       category,
                       description,
                       pid);
    }

    public void cumsumStreamErrorCounts(Handle handle, int pid, long transportErrors, long continuityErrors)
    {
        handle.execute("UPDATE T_STREAM " +
                       "    SET `trans_err_cnt` = `trans_err_cnt` + ?, " +
                       "        `cc_err_cnt` = `cc_err_cnt` + ? " +
                       "WHERE `pid` = ?",
                       transportErrors, continuityErrors, pid);
    }

    public void setStreamMarked(Handle handle, int pid, boolean marked)
    {
        handle.execute("UPDATE T_STREAM SET `marked` = ? WHERE `pid` = ?", marked, pid);
    }

    public List<StreamEntity> listPresentStreams(Handle handle)
    {
        return handle.select("SELECT * FROM T_STREAM WHERE `pkt_cnt` > 0 ORDER BY `pid`")
                     .map(streamEntityMapper)
                     .list();
    }
}
