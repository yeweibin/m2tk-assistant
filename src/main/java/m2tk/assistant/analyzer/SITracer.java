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

package m2tk.assistant.analyzer;

import m2tk.assistant.dbi.DatabaseService;
import m2tk.dvb.decoder.descriptor.*;
import m2tk.dvb.decoder.section.BATSectionDecoder;
import m2tk.dvb.decoder.section.EITSectionDecoder;
import m2tk.dvb.decoder.section.NITSectionDecoder;
import m2tk.dvb.decoder.section.SDTSectionDecoder;
import m2tk.mpeg2.decoder.DescriptorLoopDecoder;

import java.util.HashMap;
import java.util.Map;

public class SITracer
{
    private final DatabaseService databaseService;

    private final NITSectionDecoder nit;
    private final BATSectionDecoder bat;
    private final SDTSectionDecoder sdt;
    private final EITSectionDecoder eit;
    private final DescriptorLoopDecoder descloop;
    private final NetworkNameDescriptorDecoder nnd;
    private final ServiceDescriptorDecoder sd;
    private final ShortEventDescriptorDecoder sed;
    private final CableDeliverySystemDescriptorDecoder cdsd;
    private final BouquetNameDescriptorDecoder bnd;
    private final Map<String, Integer> tableVersions;
    public SITracer(DatabaseService service)
    {
        databaseService = service;
        nit = new NITSectionDecoder();
        bat = new BATSectionDecoder();
        sdt = new SDTSectionDecoder();
        eit = new EITSectionDecoder();
        descloop = new DescriptorLoopDecoder();
        nnd = new NetworkNameDescriptorDecoder();
        bnd = new BouquetNameDescriptorDecoder();
        sd = new ServiceDescriptorDecoder();
        sed = new ShortEventDescriptorDecoder();
        cdsd = new CableDeliverySystemDescriptorDecoder();
        tableVersions = new HashMap<>();
    }
}
