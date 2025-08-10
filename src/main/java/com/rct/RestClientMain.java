package com.rct;

import com.formdev.flatlaf.FlatLightLaf;
import com.rct.ui.RestClientApp;
import com.rct.util.LogManager;

import javax.swing.*;

public class RestClientMain{
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
                UIManager.put("Button.arc", 8);
                UIManager.put("Component.arc", 8);
                UIManager.put("TextComponent.arc", 8);
                UIManager.put("ScrollBar.width", 12);

                com.rct.util.FileManager.ensureAppDataDirectory();

                LogManager.getInstance().log("RestClientToolstarting up...");
                LogManager.getInstance().log("Java Version: " + System.getProperty("java.version"));
                LogManager.getInstance().log("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
                LogManager.getInstance().log("Data Directory: " + com.rct.util.FileManager.getAppDataDirectory());

                JFrame frame = new JFrame("RestClientTool");
                com.rct.util.AppIcon.setAppIcon(frame);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(1400, 900);
                frame.setLocationRelativeTo(null);
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

                RestClientApp app = new RestClientApp();
                frame.add(app);

                // Add shutdown hook to save sessions
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    LogManager.getInstance().log("Saving sessions before exit...");
                    app.saveSessions();
                }));

                frame.setVisible(true);

                LogManager.getInstance().log("Application UI initialized successfully");
                LogManager.getInstance().log("Collections and logs will be stored in: " + com.rct.util.FileManager.getAppDataDirectory());

            } catch (Exception e) {
                LogManager.getInstance().log("FATAL: Application startup failed - " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Failed to start application: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}