package m2tk.assistant.dbi.handler;

import cn.hutool.core.lang.generator.Generator;
import m2tk.assistant.dbi.entity.TR290EventEntity;
import m2tk.assistant.dbi.mapper.TR290EventEntityMapper;
import org.jdbi.v3.core.Handle;

import java.util.List;

public class TR290EventHandler
{
    private final Generator<Long> idGenerator;
    private final TR290EventEntityMapper eventEntityMapper;

    public TR290EventHandler(Generator<Long> generator)
    {
        idGenerator = generator;
        eventEntityMapper = new TR290EventEntityMapper();
    }

    public void initTable(Handle handle)
    {
        handle.execute("DROP TABLE IF EXISTS `T_TR290_EVENT`");
        handle.execute("CREATE TABLE `T_TR290_EVENT` (" +
                       "`id` BIGINT PRIMARY KEY," +
                       "`level` INT NOT NULL," +
                       "`type` VARCHAR(20) NOT NULL," +
                       "`description` VARCHAR(1000) NOT NULL," +
                       "`stream_pid` INT," +
                       "`position` BIGINT" +
                       ")");
    }

    public void resetTable(Handle handle)
    {
        handle.execute("TRUNCATE TABLE T_TR290_EVENT");
    }

    public void addTR290Event(Handle handle,
                              int level,
                              String type,
                              String description,
                              long position,
                              int pid)
    {
        handle.execute("INSERT INTO T_TR290_EVENT (`id`, `level`, `type`, `description`, `position`, `stream_pid`) " +
                       "VALUES (?,?,?,?,?,?)",
                       idGenerator.next(), level, type, description, position, pid);
    }

    public List<TR290EventEntity> listEvents(Handle handle, long start, int count)
    {
        return handle.select("SELECT * FROM T_TR290_EVENT WHERE `id` > ? ORDER BY `id` " +
                             "FETCH FIRST ? ROWS ONLY",
                             start, count)
                     .map(eventEntityMapper)
                     .list();
    }
}
