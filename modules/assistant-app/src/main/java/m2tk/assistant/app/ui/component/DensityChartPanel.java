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

import m2tk.assistant.api.domain.StreamDensityBulk;
import net.miginfocom.swing.MigLayout;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.SamplingXYLineRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DensityChartPanel extends JPanel
{
    private final Color foreground = UIManager.getColor("Label.foreground");
    private final Color background = UIManager.getColor("Panel.background");
    private final Font labelFont = UIManager.getFont("Label.font");

    public DensityChartPanel()
    {
        setLayout(new MigLayout("", "[grow]"));
    }

    public void update(List<StreamDensityBulk> bulks)
    {
        removeAll();
        for (StreamDensityBulk bulk : bulks)
        {
            ChartPanel panel = drawBulk(bulk);
            add(panel, "gaptop 20, growx, height 360, wrap");
        }
    }

    private ChartPanel drawBulk(StreamDensityBulk bulk)
    {
        XYSeries series0 = new XYSeries("传输密度");
        int[] densities = bulk.getDensities();
        for (int i = 0; i < densities.length; i++)
            series0.add(i, densities[i]);

        // 画上平均线（只需要连接一头一尾），+5只是为了让平均线略长于实际数据，便于观察，没有其他含义。
        XYSeries series1 = new XYSeries("平均密度");
        series1.add(0, bulk.getAvgDensity());
        series1.add(densities.length + 5, bulk.getAvgDensity());

        XYSeriesCollection collection = new XYSeriesCollection();
        collection.addSeries(series0);
        collection.addSeries(series1);

        SamplingXYLineRenderer renderer = new SamplingXYLineRenderer();
        renderer.setSeriesPaint(0, Color.decode("#CCCC00"));
        renderer.setSeriesPaint(1, Color.decode("#F25022"));
        renderer.setDefaultItemLabelPaint(foreground);

        NumberAxis xAxis = new NumberAxis();
        xAxis.setAutoRange(true);
        xAxis.setAutoRangeIncludesZero(false);
        xAxis.setLabelFont(labelFont);
        xAxis.setLabelPaint(foreground);
        xAxis.setAxisLinePaint(foreground);
        xAxis.setTickLabelPaint(foreground);
        NumberAxis yAxis = new NumberAxis("相邻包间距");
        yAxis.setAutoRange(true);
        yAxis.setAutoRangeIncludesZero(false);
        yAxis.setLabelFont(labelFont);
        yAxis.setLabelPaint(foreground);
        yAxis.setAxisLinePaint(foreground);
        yAxis.setTickLabelPaint(foreground);

        XYPlot plot = new XYPlot(collection, xAxis, yAxis, renderer);
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setBackgroundPaint(background);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);

        JFreeChart chart = new JFreeChart(String.format("传输包间隔轨迹，起始位置：%,d", bulk.getStartPosition()), labelFont, plot, false);
        chart.getTitle().setPaint(foreground);
        return new ChartPanel(chart);
    }
}
