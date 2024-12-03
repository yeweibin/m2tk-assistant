/*
 *  Copyright (c) M2TK Project. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package m2tk.assistant.app.ui.tracer;

import m2tk.assistant.api.M2TKDatabase;
import m2tk.assistant.api.Tracer;
import m2tk.assistant.api.domain.StreamSource;
import m2tk.multiplex.DemuxStatus;
import m2tk.multiplex.TSDemux;
import m2tk.multiplex.TSDemuxEvent;
import m2tk.multiplex.TSDemuxPayload;
import m2tk.util.BigEndian;
import org.pf4j.Extension;

import java.util.Arrays;

@Extension
public class DensityTracer implements Tracer
{
    private final DensityContext[] contexts;
    private static final int BULK_SIZE = 1000; // 不要超过1000，因为数据表里对应字段的长度上限设置为1000。

    private M2TKDatabase databaseService;
    private long t0;

    private static class DensityContext
    {
        private int ref;
        private int pid;
        private int offset;
        private int count;
        private byte[] density;
        private long lastPosition;
        private long maxDensity;
        private long minDensity;
        private double avgDensity; // 用整型会降低数据精度，导致数值错误
    }

    public DensityTracer()
    {
        contexts = new DensityContext[8192];
    }

    @Override
    public void configure(StreamSource source, TSDemux demux, M2TKDatabase database)
    {
        databaseService = database;

        demux.registerEventListener(this::processDemuxStatus);
        demux.registerRawChannel(this::processTransportPacket);
    }

    private void processDemuxStatus(TSDemuxEvent event)
    {
        if (event instanceof DemuxStatus status)
        {
            if (status.isRunning())
            {
                Arrays.fill(contexts, null);
                t0 = System.currentTimeMillis();
            } else
            {
                saveToDatabase();
            }
        }
    }

    public void processTransportPacket(TSDemuxPayload payload)
    {
        int pid = payload.getStreamPID();
        long pct = payload.getStartPacketCounter();

        updateDensity(pid, pct);
        if (System.currentTimeMillis() - t0 > 200)
            saveToDatabase();
    }

    private void updateDensity(int pid, long pct)
    {
        DensityContext context = contexts[pid];
        if (context == null)
        {
            context = new DensityContext();
            context.ref = -1;
            context.pid = pid;
            context.lastPosition = pct;
            context.count = 0;
            context.offset = 0;
            context.density = new byte[BULK_SIZE * 4];
            context.avgDensity = 0;
            context.maxDensity = 0;
            context.minDensity = Integer.MAX_VALUE;

            contexts[pid] = context;
        } else
        {
            long interval = Math.min(pct - context.lastPosition, Integer.MAX_VALUE);
            BigEndian.setUINT32(context.density, context.offset, interval);

            context.offset += 4;
            context.lastPosition = pct;
            context.maxDensity = Math.max(context.maxDensity, interval);
            context.minDensity = Math.min(context.minDensity, interval);
            context.avgDensity = (context.avgDensity * context.count + interval) / (context.count + 1);
            context.count += 1;

            if (context.ref == -1)
            {
                context.ref = databaseService.addStreamDensity(context.pid,
                                                               context.lastPosition,
                                                               context.count,
                                                               Arrays.copyOf(context.density, context.offset));
                t0 = System.currentTimeMillis();
            }
            if (context.count >= BULK_SIZE)
            {
                databaseService.updateStreamDensity(context.ref,
                                                    context.count,
                                                    Arrays.copyOf(context.density, context.offset),
                                                    context.avgDensity,
                                                    context.maxDensity,
                                                    context.minDensity);
                context.ref = -1;
                context.count = 0;
                context.offset = 0;
                context.avgDensity = 0;
                context.maxDensity = 0;
                context.minDensity = Integer.MAX_VALUE;

                Arrays.fill(context.density, (byte) 0);
                t0 = System.currentTimeMillis();
            }
        }
    }

    private void saveToDatabase()
    {
        for (DensityContext context : contexts)
        {
            if (context != null && context.ref != -1)
            {
                databaseService.updateStreamDensity(context.ref,
                                                    context.count,
                                                    Arrays.copyOf(context.density, context.offset),
                                                    context.avgDensity,
                                                    context.maxDensity,
                                                    context.minDensity);
            }
        }
        t0 = System.currentTimeMillis();
    }
}
