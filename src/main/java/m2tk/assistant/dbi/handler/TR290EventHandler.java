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
                       "`type` VARCHAR(20) NOT NULL," +
                       "`description` VARCHAR(1000) NOT NULL," +
                       "`stream_pid` INT NOT NULL," +
                       "`position` BIGINT NOT NULL," +
                       "`timestamp` DATETIME NOT NULL" +
                       ")");
    }

    public void resetTable(Handle handle)
    {
        handle.execute("TRUNCATE TABLE T_TR290_EVENT");
    }

    public void addTR290Event(Handle handle,
                              LocalDateTime timestamp,
                              String type,
                              String description,
                              long position,
                              int pid)
    {
        handle.execute("INSERT INTO T_TR290_EVENT (`id`, `timestamp`, `type`, `description`, `position`, `stream_pid`) " +
                       "VALUES (?,?,?,?,?,?)",
                       idGenerator.next(), timestamp, type, description, position, pid);
    }

    public List<TR290EventEntity> listEvents(Handle handle, long start, int count)
    {
        return handle.select("SELECT * FROM T_TR290_EVENT WHERE `id` > ? ORDER BY `id` " +
                             "FETCH FIRST ? ROWS ONLY",
                             start, count)
                     .map(eventEntityMapper)
                     .list();
    }

    public List<TR290EventEntity> listEvents(Handle handle, String type, int count)
    {
        return handle.select("SELECT * FROM T_TR290_EVENT WHERE `type` = ? ORDER BY `id` " +
                             "FETCH FIRST ? ROWS ONLY",
                             type, count)
                     .map(eventEntityMapper)
                     .list();
    }

    public List<TR290StatEntity> listStats(Handle handle)
    {
        return handle.select("SELECT A.`id` AS `id`, " +
                             "           A.`type` AS `indicator`, " +
                             "           A.`timestamp` AS `timestamp`, " +
                             "           A.`description` AS `description`, " +
                             "           B.`count` AS `count` " +
                             "FROM T_TR290_EVENT A " +
                             "INNER JOIN " +
                             "  (SELECT MAX(`id`) AS `id`, COUNT(`id`) AS `count` FROM T_TR290_EVENT " +
                             "   GROUP BY `type`) B " +
                             "ON A.`id` = B.`id` " +
                             "ORDER BY A.`type`")
                .map(statsEntityMapper).list();
    }
}
