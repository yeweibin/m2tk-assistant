package m2tk.assistant.dbi;

import cn.hutool.core.lang.generator.SnowflakeGenerator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import m2tk.assistant.dbi.entity.ProgramEntity;
import m2tk.assistant.dbi.entity.ProgramStreamMappingEntity;
import m2tk.assistant.dbi.entity.SourceEntity;
import m2tk.assistant.dbi.entity.StreamEntity;
import m2tk.assistant.dbi.handler.ProgramHandler;
import m2tk.assistant.dbi.handler.SourceHandler;
import m2tk.assistant.dbi.handler.StreamHandler;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.h2.H2DatabasePlugin;

import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public class DatabaseService
{
    private final Jdbi dbi;
    private final SourceHandler sourceHandler;
    private final StreamHandler streamHandler;
    private final ProgramHandler programHandler;

    public DatabaseService()
    {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.h2.Driver");
        config.setJdbcUrl("jdbc:h2:mem:m2tk");

        dbi = Jdbi.create(new HikariDataSource(config)).installPlugin(new H2DatabasePlugin());

        SnowflakeGenerator generator = new SnowflakeGenerator();

        sourceHandler = new SourceHandler(generator);
        streamHandler = new StreamHandler(generator);
        programHandler = new ProgramHandler(generator);
    }

    public void initDatabase()
    {
        dbi.useTransaction(handle -> {
            sourceHandler.initTable(handle);
            streamHandler.initTable(handle);
            programHandler.initTable(handle);
        });
    }

    public void resetDatabase()
    {
        dbi.useHandle(handle -> {
            sourceHandler.resetTable(handle);
            streamHandler.resetTable(handle);
            programHandler.resetTable(handle);
        });
    }

    public StreamEntity getStream(int pid)
    {
        return dbi.withHandle(handle -> streamHandler.getStream(handle, pid));
    }

    public void updateStreamStatistics(StreamEntity stream)
    {
        dbi.useHandle(handle -> streamHandler.updateStreamStatistics(handle, stream));
    }

    public void updateStreamUsage(int pid, String category, String description)
    {
        dbi.useHandle(handle -> streamHandler.updateStreamUsage(handle, pid, category, description));
    }

    public void updateStreamUsage(StreamEntity stream)
    {
        dbi.useHandle(handle -> streamHandler.updateStreamUsage(handle, stream));
    }

    public List<StreamEntity> listStreams()
    {
        return dbi.withHandle(streamHandler::listStreams);
    }

    public Map<Integer, StreamEntity> getStreamRegistry()
    {
        return dbi.withHandle(handle -> streamHandler.listStreams(handle)
                                                     .stream()
                                                     .collect(toMap(StreamEntity::getPid, Function.identity()))
                             );
    }

    public SourceEntity addSource(String name)
    {
        return dbi.withHandle(handle -> sourceHandler.addSource(handle, name));
    }

    public void updateSourceStatistics(SourceEntity source)
    {
        dbi.useHandle(handle -> sourceHandler.updateSourceStatistics(handle, source));
    }

    public void updateSourceTransportId(SourceEntity source)
    {
        dbi.useHandle(handle -> sourceHandler.updateSourceTransportId(handle, source));
    }

    public SourceEntity getSource()
    {
        return dbi.withHandle(sourceHandler::getLatestSource);
    }

    public void clearPrograms()
    {
        dbi.useHandle(programHandler::resetTable);
    }

    public ProgramEntity getProgram(int number)
    {
        return dbi.withHandle(handle -> programHandler.getProgram(handle, number));
    }

    public ProgramEntity addProgram(int tsid, int number, int pmtpid)
    {
        return dbi.withHandle(handle -> programHandler.addProgram(handle, tsid, number, pmtpid));
    }

    public void updateProgram(ProgramEntity program)
    {
        dbi.useHandle(handle -> programHandler.updateProgram(handle, program));
    }

    public void addProgramStreamMapping(int program, int pid, int type, String category, String description)
    {
        dbi.useHandle(handle -> programHandler.addProgramStreamMapping(handle, program, pid, type, category, description));
    }

    public List<ProgramEntity> listPrograms()
    {
        return dbi.withHandle(programHandler::listPrograms);
    }

    public List<ProgramStreamMappingEntity> getProgramStreamMappings(int program)
    {
        return dbi.withHandle(handle -> programHandler.listProgramStreamMappings(handle, program));
    }

    public Map<ProgramEntity, List<ProgramStreamMappingEntity>> getProgramMappings()
    {
        return dbi.withHandle(handle -> {
            List<ProgramEntity> programs = programHandler.listPrograms(handle);
            Map<ProgramEntity, List<ProgramStreamMappingEntity>> mappings = new HashMap<>();
            for (ProgramEntity program : programs)
            {
                mappings.put(program,
                             programHandler.listProgramStreamMappings(handle,
                                                                      program.getProgramNumber()));
            }
            return mappings;
        });
    }
}
