package m2tk.assistant.dbi.handler;

import cn.hutool.core.lang.generator.Generator;
import m2tk.assistant.dbi.mapper.TR290EventEntityMapper;
import org.jdbi.v3.core.Handle;

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
                       "`type_code` VARCHAR(20) NOT NULL," +
                       "`stream_pid` INT," +
                       "`position` BIGINT" +
                       ")");
    }

    public void resetTable(Handle handle)
    {
        handle.execute("TRUNCATE TABLE T_TR290_EVENT");
    }
}
