package com.roadwatch.admin.ui;

import com.roadwatch.admin.api.ApiClient;
import com.roadwatch.admin.api.Async;
import com.roadwatch.admin.api.Models;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

/** Log in, or request a new panchayat-admin account (which the district admin must approve). */
public class LoginFrame extends JFrame {

    private static final String PREF_URL = "serverUrl";
    private static final Color ASPHALT = new Color(0x1F2328);
    private static final Color MARKING = new Color(0xFFC400);

    private final Preferences prefs = Preferences.userNodeForPackage(LoginFrame.class);

    private final JTextField urlField = new JTextField(prefs.get(PREF_URL, "http://localhost:8080"), 26);
    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel message = new JLabel(" ");

    // log in
    private final JTextField userField = new JTextField(20);
    private final JPasswordField passField = new JPasswordField(20);
    private final JButton loginBtn = new JButton("Log in");

    // request account
    private final JTextField fullNameField = new JTextField(20);
    private final JTextField regUserField = new JTextField(20);
    private final JPasswordField regPassField = new JPasswordField(20);
    private final JPasswordField regPass2Field = new JPasswordField(20);
    private final JComboBox<Models.Panchayat> panchayatBox = new JComboBox<>();
    private final JButton registerBtn = new JButton("Request account");

    public LoginFrame() {
        super("Road Watch Kozhikode - Admin");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        loginBtn.addActionListener(e -> doLogin());
        registerBtn.addActionListener(e -> doRegister());
        tabs.addChangeListener(e -> {
            boolean login = tabs.getSelectedIndex() == 0;
            getRootPane().setDefaultButton(login ? loginBtn : registerBtn);
            setMessage(" ", false);
            if (!login && panchayatBox.getItemCount() <= 1) loadPanchayats();
        });
        getRootPane().setDefaultButton(loginBtn);

        pack();
        setMinimumSize(new Dimension(460, getHeight()));
        setLocationRelativeTo(null);
    }

    private JComponent buildHeader() {
        JPanel p = new JPanel(new GridLayout(2, 1));
        p.setBackground(ASPHALT);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 5, 0, MARKING),
                BorderFactory.createEmptyBorder(16, 20, 14, 20)));
        JLabel title = new JLabel("Road Watch Kozhikode");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        JLabel sub = new JLabel("Admin console for panchayat officials");
        sub.setForeground(new Color(0xD6DAE0));
        p.add(title);
        p.add(sub);
        return p;
    }

    private JComponent buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JPanel server = new JPanel(new GridBagLayout());
        row(server, 0, "Server address", urlField);
        body.add(server, BorderLayout.NORTH);

        tabs.addTab("Log in", buildLoginTab());
        tabs.addTab("Request account", buildRegisterTab());
        body.add(tabs, BorderLayout.CENTER);

        message.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
        body.add(message, BorderLayout.SOUTH);
        return body;
    }

    private JPanel buildLoginTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
        row(p, 0, "Username", userField);
        row(p, 1, "Password", passField);
        buttonRow(p, 2, loginBtn);
        return p;
    }

    private JPanel buildRegisterTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
        row(p, 0, "Full name", fullNameField);
        row(p, 1, "Panchayat", panchayatBox);
        row(p, 2, "Username", regUserField);
        row(p, 3, "Password", regPassField);
        row(p, 4, "Repeat password", regPass2Field);
        buttonRow(p, 5, registerBtn);
        return p;
    }

    private static void row(JPanel p, int y, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = y; c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(4, 4, 4, 10);
        p.add(new JLabel(label), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        c.insets = new Insets(4, 0, 4, 4);
        p.add(field, c);
    }

    private static void buttonRow(JPanel p, int y, JButton b) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1; c.gridy = y; c.anchor = GridBagConstraints.LINE_END;
        c.insets = new Insets(10, 0, 0, 4);
        p.add(b, c);
    }

    // ---------------------------------------------------------------- actions

    private void doLogin() {
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            setMessage("Enter your username and password.", true);
            return;
        }
        ApiClient api = new ApiClient(urlField.getText());
        prefs.put(PREF_URL, api.getBaseUrl());
        busy(true, "Logging in...");
        Async.run(() -> api.login(user, pass),
                resp -> {
                    Session.start(api, resp);
                    new MainFrame().setVisible(true);
                    dispose();
                },
                e -> {
                    busy(false, null);
                    setMessage(Ui.messageOf(e), true);
                });
    }

    private void loadPanchayats() {
        ApiClient api = new ApiClient(urlField.getText());
        Async.run(api::panchayats,
                list -> {
                    panchayatBox.removeAllItems();
                    panchayatBox.addItem(new Models.Panchayat(null, "Choose your panchayat", ""));
                    list.forEach(panchayatBox::addItem);
                },
                e -> setMessage(Ui.messageOf(e), true));
    }

    private void doRegister() {
        String fullName = fullNameField.getText().trim();
        String user = regUserField.getText().trim();
        String pass = new String(regPassField.getPassword());
        String pass2 = new String(regPass2Field.getPassword());
        Models.Panchayat p = (Models.Panchayat) panchayatBox.getSelectedItem();

        if (fullName.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            setMessage("Fill in all the fields.", true);
        } else if (p == null || p.id() == null) {
            setMessage("Choose your panchayat.", true);
        } else if (pass.length() < 8) {
            setMessage("Password must be at least 8 characters.", true);
        } else if (!pass.equals(pass2)) {
            setMessage("The two passwords do not match.", true);
        } else {
            ApiClient api = new ApiClient(urlField.getText());
            prefs.put(PREF_URL, api.getBaseUrl());
            busy(true, "Sending request...");
            Async.run(() -> api.register(user, pass, fullName, p.id()),
                    msg -> {
                        busy(false, null);
                        JOptionPane.showMessageDialog(this, msg, "Request sent", JOptionPane.INFORMATION_MESSAGE);
                        regPassField.setText("");
                        regPass2Field.setText("");
                        tabs.setSelectedIndex(0);
                        userField.setText(user);
                    },
                    e -> {
                        busy(false, null);
                        setMessage(Ui.messageOf(e), true);
                    });
        }
    }

    private void busy(boolean busy, String text) {
        loginBtn.setEnabled(!busy);
        registerBtn.setEnabled(!busy);
        if (busy) setMessage(text, false);
    }

    private void setMessage(String text, boolean error) {
        message.setText(text == null || text.isEmpty() ? " " : text);
        message.setForeground(error ? new Color(0xB3261E) : UIManager.getColor("Label.foreground"));
    }
}
