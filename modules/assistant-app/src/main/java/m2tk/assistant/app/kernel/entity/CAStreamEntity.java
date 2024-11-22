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
package m2tk.assistant.app.kernel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_ca_stream")
public class CAStreamEntity
{
    public static final int TYPE_EMM = 0;
    public static final int TYPE_ECM = 1;

    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("system_id")
    private Integer systemId;
    @TableField("stream_pid")
    private Integer streamPid;
    @TableField("stream_type")
    private Integer streamType;
    @TableField("stream_private_data")
    private byte[] streamPrivateData;
    @TableField("program_ref")
    private Integer programRef;
    @TableField("es_pid")
    private Integer elementaryStreamPid;
}
