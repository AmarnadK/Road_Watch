package com.roadwatch.admin.ui;

import com.roadwatch.admin.api.ApiClient;
import com.roadwatch.admin.api.Async;
import com.roadwatch.admin.api.Models;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;

/** Right-hand side of the Reports tab: photo, details, history, and the status-update controls. */
public class ReportDetailPanel extends JPanel {

    private static final int PHOTO_W = 380, PHOTO_H = 240;

    private final Consumer<Models.Report> onUpdated;

    private Models.Report current;
    private int requestSeq = 0; // ignores slow responses for a report that is no longer selected

    private final JLabel photoLabel = new JLabel("Select a report", SwingConstants.CENTER);
    private final JButton mapBtn = new JButton("Open location in maps");
    private final JTextArea detailsArea = readOnlyArea();
    private final JTextArea historyArea = readOnlyArea();
    private final JLabel notice = new JLabel(" ");
    private final JComboBox<Ui.StatusItem> nextBox = new JComboBox<>();
    private final JTextField noteField = new JTextField();
    private final JButton updateBtn = new JButton("Update status");

    public ReportDetailPanel(Consumer<Models.Report> onUpdated) {
        super(new BorderLayout(0, 8));
        this.onUpdated = onUpdated;
        setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        setMinimumSize(new Dimension(420, 300));

        photoLabel.setPreferredSize(new Dimension(PHOTO_W, PHOTO_H));
        photoLabel.setBorder(BorderFactory.createLineBorder(new Color(0xD3D8DD)));
        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.add(photoLabel, BorderLayout.CENTER);
        top.add(mapBtn, BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Details", new JScrollPane(detailsArea));
        tabs.addTab("History", new JScrollPane(historyArea));

        add(top, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(buildStatusPanel(), BorderLayout.SOUTH);

        mapBtn.addActionListener(e -> openMap());
        updateBtn.addActionListener(e -> updateStatus());
        clear();
    }

    private JPanel buildStatusPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Update progress"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 6, 3, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        p.add(notice, c);

        c.gridy = 1; c.gridwidth = 1; c.weightx = 0;
        p.add(new JLabel("Move to"), c);
        c.gridx = 1; c.weightx = 1;
        p.add(nextBox, c);

        c.gridx = 0; c.gridy = 2; c.weightx = 0;
        p.add(new JLabel("Note"), c);
        c.gridx = 1; c.weightx = 1;
        p.add(noteField, c);

        c.gridx = 1; c.gridy = 3; c.fill = GridBagConstraints.NONE; c.anchor = GridBagConstraints.LINE_END;
        p.add(updateBtn, c);
        return p;
    }

    private static JTextArea readOnlyArea() {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setMargin(new Insets(8, 8, 8, 8));
        return a;
    }

    // ---------------------------------------------------------------- public API

    public void clear() {
        requestSeq++;
        current = null;
        photoLabel.setIcon(null);
        photoLabel.setText("Select a report");
        detailsArea.setText("");
        historyArea.setText("");
        mapBtn.setEnabled(false);
        setControls(false, "Select a report to see its details.", List.of());
    }

    /** Shows a report straight away from list data, then loads the full detail and photo. */
    public void show(Models.Report listItem) {
        int seq = ++requestSeq;
        render(listItem);
        photoLabel.setIcon(null);
        photoLabel.setText("Loading photo...");

        ApiClient api = Session.current().api();
        long id = listItem.id();

        Async.run(() -> api.report(id),
                full -> { if (seq == requestSeq) render(full); },
                e -> { if (seq == requestSeq) Ui.error(this, e); });

        Async.run(() -> loadPhoto(api, id),
                img -> {
                    if (seq == requestSeq) {
                        photoLabel.setText(null);
                        photoLabel.setIcon(new ImageIcon(img));
                    }
                },
                e -> {
                    if (seq == requestSeq) {
                        photoLabel.setIcon(null);
                        photoLabel.setText("Photo not available");
                    }
                });
    }

    // ---------------------------------------------------------------- rendering

    private void render(Models.Report r) {
        current = r;
        mapBtn.setEnabled(true);

        StringBuilder d = new StringBuilder();
        d.append("Reference:   RW-").append(r.id()).append('\n');
        d.append("Area:           ").append(r.panchayatName()).append('\n');
        d.append("Status:         ").append(Ui.statusLabel(r.status())).append('\n');
        d.append("Severity:      ").append(Ui.severityLabel(r.severity())).append('\n');
        d.append("Reported:     ").append(Ui.formatTime(r.createdAt())).append('\n');
        d.append("Last update: ").append(Ui.formatTime(r.updatedAt())).append("\n\n");
        if (r.landmark() != null) d.append("Landmark: ").append(r.landmark()).append("\n\n");
        d.append(r.description()).append("\n\n");
        d.append("Location: ").append(String.format("%.6f, %.6f", r.latitude(), r.longitude()));
        d.append(r.accuracyMeters() != null
                ? String.format(" (GPS, about %.0f m)", r.accuracyMeters())
                : " (pin placed by hand)").append("\n\n");
        boolean hasName = r.reporterName() != null, hasPhone = r.reporterPhone() != null;
        if (hasName || hasPhone) {
            d.append("Reporter: ").append(hasName ? r.reporterName() : "")
                    .append(hasName && hasPhone ? "  |  " : "")
                    .append(hasPhone ? r.reporterPhone() : "").append('\n');
        } else {
            d.append("Reporter: anonymous\n");
        }
        detailsArea.setText(d.toString());
        detailsArea.setCaretPosition(0);

        StringBuilder h = new StringBuilder();
        if (r.history() == null || r.history().isEmpty()) {
            h.append("No status changes yet.");
        } else {
            for (Models.HistoryEntry e : r.history()) {
                h.append(Ui.formatTime(e.at())).append("\n")
                        .append(Ui.statusLabel(e.oldStatus())).append("  ->  ").append(Ui.statusLabel(e.newStatus()))
                        .append("   by ").append(e.changedBy()).append('\n');
                if (e.note() != null) h.append("   \"").append(e.note()).append("\"\n");
                h.append('\n');
            }
        }
        historyArea.setText(h.toString());
        historyArea.setCaretPosition(0);

        List<String> next = r.allowedNext() == null ? List.of() : r.allowedNext();
        if (!r.editable()) {
            setControls(false, "Read-only: this report belongs to " + r.panchayatName()
                    + ". Only that panchayat's admin can change it.", List.of());
        } else if (next.isEmpty()) {
            setControls(false, "No further status changes are possible.", List.of());
        } else {
            setControls(true, "This report is in your area. You can update its progress.", next);
        }
    }

    private void setControls(boolean enabled, String text, List<String> allowedNext) {
        notice.setText("<html>" + text + "</html>");
        nextBox.removeAllItems();
        allowedNext.forEach(code -> nextBox.addItem(Ui.statusItem(code)));
        nextBox.setEnabled(enabled);
        noteField.setEnabled(enabled);
        updateBtn.setEnabled(enabled);
    }

    // ---------------------------------------------------------------- actions

    private void updateStatus() {
        Ui.StatusItem item = (Ui.StatusItem) nextBox.getSelectedItem();
        if (current == null || item == null) return;
        long id = current.id();
        String note = noteField.getText();
        ApiClient api = Session.current().api();

        updateBtn.setEnabled(false);
        Async.run(() -> api.changeStatus(id, item.code(), note),
                updated -> {
                    noteField.setText("");
                    render(updated);
                    onUpdated.accept(updated);
                },
                e -> {
                    if (current != null) render(current);
                    Ui.error(this, e);
                });
    }

    private void openMap() {
        if (current == null) return;
        String url = "https://www.google.com/maps/search/?api=1&query=" + current.latitude() + "," + current.longitude();
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (IOException | RuntimeException ignored) {
            // fall back to showing the link
        }
        JTextField link = new JTextField(url);
        link.setEditable(false);
        JOptionPane.showMessageDialog(this, link, "Copy this link into a browser", JOptionPane.PLAIN_MESSAGE);
    }

    // ---------------------------------------------------------------- photo

    private static Image loadPhoto(ApiClient api, long id) throws IOException {
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(api.photo(id)));
        if (src == null) throw new IOException("Unsupported image format");
        double s = Math.min(1.0, Math.min((double) PHOTO_W / src.getWidth(), (double) PHOTO_H / src.getHeight()));
        int w = Math.max(1, (int) Math.round(src.getWidth() * s));
        int h = Math.max(1, (int) Math.round(src.getHeight() * s));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }
}
