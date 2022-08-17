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
                       "`encoding` VARBINARY(4096)," +
                       "`stream` INT," +
                       "`position` BIGINT," +
                       "`tag` VARCHAR(100)" +
                       ")");
    }

    public void resetTable(Handle handle)
    {
        handle.execute("TRUNCATE TABLE T_SECTION");
    }

    public void addSection(Handle handle, String tag, int pid, long position, byte[] encoding)
    {
        handle.execute("INSERT INTO T_SECTION (`id`, `tag`, `stream`, `position`, `encoding`) " +
                       "VALUES (?,?,?,?,?)",
                       idGenerator.next(), tag, pid, position, encoding);
    }

    public Map<String, List<SectionEntity>> getSectionGroups(Handle handle)
    {
        return handle.select("SELECT * FROM T_SECTION")
                     .map(sectionEntityMapper)
                     .collect(groupingBy(entity -> entity.getTag(), toList()));
    }

    public List<SectionEntity> getSections(Handle handle, String tagPrefix)
    {
        return handle.select("SELECT * FROM T_SECTION WHERE `tag` like CONCAT(?, '%')", tagPrefix)
                     .map(sectionEntityMapper)
                     .list();
    }
}
