package m2tk.assistant.dbi.handler;

import cn.hutool.core.lang.generator.Generator;
import m2tk.assistant.dbi.mapper.SectionEntityMapper;
import m2tk.assistant.dbi.mapper.ServiceEntityMapper;
import org.jdbi.v3.core.Handle;

public class SectionHandler
{
    private final Generator<Long> idGenerator;
    private final SectionEntityMapper sectionEntityMapper;

    public SectionHandler(Generator<Long> generator)
    {
        idGenerator = generator;
        sectionEntityMapper = new SectionEntityMapper();
    }

    public void initTable(Handle handle)
    {
        handle.execute("DROP TABLE IF EXISTS `T_SECTION`");
        handle.execute("CREATE TABLE `T_SECTION` (" +
                       "`id` BIGINT PRIMARY KEY," +
                       "`encoding` VARBINARY(4096)," +
                       "`stream_pid` INT," +
                       "`position` BIGINT," +
                       "`table_id` INT," +
                       "`name` VARCHAR(100)" +
                       ")");
    }

    public void resetTable(Handle handle)
    {
        handle.execute("TRUNCATE TABLE T_SECTION");
    }
}
