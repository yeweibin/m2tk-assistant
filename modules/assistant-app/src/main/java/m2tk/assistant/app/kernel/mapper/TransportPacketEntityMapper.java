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
import m2tk.assistant.app.kernel.entity.TransportPacketEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface TransportPacketEntityMapper extends BaseMapper<TransportPacketEntity>
{
    @Update("""
            DELETE FROM `t_transport_packet`
            WHERE `id` IN
             (SELECT `tmp`.`id` FROM
               (SELECT `id` FROM `t_transport_packet`
                WHERE `pid` = #{pid}
                ORDER BY `id` ASC
                LIMIT #{count}) AS `tmp`
             )
            """)
    void deleteOldestN(@Param("pid") int pid, @Param("count") int count);

    @Update("""
            DELETE FROM `t_transport_packet`
            WHERE `id` IN
             (SELECT `tmp`.`id` FROM
               (SELECT `id` FROM `t_transport_packet`
                WHERE `pid` = #{pid}
                ORDER BY `id` DESC
                LIMIT #{count}) AS `tmp`
             )
            """)
    void deleteRecentN(@Param("pid") int pid, @Param("count") int count);
}
