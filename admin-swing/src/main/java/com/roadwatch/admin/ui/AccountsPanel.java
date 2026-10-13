package com.roadwatch.admin.ui;

import com.roadwatch.admin.api.ApiClient;
import com.roadwatch.admin.api.Async;
import com.roadwatch.admin.api.Models;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** District admin only: approve or deactivate panchayat admins, and add panchayats. */
public class AccountsPanel extends JPanel {

    private final Session session = Session.current();
    private final UserTableModel model = new UserTableModel();
    private final JTable table = new JTable(model);
    private final JCheckBox pendingOnly = new JCheckBox("Waiting for approval only", true);
    private final JButton approveBtn = new JButton("Approve");
    private final JButton deactivateBtn = new JButton("Deactivate");
    private final Runnable onPanchayatsChanged;

    public AccountsPanel(Runnable onPanchayatsChanged) {
        super(new BorderLayout(0, 8));
        this.onPanchayatsChanged = onPanchayatsChanged;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton refresh = new JButton("Refresh");
        JButton addPanchayat = new JButton("Add panchayat...");
        bar.add(pendingOnly);
        bar.add(refresh);
        bar.add(new JSeparator(SwingConstants.VERTICAL));
        bar.add(approveBtn);
        bar.add(deactivateBtn);
        bar.add(new JSeparator(SwingConstants.VERTICAL));
        bar.add(addPanchayat);

        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);

        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        pendingOnly.addActionListener(e -> reload());
        refresh.addActionListener(e -> reload());
        approveBtn.addActionListener(e -> act(true));
        deactivateBtn.addActionListener(e -> act(false));
        addPanchayat.addActionListener(e -> addPanchayat());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateButtons();
        });

        updateButtons();
        reload();
    }

    private Models.User selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : model.get(table.convertRowIndexToModel(row));
    }

    private void updateButtons() {
        Models.User u = selected();
        boolean isAdmin = u != null && "ADMIN".equals(u.role());
        approveBtn.setEnabled(isAdmin && !u.active());
        deactivateBtn.setEnabled(isAdmin && u.active());
    }

    private void reload() {
        ApiClient api = session.api();
        boolean pending = pendingOnly.isSelected();
        Async.run(() -> api.users(pending),
                list -> {
                    model.set(list);
                    updateButtons();
                },
                e -> Ui.error(this, e));
    }

    private void act(boolean approve) {
        Models.User u = selected();
        if (u == null) return;
        ApiClient api = session.api();
        long id = u.id();
        Async.run(() -> approve ? api.approve(id) : api.deactivate(id),
                result -> reload(),
                e -> Ui.error(this, e));
    }

    private void addPanchayat() {
        JTextField name = new JTextField(20);
        JComboBox<String> type = new JComboBox<>(new String[]{"GRAM_PANCHAYAT", "MUNICIPALITY", "CORPORATION"});
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 6));
        form.add(new JLabel("Name"));
        form.add(name);
        form.add(new JLabel("Type"));
        form.add(type);

        int ok = JOptionPane.showConfirmDialog(this, form, "Add panchayat",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        String n = name.getText().trim();
        if (ok != JOptionPane.OK_OPTION || n.isEmpty()) return;

        ApiClient api = session.api();
        String t = (String) type.getSelectedItem();
        Async.run(() -> api.createPanchayat(n, t),
                p -> {
                    JOptionPane.showMessageDialog(this, p.name() + " was added.");
                    onPanchayatsChanged.run();
                },
                e -> Ui.error(this, e));
    }

    static class UserTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Name", "Username", "Role", "Panchayat", "Account", "Registered"};
        private List<Models.User> rows = new ArrayList<>();

        void set(List<Models.User> users) {
            rows = new ArrayList<>(users);
            fireTableDataChanged();
        }

        Models.User get(int row) { return rows.get(row); }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int c) { return COLUMNS[c]; }

        @Override
        public Object getValueAt(int row, int col) {
            Models.User u = rows.get(row);
            return switch (col) {
                case 0 -> u.fullName();
                case 1 -> u.username();
                case 2 -> "SUPER_ADMIN".equals(u.role()) ? "District admin" : "Panchayat admin";
                case 3 -> u.panchayatName() == null ? "All" : u.panchayatName();
                case 4 -> u.active() ? "Active" : "Waiting / inactive";
                default -> Ui.formatTime(u.createdAt());
            };
        }
    }
}
