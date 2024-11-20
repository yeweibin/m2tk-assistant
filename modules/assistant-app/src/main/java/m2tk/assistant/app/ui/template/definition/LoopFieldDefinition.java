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

package m2tk.assistant.app.ui.template.definition;

import cn.hutool.core.util.StrUtil;

import java.util.List;

public class LoopFieldDefinition implements SyntaxFieldDefinition
{
    public static final String ATTRIBUTE_NOT_AVAILABLE = "n/a";
    public static final String IMPLICIT_LENGTH = "implicit";
    private static final String SIGNED_INTEGER_PATTERN = "0|-?[1-9]\\d*";

    private String name;
    private String lengthType;
    private String lengthField;
    private String lengthCorrection;
    private LoopPresentation presentation;
    private List<SyntaxFieldDefinition> body;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getLengthType()
    {
        return lengthType;
    }

    public void setLengthType(String lengthType)
    {
        this.lengthType = lengthType;
    }

    public String getLengthField()
    {
        return lengthField;
    }

    public void setLengthField(String lengthField)
    {
        this.lengthField = lengthField;
    }

    public String getLengthCorrection()
    {
        return lengthCorrection;
    }

    public void setLengthCorrection(String lengthCorrection)
    {
        this.lengthCorrection = lengthCorrection;
    }

    public LoopPresentation getPresentation()
    {
        return presentation;
    }

    public void setPresentation(LoopPresentation presentation)
    {
        this.presentation = presentation;
    }

    public List<SyntaxFieldDefinition> getBody()
    {
        return body;
    }

    public void setBody(List<SyntaxFieldDefinition> body)
    {
        this.body = body;
    }

    public boolean isLengthInBytes()
    {
        return StrUtil.isEmpty(lengthType) || lengthField.equals("length_in_bytes");
    }

    public boolean isLengthOfCount()
    {
        return StrUtil.isNotEmpty(lengthType) && lengthType.equals("count");
    }

    public boolean isImplicitLength()
    {
        return StrUtil.isNotEmpty(lengthField) && lengthField.equals(IMPLICIT_LENGTH);
    }

    public int getLengthCorrectionValue()
    {
        return hasLengthCorrection() ? Integer.parseInt(lengthCorrection) : 0;
    }

    public boolean hasLengthCorrection()
    {
        return StrUtil.isNotEmpty(lengthCorrection) && lengthCorrection.matches(SIGNED_INTEGER_PATTERN);
    }

    @Override
    public boolean verify()
    {
        return StrUtil.isNotEmpty(name) &&
               StrUtil.isNotEmpty(lengthField) &&
               (body != null && !body.isEmpty()) &&
               (isLengthInBytes() || isLengthOfCount());
    }
}