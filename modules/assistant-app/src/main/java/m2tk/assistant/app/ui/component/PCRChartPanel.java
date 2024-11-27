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
package m2tk.assistant.app.ui.component;

import m2tk.assistant.api.domain.PCRCheck;
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
import org.jfree.chart.renderer.xy.SamplingXYLineRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.function.Function;

public class PCRChartPanel extends JPanel
{
    private XYSeriesCollection pcrValues;
    private XYSeriesCollection bitrateValues;
    private XYSeriesCollection accuracyValues1;
    private XYSeriesCollection intervalValues1;
    private XYSeriesCollection accuracyValues2;
    private XYSeriesCollection intervalValues2;

    private final Color foreground = UIManager.getColor("Label.foreground");
    private final Color background = UIManager.getColor("Panel.background");
    private final Font labelFont = UIManager.getFont("Label.font");

    public PCRChartPanel()
    {
        initUI();
    }

    private void initUI()
    {
        pcrValues = new XYSeriesCollection();
        bitrateValues = new XYSeriesCollection();
        accuracyValues1 = new XYSeriesCollection();
        intervalValues1 = new XYSeriesCollection();
        accuracyValues2 = new XYSeriesCollection();
        intervalValues2 = new XYSeriesCollection();

        // TabbedPane里面的图表太顶，给加上一点内边距。
        Function<JComponent, JComponent> addBorder = c ->
        {
            c.setBorder(new EmptyBorder(10, 0, 0, 0));
            return c;
        };
        JTabbedPane tabbedPane1 = new JTabbedPane();
        tabbedPane1.add("PCR精度统计", addBorder.apply(new ChartPanel(createPCRAccuracyStatsChart())));
        tabbedPane1.add("PCR精度轨迹", addBorder.apply(new ChartPanel(createPCRAccuracyTraceChart())));

        JTabbedPane tabbedPane2 = new JTabbedPane();
        tabbedPane2.add("PCR间隔统计", addBorder.apply(new ChartPanel(createPCRIntervalStatsChart())));
        tabbedPane2.add("PCR间隔轨迹", addBorder.apply(new ChartPanel(createPCRIntervalTraceChart())));

        setLayout(new MigLayout("fill", "[][]", "[][]"));
        add(new ChartPanel(createPCRValueChart()), "grow");
        add(new ChartPanel(createBitrateChart()), "grow, wrap");
        add(tabbedPane1, "grow");
        add(tabbedPane2, "grow, wrap");
    }

    private JFreeChart createPCRValueChart()
    {
        XYDotRenderer renderer = new XYDotRenderer();
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setDefaultItemLabelPaint(foreground);

        NumberAxis xAxis = new NumberAxis("PCR位置");
        xAxis.setAutoRange(true);
        xAxis.setAutoRangeIncludesZero(false);
        xAxis.setLabelFont(labelFont.deriveFont(14.0f));
        xAxis.setLabelPaint(foreground);
        xAxis.setAxisLinePaint(foreground);
        xAxis.setTickLabelPaint(foreground);
        NumberAxis yAxis = new NumberAxis("PCR时间点");
        yAxis.setAutoRange(true);
        yAxis.setAutoRangeIncludesZero(false);
        yAxis.setLabelFont(labelFont.deriveFont(14.0f));
        yAxis.setLabelPaint(foreground);
        yAxis.setAxisLinePaint(foreground);
        yAxis.setTickLabelPaint(foreground);

        XYPlot plot = new XYPlot(pcrValues, xAxis, yAxis, renderer);
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setBackgroundPaint(background);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);

        JFreeChart chart = new JFreeChart("PCR轨迹", labelFont.deriveFont(18.0f), plot, false);
        chart.getTitle().setPaint(foreground);
        return chart;
    }

    private JFreeChart createBitrateChart()
    {
        SamplingXYLineRenderer renderer = new SamplingXYLineRenderer();
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setDefaultItemLabelPaint(foreground);

        NumberAxis xAxis = new NumberAxis("PCR位置");
        xAxis.setAutoRange(true);
        xAxis.setAutoRangeIncludesZero(false);
        xAxis.setLabelFont(labelFont.deriveFont(14.0f));
        xAxis.setLabelPaint(foreground);
        xAxis.setAxisLinePaint(foreground);
        xAxis.setTickLabelPaint(foreground);
        NumberAxis yAxis = new NumberAxis("瞬时码率（Mb/s）");
        yAxis.setRange(new Range(0, 75));
        yAxis.setLabelFont(labelFont.deriveFont(14.0f));
        yAxis.setLabelPaint(foreground);
        yAxis.setAxisLinePaint(foreground);
        yAxis.setTickLabelPaint(foreground);

        XYPlot plot = new XYPlot(bitrateValues, xAxis, yAxis, renderer);
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setBackgroundPaint(background);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);

        JFreeChart chart = new JFreeChart("码率轨迹", labelFont.deriveFont(18.0f), plot, false);
        chart.getTitle().setPaint(foreground);
        return chart;
    }

    private JFreeChart createPCRAccuracyStatsChart()
    {
        XYBarRenderer renderer = new XYBarRenderer();
        renderer.setSeriesPaint(0, Color.MAGENTA);
        renderer.setBarPainter(new StandardXYBarPainter());
        renderer.setShadowVisible(false);
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelPaint(foreground);
        renderer.setDefaultItemLabelGenerator(new StandardXYItemLabelGenerator());
        renderer.setDefaultPositiveItemLabelPosition(new ItemLabelPosition(ItemLabelAnchor.INSIDE12, TextAnchor.TOP_CENTER));

        SymbolAxis xAxis = new SymbolAxis("PCR精度区间（单位：ns）",
                                          new String[] {"-∞", "-800", "-500", "-300", "-150", "-50",
                                                        "50", "150", "300", "500", "800", "+∞"});
        xAxis.setRange(new Range(0, 11));
        xAxis.setLabelFont(labelFont.deriveFont(14.0f));
        xAxis.setLabelPaint(foreground);
        xAxis.setAxisLinePaint(foreground);
        xAxis.setTickLabelPaint(foreground);
        xAxis.setGridBandsVisible(false);
        NumberAxis yAxis = new NumberAxis("PCR点数");
        yAxis.setAutoRange(true);
        yAxis.setAutoRangeStickyZero(true);
        yAxis.setAutoRangeIncludesZero(true);
        yAxis.setLabelFont(labelFont.deriveFont(14.0f));
        yAxis.setLabelPaint(foreground);
        yAxis.setAxisLinePaint(foreground);
        yAxis.setTickLabelPaint(foreground);

        XYPlot plot = new XYPlot(accuracyValues1, xAxis, yAxis, renderer);
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setBackgroundPaint(background);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.addDomainMarker(0, new ValueMarker(2, Color.ORANGE, new BasicStroke(1.0f)), Layer.FOREGROUND);
        plot.addDomainMarker(0, new ValueMarker(9, Color.ORANGE, new BasicStroke(1.0f)), Layer.FOREGROUND);

        JFreeChart chart = new JFreeChart("PCR精度统计", labelFont.deriveFont(18.0f), plot, false);
        chart.getTitle().setPaint(foreground);
        return chart;
    }

    private JFreeChart createPCRAccuracyTraceChart()
    {
        SamplingXYLineRenderer renderer = new SamplingXYLineRenderer();
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setSeriesStroke(0, new BasicStroke(1.0f));
        renderer.setDefaultItemLabelPaint(foreground);

        NumberAxis xAxis = new NumberAxis("PCR位置");
        xAxis.setAutoRange(true);
        xAxis.setAutoRangeIncludesZero(false);
        xAxis.setLabelFont(labelFont.deriveFont(14.0f));
        xAxis.setLabelPaint(foreground);
        xAxis.setAxisLinePaint(foreground);
        xAxis.setTickLabelPaint(foreground);
        NumberAxis yAxis = new NumberAxis("PCR精度（单位：ns）");
        yAxis.setAutoRange(false);
        yAxis.setRange(-800, 800);
        yAxis.setAutoRangeIncludesZero(true);
        yAxis.setLabelFont(labelFont.deriveFont(14.0f));
        yAxis.setLabelPaint(foreground);
        yAxis.setAxisLinePaint(foreground);
        yAxis.setTickLabelPaint(foreground);

        XYPlot plot = new XYPlot(accuracyValues2, xAxis, yAxis, renderer);
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setBackgroundPaint(background);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.addRangeMarker(0, new ValueMarker(0, Color.LIGHT_GRAY, new BasicStroke(0.5f)), Layer.FOREGROUND);
        plot.addRangeMarker(0, new ValueMarker(-500, Color.ORANGE, new BasicStroke(1.0f)), Layer.FOREGROUND);
        plot.addRangeMarker(0, new ValueMarker(+500, Color.ORANGE, new BasicStroke(1.0f)), Layer.FOREGROUND);

        JFreeChart chart = new JFreeChart("PCR精度轨迹", labelFont.deriveFont(18.0f), plot, false);
        chart.getTitle().setPaint(foreground);
        return chart;
    }

    private JFreeChart createPCRIntervalStatsChart()
    {
        XYBarRenderer renderer = new XYBarRenderer();
        renderer.setSeriesPaint(0, Color.MAGENTA);
        renderer.setBarPainter(new StandardXYBarPainter());
        renderer.setShadowVisible(false);
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelPaint(foreground);
        renderer.setDefaultItemLabelGenerator(new StandardXYItemLabelGenerator());
        renderer.setDefaultPositiveItemLabelPosition(new ItemLabelPosition(ItemLabelAnchor.INSIDE12, TextAnchor.TOP_CENTER));

        SymbolAxis xAxis = new SymbolAxis("PCR间隔区间（单位：ms）",
                                          new String[]{"0", "8", "16", "24", "32", "40", "48", "56", "64", "72", "80", "+∞"});
        xAxis.setRange(0, 11);
        xAxis.setLabelFont(labelFont.deriveFont(14.0f));
        xAxis.setGridBandsVisible(false);
        xAxis.setLabelPaint(foreground);
        xAxis.setAxisLinePaint(foreground);
        xAxis.setTickLabelPaint(foreground);
        NumberAxis yAxis = new NumberAxis("PCR点数");
        yAxis.setAutoRange(true);
        yAxis.setAutoRangeStickyZero(true);
        yAxis.setAutoRangeIncludesZero(true);
        yAxis.setLabelFont(labelFont.deriveFont(14.0f));
        yAxis.setLabelPaint(foreground);
        yAxis.setAxisLinePaint(foreground);
        yAxis.setTickLabelPaint(foreground);

        XYPlot plot = new XYPlot(intervalValues1, xAxis, yAxis, renderer);
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setBackgroundPaint(background);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.addDomainMarker(0, new ValueMarker(5, Color.ORANGE, new BasicStroke(1.0f)), Layer.FOREGROUND);

        JFreeChart chart = new JFreeChart("PCR间隔统计", labelFont.deriveFont(18.0f), plot, false);
        chart.getTitle().setPaint(foreground);
        return chart;
    }

    private JFreeChart createPCRIntervalTraceChart()
    {
        SamplingXYLineRenderer renderer = new SamplingXYLineRenderer();
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setSeriesStroke(0, new BasicStroke(1.0f));
        renderer.setDefaultItemLabelPaint(foreground);

        NumberAxis xAxis = new NumberAxis("PCR位置");
        xAxis.setAutoRange(true);
        xAxis.setAutoRangeIncludesZero(false);
        xAxis.setLabelFont(labelFont.deriveFont(14.0f));
        xAxis.setLabelPaint(foreground);
        xAxis.setAxisLinePaint(foreground);
        xAxis.setTickLabelPaint(foreground);
        NumberAxis yAxis = new NumberAxis("PCR间隔（单位：ms）");
        yAxis.setAutoRange(false);
        yAxis.setRange(0, 100);
        yAxis.setAutoRangeIncludesZero(true);
        yAxis.setLabelFont(labelFont.deriveFont(14.0f));
        yAxis.setLabelPaint(foreground);
        yAxis.setAxisLinePaint(foreground);
        yAxis.setTickLabelPaint(foreground);

        XYPlot plot = new XYPlot(intervalValues2, xAxis, yAxis, renderer);
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setBackgroundPaint(background);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.addRangeMarker(0, new ValueMarker(40, Color.ORANGE, new BasicStroke(1.0f)), Layer.FOREGROUND);

        JFreeChart chart = new JFreeChart("PCR间隔轨迹", labelFont.deriveFont(18.0f), plot, false);
        chart.getTitle().setPaint(foreground);
        return chart;
    }

    public void update(List<PCRCheck> checks)
    {
        pcrValues.removeAllSeries();
        bitrateValues.removeAllSeries();
        accuracyValues1.removeAllSeries();
        intervalValues1.removeAllSeries();
        accuracyValues2.removeAllSeries();
        intervalValues2.removeAllSeries();

        if (!checks.isEmpty())
        {
            XYSeries series1 = new XYSeries("PCR");
            XYSeries series2 = new XYSeries("瞬时码率（Mb/s）");
            XYSeries series3 = new XYSeries("PCR精度统计");
            XYSeries series4 = new XYSeries("PCR间隔统计");
            XYSeries series5 = new XYSeries("PCR精度轨迹");
            XYSeries series6 = new XYSeries("PCR间隔轨迹");

            int[] accuracyGroups = new int[11];
            int[] intervalGroups = new int[11];

            for (PCRCheck check : checks)
            {
                series1.add(check.getCurrPosition(), ProgramClockReference.toTimepoint(check.getCurrValue()));

                // bps -> Mbps
                series2.add(check.getCurrPosition(), check.getBitrate() / 1000000.0d);

                int groupA = getAccuracyGroup(check.getAccuracyNanos());
                int groupI = getIntervalGroup(check.getIntervalNanos());
                accuracyGroups[groupA] += 1;
                intervalGroups[groupI] += 1;
            }

            for (int i = 0; i < 11; i++)
            {
                // 加0.5是为了把柱图画在分格的中间。一个格是1.0个单位（0.5就是中间值）
                series3.add(i + 0.5, accuracyGroups[i]);
                series4.add(i + 0.5, intervalGroups[i]);
            }

            List<PCRCheck> recent500Checks = checks.subList(Math.max(0, checks.size() - 400), checks.size());
            for (PCRCheck check : recent500Checks)
            {
                series5.add(check.getCurrPosition(), check.getAccuracyNanos());
                series6.add((double) check.getCurrPosition(), check.getIntervalNanos() / 1000000.0d);
            }

            pcrValues.addSeries(series1);
            bitrateValues.addSeries(series2);
            accuracyValues1.addSeries(series3);
            intervalValues1.addSeries(series4);
            accuracyValues2.addSeries(series5);
            intervalValues2.addSeries(series6);
        }
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
        if (intervalNanos <= 8000000)
            return 0;
        if (intervalNanos <= 16000000)
            return 1;
        if (intervalNanos <= 24000000)
            return 2;
        if (intervalNanos <= 32000000)
            return 3;
        if (intervalNanos <= 40000000)
            return 4;
        if (intervalNanos <= 48000000)
            return 5;
        if (intervalNanos <= 56000000)
            return 6;
        if (intervalNanos <= 64000000)
            return 7;
        if (intervalNanos <= 72000000)
            return 8;
        if (intervalNanos <= 80000000)
            return 9;
        return 10;
    }
}
