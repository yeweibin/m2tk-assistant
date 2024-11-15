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

package m2tk.assistant.kernel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_pcr_check")
public class PCRCheckEntity
{
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("pid")
    private Integer pid;

    @TableField("pre_pcr")
    private Long previousValue;
    @TableField("pre_pct")
    private Long previousPosition;
    @TableField("cur_pcr")
    private Long currentValue;
    @TableField("cur_pct")
    private Long currentPosition;

    @TableField("bitrate")
    private Long bitrate;

    @TableField("int_ns")
    private Long intervalNanos;
    @TableField("dif_ns")
    private Long diffNanos;
    @TableField("acc_ns")
    private Long accuracyNanos;

    @TableField("is_rep_check_failed")
    private Boolean repetitionCheckFailed;
    @TableField("is_dct_check_failed")
    private Boolean discontinuityCheckFailed;
    @TableField("is_acc_check_failed")
    private Boolean accuracyCheckFailed;
}
