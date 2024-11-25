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
package m2tk.assistant.app.kernel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_stream_source")
public class StreamSourceEntity
{
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("bitrate")
    private Integer bitrate;
    @TableField("frame_size")
    private Integer frameSize;
    @TableField("transport_stream_id")
    private Integer transportStreamId;
    @TableField("packet_count")
    private Long packetCount;
    @TableField("stream_count")
    private Integer streamCount;
    @TableField("program_count")
    private Integer programCount;
    @TableField("is_scrambled")
    private Boolean scrambled;
    @TableField("is_ecm_present")
    private Boolean ecmPresent;
    @TableField("is_emm_present")
    private Boolean emmPresent;
    @TableField("is_pat_present")
    private Boolean patPresent;
    @TableField("is_pmt_present")
    private Boolean pmtPresent;
    @TableField("is_cat_present")
    private Boolean catPresent;
    @TableField("is_nit_actual_present")
    private Boolean nitActualPresent;
    @TableField("is_nit_other_present")
    private Boolean nitOtherPresent;
    @TableField("is_sdt_actual_present")
    private Boolean sdtActualPresent;
    @TableField("is_sdt_other_present")
    private Boolean sdtOtherPresent;
    @TableField("is_eit_pnf_actual_present")
    private Boolean eitPnfActualPresent;
    @TableField("is_eit_pnf_other_present")
    private Boolean eitPnfOtherPresent;
    @TableField("is_eit_sch_actual_present")
    private Boolean eitSchActualPresent;
    @TableField("is_eit_sch_other_present")
    private Boolean eitSchOtherPresent;
    @TableField("is_bat_present")
    private Boolean batPresent;
    @TableField("is_tdt_present")
    private Boolean tdtPresent;
    @TableField("is_tot_present")
    private Boolean totPresent;
    @TableField("source_name")
    private String sourceName;
    @TableField("source_uri")
    private String sourceUri;
}
