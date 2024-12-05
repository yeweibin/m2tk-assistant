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

import cn.hutool.core.util.StrUtil;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.api.domain.ElementaryStream;
import m2tk.assistant.api.domain.StreamSource;
import m2tk.assistant.api.presets.StreamTypes;
import m2tk.assistant.app.ui.model.StreamInfoTableModel;
import m2tk.assistant.app.ui.util.ComponentUtil;
import m2tk.assistant.app.ui.util.FormatUtil;
import m2tk.assistant.app.ui.util.ThreeStateRowSorterListener;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.fluentui.FluentUiFilledAL;
import org.kordamp.ikonli.fluentui.FluentUiFilledMZ;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

@Slf4j
public class StreamInfoPanel extends JPanel
{
    private JTextField fieldSourceName;
    private JTextField fieldBitrates;
    private JTextField fieldPacketCount, fieldStreamCount, fieldProgramCount;
    private JTextField fieldTransportStreamId;
    private JToggleButton btnShowDetails;
    private JToggleButton btnTableSearch;
    private JRadioButton radio188Bytes, radio204Bytes;
    private JRadioButton radioClear, radioScrambled;
    private JCheckBox checkECMPresent, checkEMMPresent;
    private JCheckBox checkPATPresent, checkPMTPresent, checkCATPresent;
    private JCheckBox checkNITActualPresent, checkNITOtherPresent, checkSDTActualPresent, checkSDTOtherPresent;
    private JCheckBox checkEITPnfActualPresent, checkEITPnfOtherPresent, checkEITSchActualPresent, checkEITSchOtherPresent;
    private JCheckBox checkBATPresent, checkTDTPresent, checkTOTPresent;
    private StreamInfoTableModel model;

    private transient BiConsumer<MouseEvent, ElementaryStream> popupListener;

    private int maxSourceBitrate = 0, minSourceBitrate = Integer.MAX_VALUE, insSourceBitrate = 0;

    public StreamInfoPanel()
    {
        initUI();
    }

    private void initUI()
    {
        model = new StreamInfoTableModel();
        TableRowSorter<StreamInfoTableModel> rowSorter = new TableRowSorter<>(model);
        rowSorter.addRowSorterListener(new ThreeStateRowSorterListener(rowSorter));

        fieldSourceName = readonlyTextField("");
        fieldBitrates = readonlyTextField("0 bps / 0 bps / 0 bps");
        fieldPacketCount = readonlyTextField("0");
        fieldStreamCount = readonlyTextField("0");
        fieldProgramCount = readonlyTextField("0");
        fieldTransportStreamId = readonlyTextField("未知");

        JRadioButton[] packetSizeOptions = readonlySingleChoices("188 字节", "204 字节");
        radio188Bytes = packetSizeOptions[0];
        radio204Bytes = packetSizeOptions[1];
        radio188Bytes.setSelected(true);

        JRadioButton[] scrambleStateOptions = readonlySingleChoices("清流", "加扰");
        radioClear = scrambleStateOptions[0];
        radioScrambled = scrambleStateOptions[1];
        radioClear.setSelected(true);

        JCheckBox[] caStreamOptions = readonlyOptionalChoices("ECM", "EMM");
        checkECMPresent = caStreamOptions[0];
        checkEMMPresent = caStreamOptions[1];

        JCheckBox[] psiTableOptions = readonlyOptionalChoices("PAT", "PMT", "CAT");
        checkPATPresent = psiTableOptions[0];
        checkPMTPresent = psiTableOptions[1];
        checkCATPresent = psiTableOptions[2];

        JCheckBox[] siTableOptions = readonlyOptionalChoices("NIT（当前网络）", "NIT（其它网络）", "SDT（当前流）", "SDT（其他流）",
                                                             "EIT（即时，当前流）", "EIT（即时，其它流）", "EIT（排期，当前流）", "EIT（排期，其它流）",
                                                             "BAT", "TDT", "TOT");
        checkNITActualPresent = siTableOptions[0];
        checkNITOtherPresent = siTableOptions[1];
        checkSDTActualPresent = siTableOptions[2];
        checkSDTOtherPresent = siTableOptions[3];
        checkEITPnfActualPresent = siTableOptions[4];
        checkEITPnfOtherPresent = siTableOptions[5];
        checkEITSchActualPresent = siTableOptions[6];
        checkEITSchOtherPresent = siTableOptions[7];
        checkBATPresent = siTableOptions[8];
        checkTDTPresent = siTableOptions[9];
        checkTOTPresent = siTableOptions[10];

        JPanel sourceDetailsPanel = new JPanel(new MigLayout());
        sourceDetailsPanel.add(new JLabel("传输码率"), "gap 15 5, right");
        sourceDetailsPanel.add(fieldBitrates, "span 3, growx");
        sourceDetailsPanel.add(new JLabel("（最大 / 最小 / 实时）"), "span 2, wrap");
        sourceDetailsPanel.add(new JLabel("传输包数"), "gap 15 5, right");
        sourceDetailsPanel.add(fieldPacketCount, "width 120, growx");
        sourceDetailsPanel.add(new JLabel("基本流数"), "gap 15 5, right");
        sourceDetailsPanel.add(fieldStreamCount, "width 120, growx, wrap");
        sourceDetailsPanel.add(new JLabel("传输流号"), "gap 15 5, right");
        sourceDetailsPanel.add(fieldTransportStreamId, "width 120, growx");
        sourceDetailsPanel.add(new JLabel("节目数"), "gap 15 5, right");
        sourceDetailsPanel.add(fieldProgramCount, "width 120, growx, wrap");
        sourceDetailsPanel.add(new JLabel("包大小"), "gap 15 5, right");
        sourceDetailsPanel.add(radio188Bytes);
        sourceDetailsPanel.add(radio204Bytes, "wrap");
        sourceDetailsPanel.add(new JLabel("加扰状态"), "gap 15 5, right");
        sourceDetailsPanel.add(radioClear);
        sourceDetailsPanel.add(radioScrambled, "wrap");
        sourceDetailsPanel.add(new JLabel("条件接收"), "gap 15 5, right");
        sourceDetailsPanel.add(checkECMPresent);
        sourceDetailsPanel.add(checkEMMPresent, "wrap");
        sourceDetailsPanel.add(new JLabel("节目信息"), "gap 15 5, right");
        sourceDetailsPanel.add(checkPATPresent);
        sourceDetailsPanel.add(checkPMTPresent);
        sourceDetailsPanel.add(checkCATPresent, "wrap");
        sourceDetailsPanel.add(new JLabel("业务信息"), "gap 15 5, right");
        sourceDetailsPanel.add(checkNITActualPresent);
        sourceDetailsPanel.add(checkNITOtherPresent);
        sourceDetailsPanel.add(checkSDTActualPresent);
        sourceDetailsPanel.add(checkSDTOtherPresent, "wrap");
        sourceDetailsPanel.add(checkEITPnfActualPresent, "skip 1");
        sourceDetailsPanel.add(checkEITPnfOtherPresent);
        sourceDetailsPanel.add(checkEITSchActualPresent);
        sourceDetailsPanel.add(checkEITSchOtherPresent, "wrap");
        sourceDetailsPanel.add(checkBATPresent, "skip 1");
        sourceDetailsPanel.add(checkTDTPresent);
        sourceDetailsPanel.add(checkTOTPresent, "wrap");

        Icon iconShowDetails = FontIcon.of(FluentUiRegularAL.CHEVRON_DOWN_20, 20, UIManager.getColor("Label.foreground"));
        Icon iconHideDetails = FontIcon.of(FluentUiRegularAL.CHEVRON_UP_20, 20, UIManager.getColor("Label.foreground"));
        btnShowDetails = new JToggleButton();
        btnShowDetails.setIcon(iconShowDetails);
        btnShowDetails.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        btnShowDetails.addActionListener(event -> {
            sourceDetailsPanel.setVisible(btnShowDetails.isSelected());
            btnShowDetails.setIcon(btnShowDetails.isSelected() ? iconHideDetails : iconShowDetails);
            revalidate();
        });

        JTextField fieldSearch = new JTextField();
        fieldSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "输入PID或基本流描述中的关键字进行搜索……");
        fieldSearch.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSearchIcon());
        fieldSearch.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        fieldSearch.putClientProperty(FlatClientProperties.TEXT_FIELD_CLEAR_CALLBACK, (Runnable) () -> {
            fieldSearch.setText("");
            rowSorter.setRowFilter(null);
        });
        Runnable doFiltering = () -> {
            String keyword = fieldSearch.getText();
            rowSorter.setRowFilter(StrUtil.isNotEmpty(keyword)
                                   ? RowFilter.regexFilter("(?i)" + keyword, 1, 2) // 忽略大小写搜索
                                   : null);
        };
        fieldSearch.addActionListener(e -> doFiltering.run());
        fieldSearch.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                if (!e.isActionKey())
                    doFiltering.run();
            }
        });
        fieldSearch.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentShown(ComponentEvent e)
            {
                fieldSearch.requestFocusInWindow();
            }
        });

        Icon iconTableSearch = FontIcon.of(FluentUiRegularMZ.SEARCH_24, 20, UIManager.getColor("Label.foreground"));
        btnTableSearch = new JToggleButton();
        btnTableSearch.setIcon(iconTableSearch);
        btnTableSearch.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        btnTableSearch.addActionListener(event -> {
            rowSorter.setRowFilter(null);
            fieldSearch.setVisible(btnTableSearch.isSelected());
            revalidate();
        });

        JTable table = new JTable();
        table.setModel(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setRowSorter(rowSorter);
        table.setRowHeight(25);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseReleased(MouseEvent e)
            {
                if (e.isPopupTrigger() && popupListener != null)
                {
                    int selectedRow = table.getSelectedRow();
                    int rowAtPoint = table.rowAtPoint(e.getPoint());

                    if (rowAtPoint == -1)
                        return;
                    if (rowAtPoint != selectedRow)
                    {
                        table.setRowSelectionInterval(table.convertRowIndexToModel(rowAtPoint),
                                                      table.convertRowIndexToModel(rowAtPoint));
                        selectedRow = rowAtPoint;
                    }
                    try
                    {
                        popupListener.accept(e, model.getRow(table.convertRowIndexToModel(selectedRow)));
                    } catch (Exception ignored)
                    {
                    }
                }
            }
        });

        ToolTipManager.sharedInstance().registerComponent(table);

        DefaultTableCellRenderer centeredRenderer = new DefaultTableCellRenderer();
        centeredRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        DefaultTableCellRenderer leadingRenderer = new DefaultTableCellRenderer();
        leadingRenderer.setHorizontalAlignment(SwingConstants.LEADING);
        DefaultTableCellRenderer trailingRenderer = new DefaultTableCellRenderer();
        trailingRenderer.setHorizontalAlignment(SwingConstants.TRAILING);
        StreamRatioCellRenderer ratioRenderer = new StreamRatioCellRenderer();
        StreamStateCellRenderer stateRenderer = new StreamStateCellRenderer();

        // "序号", "PID", "基本流描述", "带宽占比", "传输错误", "连续计数错误", "流状态"
        TableColumnModel columnModel = table.getColumnModel();
        ComponentUtil.configTableColumn(columnModel, 0, centeredRenderer, 80, false);
        ComponentUtil.configTableColumn(columnModel, 1, trailingRenderer, 150, false);
        ComponentUtil.configTableColumn(columnModel, 2, leadingRenderer, 500, true);
        ComponentUtil.configTableColumn(columnModel, 3, ratioRenderer, 150, false);
        ComponentUtil.configTableColumn(columnModel, 4, trailingRenderer, 100, false);
        ComponentUtil.configTableColumn(columnModel, 5, trailingRenderer, 100, false);
        ComponentUtil.configTableColumn(columnModel, 6, trailingRenderer, 100, false);
        ComponentUtil.configTableColumn(columnModel, 7, stateRenderer, 200, true);

        setLayout(new MigLayout("", "[][grow][][]", "[][][][grow]"));

        add(new JLabel("输入源"));
        add(fieldSourceName, "growx");
        add(btnShowDetails);
        add(btnTableSearch, "wrap");
        add(sourceDetailsPanel, "hidemode 2, span 4, growx, wrap");
        add(fieldSearch, "hidemode 2, span 4, growx, wrap");
        add(new JScrollPane(table), "span 4, grow, push");

        sourceDetailsPanel.setVisible(false);
        fieldSearch.setVisible(false);
    }

    public void setPopupListener(BiConsumer<MouseEvent, ElementaryStream> listener)
    {
        popupListener = listener;
    }

    public void updateStreamInfo(StreamSource source, List<ElementaryStream> streams)
    {
        String oldSourceName = fieldSourceName.getText();
        if (!Objects.equals(oldSourceName, source.getName()))
        {
            fieldSourceName.setText(source.getName());
            fieldSourceName.setToolTipText(source.getName());
        }

        if (source.getBitrate() > 0)
        {
            maxSourceBitrate = Math.max(maxSourceBitrate, source.getBitrate());
            minSourceBitrate = Math.min(minSourceBitrate, source.getBitrate());
            insSourceBitrate = source.getBitrate();

            fieldBitrates.setText(String.format("%s / %s / %s",
                                                FormatUtil.formatBitrate(maxSourceBitrate),
                                                FormatUtil.formatBitrate(minSourceBitrate),
                                                FormatUtil.formatBitrate(insSourceBitrate)));
        }

        fieldPacketCount.setText(String.format("%,d", source.getPacketCount()));
        fieldStreamCount.setText(String.format("%,d", source.getStreamCount()));
        fieldProgramCount.setText(String.format("%,d", source.getProgramCount()));
        fieldTransportStreamId.setText("" + source.getTransportStreamId());

        radio188Bytes.setSelected(source.getFrameSize() == 188);
        radioScrambled.setSelected(source.isScrambled());
        checkECMPresent.setSelected(source.isEcmPresent());
        checkEMMPresent.setSelected(source.isEmmPresent());
        checkPATPresent.setSelected(source.isPatPresent());
        checkPMTPresent.setSelected(source.isPmtPresent());
        checkCATPresent.setSelected(source.isCatPresent());
        checkNITActualPresent.setSelected(source.isNitActualPresent());
        checkNITOtherPresent.setSelected(source.isNitOtherPresent());
        checkSDTActualPresent.setSelected(source.isSdtActualPresent());
        checkSDTOtherPresent.setSelected(source.isSdtOtherPresent());
        checkEITPnfActualPresent.setSelected(source.isEitPnfActualPresent());
        checkEITPnfOtherPresent.setSelected(source.isEitPnfOtherPresent());
        checkEITSchActualPresent.setSelected(source.isEitSchActualPresent());
        checkEITSchOtherPresent.setSelected(source.isEitSchOtherPresent());
        checkBATPresent.setSelected(source.isBatPresent());
        checkTDTPresent.setSelected(source.isTdtPresent());
        checkTOTPresent.setSelected(source.isTotPresent());

        for (ElementaryStream stream : streams)
        {
            if (StrUtil.contains(stream.getDescription(), "ECM"))
                checkECMPresent.setSelected(true);
            else if (StrUtil.contains(stream.getDescription(), "EMM"))
                checkEMMPresent.setSelected(true);
        }

        model.update(streams);
    }

    private JTextField readonlyTextField(String text)
    {
        JTextField field = new JTextField(text);
        field.setEditable(false);
        field.setFocusable(false);
        return field;
    }

    private JRadioButton[] readonlySingleChoices(String... options)
    {
        JRadioButton[] buttons = new JRadioButton[options.length];
        ButtonGroup group = new ButtonGroup();

        for (int i = 0; i < options.length; i++)
        {
            JRadioButton button = new JRadioButton(options[i]);

            // 只能通过API修改单选按钮的状态，不响应UI操作事件。
            button.setFocusable(false);
            MouseListener[] listeners = button.getMouseListeners();
            for (MouseListener listener : listeners)
                button.removeMouseListener(listener);

            buttons[i] = button;
            group.add(button);
        }

        return buttons;
    }

    private JCheckBox[] readonlyOptionalChoices(String... options)
    {
        JCheckBox[] buttons = new JCheckBox[options.length];

        for (int i = 0; i < options.length; i++)
        {
            JCheckBox button = new JCheckBox(options[i]);

            // 只能通过API修改复选按钮的状态，不响应UI操作事件。
            button.setFocusable(false);
            MouseListener[] listeners = button.getMouseListeners();
            for (MouseListener listener : listeners)
                button.removeMouseListener(listener);

            buttons[i] = button;
        }

        return buttons;
    }

    private static class StreamRatioCellRenderer extends JPanel implements TableCellRenderer
    {
        private final JProgressBar progressBar;

        StreamRatioCellRenderer()
        {
            progressBar = new JProgressBar(0, 100);
            progressBar.setStringPainted(true);
            setLayout(new MigLayout("ins 3, fill"));
            add(progressBar, "grow");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            if (table == null)
            {
                return this;
            }

            if (isSelected)
            {
                setBackground(table.getSelectionBackground());
            } else
            {
                Color background = table.getBackground();
                if (background == null || background instanceof javax.swing.plaf.UIResource)
                {
                    Color alternateColor = UIManager.getColor("Table.alternateRowColor");
                    if (alternateColor != null && row % 2 != 0)
                    {
                        background = alternateColor;
                    }
                }
                setBackground(background);
            }

            if (hasFocus && !isSelected && table.isCellEditable(row, column))
            {
                Color col = UIManager.getColor("Table.focusCellBackground");
                if (col != null)
                    setBackground(col);
            }

            setValue(value);

            return this;
        }

        private void setValue(Object value)
        {
            try
            {
                Double ratio = (Double) value;
                progressBar.setValue((int) (100 * ratio));
                setToolTipText(String.format("%.2f", ratio));
            } catch (Exception ex)
            {
                log.error("转译带宽占比表示时异常：{}, ex：{}", value, ex.getMessage());
            }
        }
    }

    private static class StreamStateCellRenderer extends JPanel implements TableCellRenderer
    {
        private final JLabel labelDataP;
        private final JLabel labelDataS;
        private final JLabel labelVideoP;
        private final JLabel labelVideoS;
        private final JLabel labelAudioP;
        private final JLabel labelAudioS;
        private final JLabel labelPrivate;
        private final JLabel labelPCR;
        private final JLabel labelTSE;
        private final JLabel labelCCE;

        private final Color RED = Color.decode("#FD1D1D");
        private final Color GREEN = Color.decode("#7FBA00");
        private final Color ORANGE = Color.decode("#F25022");
        private final Color LIGHT_BLUE = Color.decode("#89D3DF");
        private final Color BRIGHT_BLUE = Color.decode("#4285F4");

        StreamStateCellRenderer()
        {
            labelDataP = new JLabel(FontIcon.of(FluentUiFilledMZ.TABLE_24, 20, GREEN));
            labelDataS = new JLabel(FontIcon.of(FluentUiFilledMZ.TABLE_24, 20, RED));
            labelVideoP = new JLabel(FontIcon.of(FluentUiFilledMZ.VIDEO_24, 20, GREEN));
            labelVideoS = new JLabel(FontIcon.of(FluentUiFilledMZ.VIDEO_24, 20, RED));
            labelAudioP = new JLabel(FontIcon.of(FluentUiFilledMZ.SPEAKER_24, 20, GREEN));
            labelAudioS = new JLabel(FontIcon.of(FluentUiFilledMZ.SPEAKER_24, 20, RED));
            labelPrivate = new JLabel(FontIcon.of(FluentUiFilledMZ.SLIDE_TEXT_24, 20, LIGHT_BLUE));
            labelPCR = new JLabel(FontIcon.of(FluentUiFilledAL.CLOCK_24, 20, BRIGHT_BLUE));
            labelTSE = new JLabel(FontIcon.of(FluentUiFilledMZ.WARNING_24, 20, ORANGE));
            labelCCE = new JLabel(FontIcon.of(FluentUiFilledAL.CLOSED_CAPTION_24, 22, ORANGE));

            setLayout(new MigLayout("ins 2 10 2 10, hidemode 3"));
            add(labelDataP, "cell 0 0");
            add(labelDataS, "cell 0 0");
            add(labelVideoP, "cell 0 0");
            add(labelVideoS, "cell 0 0");
            add(labelAudioP, "cell 0 0");
            add(labelAudioS, "cell 0 0");
            add(labelPrivate, "cell 0 0");
            add(labelPCR);
            add(labelTSE);
            add(labelCCE);
            add(Box.createHorizontalGlue(), "grow");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            if (table == null)
                return this;

            if (isSelected)
            {
                setBackground(table.getSelectionBackground());
            } else
            {
                Color background = table.getBackground();
                if (background == null || background instanceof javax.swing.plaf.UIResource)
                {
                    Color alternateColor = UIManager.getColor("Table.alternateRowColor");
                    if (alternateColor != null && row % 2 != 0)
                    {
                        background = alternateColor;
                    }
                }
                setBackground(background);
            }

            if (hasFocus && !isSelected && table.isCellEditable(row, column))
            {
                Color col = UIManager.getColor("Table.focusCellBackground");
                if (col != null)
                    setBackground(col);
            }

            setValue(value);

            return this;
        }

        private void setValue(Object value)
        {
            try
            {
                String s = (String) value;
                String[] states = s.split(",");

                labelDataP.setVisible(StrUtil.equals(states[0], StreamTypes.CATEGORY_DATA) && !Boolean.parseBoolean(states[1]));
                labelDataS.setVisible(StrUtil.equals(states[0], StreamTypes.CATEGORY_DATA) && Boolean.parseBoolean(states[1]));
                labelVideoP.setVisible(StrUtil.equals(states[0], StreamTypes.CATEGORY_VIDEO) && !Boolean.parseBoolean(states[1]));
                labelVideoS.setVisible(StrUtil.equals(states[0], StreamTypes.CATEGORY_VIDEO) && Boolean.parseBoolean(states[1]));
                labelAudioP.setVisible(StrUtil.equals(states[0], StreamTypes.CATEGORY_AUDIO) && !Boolean.parseBoolean(states[1]));
                labelAudioS.setVisible(StrUtil.equals(states[0], StreamTypes.CATEGORY_AUDIO) && Boolean.parseBoolean(states[1]));
                labelPrivate.setVisible(StrUtil.equals(states[0], StreamTypes.CATEGORY_USER_PRIVATE));
                labelPCR.setVisible(Boolean.parseBoolean(states[2]));
                labelTSE.setVisible(Boolean.parseBoolean(states[3]));
                labelCCE.setVisible(Boolean.parseBoolean(states[4]));

                String category = switch (states[0])
                {
                    case StreamTypes.CATEGORY_DATA -> "数据";
                    case StreamTypes.CATEGORY_VIDEO -> "视频";
                    case StreamTypes.CATEGORY_AUDIO -> "音频";
                    case StreamTypes.CATEGORY_USER_PRIVATE -> "用户私有格式";
                    default -> "未知";
                };
                setToolTipText(String.format("""
                                             流类型：%s
                                             携带PCR：%s
                                             加扰：%s
                                             """,
                                             category,
                                             Boolean.parseBoolean(states[2]) ? "是" : "否",
                                             Boolean.parseBoolean(states[1]) ? "是" : "否"));
            } catch (Exception ex)
            {
                log.error("转译流状态表示时异常：{}", value);
            }
        }
    }
}
