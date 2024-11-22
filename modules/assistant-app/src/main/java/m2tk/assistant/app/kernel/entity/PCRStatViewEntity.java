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

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("v_pcr_stat")
public class PCRStatViewEntity
{
    @TableId
    private Integer pid;
    @TableField("pcr_count")
    private Long pcrCount;
    @TableField("avg_bitrate")
    private Long avgBitrate;
    @TableField("avg_interval")
    private Long avgInterval;
    @TableField("min_interval")
    private Long minInterval;
    @TableField("max_interval")
    private Long maxInterval;
    @TableField("avg_accuracy")
    private Long avgAccuracy;
    @TableField("min_accuracy")
    private Long minAccuracy;
    @TableField("max_accuracy")
    private Long maxAccuracy;
    @TableField("rep_errors")
    private Long repetitionErrors;
    @TableField("dct_errors")
    private Long discontinuityErrors;
    @TableField("acc_errors")
    private Long accuracyErrors;
}
