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
import m2tk.assistant.app.kernel.entity.ElementaryStreamEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ElementaryStreamEntityMapper extends BaseMapper<ElementaryStreamEntity>
{
    @Update("""
            UPDATE `t_elementary_stream`
            SET
             `tse_cnt` = `tse_cnt` + #{tse},
             `cce_cnt` = `cce_cnt` + #{cce}
            WHERE
             `pid` = #{pid}
            """)
    void accumulateStreamErrors(@Param("pid") int pid,
                                @Param("tse") int transportErrors,
                                @Param("cce") int continuityErrors);
}
