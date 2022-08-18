package m2tk.assistant.ui.builder;

import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public interface TreeNodeBuilder
{
    MutableTreeNode build(Encoding encoding);

    default MutableTreeNode create(Object userObject)
    {
        return new DefaultMutableTreeNode(userObject);
    }
}
