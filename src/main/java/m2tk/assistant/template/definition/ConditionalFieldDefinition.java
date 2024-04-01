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

public class ConditionalFieldDefinition implements SyntaxFieldDefinition
{
    private Condition condition;
    private List<SyntaxFieldDefinition> thenPart;
    private List<SyntaxFieldDefinition> elsePart;

    public Condition getCondition()
    {
        return condition;
    }

    public void setCondition(Condition condition)
    {
        this.condition = condition;
    }

    public List<SyntaxFieldDefinition> getThenPart()
    {
        return thenPart;
    }

    public void setThenPart(List<SyntaxFieldDefinition> thenPart)
    {
        this.thenPart = thenPart;
    }

    public List<SyntaxFieldDefinition> getElsePart()
    {
        return elsePart;
    }

    public void setElsePart(List<SyntaxFieldDefinition> elsePart)
    {
        this.elsePart = elsePart;
    }

    @Override
    public boolean verify()
    {
        return condition != null && thenPart != null && !thenPart.isEmpty();
    }
}
