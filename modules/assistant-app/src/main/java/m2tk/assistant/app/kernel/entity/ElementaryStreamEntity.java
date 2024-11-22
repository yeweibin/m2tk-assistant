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
@TableName("t_elementary_stream")
public class ElementaryStreamEntity
{
    @TableId(type = IdType.INPUT)
    private Integer pid;
    @TableField("last_pct")
    private Long lastPct;
    @TableField("pkt_cnt")
    private Long packetCount;
    @TableField("pcr_cnt")
    private Long pcrCount;
    @TableField("tse_cnt")
    private Long transportErrorCount;
    @TableField("cce_cnt")
    private Long continuityErrorCount;
    @TableField("bitrate")
    private Integer bitrate;
    @TableField("ratio")
    private Double ratio;
    @TableField("stream_type")
    private Integer streamType;
    @TableField("is_scrambled")
    private Boolean scrambled;
    @TableField("category")
    private String category;
    @TableField("description")
    private String description;
}
