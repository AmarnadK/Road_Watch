package com.roadwatch.admin.ui;

import com.roadwatch.admin.api.Models;

import javax.swing.*;
import java.awt.*;

/** The main window after login: reports for everyone, accounts for the district admin. */
public class MainFrame extends JFrame {

    private static final Color ASPHALT = new Color(0x1F2328);
    private static final Color MARKING = new Color(0xFFC400);

    public MainFrame() {
        super("Road Watch Kozhikode - Admin");
        Session session = Session.current();
        Models.AuthResponse me = session.user();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1240, 740);
        setMinimumSize(new Dimension(980, 560));
        setLocationRelativeTo(null);

        ReportsPanel reports = new ReportsPanel();
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Reports", reports);
        if (session.isSuperAdmin()) {
            tabs.addTab("Accounts", new AccountsPanel(reports::reloadPanchayats));
        }

        add(buildHeader(me), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        session.setOnExpire(() -> {
            JOptionPane.showMessageDialog(this, "Your session has expired. Please log in again.",
                    "Road Watch", JOptionPane.INFORMATION_MESSAGE);
            logout();
        });
    }

    private JComponent buildHeader(Models.AuthResponse me) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(ASPHALT);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 4, 0, MARKING),
                BorderFactory.createEmptyBorder(10, 16, 8, 16)));

        JLabel title = new JLabel("Road Watch Kozhikode");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

        String where = me.panchayatName() != null ? me.panchayatName() : "District admin";
        JLabel who = new JLabel(me.fullName() + "  |  " + where);
        who.setForeground(new Color(0xD6DAE0));

        JButton logout = new JButton("Log out");
        logout.addActionListener(e -> logout());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(who);
        right.add(logout);

        bar.add(title, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void logout() {
        Session.end();
        dispose();
        new LoginFrame().setVisible(true);
    }
}
