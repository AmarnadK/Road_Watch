package com.roadwatch.admin.ui;

import com.roadwatch.admin.api.ApiClient;
import com.roadwatch.admin.api.Async;
import com.roadwatch.admin.api.Models;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Filterable table of reports with a detail view beside it. */
public class ReportsPanel extends JPanel {

    private final Session session = Session.current();
    private final ReportTableModel model = new ReportTableModel();
    private final JTable table = new JTable(model);
    private final ReportDetailPanel detail = new ReportDetailPanel(this::onReportUpdated);

    private final JComboBox<Models.Panchayat> areaBox = new JComboBox<>();
    private final JComboBox<Ui.StatusItem> statusBox = new JComboBox<>();
    private final JCheckBox mineOnly = new JCheckBox("Only my panchayat", true);
    private final JLabel countLabel = new JLabel(" ");
    private boolean adjusting = false; // true while combos are being filled, so listeners stay quiet

    public ReportsPanel() {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildSplit(), BorderLayout.CENTER);
        add(countLabel, BorderLayout.SOUTH);

        configureTable();
        loadFilters();
    }

    // ---------------------------------------------------------------- layout

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

        statusBox.addItem(new Ui.StatusItem(null, "All statuses"));
        Ui.allStatuses().forEach(statusBox::addItem);

        if (!session.isSuperAdmin()) {
            bar.add(mineOnly);
        }
        bar.add(new JLabel("Area"));
        bar.add(areaBox);
        bar.add(new JLabel("Status"));
        bar.add(statusBox);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> reload());
        bar.add(refresh);

        mineOnly.addActionListener(e -> {
            areaBox.setEnabled(!mineOnly.isSelected());
            reload();
        });
        areaBox.addActionListener(e -> { if (!adjusting) reload(); });
        statusBox.addActionListener(e -> { if (!adjusting) reload(); });
        areaBox.setEnabled(session.isSuperAdmin());
        return bar;
    }

    private JComponent buildSplit() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(table), detail);
        split.setResizeWeight(0.55);
        split.setDividerLocation(660);
        return split;
    }

    private void configureTable() {
        table.setRowHeight(26);
        table.setAutoCreateRowSorter(false);
        table.setRowSorter(new TableRowSorter<>(model));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);

        ReportCellRenderer renderer = new ReportCellRenderer();
        table.setDefaultRenderer(Object.class, renderer);
        table.setDefaultRenderer(String.class, renderer);
        table.setDefaultRenderer(Long.class, renderer);
        table.setDefaultRenderer(Instant.class, renderer);

        int[] widths = {70, 130, 130, 80, 90, 150, 70};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) {
                detail.clear();
            } else {
                detail.show(model.getReport(table.convertRowIndexToModel(viewRow)));
            }
        });
    }

    // ---------------------------------------------------------------- loading

    private void loadFilters() {
        Async.run(() -> session.api().panchayats(),
                list -> {
                    fillAreaBox(list);
                    reload();
                },
                e -> Ui.error(this, e));
    }

    /** Called again after the district admin adds a panchayat. */
    public void reloadPanchayats() {
        Async.run(() -> session.api().panchayats(), this::fillAreaBox, e -> Ui.error(this, e));
    }

    private void fillAreaBox(List<Models.Panchayat> list) {
        Models.Panchayat previous = (Models.Panchayat) areaBox.getSelectedItem();
        adjusting = true;
        areaBox.removeAllItems();
        areaBox.addItem(new Models.Panchayat(null, "All areas", ""));
        list.forEach(areaBox::addItem);
        if (previous != null && previous.id() != null) {
            for (int i = 0; i < areaBox.getItemCount(); i++) {
                if (previous.id().equals(areaBox.getItemAt(i).id())) areaBox.setSelectedIndex(i);
            }
        }
        adjusting = false;
    }

    public void reload() {
        Long panchayatId;
        if (!session.isSuperAdmin() && mineOnly.isSelected()) {
            panchayatId = session.user().panchayatId();
        } else {
            Models.Panchayat p = (Models.Panchayat) areaBox.getSelectedItem();
            panchayatId = p == null ? null : p.id();
        }
        Ui.StatusItem s = (Ui.StatusItem) statusBox.getSelectedItem();
        String status = s == null ? null : s.code();

        ApiClient api = session.api();
        countLabel.setText("Loading...");
        Async.run(() -> api.reports(panchayatId, status),
                list -> {
                    model.setReports(list);
                    detail.clear();
                    long editable = list.stream().filter(Models.Report::editable).count();
                    countLabel.setText(list.size() + " report(s), " + editable + " you can update");
                },
                e -> {
                    countLabel.setText(" ");
                    Ui.error(this, e);
                });
    }

    private void onReportUpdated(Models.Report updated) {
        model.replace(updated);
    }

    // ---------------------------------------------------------------- table model + renderer

    static class ReportTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Ref", "Reported", "Area", "Severity", "Status", "Landmark", "Can edit"};
        private List<Models.Report> rows = new ArrayList<>();

        void setReports(List<Models.Report> reports) {
            rows = new ArrayList<>(reports);
            fireTableDataChanged();
        }

        Models.Report getReport(int modelRow) { return rows.get(modelRow); }

        void replace(Models.Report updated) {
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).id().equals(updated.id())) {
                    rows.set(i, updated);
                    fireTableRowsUpdated(i, i);
                    return;
                }
            }
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int c) { return COLUMNS[c]; }

        @Override
        public Class<?> getColumnClass(int c) {
            return switch (c) {
                case 0 -> Long.class;
                case 1 -> Instant.class;
                default -> String.class;
            };
        }

        @Override
        public Object getValueAt(int row, int col) {
            Models.Report r = rows.get(row);
            return switch (col) {
                case 0 -> r.id();
                case 1 -> Ui.parseInstant(r.createdAt());
                case 2 -> r.panchayatName();
                case 3 -> Ui.severityLabel(r.severity());
                case 4 -> Ui.statusLabel(r.status());
                case 5 -> r.landmark() == null ? "" : r.landmark();
                default -> r.editable() ? "Yes" : "";
            };
        }
    }

    /** Greys out reports the admin cannot edit and colours the status column. */
    private class ReportCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                       boolean focus, int row, int col) {
            String text;
            if (value instanceof Long id) text = "RW-" + id;
            else if (value instanceof Instant when) text = Ui.formatInstant(when);
            else text = value == null ? "" : value.toString();

            super.getTableCellRendererComponent(t, text, selected, focus, row, col);
            Models.Report r = model.getReport(t.convertRowIndexToModel(row));
            setFont(t.getFont());
            if (!selected) {
                setForeground(r.editable() ? t.getForeground() : Color.GRAY);
                if (col == 4) {
                    setForeground(Ui.statusColor(r.status()));
                    setFont(t.getFont().deriveFont(Font.BOLD));
                }
            }
            return this;
        }
    }
}
