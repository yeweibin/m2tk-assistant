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

public class LoopPresentation
{
    private boolean noLoopHeader;
    private Label loopHeader;
    private Label loopEmpty;
    private LoopEntryPresentation loopEntryPresentation;

    public static LoopPresentation forEmptyLoop(String text)
    {
        LoopPresentation presentation = new LoopPresentation();
        presentation.setNoLoopHeader(true);
        presentation.setLoopEmpty(Label.plain(text));
        return presentation;
    }

    public boolean isNoLoopHeader()
    {
        return (loopHeader == null) || noLoopHeader;
    }

    public void setNoLoopHeader(boolean noLoopHeader)
    {
        this.noLoopHeader = noLoopHeader;
    }

    public Label getLoopHeader()
    {
        return loopHeader;
    }

    public void setLoopHeader(Label loopHeader)
    {
        this.loopHeader = loopHeader;
    }

    public Label getLoopEmpty()
    {
        return loopEmpty;
    }

    public void setLoopEmpty(Label loopEmpty)
    {
        this.loopEmpty = loopEmpty;
    }

    public LoopEntryPresentation getLoopEntryPresentation()
    {
        return loopEntryPresentation;
    }

    public void setLoopEntryPresentation(LoopEntryPresentation loopEntryPresentation)
    {
        this.loopEntryPresentation = loopEntryPresentation;
    }
}
