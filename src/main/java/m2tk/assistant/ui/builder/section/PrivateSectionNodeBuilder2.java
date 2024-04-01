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

package m2tk.assistant.ui.builder.section;

import m2tk.assistant.template.PlainTreeNodeSyntaxPresenter;
import m2tk.assistant.template.SectionDecoder;
import m2tk.assistant.template.SyntaxField;
import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.encoding.Encoding;

import javax.swing.tree.MutableTreeNode;

public class PrivateSectionNodeBuilder2 implements TreeNodeBuilder
{
    private final SectionDecoder decoder = new SectionDecoder();
    private final PlainTreeNodeSyntaxPresenter presenter = new PlainTreeNodeSyntaxPresenter();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
        return presenter.render(syntax);
    }
}