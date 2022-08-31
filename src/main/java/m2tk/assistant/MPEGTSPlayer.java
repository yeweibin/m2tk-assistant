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

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.ui.util.ComponentUtil;
import m2tk.assistant.util.FFmpegTSFrameGrabber;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_QUIET;

@Slf4j
public class MPEGTSPlayer
{
    private final BlockingDeque<Frame> vQueue;
    private final BlockingDeque<Frame> aQueue;
    private Rectangle bounds;

    private CanvasFrame canvasFrame;
    private volatile long timelineStart = -1;
    private volatile long prevAudioTimestamp = -1;
    private volatile long prevAudioPosition = 0;
    private boolean isFullScreen;

    public MPEGTSPlayer()
    {
        vQueue = new LinkedBlockingDeque<>();
        aQueue = new LinkedBlockingDeque<>();
    }

    public void stop()
    {
        if (canvasFrame != null && canvasFrame.isVisible())
        {
            canvasFrame.setVisible(false);
            canvasFrame.dispose();
        }
    }

    public void playVideoAndAudio(InputStream in, int videoPid, int audioPid) throws FFmpegTSFrameGrabber.Exception
    {
        avutil.av_log_set_level(AV_LOG_QUIET);

        FFmpegTSFrameGrabber grabber = new FFmpegTSFrameGrabber(in, 0);
        grabber.setCloseInputStream(true);
        grabber.setVideoStreamPid(videoPid);
        grabber.setAudioStreamPid(audioPid);
        grabber.start();

        double fps = grabber.getVideoFrameRate();
        int width = grabber.getImageWidth();
        int height = grabber.getImageHeight();
        int frameGaps = (int) (1000 / fps);
        int sampleFormat = grabber.getSampleFormat();
        SourceDataLine sourceDataLine = initSourceDataLine(grabber);
        PlaybackTimer playbackTimer = new PlaybackTimer(sourceDataLine);

        canvasFrame = new CanvasFrame("播放", CanvasFrame.getDefaultGamma() / grabber.getGamma());

        Future<?> task0 = ThreadUtil.execAsync(() -> grabFrames(canvasFrame, grabber, playbackTimer));
        Future<?> task1 = ThreadUtil.execAsync(() -> processVideoFrame(canvasFrame, frameGaps, playbackTimer));
        Future<?> task2 = ThreadUtil.execAsync(() -> processAudioFrame(canvasFrame, sourceDataLine, sampleFormat));

        if (width > 1280)
        {
            double ratio = 1.0 * height / width;
            width = 1280;
            height = (int) (width * ratio);
        }
        canvasFrame.setCanvasSize(width, height);
        ComponentUtil.setLocateToCenter(canvasFrame);
        canvasFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        canvasFrame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent e)
            {
                vQueue.clear();
                aQueue.clear();
                try
                {
                    if (sourceDataLine != null)
                        sourceDataLine.close();

                    task2.get();
                    task1.get();
                    task0.get();
                    grabber.close();
                } catch (Exception ex)
                {
                    log.warn("{}", ex.getMessage());
                }
                log.debug("播放进程结束");
            }
        });
        JRootPane rootPane = canvasFrame.getRootPane();
        rootPane.registerKeyboardAction(e -> canvasFrame.setVisible(false),
                                        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                                        JComponent.WHEN_IN_FOCUSED_WINDOW);
        rootPane.registerKeyboardAction(e -> canvasFrame.setVisible(false),
                                        KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0),
                                        JComponent.WHEN_IN_FOCUSED_WINDOW);
        rootPane.registerKeyboardAction(e -> {
                                            if (isFullScreen)
                                            {
                                                canvasFrame.setBounds(bounds);
                                                isFullScreen = false;
                                            } else
                                            {
                                                bounds = canvasFrame.getBounds();
                                                canvasFrame.setExtendedState(canvasFrame.getExtendedState() | java.awt.Frame.MAXIMIZED_BOTH);
                                                isFullScreen = true;
                                            }
                                        },
                                        KeyStroke.getKeyStroke(KeyEvent.VK_F, 0),
                                        JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void grabFrames(CanvasFrame canvas, FrameGrabber grabber, PlaybackTimer timer)
    {
        timelineStart = -1;
        prevAudioPosition = 0;
        prevAudioTimestamp = -1;
        vQueue.clear();
        aQueue.clear();
        timer.start();

        while (canvas.isVisible())
        {
            try
            {
                Frame frame = grabber.grab();
                if (frame == null)
                    continue;
                if (frame.image != null)
                    vQueue.add(frame.clone());
                if (frame.samples != null)
                    aQueue.add(frame.clone());
            } catch (Exception ex)
            {
                log.warn("[抽帧] {}", ex.getMessage());
            }
        }
        log.debug("抽帧线程结束");
    }

    private void processVideoFrame(CanvasFrame canvas, int frameGaps, PlaybackTimer timer)
    {
        // 如果视频的fps很小或接近于零，则会导致frameGaps超级大。这里做一个保护。
        int waitQueue = Math.min(50, frameGaps / 2);
        while (canvas.isVisible())
        {
            try
            {
                Frame image = vQueue.poll(waitQueue, TimeUnit.MILLISECONDS);
                if (image == null)
                    continue;

                if (timelineStart == -1 || image.timestamp < timelineStart)
                {
                    image.close();
                    continue;
                }

                long vaOffset = image.timestamp - timelineStart - timer.elapsedMicros();
                if (vaOffset > 0)
                {
                    ThreadUtil.safeSleep(Math.min(frameGaps, vaOffset / 1000));
                }

                // ??? 原示例是直接showImage，并没有在EDT里showImage，可以吗？
                Runnable r = () ->
                {
                    canvas.showImage(image);
                    image.close();
                };
                if (EventQueue.isDispatchThread())
                    r.run();
                else
                    EventQueue.invokeLater(r);
            } catch (Exception ex)
            {
                log.warn("[视频] {}", ex.getMessage());
            }
        }
        log.debug("视频处理线程结束");
    }

    private void processAudioFrame(CanvasFrame canvas, SourceDataLine sourceDataLine, int sampleFormat)
    {
        while (canvas.isVisible())
        {
            try
            {
                Frame audio = aQueue.poll(50, TimeUnit.MILLISECONDS);
                if (audio != null)
                {
                    playAudioSample(sourceDataLine, sampleFormat, audio.timestamp, audio.samples);
                    audio.close();
                }
            } catch (Exception ex)
            {
                log.warn("[音频] {}", ex.getMessage());
            }
        }
        log.debug("音频处理线程结束");
    }

    private void playAudioSample(SourceDataLine sourceDataLine, int sampleFormat, long timestamp, Buffer[] samples)
    {
        if (sourceDataLine == null || samples == null)
            return;

        float vol = 1;//音量
        byte[] data = null;

        switch (sampleFormat)
        {
            case avutil.AV_SAMPLE_FMT_FLTP://平面型左右声道分开
            case avutil.AV_SAMPLE_FMT_S16P://平面型左右声道分开
            {
                if (samples.length < 2 || samples[0] == null || samples[1] == null)
                    return;
                ByteBuffer TLData = toByteBuffer(samples[0], vol);
                ByteBuffer TRData = toByteBuffer(samples[1], vol);
                data = combineChannels(TLData.array(), TRData.array());
                break;
            }
            case avutil.AV_SAMPLE_FMT_S16://非平面型左右声道在一个buffer中
            case avutil.AV_SAMPLE_FMT_FLT://float非平面型
            {
                if (samples.length < 1 || samples[0] == null)
                    return;
                ByteBuffer TLData = toByteBuffer(samples[0], vol);
                data = TLData.array();
                break;
            }
            default:
                break;
        }

        if (data == null)
            return;

        long currAudioPosition = sourceDataLine.getMicrosecondPosition();
        if (timelineStart == -1)
        {
            timelineStart = timestamp;
        } else
        {
            long offset = timestamp - prevAudioTimestamp - (currAudioPosition - prevAudioPosition);
            ThreadUtil.safeSleep(Math.min(10, offset / 1000));
        }
        sourceDataLine.write(data, 0, data.length);
        prevAudioPosition = currAudioPosition;
        prevAudioTimestamp = timestamp;
    }

    private SourceDataLine initSourceDataLine(FrameGrabber grabber)
    {
        AudioFormat af = null;
        switch (grabber.getSampleFormat())
        {
            case avutil.AV_SAMPLE_FMT_U8://无符号short 8bit
                break;
            case avutil.AV_SAMPLE_FMT_S16://有符号short 16bit
            case avutil.AV_SAMPLE_FMT_FLT:
            case avutil.AV_SAMPLE_FMT_S16P://有符号short 16bit,平面型
            case avutil.AV_SAMPLE_FMT_FLTP://float 平面型 需转为16bit short
                af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                     grabber.getSampleRate(),
                                     16,
                                     grabber.getAudioChannels(),
                                     grabber.getAudioChannels() * 2,
                                     grabber.getSampleRate(),
                                     true);
                break;
            case avutil.AV_SAMPLE_FMT_S32:
            case avutil.AV_SAMPLE_FMT_DBL:
            case avutil.AV_SAMPLE_FMT_U8P:
            case avutil.AV_SAMPLE_FMT_DBLP:
                break;
            case avutil.AV_SAMPLE_FMT_S32P://有符号short 32bit，平面型，但是32bit的话可能电脑声卡不支持，这种音乐也少见
                af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                     grabber.getSampleRate(),
                                     32,
                                     grabber.getAudioChannels(),
                                     grabber.getAudioChannels() * 2,
                                     grabber.getSampleRate(),
                                     true);
                break;
            case avutil.AV_SAMPLE_FMT_S64://有符号short 64bit 非平面型
            case avutil.AV_SAMPLE_FMT_S64P://有符号short 64bit平面型
                break;
            default:
                log.warn("不支持的声音格式：{}", grabber.getSampleFormat());
                return null;
        }

        try
        {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, af, AudioSystem.NOT_SPECIFIED);
            SourceDataLine dataLine = (SourceDataLine) AudioSystem.getLine(info);
            dataLine.open(af);
            dataLine.start();
            return dataLine;
        } catch (LineUnavailableException e)
        {
            return null;
        }
    }

    private ByteBuffer toByteBuffer(Buffer input, float volume)
    {
        if (input instanceof FloatBuffer)
            return floatToByteValue((FloatBuffer) input, volume);
        if (input instanceof ShortBuffer)
            return shortToByteValue((ShortBuffer) input, volume);
        return (ByteBuffer) input;
    }

    private ByteBuffer shortToByteValue(ShortBuffer arr, float vol)
    {
        int len = arr.capacity();
        ByteBuffer bb = ByteBuffer.allocate(len * 2);
        for (int i = 0; i < len; i++)
        {
            bb.putShort(i * 2, (short) (arr.get(i) * vol));
        }
        return bb; // 默认转为大端序
    }

    private ByteBuffer floatToByteValue(FloatBuffer arr, float vol)
    {
        int len = arr.capacity();
        float f;
        float v;
        ByteBuffer res = ByteBuffer.allocate(len * 2);
        v = 32768.0f * vol;
        for (int i = 0; i < len; i++)
        {
            f = arr.get(i) * v;//Ref：https://stackoverflow.com/questions/15087668/how-to-convert-pcm-samples-in-byte-array-as-floating-point-numbers-in-the-range
            if (f > v) f = v;
            if (f < -v) f = v;
            //默认转为大端序
            res.putShort(i * 2, (short) f);//注意乘以2，因为一次写入两个字节。
        }
        return res;
    }

    private byte[] combineChannels(byte[] tl, byte[] tr)
    {
        byte[] combined = new byte[tl.length + tr.length];
        int k = 0;
        for (int i = 0; i < tl.length; i = i + 2)
        {
            //混合两个声道。
            combined[4 * k    ] = tl[i    ];
            combined[4 * k + 1] = tl[i + 1];
            combined[4 * k + 2] = tr[i    ];
            combined[4 * k + 3] = tr[i + 1];
            k++;
        }
        return combined;
    }

    private static class PlaybackTimer
    {
        private volatile long startTime = -1L;
        private final DataLine soundLine;

        public PlaybackTimer(DataLine soundLine)
        {
            this.soundLine = soundLine;
        }

        public void start()
        {
            if (soundLine == null)
            {
                startTime = System.nanoTime();
            }
        }

        public long elapsedMicros()
        {
            if (soundLine == null)
            {
                if (startTime < 0)
                    throw new IllegalStateException("PlaybackTimer not initialized.");

                return (System.nanoTime() - startTime) / 1000;
            } else
            {
                return soundLine.getMicrosecondPosition();
            }
        }
    }
}
