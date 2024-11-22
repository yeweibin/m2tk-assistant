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
package m2tk.assistant.app.kernel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import m2tk.assistant.app.kernel.entity.SIEventEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface SIEventEntityMapper extends BaseMapper<SIEventEntity>
{
    @Update("""
            UPDATE `t_si_event`
            SET
             `language_code` = #{language},
             `event_name` = #{title},
             `event_description` = #{description}
            WHERE `id` = #{id}
            """)
    int updateEventDesc(@Param("id") int id,
                        @Param("language") String languageCode,
                        @Param("title") String eventTitle,
                        @Param("description") String eventDescription);

    @Update("""
            UPDATE `t_si_event`
            SET
             `nvod_time_shifted` = true,
             `reference_service_id` = #{service},
             `reference_event_id` = #{event}
            WHERE `id` = #{id}
            """)
    int updateNVODEventDesc(@Param("id") int id,
                            @Param("service") int referenceServiceId,
                            @Param("event") int referenceEventId);
}
