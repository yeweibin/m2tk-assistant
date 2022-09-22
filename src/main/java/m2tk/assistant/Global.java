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
import m2tk.assistant.analyzer.StreamAnalyzer;
import m2tk.assistant.dbi.DatabaseService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Global
{
    private static final DatabaseService databaseService;
    private static final StreamAnalyzer streamAnalyser;
    private static final List<Integer> userPrivateSectionStreams;
    private static String inputResource;
    private static volatile long currentTransactionId;
    private static final EventBus eventBus;

    static
    {
        databaseService = new DatabaseService();
        streamAnalyser = new StreamAnalyzer();
        userPrivateSectionStreams = new ArrayList<>();
        streamAnalyser.setDatabaseService(databaseService);
        inputResource = null;
        currentTransactionId = -1;
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

    public static void postEvent(Object event)
    {
        eventBus.post(event);
    }

    public static void setCurrentTransactionId(long transactionId)
    {
        currentTransactionId = transactionId;
    }

    public static long getCurrentTransactionId()
    {
        return currentTransactionId;
    }

    public static void registerSubscriber(Object subscriber)
    {
        eventBus.register(subscriber);
    }

    public static void unregisterSubscriber(Object subscriber)
    {
        eventBus.unregister(subscriber);
    }
}
