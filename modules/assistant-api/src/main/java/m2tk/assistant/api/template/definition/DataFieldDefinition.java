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

public class DataFieldDefinition implements SyntaxFieldDefinition
{
    public static final String ATTRIBUTE_NOT_AVAILABLE = "n/a";
    public static final String IMPLICIT_LENGTH = "implicit";
    private static final String UNSIGNED_INTEGER_PATTERN = "0|[1-9]\\d*";
    private static final String SIGNED_INTEGER_PATTERN = "0|-?[1-9]\\d*";

    private String name;
    private String encoding;
    private String stringType;
    private String length;
    private String lengthField;
    private String lengthCorrection;
    private FieldPresentation presentation;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getEncoding()
    {
        return encoding;
    }

    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }

    public String getStringType()
    {
        return stringType;
    }

    public void setStringType(String stringType)
    {
        this.stringType = stringType;
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

    public FieldPresentation getPresentation()
    {
        return presentation;
    }

    public void setPresentation(FieldPresentation presentation)
    {
        this.presentation = presentation;
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
        return (name != null && !name.isEmpty()) &&
               (encoding != null && !encoding.isEmpty()) &&
               (isDirectLength() || isIndirectLength());
    }

    public static DataFieldDefinition ofDirectLength(String name, String encoding, String stringType,
                                                     String length,
                                                     FieldPresentation presentation)
    {
        DataFieldDefinition definition = new DataFieldDefinition();
        definition.setName(name);
        definition.setEncoding(encoding);
        definition.setStringType(stringType);
        definition.setLength(length);
        definition.setLengthField(ATTRIBUTE_NOT_AVAILABLE);
        definition.setLengthCorrection(ATTRIBUTE_NOT_AVAILABLE);
        definition.setPresentation(presentation);
        return definition;
    }

    public static DataFieldDefinition ofIndirectLength(String name, String encoding, String stringType,
                                                       String lengthField, String lengthCorrection,
                                                       FieldPresentation presentation)
    {
        DataFieldDefinition definition = new DataFieldDefinition();
        definition.setName(name);
        definition.setEncoding(encoding);
        definition.setStringType(stringType);
        definition.setLength(ATTRIBUTE_NOT_AVAILABLE);
        definition.setLengthField(lengthField);
        definition.setLengthCorrection(lengthCorrection);
        definition.setPresentation(presentation);
        return definition;
    }

    public static DataFieldDefinition ofImplicitLength(String name, String encoding, String stringType,
                                                       String lengthCorrection,
                                                       FieldPresentation presentation)
    {
        DataFieldDefinition definition = new DataFieldDefinition();
        definition.setName(name);
        definition.setEncoding(encoding);
        definition.setStringType(stringType);
        definition.setLength(ATTRIBUTE_NOT_AVAILABLE);
        definition.setLengthField(IMPLICIT_LENGTH);
        definition.setLengthCorrection(lengthCorrection);
        definition.setPresentation(presentation);
        return definition;
    }

    public static DataFieldDefinition number(String name,
                                             String encoding, String length,
                                             FieldPresentation presentation)
    {
        return ofDirectLength(name, encoding, ATTRIBUTE_NOT_AVAILABLE, length, presentation);
    }

    public static DataFieldDefinition text(String name,
                                           String stringType, String lengthField, String lengthCorrection,
                                           FieldPresentation presentation)
    {
        return ofIndirectLength(name, "text", stringType, lengthField, lengthCorrection, presentation);
    }

    public static DataFieldDefinition bytes(String name,
                                            String lengthField, String lengthCorrection,
                                            FieldPresentation presentation)
    {
        return ofIndirectLength(name, "octets", ATTRIBUTE_NOT_AVAILABLE, lengthField, lengthCorrection, presentation);
    }
}
