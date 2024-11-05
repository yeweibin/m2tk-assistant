/*
 * Copyright (c) M2TK Project. All rights reserved.
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

package m2tk.assistant.ui.builder;

import m2tk.assistant.ui.builder.descriptor.*;

import java.util.HashMap;
import java.util.Map;

public final class DescriptorNodeBuilders
{
    private static final Map<Integer, DescriptorNodeBuilder> builders;

    static
    {
        builders = new HashMap<>();

        // PSI Descriptors
        builders.put(0x02, new DescriptorNodeBuilder("video_stream_descriptor"));
        builders.put(0x03, new DescriptorNodeBuilder("audio_stream_descriptor"));
        builders.put(0x04, new DescriptorNodeBuilder("hierarchy_descriptor"));
        builders.put(0x05, new DescriptorNodeBuilder("registration_descriptor"));
        builders.put(0x06, new DescriptorNodeBuilder("data_stream_alignment_descriptor"));
        builders.put(0x07, new DescriptorNodeBuilder("target_background_grid_descriptor"));
        builders.put(0x08, new DescriptorNodeBuilder("Video_window_descriptor"));
        builders.put(0x09, new DescriptorNodeBuilder("CA_descriptor", new CADescriptorPayloadNodeBuilder()));
        builders.put(0x0A, new DescriptorNodeBuilder("ISO_639_language_descriptor", new ISO639LanguageDescriptorPayloadNodeBuilder()));
        builders.put(0x0B, new DescriptorNodeBuilder("System_clock_descriptor"));
        builders.put(0x0C, new DescriptorNodeBuilder("Multiplex_buffer_utilization_descriptor"));
        builders.put(0x0D, new DescriptorNodeBuilder("Copyright_descriptor"));
        builders.put(0x0E, new DescriptorNodeBuilder("Maximum_bitrate_descriptor"));
        builders.put(0x0F, new DescriptorNodeBuilder("Private_data_identifier_descriptor"));
        builders.put(0x10, new DescriptorNodeBuilder("Smoothing_buffer_descriptor"));
        builders.put(0x11, new DescriptorNodeBuilder("STD_descriptor"));
        builders.put(0x12, new DescriptorNodeBuilder("IBP_descriptor"));
        builders.put(0x1B, new DescriptorNodeBuilder("MPEG-4_video_descriptor"));
        builders.put(0x1C, new DescriptorNodeBuilder("MPEG-4_audio_descriptor"));
        builders.put(0x1D, new DescriptorNodeBuilder("IOD_descriptor"));
        builders.put(0x1E, new DescriptorNodeBuilder("SL_descriptor"));
        builders.put(0x1F, new DescriptorNodeBuilder("FMC_descriptor"));
        builders.put(0x20, new DescriptorNodeBuilder("External_ES_ID_descriptor"));
        builders.put(0x21, new DescriptorNodeBuilder("MuxCode_descriptor"));
        builders.put(0x22, new DescriptorNodeBuilder("FmxBufferSize_descriptor"));
        builders.put(0x23, new DescriptorNodeBuilder("MultiplexBuffer_descriptor"));

        // SI Descriptors
        builders.put(0x40, new DescriptorNodeBuilder("network_name_descriptor", new NetworkNameDescriptorPayloadNodeBuilder()));
        builders.put(0x41, new DescriptorNodeBuilder("service_list_descriptor", new ServiceListDescriptorPayloadNodeBuilder()));
        builders.put(0x42, new DescriptorNodeBuilder("stuffing_descriptor", new StuffingDescriptorPayloadNodeBuilder()));
        builders.put(0x43, new DescriptorNodeBuilder("satellite_delivery_system_descriptor", new SatelliteDeliverySystemDescriptorPayloadNodeBuilder()));
        builders.put(0x44, new DescriptorNodeBuilder("cable_delivery_system_descriptor", new CableDeliverySystemDescriptorPayloadNodeBuilder()));
        builders.put(0x45, new DescriptorNodeBuilder("VBI_data_descriptor"));
        builders.put(0x46, new DescriptorNodeBuilder("VBI_teletext_descriptor"));
        builders.put(0x47, new DescriptorNodeBuilder("bouquet_name_descriptor", new BouquetNameDescriptorPayloadNodeBuilder()));
        builders.put(0x48, new DescriptorNodeBuilder("service_descriptor", new ServiceDescriptorPayloadNodeBuilder()));
        builders.put(0x49, new DescriptorNodeBuilder("country_availability_descriptor", new CountryAvailabilityDescriptorPayloadNodeBuilder()));
        builders.put(0x4A, new DescriptorNodeBuilder("linkage_descriptor", new LinkageDescriptorPayloadNodeBuilder()));
        builders.put(0x4B, new DescriptorNodeBuilder("NVOD_reference_descriptor", new NVODReferenceDescriptorPayloadNodeBuilder()));
        builders.put(0x4C, new DescriptorNodeBuilder("time_shifted_service_descriptor", new TimeShiftedServiceDescriptorPayloadNodeBuilder()));
        builders.put(0x4D, new DescriptorNodeBuilder("short_event_descriptor", new ShortEventDescriptorPayloadNodeBuilder()));
        builders.put(0x4E, new DescriptorNodeBuilder("extended_event_descriptor", new ExtendedEventDescriptorPayloadNodeBuilder()));
        builders.put(0x4F, new DescriptorNodeBuilder("time_shifted_event_descriptor", new TimeShiftedEventDescriptorPayloadNodeBuilder()));
        builders.put(0x50, new DescriptorNodeBuilder("component_descriptor", new ComponentDescriptorPayloadNodeBuilder()));
        builders.put(0x51, new DescriptorNodeBuilder("mosaic_descriptor"));
        builders.put(0x52, new DescriptorNodeBuilder("stream_identifier_descriptor", new StreamIdentifierDescriptorPayloadNodeBuilder()));
        builders.put(0x53, new DescriptorNodeBuilder("CA_identifier_descriptor", new CAIdentifierDescriptorPayloadNodeBuilder()));
        builders.put(0x54, new DescriptorNodeBuilder("content_descriptor", new ContentDescriptorPayloadNodeBuilder()));
        builders.put(0x55, new DescriptorNodeBuilder("parental_rating_descriptor", new ParentalRatingDescriptorPayloadNodeBuilder()));
        builders.put(0x56, new DescriptorNodeBuilder("teletext_descriptor"));
        builders.put(0x57, new DescriptorNodeBuilder("telephone_descriptor"));
        builders.put(0x58, new DescriptorNodeBuilder("local_time_offset_descriptor", new LocalTimeOffsetDescriptorPayloadNodeBuilder()));
        builders.put(0x59, new DescriptorNodeBuilder("subtitling_descriptor", new SubtitlingDescriptorPayloadNodeBuilder()));
        builders.put(0x5A, new DescriptorNodeBuilder("terrestrial_delivery_system_descriptor", new TerrestrialDeliverySystemDescriptorPayloadNodeBuilder()));
        builders.put(0x5B, new DescriptorNodeBuilder("multilingual_network_name_descriptor", new MultilingualNetworkNameDescriptorPayloadNodeBuilder()));
        builders.put(0x5C, new DescriptorNodeBuilder("multilingual_bouquet_name_descriptor", new MultilingualBouquetNameDescriptorPayloadNodeBuilder()));
        builders.put(0x5D, new DescriptorNodeBuilder("multilingual_service_name_descriptor", new MultilingualServiceNameDescriptorPayloadNodeBuilder()));
        builders.put(0x5E, new DescriptorNodeBuilder("multilingual_component_descriptor", new MultilingualComponentDescriptorPayloadNodeBuilder()));
        builders.put(0x5F, new DescriptorNodeBuilder("private_data_specifier_descriptor", new PrivateDataSpecifierDescriptorPayloadNodeBuilder()));
        builders.put(0x60, new DescriptorNodeBuilder("service_move_descriptor"));
        builders.put(0x61, new DescriptorNodeBuilder("short_smoothing_buffer_descriptor"));
        builders.put(0x62, new DescriptorNodeBuilder("frequency_list_descriptor", new FrequencyListDescriptorPayloadNodeBuilder()));
        builders.put(0x63, new DescriptorNodeBuilder("partial_transport_stream_descriptor"));
        builders.put(0x64, new DescriptorNodeBuilder("data_broadcast_descriptor", new DataBroadcastDescriptorPayloadNodeBuilder()));
        builders.put(0x65, new DescriptorNodeBuilder("scrambling_descriptor"));
        builders.put(0x66, new DescriptorNodeBuilder("data_broadcast_id_descriptor", new DataBroadcastIdentifierDescriptorPayloadNodeBuilder()));
        builders.put(0x67, new DescriptorNodeBuilder("transport_stream_descriptor"));
        builders.put(0x68, new DescriptorNodeBuilder("DSNG_descriptor"));
        builders.put(0x69, new DescriptorNodeBuilder("PDC_descriptor"));
        builders.put(0x6A, new DescriptorNodeBuilder("AC-3_descriptor"));
        builders.put(0x6B, new DescriptorNodeBuilder("ancillary_data_descriptor", new AncillaryDataDescriptorPayloadNodeBuilder()));
        builders.put(0x6C, new DescriptorNodeBuilder("cell_list_descriptor"));
        builders.put(0x6D, new DescriptorNodeBuilder("cell_frequency_link_descriptor"));
        builders.put(0x6E, new DescriptorNodeBuilder("announcement_support_descriptor"));
        builders.put(0x6F, new DescriptorNodeBuilder("application_signalling_descriptor"));
        builders.put(0x70, new DescriptorNodeBuilder("adaptation_field_data_descriptor", new AdaptationFieldDataDescriptorPayloadNodeBuilder()));
        builders.put(0x71, new DescriptorNodeBuilder("service_identifier_descriptor"));
        builders.put(0x72, new DescriptorNodeBuilder("service_availability_descriptor"));
        builders.put(0x73, new DescriptorNodeBuilder("default_authority_descriptor"));
        builders.put(0x74, new DescriptorNodeBuilder("related_content_descriptor"));
        builders.put(0x75, new DescriptorNodeBuilder("TVA_id_descriptor"));
        builders.put(0x76, new DescriptorNodeBuilder("content_identifier_descriptor"));
        builders.put(0x77, new DescriptorNodeBuilder("time_slice_fec_identifier_descriptor"));
        builders.put(0x78, new DescriptorNodeBuilder("ECM_repetition_rate_descriptor"));
        builders.put(0x79, new DescriptorNodeBuilder("S2_satellite_delivery_system_descriptor"));
        builders.put(0x7A, new DescriptorNodeBuilder("enhanced_AC-3_descriptor"));
        builders.put(0x7B, new DescriptorNodeBuilder("DTS__descriptor"));
        builders.put(0x7C, new DescriptorNodeBuilder("AAC_descriptor"));
        builders.put(0x7D, new DescriptorNodeBuilder("XAIT_location_descriptor"));
        builders.put(0x7E, new DescriptorNodeBuilder("FTA_content_management_descriptor"));
        builders.put(0x7F, new DescriptorNodeBuilder("extension_descriptor"));
    }

    private DescriptorNodeBuilders()
    {}

    public static DescriptorNodeBuilder getBuilder(int tag)
    {
        return builders.getOrDefault(tag, new DescriptorNodeBuilder());
    }
}
