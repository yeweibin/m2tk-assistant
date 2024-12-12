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
package m2tk.assistant.ebs;

import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.api.template.DescriptorDecoder;
import m2tk.assistant.api.template.SectionDecoder;
import m2tk.assistant.api.template.TemplateReader;
import m2tk.assistant.api.template.definition.DescriptorTemplate;
import m2tk.assistant.api.template.definition.M2TKTemplate;
import m2tk.assistant.api.template.definition.TableTemplate;
import org.pf4j.Plugin;

import java.util.List;

@Slf4j
public class EBSPlugin extends Plugin
{
    @Override
    public void start()
    {
        TemplateReader reader = new TemplateReader();
        M2TKTemplate ebsTemplate = reader.parse(getClass().getResource("/template/EBSTemplate.xml"));
        if (ebsTemplate != null)
        {
            List<TableTemplate> tableTemplates = ebsTemplate.getTableTemplates();
            List<DescriptorTemplate> descriptorTemplates = ebsTemplate.getDescriptorTemplates();
            tableTemplates.forEach(SectionDecoder::registerTemplate);
            descriptorTemplates.forEach(DescriptorDecoder::registerTemplate);
            log.info("[EBS] {} table templates and {} descriptor templates are loaded.",
                     tableTemplates.size(),
                     descriptorTemplates.size());
        }
    }
}
