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
@TableName("t_bouquet_service_mapping")
public class BouquetServiceMappingEntity
{
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("bouquet_ref")
    private Integer bouquetRef;
    @TableField("original_network_id")
    private Integer originalNetworkId;
    @TableField("transport_stream_id")
    private Integer transportStreamId;
    @TableField("service_id")
    private Integer serviceId;
}
