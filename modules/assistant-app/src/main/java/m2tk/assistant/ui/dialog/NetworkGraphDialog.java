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

package m2tk.assistant.ui.dialog;

import hu.kazocsaba.imageviewer.ImageViewer;
import hu.kazocsaba.imageviewer.ResizeStrategy;
import m2tk.assistant.ui.util.ComponentUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class NetworkGraphDialog extends JFrame
{
    private transient ImageViewer viewer;

    public NetworkGraphDialog()
    {
        initUI();
    }

    private void initUI()
    {
        viewer = new ImageViewer();
        viewer.setResizeStrategy(ResizeStrategy.NO_RESIZE);
        getContentPane().add(viewer.getComponent(), BorderLayout.CENTER);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("网络结构");
    }

    public void showImage(BufferedImage image)
    {
        viewer.setImage(image);
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.75, 0.75);
        setVisible(true);
    }
}
