package com.rct.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LogManager {
 private static LogManager instance;
 private List<String> logs;
 private JDialog consoleWindow;
 private JTextArea consoleArea;
 private DateTimeFormatter formatter;

 private LogManager() {
 logs = new ArrayList<>();
 formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
 }

 public static LogManager getInstance() {
 if (instance == null) {
 instance = new LogManager();
 }
 return instance;
 }

 public void log(String message) {
 String timestamp = LocalDateTime.now().format(formatter);
 String logEntry = "[" + timestamp + "] " + message;
 logs.add(logEntry);

 System.out.println(logEntry);

 if (consoleArea != null) {
 SwingUtilities.invokeLater(() -> {
 consoleArea.append(logEntry + "\n");
 consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
 });
 }
 }

 public void showConsole(Component parent) {
 if (consoleWindow == null) {
 createConsoleWindow(parent);
 }
 consoleWindow.setVisible(true);
 consoleWindow.toFront();
 }

 private void createConsoleWindow(Component parent) {
 Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(parent);
 consoleWindow = new JDialog(parentFrame, false);
 consoleWindow.setUndecorated(true);
 consoleWindow.setSize(800, 600);
 consoleWindow.setLocationRelativeTo(parent);

 consoleArea = new JTextArea();
 consoleArea.setEditable(false);
 consoleArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
 consoleArea.setBackground(new Color(30, 30, 30));
  consoleArea.setForeground(new Color(220, 220, 220));
 consoleArea.setCaretColor(Color.WHITE);
 consoleArea.setTabSize(2);

 for (String log : logs) {
 consoleArea.append(log + "\n");
 }
 consoleArea.setCaretPosition(consoleArea.getDocument().getLength());

 JScrollPane scrollPane = new JScrollPane(consoleArea);
 scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

 JPanel buttonPanel = new JPanel(new FlowLayout());
 JButton clearBtn = new JButton("🗑️Clear");
 JButton saveBtn = new JButton("💾Save");
 JButton closeBtn = new JButton("❌Close");

 clearBtn.addActionListener(e -> {
 logs.clear();
 consoleArea.setText("");
 log("Console cleared");
 });

 saveBtn.addActionListener(e -> {
 try {
 File logsDir = getLogsDirectory();
 String timestamp = java.time.LocalDateTime.now().format(
 java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
 File logFile = new File(logsDir, "rest-client-logs_" + timestamp + ".txt");

 java.nio.file.Files.write(logFile.toPath(), String.join("\n", logs).getBytes());
 JOptionPane.showMessageDialog(consoleWindow,
 "Logs saved to: " + logFile.getAbsolutePath());
 } catch (Exception ex) {
 JOptionPane.showMessageDialog(consoleWindow, "Error saving logs: " + ex.getMessage());
 }
 });

 closeBtn.addActionListener(e -> consoleWindow.setVisible(false));

 buttonPanel.add(clearBtn);
 buttonPanel.add(saveBtn);
 buttonPanel.add(closeBtn);

 // Create custom title bar
 JPanel titleBar = createTitleBar();

 JPanel contentPanel = new JPanel(new BorderLayout());
 contentPanel.add(scrollPane, BorderLayout.CENTER);
 contentPanel.add(buttonPanel, BorderLayout.SOUTH);

 consoleWindow.setLayout(new BorderLayout());
 consoleWindow.add(titleBar, BorderLayout.NORTH);
 consoleWindow.add(contentPanel, BorderLayout.CENTER);

 // Make window draggable
 makeDraggable(titleBar);
 }

 private JPanel createTitleBar() {
 JPanel titleBar = new JPanel(new BorderLayout());
 titleBar.setBackground(new Color(45, 45, 45));
 titleBar.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 8));
 titleBar.setPreferredSize(new Dimension(800, 40));
 titleBar.setMinimumSize(new Dimension(200, 40));

 JLabel titleLabel = new JLabel("🖥️Console Logs");
 titleLabel.setForeground(Color.WHITE);
 titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

 JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
 buttonPanel.setOpaque(false);

 JButton minimizeBtn = createTitleButton("-", "Hide");
 JButton maximizeBtn = createTitleButton("□", "Maximize");
 JButton closeBtn = createTitleButton("×", "Close");

 minimizeBtn.addActionListener(e -> consoleWindow.setVisible(false));
 maximizeBtn.addActionListener(e -> toggleMaximize());
 closeBtn.addActionListener(e -> consoleWindow.setVisible(false));

 buttonPanel.add(minimizeBtn);
 buttonPanel.add(maximizeBtn);
 buttonPanel.add(closeBtn);

 titleBar.add(titleLabel, BorderLayout.WEST);
 titleBar.add(buttonPanel, BorderLayout.EAST);

 return titleBar;
 }

 private JButton createTitleButton(String text, String tooltip) {
 JButton button = new JButton(text);
 button.setPreferredSize(new Dimension(25, 20));
 button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
 button.setForeground(Color.WHITE);
 button.setBackground(new Color(60, 60, 60));
 button.setBorderPainted(false);
 button.setFocusPainted(false);
 button.setToolTipText(tooltip);

 button.addMouseListener(new java.awt.event.MouseAdapter() {
 @Override
 public void mouseEntered(java.awt.event.MouseEvent e) {
 button.setBackground(new Color(80, 80, 80));
 }

 @Override
 public void mouseExited(java.awt.event.MouseEvent e) {
  button.setBackground(new Color(60, 60, 60));
 }
 });

 return button;
 }

 private boolean isMaximized = false;
 private Rectangle normalBounds;

 private void toggleMaximize() {
 if (!isMaximized) {
 normalBounds = consoleWindow.getBounds();
 GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
 Rectangle maxBounds = env.getMaximumWindowBounds();
 consoleWindow.setBounds(maxBounds);
 isMaximized = true;
 } else {
 if (normalBounds != null) {
 consoleWindow.setBounds(normalBounds);
 }
 isMaximized = false;
 }
 }

 private void makeDraggable(JPanel titleBar) {
 final Point[] mouseDownCompCoords = new Point[1];

 titleBar.addMouseListener(new java.awt.event.MouseAdapter() {
 @Override
 public void mousePressed(java.awt.event.MouseEvent e) {
 mouseDownCompCoords[0] = e.getPoint();
 }
 });

 titleBar.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
 @Override
 public void mouseDragged(java.awt.event.MouseEvent e) {
 Point currCoords = e.getLocationOnScreen();
 consoleWindow.setLocation(currCoords.x - mouseDownCompCoords[0].x,
 currCoords.y - mouseDownCompCoords[0].y);
 }
 });
 }

 private File getLogsDirectory() {
 File logsDir = new File(FileManager.getAppDataDirectory(), "logs");
 if (!logsDir.exists()) {
 logsDir.mkdirs();
 }
 return logsDir;
 }
}
