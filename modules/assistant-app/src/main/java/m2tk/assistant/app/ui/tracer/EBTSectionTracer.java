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

import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.api.M2TKDatabase;
import m2tk.assistant.api.Tracer;
import m2tk.assistant.api.domain.StreamSource;
import m2tk.mpeg2.decoder.ExtendedSectionDecoder;
import m2tk.multiplex.TSDemux;
import m2tk.multiplex.TSDemuxPayload;
import org.pf4j.Extension;

import java.util.HashSet;
import java.util.Set;

/**
 * 应急广播（地面）数据段分析器
 */
@Slf4j
@Extension
public class EBTSectionTracer implements Tracer
{
    private static final int MAX_SECTION_COUNT = 1000;

    private final ExtendedSectionDecoder decoder;
    private final Set<String> cache;

    private M2TKDatabase databaseService;
    private long transactionId;

    public EBTSectionTracer()
    {
        decoder = new ExtendedSectionDecoder();
        cache = new HashSet<>();
    }

    @Override
    public void configure(StreamSource source, TSDemux demux, M2TKDatabase database)
    {
        databaseService = database;
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

        databaseService.addPrivateSection("eb-section" + translateSectionType(tableId),
                                          payload.getStreamPID(),
                                          payload.getFinishPacketCounter(),
                                          payload.getEncoding().getBytes());

        cache.add(key);
    }

    private String translateSectionType(int tableId)
    {
        return switch (tableId)
        {
            case 0xFD -> "EBIndex";
            case 0xFE -> "EBContent";
            case 0xFC -> "EBCertAuth";
            case 0xFB -> "EBConfigure";
            default -> "Undefined";
        };
    }
}
