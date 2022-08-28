package m2tk.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import m2tk.assistant.analyzer.StreamAnalyzer;
import m2tk.assistant.dbi.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Global
{
    private static final Logger logger;
    private static final ObjectMapper objectMapper;
    private static final DatabaseService databaseService;
    private static final StreamAnalyzer streamAnalyser;
    private static final List<Integer> userPrivateSectionStreams;
    private static String inputResource;

    static
    {
        logger = LoggerFactory.getLogger(Global.class);
        objectMapper = new ObjectMapper();
        databaseService = new DatabaseService();
        streamAnalyser = new StreamAnalyzer();
        userPrivateSectionStreams = new ArrayList<>();
        streamAnalyser.setDatabaseService(databaseService);
        inputResource = null;
    }

    private Global()
    {
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

    public static void setUserPrivateSectionStreams(Collection<Integer> streams)
    {
        userPrivateSectionStreams.clear();
        userPrivateSectionStreams.addAll(streams);
    }

    public static Collection<Integer> getUserPrivateSectionStreamList()
    {
        return userPrivateSectionStreams;
    }

    public static int getPrivateSectionFilteringLimit()
    {
        return 1000;
    }

    public static String getInputResource()
    {
        return inputResource;
    }

    public static void setInputResource(String inputResource)
    {
        Global.inputResource = inputResource;
    }
}
