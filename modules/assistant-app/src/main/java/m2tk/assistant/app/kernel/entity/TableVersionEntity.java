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
@TableName("t_table_version")
public class TableVersionEntity
{
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("table_id")
    private Integer tableId;
    @TableField("table_id_ext")
    private Integer tableIdExtension;
    @TableField("version")
    private Integer version;
    @TableField("pid")
    private Integer stream;
    @TableField("pct")
    private Long position;
    @TableField("tag")
    private String tag;
}
