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
                       "`transaction_id` BIGINT NOT NULL," +
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
}

    public void initForTransaction(Handle handle, long transactionId)
    {
        for (int pid = 0; pid < 8191; pid++)
            handle.execute("INSERT INTO T_STREAM (`id`, `transaction_id`, `pid`, `category`, `description`) " +
                           "VALUES (?, ?, ?, ?, ?)",
                           idGenerator.next(), transactionId, pid, StreamTypes.CATEGORY_USER_PRIVATE, "私有数据");
        handle.execute("INSERT INTO T_STREAM (`id`, `transaction_id`, `pid`, `category`, `description`) " +
                       "VALUES (?, ?, ?, ?, ?)",
                       idGenerator.next(), transactionId, 8191, StreamTypes.CATEGORY_USER_PRIVATE, "空包");

        handle.execute("UPDATE T_STREAM SET `marked` = TRUE WHERE `pid` = 0");
    }

    public StreamEntity getStream(Handle handle, long transactionId, int pid)
    {
        return handle.select("SELECT * FROM T_STREAM WHERE `transaction_id` = ? AND `pid` = ?",
                             transactionId, pid)
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
                       "WHERE `transaction_id` = ? AND `pid` = ?",
                       entity.getRatio(),
                       entity.getBitrate(),
                       entity.getPacketCount(),
                       entity.getPcrCount(),
                       entity.isScrambled(),
                       entity.getTransactionId(),
                       entity.getPid());
    }

    public void updateStreamUsage(Handle handle, long transactionId, int pid, String category, String description)
    {
        handle.execute("UPDATE T_STREAM " +
                       "SET `category` = ?, " +
                       "    `description` = ? " +
                       "WHERE `transaction_id` = ? AND `pid` = ?",
                       category,
                       description,
                       transactionId,
                       pid);
    }

    public void cumsumStreamErrorCounts(Handle handle, long transactionId, int pid, long transportErrors, long continuityErrors)
    {
        handle.execute("UPDATE T_STREAM " +
                       "    SET `trans_err_cnt` = `trans_err_cnt` + ?, " +
                       "        `cc_err_cnt` = `cc_err_cnt` + ? " +
                       "WHERE `transaction_id` = ? AND `pid` = ?",
                       transportErrors, continuityErrors, transactionId, pid);
    }

    public void setStreamMarked(Handle handle, long transactionId, int pid, boolean marked)
    {
        handle.execute("UPDATE T_STREAM SET `marked` = ? WHERE `transaction_id` = ? AND `pid` = ?",
                       marked, transactionId, pid);
    }

    public List<StreamEntity> listPresentStreams(Handle handle, long transactionId)
    {
        return handle.select("SELECT * FROM T_STREAM WHERE `transaction_id` = ? AND `pkt_cnt` > 0 ORDER BY `pid`",
                             transactionId)
                     .map(streamEntityMapper)
                     .list();
    }
}
