/*
 * Copyright (c) Ye Weibin. All rights reserved.
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

public class DescriptorTemplate implements TemplateDefinition
{
    private int tag;
    private int tagExtension;
    private String name;
    private Label displayName;
    private List<String> mayOccurIns;
    private List<SyntaxFieldDefinition> descriptorSyntax;

    public int getTag()
    {
        return tag;
    }

    public void setTag(int tag)
    {
        this.tag = tag;
    }

    public int getTagExtension()
    {
        return tagExtension;
    }

    public void setTagExtension(int tagExtension)
    {
        this.tagExtension = tagExtension;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Label getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName(Label displayName)
    {
        this.displayName = displayName;
    }

    public List<String> getMayOccurIns()
    {
        return mayOccurIns;
    }

    public void setMayOccurIns(List<String> mayOccurIns)
    {
        this.mayOccurIns = mayOccurIns;
    }

    public List<SyntaxFieldDefinition> getDescriptorSyntax()
    {
        return descriptorSyntax;
    }

    public void setDescriptorSyntax(List<SyntaxFieldDefinition> descriptorSyntax)
    {
        this.descriptorSyntax = descriptorSyntax;
    }
}
