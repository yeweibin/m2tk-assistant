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
package m2tk.assistant.api.template.definition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class M2TKTemplate
{
    private final List<TableTemplate> tableTemplates;
    private final List<DescriptorTemplate> descriptorTemplates;

    public M2TKTemplate(List<TemplateDefinition> templates)
    {
        tableTemplates = new ArrayList<>();
        descriptorTemplates = new ArrayList<>();
        for (TemplateDefinition template : templates)
        {
            if (template instanceof TableTemplate tableTemplate)
                tableTemplates.add(tableTemplate);
            if (template instanceof DescriptorTemplate descriptorTemplate)
                descriptorTemplates.add(descriptorTemplate);
        }
    }

    public List<TableTemplate> getTableTemplates()
    {
        return Collections.unmodifiableList(tableTemplates);
    }

    public List<DescriptorTemplate> getDescriptorTemplates()
    {
        return Collections.unmodifiableList(descriptorTemplates);
    }
}
