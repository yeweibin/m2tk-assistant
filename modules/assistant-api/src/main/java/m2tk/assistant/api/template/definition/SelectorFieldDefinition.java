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

public class SelectorFieldDefinition implements SyntaxFieldDefinition
{
    public static final String ATTRIBUTE_NOT_AVAILABLE = "n/a";
    public static final String IMPLICIT_LENGTH = "implicit";
    private static final String UNSIGNED_INTEGER_PATTERN = "0|[1-9]\\d*";
    private static final String SIGNED_INTEGER_PATTERN = "0|-?[1-9]\\d*";

    private String name;
    private String length;
    private String lengthField;
    private String lengthCorrection;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getLength()
    {
        return length;
    }

    public void setLength(String length)
    {
        this.length = length;
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

    public int getLengthValue()
    {
        return isDirectLength() ? Integer.parseUnsignedInt(length) : 0;
    }

    public int getLengthCorrectionValue()
    {
        return hasLengthCorrection() ? Integer.parseInt(lengthCorrection) : 0;
    }

    public boolean isDirectLength()
    {
        // 非负整数
        return length != null && !length.isEmpty() && length.matches(UNSIGNED_INTEGER_PATTERN);
    }

    public boolean isIndirectLength()
    {
        return lengthField != null && !lengthField.isEmpty() && !lengthField.equals(ATTRIBUTE_NOT_AVAILABLE);
    }

    public boolean isImplicitLength()
    {
        return lengthField != null && !lengthField.isEmpty() && lengthField.equals(IMPLICIT_LENGTH);
    }

    public boolean hasLengthCorrection()
    {
        return lengthCorrection != null && !lengthCorrection.isEmpty() && lengthCorrection.matches(SIGNED_INTEGER_PATTERN);
    }

    @Override
    public boolean verify()
    {
        return name != null && !name.isEmpty();
    }
}
