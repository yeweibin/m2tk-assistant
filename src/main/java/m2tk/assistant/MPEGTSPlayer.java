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

package m2tk.assistant;

import com.github.kokorin.jaffree.ffmpeg.Frame;
import com.github.kokorin.jaffree.ffmpeg.*;
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.ui.util.ComponentUtil;
import m2tk.util.BigEndian;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MPEGTSPlayer
{
    private volatile FFmpegResultFuture ffmpegTask;

    public void stop()
    {
        if (ffmpegTask != null)
        {
            ffmpegTask.graceStop();

            try
            {
                ffmpegTask.get(2, TimeUnit.SECONDS);
            } catch (Exception ex)
            {
                log.warn("等待播放任务结束时超时：{}", ex.getMessage());
            }
        }
    }

    public void playProgram(String url, int programNumber)
    {
        stop();

        CanvasFrame canvasFrame = new CanvasFrame("播放" + url + "，节目号：" + programNumber);
        JRootPane rootPane = canvasFrame.getRootPane();
        rootPane.registerKeyboardAction(e ->
                                        {
                                            canvasFrame.setVisible(false);
                                            canvasFrame.dispose();
                                        },
                                        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                                        JComponent.WHEN_IN_FOCUSED_WINDOW);
        rootPane.registerKeyboardAction(e ->
                                        {
                                            canvasFrame.setVisible(false);
                                            canvasFrame.dispose();
                                        },
                                        KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0),
                                        JComponent.WHEN_IN_FOCUSED_WINDOW);

        FFmpegResultFuture future = FFmpeg.atPath(Paths.get(System.getProperty("user.dir"), "ffmpeg"))
                                          .addInput(UrlInput.fromUrl(url).setReadAtFrameRate(true))
                                          .addOutput(new CustomFrameOutput(new NutFrameConsumer(canvasFrame))
                                                             .addMap(0, "p:" + programNumber))
                                          .setExecutorTimeoutMillis(10000)
                                          .executeAsync();
        future.toCompletableFuture()
              .whenComplete((r, t) ->
                            {
                                if (t != null)
                                {
                                    SwingUtilities.invokeLater(() -> {
                                        canvasFrame.setVisible(false);
                                        canvasFrame.dispose();

                                        JOptionPane.showMessageDialog(AssistantApp.getInstance().getMainFrame(),
                                                                      "播放器异常",
                                                                      "错误",
                                                                      JOptionPane.WARNING_MESSAGE);
                                    });
                                } else
                                {
                                    SwingUtilities.invokeLater(() -> {
                                        canvasFrame.setVisible(false);
                                        canvasFrame.dispose();
                                    });
                                }
                            });

        canvasFrame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent e)
            {
                future.graceStop();
                log.debug("结束播放：{}", url);
            }
        });
        canvasFrame.setAlwaysOnTop(true);
        canvasFrame.setVisible(true);

        ffmpegTask = future;
    }

    public void playVideo(String url, int videoPid)
    {
        stop();

        CanvasFrame canvasFrame = new CanvasFrame("播放" + url + "，视频PID：" + videoPid);
        JRootPane rootPane = canvasFrame.getRootPane();
        rootPane.registerKeyboardAction(e ->
                                        {
                                            canvasFrame.setVisible(false);
                                            canvasFrame.dispose();
                                        },
                                        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                                        JComponent.WHEN_IN_FOCUSED_WINDOW);
        rootPane.registerKeyboardAction(e ->
                                        {
                                            canvasFrame.setVisible(false);
                                            canvasFrame.dispose();
                                        },
                                        KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0),
                                        JComponent.WHEN_IN_FOCUSED_WINDOW);

        FFmpegResultFuture future = FFmpeg.atPath(Paths.get(System.getProperty("user.dir"), "ffmpeg"))
                                          .addInput(UrlInput.fromUrl(url).setReadAtFrameRate(true))
                                          .addOutput(new CustomFrameOutput(new NutFrameConsumer(canvasFrame))
                                                             .addMap(0, "i:" + videoPid))
                                          .setExecutorTimeoutMillis(10000)
                                          .executeAsync();
        future.toCompletableFuture()
              .whenComplete((r, t) ->
                            {
                                if (t != null)
                                {
                                    SwingUtilities.invokeLater(() -> {
                                        canvasFrame.setVisible(false);
                                        canvasFrame.dispose();

                                        JOptionPane.showMessageDialog(AssistantApp.getInstance().getMainFrame(),
                                                                      "播放器异常",
                                                                      "错误",
                                                                      JOptionPane.WARNING_MESSAGE);
                                    });
                                } else
                                {
                                    SwingUtilities.invokeLater(() -> {
                                        canvasFrame.setVisible(false);
                                        canvasFrame.dispose();
                                    });
                                }
                            });


        canvasFrame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent e)
            {
                future.graceStop();
                log.debug("结束播放：{}，video_pid = {}", url, videoPid);
            }
        });
        canvasFrame.setAlwaysOnTop(true);
        canvasFrame.setVisible(true);

        ffmpegTask = future;
    }

    public void playAudio(String url, int audioPid)
    {
        stop();

        CanvasFrame canvasFrame = new CanvasFrame("播放" + url + "，音频PID：" + audioPid);
        JRootPane rootPane = canvasFrame.getRootPane();
        rootPane.registerKeyboardAction(e ->
                                        {
                                            canvasFrame.setVisible(false);
                                            canvasFrame.dispose();
                                        },
                                        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                                        JComponent.WHEN_IN_FOCUSED_WINDOW);
        rootPane.registerKeyboardAction(e ->
                                        {
                                            canvasFrame.setVisible(false);
                                            canvasFrame.dispose();
                                        },
                                        KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0),
                                        JComponent.WHEN_IN_FOCUSED_WINDOW);

        FFmpegResultFuture future = FFmpeg.atPath(Paths.get(System.getProperty("user.dir"), "ffmpeg"))
                                          .addInput(UrlInput.fromUrl(url).setReadAtFrameRate(true))
                                          .addOutput(new CustomFrameOutput(new NutFrameConsumer(canvasFrame))
                                                             .addMap(0, "i:" + audioPid))
                                          .setExecutorTimeoutMillis(10000)
                                          .executeAsync();
        future.toCompletableFuture()
              .whenComplete((r, t) ->
                            {
                                if (t != null)
                                {
                                    SwingUtilities.invokeLater(() -> {
                                        canvasFrame.setVisible(false);
                                        canvasFrame.dispose();

                                        JOptionPane.showMessageDialog(AssistantApp.getInstance().getMainFrame(),
                                                                      "播放器异常",
                                                                      "错误",
                                                                      JOptionPane.WARNING_MESSAGE);
                                    });
                                } else
                                {
                                    SwingUtilities.invokeLater(() -> {
                                        canvasFrame.setVisible(false);
                                        canvasFrame.dispose();
                                    });
                                }
                            });

        canvasFrame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent e)
            {
                future.graceStop();
                log.debug("结束播放：{}，audio_pid = {}", url, audioPid);
            }
        });

        Image audioImage = ((ImageIcon) LargeIcons.SOUND_WAVE_128).getImage();
        canvasFrame.adjust(audioImage.getWidth(null), audioImage.getHeight(null));
        canvasFrame.show(audioImage);
        canvasFrame.setAlwaysOnTop(true);
        canvasFrame.setVisible(true);

        ffmpegTask = future;
    }

    private static class CustomFrameOutput extends FrameOutput
    {
        protected CustomFrameOutput(FrameConsumer consumer)
        {
            super(new NutFrameReader(consumer, ImageFormats.BGR24), "nut",
                  "rawvideo", ImageFormats.BGR24.getPixelFormat(), "pcm_s16be");
        }
    }

    private static class NutFrameConsumer implements FrameConsumer
    {
        private final CanvasFrame canvas;
        private SourceDataLine dataLine;

        public NutFrameConsumer(CanvasFrame player)
        {
            this.canvas = player;
        }

        @Override
        public void consumeStreams(List<Stream> streams)
        {
            for (Stream stream : streams)
            {
                if (stream.getType() == Stream.Type.VIDEO)
                {
                    SwingUtilities.invokeLater(() -> {
                        if (canvas.isVisible())
                        {
                            canvas.adjust(stream.getWidth(), stream.getHeight());
                        }
                    });
                }

                if (stream.getType() == Stream.Type.AUDIO)
                {
                    try
                    {
                        // pcm_s32be
                        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                                                  stream.getSampleRate(),
                                                                  16,
                                                                  stream.getChannels(),
                                                                  stream.getChannels() * 2,
                                                                  stream.getSampleRate(),
                                                                  true);
                        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);

                        dataLine = (SourceDataLine) AudioSystem.getLine(info);
                        dataLine.open(audioFormat);
                    } catch (LineUnavailableException e)
                    {
                        dataLine = null;
                    }

                    if (dataLine != null)
                        dataLine.start();
                }
            }
        }

        @Override
        public void consume(Frame frame)
        {
            if (frame == null)
                return;

            if (frame.getSamples() != null && dataLine != null && dataLine.isOpen())
            {
                byte[] data = samples2bytes(frame.getSamples());
                dataLine.write(data, 0, data.length);
            }

            if (frame.getImage() != null && canvas.isVisible())
            {
                BufferedImage image = frame.getImage();
                SwingUtilities.invokeLater(() -> canvas.show(image));
            }
        }

        private byte[] samples2bytes(int[] samples)
        {
            byte[] bytes = new byte[samples.length * 4];
            for (int i = 0; i < samples.length; i++)
            {
                BigEndian.setUINT32(bytes, i * 4, samples[i]);
            }
            return bytes;
        }
    }

    private static class Canvas extends JPanel
    {
        private Image image;

        public void setImage(Image image)
        {
            this.image = image;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            if (image == null)
            {
                String text = "无视频";
                g.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD, 18));
                int width = g.getFontMetrics().stringWidth(text);
                g.drawString(text, (getSize().width - width) / 2, getSize().height / 2);
            } else
            {
                // 根据当前播放窗口的大小调整图像比例
                g.drawImage(image.getScaledInstance(getWidth(), getHeight(), Image.SCALE_SMOOTH), 0, 0, this);
            }
        }
    }

    private static class CanvasFrame extends JFrame
    {
        private final Canvas canvas;

        public CanvasFrame(String title)
        {
            canvas = new Canvas();
            canvas.setPreferredSize(new Dimension(720, 576));
            getContentPane().add(canvas);
            setTitle(title);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            pack();
            ComponentUtil.setLocateToCenter(this);
        }

        public void adjust(int width, int height)
        {
            canvas.setPreferredSize(new Dimension(width, height));
            canvas.setSize(new Dimension(width, height));
            pack();
            ComponentUtil.setLocateToCenter(this);
        }

        public void show(Image image)
        {
            canvas.setImage(image);
        }
    }
}
