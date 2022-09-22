package m2tk.assistant.dbi.handler;

import cn.hutool.core.lang.generator.Generator;
import m2tk.assistant.dbi.entity.SectionEntity;
import m2tk.assistant.dbi.mapper.SectionEntityMapper;
import org.jdbi.v3.core.Handle;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

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
                       "`transaction_id` BIGINT NOT NULL," +
                       "`encoding` VARBINARY(4096)," +
                       "`stream` INT," +
                       "`position` BIGINT," +
                       "`tag` VARCHAR(100)" +
                       ")");
    }

    public void addSection(Handle handle, long transactionId, String tag, int pid, long position, byte[] encoding)
    {
        handle.execute("INSERT INTO T_SECTION (`id`, `transaction_id`, `tag`, `stream`, `position`, `encoding`) " +
                       "VALUES (?,?,?,?,?,?)",
                       idGenerator.next(), transactionId, tag, pid, position, encoding);
    }

    public Map<String, List<SectionEntity>> getSectionGroups(Handle handle, long transactionId)
    {
        return handle.select("SELECT * FROM T_SECTION WHERE `transaction_id` = ?", transactionId)
                     .map(sectionEntityMapper)
                     .collect(groupingBy(SectionEntity::getTag, toList()));
    }

    public List<SectionEntity> getSections(Handle handle, long transactionId, String tagPrefix)
    {
        return handle.select("SELECT * FROM T_SECTION WHERE `transaction_id` = ? AND `tag` like CONCAT(?, '%')",
                             transactionId, tagPrefix)
                     .map(sectionEntityMapper)
                     .list();
    }
}
