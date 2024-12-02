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
import org.pf4j.Extension;

import java.util.Arrays;

@Extension
public class DensityTracer implements Tracer
{
    // 密度值采用UTF8编码规则，最大可编码值为Integer.MAX，占用6字节。
    // 一个特殊编码：0b11111111：表示包间隔超出可编码范围(上溢）。
    private final DensityContext[] contexts;
    private static final int BULK_SIZE = 1000; // 每组最多记录1000个包间隔，最大占用6000字节（UTF8/6）
    private static final byte OVERFLOWED = -1;

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
        private long avgDensity;
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
            context.density = new byte[BULK_SIZE * 6];
            context.avgDensity = 0;
            context.maxDensity = 0;
            context.minDensity = Integer.MAX_VALUE;

            contexts[pid] = context;
        } else
        {
            long interval = pct - context.lastPosition;
            context.offset += writInterval(context.density, context.offset, interval);
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

    private int writInterval(byte[] density, int offset, long interval)
    {
        // 遵循UTF8编码规则，动态编码长度1至6个字节。
        if (interval <= 0x7F)
        {
            density[offset] = (byte) (0b01111111 & interval);
            return 1;
        }
        if (interval <= 0x7FF)
        {
            density[offset] = (byte) (0b11000000 | (0b00011111 & (interval >>> 6)));
            density[offset + 1] = (byte) (0b10000000 | (0b00111111 & interval));
            return 2;
        }
        if (interval <= 0xFFFF)
        {
            density[offset] = (byte) (0b11100000 | (0b00001111 & (interval >>> 12)));
            density[offset + 1] = (byte) (0b10000000 | (0b00111111 & (interval >>> 6)));
            density[offset + 2] = (byte) (0b10000000 | (0b00111111 & interval));
            return 3;
        }
        if (interval <= 0x1FFFFF)
        {
            density[offset] = (byte) (0b11110000 | (0b00000111 & (interval >>> 18)));
            density[offset + 1] = (byte) (0b10000000 | (0b00111111 & (interval >>> 12)));
            density[offset + 2] = (byte) (0b10000000 | (0b00111111 & (interval >>> 6)));
            density[offset + 3] = (byte) (0b10000000 | (0b00111111 & interval));
            return 4;
        }
        if (interval <= 0x3FFFFFF)
        {
            density[offset] = (byte) (0b11111000 | (0b00000011 & (interval >>> 24)));
            density[offset + 1] = (byte) (0b10000000 | (0b00111111 & (interval >>> 18)));
            density[offset + 2] = (byte) (0b10000000 | (0b00111111 & (interval >>> 12)));
            density[offset + 3] = (byte) (0b10000000 | (0b00111111 & (interval >>> 6)));
            density[offset + 4] = (byte) (0b10000000 | (0b00111111 & interval));
            return 5;
        }
        if (interval <= 0x7FFFFFFF)
        {
            density[offset] = (byte) (0b11111100 | (0b00000001 & (interval >>> 30)));
            density[offset + 1] = (byte) (0b10000000 | (0b00111111 & (interval >>> 24)));
            density[offset + 2] = (byte) (0b10000000 | (0b00111111 & (interval >>> 18)));
            density[offset + 3] = (byte) (0b10000000 | (0b00111111 & (interval >>> 12)));
            density[offset + 4] = (byte) (0b10000000 | (0b00111111 & (interval >>> 6)));
            density[offset + 5] = (byte) (0b10000000 | (0b00111111 & interval));
            return 6;
        }

        // 间隔过大，上溢。
        density[offset] = OVERFLOWED;
        return 1;
    }
}
