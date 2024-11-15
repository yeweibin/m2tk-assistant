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

package m2tk.assistant.kernel.entity;

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
    @TableField("source_name")
    private String sourceName;
    @TableField("source_uri")
    private String sourceUri;
}
