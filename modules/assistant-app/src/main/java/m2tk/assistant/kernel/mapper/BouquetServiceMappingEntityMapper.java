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

package m2tk.assistant.kernel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import m2tk.assistant.kernel.entity.BouquetServiceMappingEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface BouquetServiceMappingEntityMapper extends BaseMapper<BouquetServiceMappingEntity>
{
    @Select("""
            SELECT * FROM `t_bouquet_service_mapping`
            WHERE `bouquet_ref` = #{ref}
            """)
    List<BouquetServiceMappingEntity> selectBouquetMappings(@Param("ref") int bouquetRef);

    @Select("""
            SELECT COUNT(`id`) FROM `t_bouquet_service_mapping`
            WHERE `bouquet_ref` = #{ref}
            """)
    int countBouquetMappings(@Param("ref") int bouquetRef);
}
