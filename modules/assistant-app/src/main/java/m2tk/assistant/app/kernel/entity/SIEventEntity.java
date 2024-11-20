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

import java.time.LocalDateTime;

@Data
@TableName("t_si_event")
public class SIEventEntity
{
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("transport_stream_id")
    private Integer transportStreamId;
    @TableField("original_network_id")
    private Integer originalNetworkId;
    @TableField("service_id")
    private Integer serviceId;
    @TableField("event_id")
    private Integer eventId;
    @TableField("reference_service_id")
    private Integer referenceServiceId;
    @TableField("reference_event_id")
    private Integer referenceEventId;
    @TableField("start_time")
    private LocalDateTime startTime;
    @TableField("duration")
    private Integer duration;
    @TableField("running_status")
    private Integer runningStatus;
    @TableField("event_name")
    private String eventName;
    @TableField("event_description")
    private String eventDescription;
    @TableField("language_code")
    private String languageCode;

    @TableField("is_free_access")
    private Boolean freeAccess;
    @TableField("is_present_evt")
    private Boolean presentEvent;
    @TableField("is_schedule_evt")
    private Boolean scheduleEvent;
    @TableField("is_nvod_shift_evt")
    private Boolean nvodTimeShiftedEvent;
}
