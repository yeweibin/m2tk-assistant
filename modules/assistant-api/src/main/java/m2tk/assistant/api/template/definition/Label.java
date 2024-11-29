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

public class Label
{
    private String text;
    private String color;
    private boolean bold;

    public static Label plain(String text)
    {
        Label label = new Label();
        label.setText(text);
        return label;
    }

    public static Label bold(String text)
    {
        Label label = new Label();
        label.setText(text);
        label.setBold(true);
        return label;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public String getColor()
    {
        return color;
    }

    public void setColor(String color)
    {
        this.color = color;
    }

    public boolean isBold()
    {
        return bold;
    }

    public void setBold(boolean bold)
    {
        this.bold = bold;
    }
}
