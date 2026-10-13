package com.roadwatch.admin;

import com.formdev.flatlaf.FlatLightLaf;
import com.roadwatch.admin.ui.LoginFrame;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            new LoginFrame().setVisible(true);
        });
    }
}
