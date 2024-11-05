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

package m2tk.assistant.template.definition;

import java.util.List;

public class TableTemplate implements TemplateDefinition
{
    private String name;
    private String group;
    private List<TableId> tableIds;
    private List<SyntaxFieldDefinition> tableSyntax;
    private UniqueKey uniqueKey;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getGroup()
    {
        return group;
    }

    public void setGroup(String group)
    {
        this.group = group;
    }

    public List<TableId> getTableIds()
    {
        return tableIds;
    }

    public void setTableIds(List<TableId> tableIds)
    {
        this.tableIds = tableIds;
    }

    public List<SyntaxFieldDefinition> getTableSyntax()
    {
        return tableSyntax;
    }

    public void setTableSyntax(List<SyntaxFieldDefinition> tableSyntax)
    {
        this.tableSyntax = tableSyntax;
    }

    public UniqueKey getUniqueKey()
    {
        return uniqueKey;
    }

    public void setUniqueKey(UniqueKey uniqueKey)
    {
        this.uniqueKey = uniqueKey;
    }
}
