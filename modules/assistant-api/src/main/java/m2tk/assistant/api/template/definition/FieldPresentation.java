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

import java.util.Collections;
import java.util.List;

public class FieldPresentation
{
    private Label prefix;
    private Label format;
    private List<ValueMapping> valueMappings;

    public static FieldPresentation format(String format)
    {
        FieldPresentation presentation = new FieldPresentation();
        presentation.setFormat(Label.plain(format));
        presentation.setValueMappings(Collections.emptyList());
        return presentation;
    }

    public static FieldPresentation of(String prefix)
    {
        FieldPresentation presentation = new FieldPresentation();
        presentation.setPrefix(Label.plain(prefix));
        presentation.setValueMappings(Collections.emptyList());
        return presentation;
    }

    public static FieldPresentation of(String prefix, String format)
    {
        FieldPresentation presentation = new FieldPresentation();
        presentation.setPrefix(Label.plain(prefix));
        presentation.setFormat(Label.plain(format));
        presentation.setValueMappings(Collections.emptyList());
        return presentation;
    }

    public Label getPrefix()
    {
        return prefix;
    }

    public void setPrefix(Label prefix)
    {
        this.prefix = prefix;
    }

    public Label getFormat()
    {
        return format;
    }

    public void setFormat(Label format)
    {
        this.format = format;
    }

    public List<ValueMapping> getValueMappings()
    {
        return valueMappings;
    }

    public void setValueMappings(List<ValueMapping> valueMappings)
    {
        this.valueMappings = valueMappings;
    }

    public boolean hasPrefix()
    {
        return prefix != null;
    }

    public boolean hasFormat()
    {
        return format != null;
    }

    public boolean hasValueMappings()
    {
        return valueMappings != null && !valueMappings.isEmpty();
    }
}
