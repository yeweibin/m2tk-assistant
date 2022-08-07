package m2tk.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import m2tk.assistant.analyzer.StreamAnalyzer;
import m2tk.assistant.dbi.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Global
{
    private static final Logger logger;
    private static final ObjectMapper objectMapper;
    private static final DatabaseService databaseService;
    private static final StreamAnalyzer streamAnalyser;

    static
    {
        logger = LoggerFactory.getLogger(Global.class);
        objectMapper = new ObjectMapper();
        databaseService = new DatabaseService();
        streamAnalyser = new StreamAnalyzer();
        streamAnalyser.setDatabaseService(databaseService);
    }

    public static ObjectMapper getObjectMapper()
    {
        return objectMapper;
    }

    public static StreamAnalyzer getStreamAnalyser()
    {
        return streamAnalyser;
    }

    public static void init()
    {
        databaseService.initDatabase();
    }

    public static DatabaseService getDatabaseService()
    {
        return databaseService;
    }
}
