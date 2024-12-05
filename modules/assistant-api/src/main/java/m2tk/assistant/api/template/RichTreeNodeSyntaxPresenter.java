/*
 *  Copyright (c) M2TK Project. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package m2tk.assistant.api.template;

import lombok.Data;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class RichTreeNodeSyntaxPresenter extends PlainTreeNodeSyntaxPresenter
{
    @Data
    public static class NodeContext
    {
        private String label;
        private SyntaxField syntax;

        @Override
        public String toString()
        {
            return label;
        }
    }

    @Override
    public MutableTreeNode render(SyntaxField syntax)
    {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) super.render(syntax);
        if (node != null)
        {
            NodeContext context = new NodeContext();
            context.label = (String) node.getUserObject();
            context.syntax = syntax;
            node.setUserObject(context);
        }
        return node;
    }
}
