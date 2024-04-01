/*
 * Copyright (c) Ye Weibin. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package m2tk.assistant;

import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.analyzer.StreamAnalyzer;
import m2tk.assistant.dbi.DatabaseService;
import m2tk.assistant.dbi.entity.SourceEntity;
import m2tk.assistant.template.TemplateReader;
import m2tk.assistant.template.decoder.DescriptorDecoder;
import m2tk.assistant.template.decoder.SectionDecoder;
import m2tk.assistant.template.definition.M2TKTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
public final class Global
{
    private static final DatabaseService databaseService;
    private static final StreamAnalyzer streamAnalyser;
    private static final Set<Integer> userPrivateSectionStreams;
    private static volatile long latestTransactionId = -1L;
    private static String latestSourceUrl;

    private static final EventBus eventBus;

    static
    {
        databaseService = new DatabaseService();
        streamAnalyser = new StreamAnalyzer();
        userPrivateSectionStreams = new HashSet<>();
        streamAnalyser.setDatabaseService(databaseService);
        eventBus = new EventBus();
    }

    private Global()
    {
    }

    public static StreamAnalyzer getStreamAnalyser()
    {
        return streamAnalyser;
    }

    public static void init()
    {
        databaseService.initDatabase();
        loadUserDefinedTemplates();
    }

    private static void loadUserDefinedTemplates()
    {
        Path folder = Paths.get(System.getProperty("user.dir"), "template");

        try (Stream<Path> stream = Files.list(folder))
        {
            TemplateReader reader = new TemplateReader();
            stream.filter(path -> path.getFileName().toString().endsWith(".xml"))
                  .forEach(file -> {
                      M2TKTemplate template = reader.parse(file.toFile());
                      if (template == null)
                      {
                          log.warn("无法加载模板：{}", file);
                          return;
                      }

                      template.getTableTemplates().forEach(SectionDecoder::registerTemplate);
                      template.getDescriptorTemplates().forEach(DescriptorDecoder::registerTemplate);
                  });
        } catch (Exception ex)
        {
            log.warn("加载自定义解析模板时异常：{}", ex.getMessage());
        }
    }

    public static DatabaseService getDatabaseService()
    {
        return databaseService;
    }

    public static void addUserPrivateSectionStreams(Collection<Integer> streams)
    {
        userPrivateSectionStreams.addAll(streams);
    }

    public static void resetUserPrivateSectionStreams()
    {
        userPrivateSectionStreams.clear();
    }

    public static Collection<Integer> getUserPrivateSectionStreamList()
    {
        return userPrivateSectionStreams;
    }

    public static int getPrivateSectionFilteringLimit()
    {
        return 1000;
    }

    public static List<String> getSourceHistory()
    {
        return databaseService.getSourceHistory();
    }

    public static String getLatestSourceUrl()
    {
        return latestSourceUrl;
    }

    public static long getLatestTransactionId()
    {
        return latestTransactionId;
    }

    public static void updateSource(SourceEntity source)
    {
        latestTransactionId = source.getTransactionId();
        latestSourceUrl = source.getSourceUrl();
    }

    public static void postEvent(Object event)
    {
        eventBus.post(event);
    }

    public static void registerSubscriber(Object subscriber)
    {
        eventBus.register(subscriber);
    }
}
