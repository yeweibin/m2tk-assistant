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
@TableName("t_si_service")
public class SIServiceEntity
{
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("transport_stream_id")
    private Integer transportStreamId;
    @TableField("original_network_id")
    private Integer originalNetworkId;
    @TableField("service_id")
    private Integer serviceId;
    @TableField("reference_service_id")
    private Integer referenceServiceId;
    @TableField("service_type")
    private Integer serviceType;
    @TableField("service_name")
    private String serviceName;
    @TableField("service_provider")
    private String serviceProvider;
    @TableField("running_status")
    private Integer runningStatus;
    @TableField("is_free_access")
    private Boolean freeAccess;
    @TableField("is_pnf_eit_enabled")
    private Boolean presentFollowingEITEnabled;
    @TableField("is_sch_eit_enabled")
    private Boolean scheduleEITEnabled;
    @TableField("is_actual_ts")
    private Boolean actualTransportStream;
    @TableField("is_nvod_ref_srv")
    private Boolean nvodReferenceService;
    @TableField("is_nvod_shift_srv")
    private Boolean nvodTimeShiftedService;
}
