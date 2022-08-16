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

package m2tk.assistant.ui.component;

import m2tk.assistant.dbi.entity.PCRCheckEntity;
import m2tk.assistant.dbi.entity.PCREntity;
import m2tk.mpeg2.ProgramClockReference;
import net.miginfocom.swing.MigLayout;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class PCRChartPanel extends JPanel
{
    private XYSeriesCollection pcrValues;
    private XYSeriesCollection bitrateValues;
    private XYSeriesCollection accuracyValues;
    private XYSeriesCollection intervalValues;

    public PCRChartPanel()
    {
        initUI();
    }

    private void initUI()
    {
        pcrValues = new XYSeriesCollection();
        bitrateValues = new XYSeriesCollection();
        accuracyValues = new XYSeriesCollection();
        intervalValues = new XYSeriesCollection();

        setLayout(new MigLayout("fill", "[][]", "[][]"));
        add(new ChartPanel(createPCRValueChart()), "grow");
        add(new ChartPanel(createBitrateChart()), "grow, wrap");
        add(new ChartPanel(createPCRAccuracyChart()), "grow");
        add(new ChartPanel(createPCRIntervalChart()), "grow, wrap");
    }

    private JFreeChart createPCRValueChart()
    {
        XYDotRenderer renderer = new XYDotRenderer();
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setSeriesStroke(0, new BasicStroke(4.0f));

        NumberAxis xAxis = new NumberAxis("PCR位置");
        xAxis.setAutoRange(true);
        xAxis.setAutoRangeIncludesZero(false);
        xAxis.setLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        NumberAxis yAxis = new NumberAxis("PCR时间点");
        yAxis.setAutoRange(true);
        yAxis.setAutoRangeIncludesZero(false);
        yAxis.setLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        XYPlot plot = new XYPlot(pcrValues, xAxis, yAxis, renderer);
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);

        return new JFreeChart("PCR轨迹", new Font(Font.DIALOG, Font.BOLD, 18), plot, false);
    }

    private JFreeChart createBitrateChart()
    {
        XYDotRenderer renderer = new XYDotRenderer();
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesStroke(0, new BasicStroke(4.0f));

        NumberAxis xAxis = new NumberAxis("PCR位置");
        xAxis.setAutoRange(true);
        xAxis.setAutoRangeIncludesZero(false);
        xAxis.setLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        NumberAxis yAxis = new NumberAxis("瞬时码率（Mb/s）");
        yAxis.setRange(new Range(-5, 40));
        yAxis.setLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        XYPlot plot = new XYPlot(bitrateValues, xAxis, yAxis, renderer);
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);

        return new JFreeChart("码率轨迹", new Font(Font.DIALOG, Font.BOLD, 18), plot, false);
    }

    private JFreeChart createPCRAccuracyChart()
    {
        XYBarRenderer renderer = new XYBarRenderer();
        renderer.setSeriesPaint(0, Color.MAGENTA);
        renderer.setBarPainter(new StandardXYBarPainter());
        renderer.setShadowVisible(false);
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelPaint(Color.WHITE);
        renderer.setDefaultItemLabelGenerator(new StandardXYItemLabelGenerator());
        renderer.setDefaultPositiveItemLabelPosition(new ItemLabelPosition(ItemLabelAnchor.INSIDE12, TextAnchor.TOP_CENTER));

        SymbolAxis xAxis = new SymbolAxis("PCR精度区间（单位：ns）",
                                          new String[] {"-∞", "-800", "-500", "-300", "-150", "-50",
                                                        "50", "150", "300", "500", "800", "+∞"});
        xAxis.setRange(new Range(0, 11));
        xAxis.setLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        xAxis.setGridBandsVisible(false);
        NumberAxis yAxis = new NumberAxis("PCR点数");
        yAxis.setAutoRange(true);
        yAxis.setAutoRangeStickyZero(true);
        yAxis.setAutoRangeIncludesZero(true);
        yAxis.setLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        XYPlot plot = new XYPlot(accuracyValues, xAxis, yAxis, renderer);
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.addDomainMarker(0, new ValueMarker(2, Color.ORANGE, new BasicStroke(2.0f)), Layer.FOREGROUND);
        plot.addDomainMarker(0, new ValueMarker(9, Color.ORANGE, new BasicStroke(2.0f)), Layer.FOREGROUND);

        return new JFreeChart("PCR精度统计", new Font(Font.DIALOG, Font.BOLD, 18), plot, false);
    }

    private JFreeChart createPCRIntervalChart()
    {
        XYBarRenderer renderer = new XYBarRenderer();
        renderer.setSeriesPaint(0, Color.MAGENTA);
        renderer.setBarPainter(new StandardXYBarPainter());
        renderer.setShadowVisible(false);
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelPaint(Color.WHITE);
        renderer.setDefaultItemLabelGenerator(new StandardXYItemLabelGenerator());
        renderer.setDefaultPositiveItemLabelPosition(new ItemLabelPosition(ItemLabelAnchor.INSIDE12, TextAnchor.TOP_CENTER));

        SymbolAxis xAxis = new SymbolAxis("PCR间隔区间（单位：ms）",
                                          new String[]{"0", "8", "16", "24", "32", "40", "48", "56", "64", "72", "80", "+∞"});
        xAxis.setRange(0, 11);
        xAxis.setLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        xAxis.setGridBandsVisible(false);
        NumberAxis yAxis = new NumberAxis("PCR点数");
        yAxis.setAutoRange(true);
        yAxis.setAutoRangeStickyZero(true);
        yAxis.setAutoRangeIncludesZero(true);
        yAxis.setLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        XYPlot plot = new XYPlot(intervalValues, xAxis, yAxis, renderer);
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setDomainCrosshairVisible(false);
        plot.addDomainMarker(0, new ValueMarker(5, Color.ORANGE, new BasicStroke(2.0f)), Layer.FOREGROUND);

        return new JFreeChart("PCR间隔统计", new Font(Font.DIALOG, Font.BOLD, 18), plot, false);
    }

    public void update(List<PCREntity> records, List<PCRCheckEntity> checks)
    {
        pcrValues.removeAllSeries();
        bitrateValues.removeAllSeries();
        accuracyValues.removeAllSeries();
        intervalValues.removeAllSeries();

        if (!records.isEmpty())
        {
            XYSeries series = new XYSeries("PCR");
            for (PCREntity pcr : records)
            {
                series.add(pcr.getPosition(),
                           ProgramClockReference.toTimepoint(pcr.getValue()));
            }
            pcrValues.addSeries(series);
        }

        if (!checks.isEmpty())
        {
            XYSeries series1 = new XYSeries("瞬时码率（Mb/s）");
            XYSeries series2 = new XYSeries("PCR精度");
            XYSeries series3 = new XYSeries("PCR间隔");

            int[] accuracyGroups = new int[11];
            int[] intervalGroups = new int[11];

            for (PCRCheckEntity check : checks)
            {
                int _10kbps = (int) (check.getBitrate() / 10000);
                series1.add(check.getCurrentPosition(), _10kbps / 100.0);

                int groupA = getAccuracyGroup(check.getAccuracyNanos());
                int groupI = getIntervalGroup(check.getIntervalNanos());
                accuracyGroups[groupA] += 1;
                intervalGroups[groupI] += 1;
            }

            for (int i = 0; i < 11; i++)
            {
                // 加0.5是为了把柱图画在分格的中间。一个格是1.0个单位（0.5就是中间值）
                series2.add(i + 0.5, accuracyGroups[i]);
                series3.add(i + 0.5, intervalGroups[i]);
            }

            bitrateValues.addSeries(series1);
            accuracyValues.addSeries(series2);
            intervalValues.addSeries(series3);
        }
    }

    public void reset()
    {
        update(Collections.emptyList(), Collections.emptyList());
    }

    private int getAccuracyGroup(long accuracyNanos)
    {
        if (accuracyNanos <= -800)
            return 0;
        if (accuracyNanos <= -500)
            return 1;
        if (accuracyNanos <= -300)
            return 2;
        if (accuracyNanos <= -150)
            return 3;
        if (accuracyNanos <= -50)
            return 4;
        if (accuracyNanos <= 50)
            return 5;
        if (accuracyNanos <= 150)
            return 6;
        if (accuracyNanos <= 300)
            return 7;
        if (accuracyNanos <= 500)
            return 8;
        if (accuracyNanos <= 800)
            return 9;
        return 10;
    }

    private int getIntervalGroup(long intervalNanos)
    {
        if (intervalNanos <= 8_000_000L)
            return 0;
        if (intervalNanos <= 16_000_000L)
            return 1;
        if (intervalNanos <= 24_000_000L)
            return 2;
        if (intervalNanos <= 32_000_000L)
            return 3;
        if (intervalNanos <= 40_000_000L)
            return 4;
        if (intervalNanos <= 48_000_000L)
            return 5;
        if (intervalNanos <= 56_000_000L)
            return 6;
        if (intervalNanos <= 64_000_000L)
            return 7;
        if (intervalNanos <= 72_000_000L)
            return 8;
        if (intervalNanos <= 80_000_000L)
            return 9;
        else
            return 10;
    }
}
