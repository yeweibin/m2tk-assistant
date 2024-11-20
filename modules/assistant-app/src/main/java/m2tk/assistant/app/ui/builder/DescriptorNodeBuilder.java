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

package m2tk.assistant.app.ui.builder;

import m2tk.assistant.app.ui.builder.descriptor.GenericDescriptorPayloadNodeBuilder;
import m2tk.encoding.Encoding;
import m2tk.mpeg2.decoder.DescriptorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import java.util.Objects;

public class DescriptorNodeBuilder implements TreeNodeBuilder
{
    private static final Logger logger = LoggerFactory.getLogger(DescriptorNodeBuilder.class);
    private final String descriptorName;
    private final DescriptorPayloadNodeBuilder payloadNodeBuilder;
    private final DescriptorDecoder decoder;

    public DescriptorNodeBuilder()
    {
        this("descriptor");
    }

    public DescriptorNodeBuilder(String name)
    {
        this(name, new GenericDescriptorPayloadNodeBuilder());
    }

    public DescriptorNodeBuilder(String name, DescriptorPayloadNodeBuilder builder)
    {
        descriptorName = Objects.requireNonNull(name);
        payloadNodeBuilder = Objects.requireNonNull(builder);
        decoder = new DescriptorDecoder();
    }

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode();
        node.setUserObject(descriptorName.equals("descriptor")
                           ? String.format("descriptor (0x%02X)", decoder.getTag())
                           : descriptorName);

        node.add(create(String.format("tag = 0x%02X", decoder.getTag())));
        node.add(create(String.format("length = %d", decoder.getPayloadLength())));

        try
        {
            payloadNodeBuilder.build(node, decoder.getPayload());
        } catch (Exception ex)
        {
            logger.warn("描述符解析错误：{}", ex.getMessage());
        }

        return node;
    }
}
