package m2tk.assistant.dbi.handler;

import cn.hutool.core.lang.generator.Generator;
import m2tk.assistant.dbi.mapper.ServiceEntityMapper;
import org.jdbi.v3.core.Handle;

public class ServiceHandler
{
    private final Generator<Long> idGenerator;
    private final ServiceEntityMapper serviceEntityMapper;

    public ServiceHandler(Generator<Long> generator)
    {
        idGenerator = generator;
        serviceEntityMapper = new ServiceEntityMapper();
    }

    public void initTable(Handle handle)
    {
        handle.execute("DROP TABLE IF EXISTS `T_SERVICE`");
        handle.execute("CREATE TABLE `T_SERVICE` (" +
                       "`id` BIGINT PRIMARY KEY," +
                       "`srv_id` INT," +
                       "`ts_id` INT," +
                       "`org_net_id` INT," +
                       "`free_access` BOOLEAN," +
                       "`srv_type` VARCHAR(100)," +
                       "`srv_name` VARCHAR(100)," +
                       "`srv_provider` VARCHAR(100)" +
                       ")");
    }

    public void resetTable(Handle handle)
    {
        handle.execute("TRUNCATE TABLE T_SERVICE");
    }
}
