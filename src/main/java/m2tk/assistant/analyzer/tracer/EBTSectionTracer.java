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

package m2tk.assistant.analyzer.tracer;

import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.dbi.DatabaseService;
import m2tk.mpeg2.decoder.ExtendedSectionDecoder;
import m2tk.multiplex.TSDemux;
import m2tk.multiplex.TSDemuxPayload;

import java.util.HashSet;
import java.util.Set;

/**
 * 应急广播（地面）数据段分析器
 */
@Slf4j
public class EBTSectionTracer implements Tracer
{
    private static final int MAX_SECTION_COUNT = 1000;

    private final DatabaseService databaseService;
    private final long transactionId;

    private final ExtendedSectionDecoder decoder;
    private final Set<String> cache;

    public EBTSectionTracer(DatabaseService service, long transaction)
    {
        databaseService = service;
        transactionId = transaction;

        decoder = new ExtendedSectionDecoder();
        cache = new HashSet<>();
    }

    @Override
    public void configureDemux(TSDemux demux)
    {
        demux.registerSectionChannel(0x0021, this::processSection);
    }

    private void processSection(TSDemuxPayload payload)
    {
        if (payload.getStreamPID() != 0x0021 ||
            payload.getType() != TSDemuxPayload.Type.SECTION ||
            !decoder.isAttachable(payload.getEncoding()))
            return;

        decoder.attach(payload.getEncoding());
        int tableId = decoder.getTableID();
        int tableIdEx = decoder.getTableIDExtension();
        int vernum = decoder.getVersionNumber();
        int secnum = decoder.getSectionNumber();
        int lastnum = decoder.getLastSectionNumber();
        long crc32 = decoder.getChecksum();
        String key = String.format("%02x.%02x.%d.%d.%d.%08x", tableId, tableIdEx, secnum, lastnum, vernum, crc32);
        if (tableId == 0xFE)
        {
            String ebmId = decoder.getPayload().toHexString(0, 18).substring(1);
            key = key + "." + ebmId;

            log.info("EBM_ID: {}", ebmId);
        }
        if (cache.contains(key))
            return;

        databaseService.addSection(transactionId,
                                   "eb-section." + translateSectionType(tableId),
                                   payload.getStreamPID(),
                                   payload.getFinishPacketCounter(),
                                   payload.getEncoding().getBytes());
        cache.add(key);
    }

    private String translateSectionType(int tableId)
    {
        switch (tableId)
        {
            case 0xFD:
                return "EBIndex";
            case 0xFE:
                return "EBContent";
            case 0xFC:
                return "EBCertAuth";
            case 0xFB:
                return "EBConfigure";
            default:
                return "Undefined";
        }
    }
}
