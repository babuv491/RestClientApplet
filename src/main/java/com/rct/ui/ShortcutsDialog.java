package com.rct.ui;

import com.rct.util.KeyboardShortcuts;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

public class ShortcutsDialog extends JDialog {

 public ShortcutsDialog(Frame parent) {
 super(parent, "Keyboard Shortcuts", true);
 setSize(600, 500);
 setLocationRelativeTo(parent);
 initializeComponents();
 }

 private void initializeComponents() {
 setLayout(new BorderLayout());
 
 JLabel titleLabel = new JLabel("⌨️Keyboard Shortcuts", SwingConstants.CENTER);
 titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
 titleLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
 add(titleLabel, BorderLayout.NORTH);

 String[] columns = {"Shortcut", "Action", "Description"};
 DefaultTableModel model = new DefaultTableModel(columns, 0) {
 @Override
 public boolean isCellEditable(int row, int column) { return false; }
 };

 JTable table = new JTable(model);
 table.setRowHeight(28);
 table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
 table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
 table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

 // Populate shortcuts
 Map<String, KeyboardShortcuts.ShortcutInfo> shortcuts = KeyboardShortcuts.getAllShortcuts();

 // Group shortcuts by category
 addCategoryRow(model, "📁File Operations");
 addShortcutRow(model, shortcuts, "newRequest");
 addShortcutRow(model, shortcuts, "saveRequest");
 addShortcutRow(model, shortcuts, "importCurl");
 addShortcutRow(model, shortcuts, "exportCollection");

 addCategoryRow(model, "📑Tab Operations");
 addShortcutRow(model, shortcuts, "newTab");
 addShortcutRow(model, shortcuts, "closeTab");
 addShortcutRow(model, shortcuts, "duplicateTab");
 addShortcutRow(model, shortcuts, "nextTab");
 addShortcutRow(model, shortcuts, "prevTab");

 addCategoryRow(model, "🚀Request Operations");
 addShortcutRow(model, shortcuts, "sendRequest");
 addShortcutRow(model, shortcuts, "refreshRequest");
 addShortcutRow(model, shortcuts, "cancelRequest");

 addCategoryRow(model, "🧭Navigation");
 addShortcutRow(model, shortcuts, "focusUrl");
 addShortcutRow(model, shortcuts, "focusParams");
 addShortcutRow(model, shortcuts, "focusHeaders");
 addShortcutRow(model, shortcuts, "focusBody");

 addCategoryRow(model, "🛠️Tools");
 addShortcutRow(model, shortcuts, "bulkExecution");
 addShortcutRow(model, shortcuts, "performanceTest");
 addShortcutRow(model, shortcuts, "codeGeneration");
 addShortcutRow(model, shortcuts, "documentation");
 addShortcutRow(model, shortcuts, "console");

 addCategoryRow(model, "📝JSON Operations");
 addShortcutRow(model, shortcuts, "formatJson");
 addShortcutRow(model, shortcuts, "minifyJson");
 addShortcutRow(model, shortcuts, "validateJson");

 addCategoryRow(model, "📋Copy Operations");
 addShortcutRow(model, shortcuts, "copyAsCurl");
 addShortcutRow(model, shortcuts, "copyResponse");

 addCategoryRow(model, "👁️View");
 addShortcutRow(model, shortcuts, "toggleFullscreen");
 addShortcutRow(model, shortcuts, "zoomIn");
 addShortcutRow(model, shortcuts, "zoomOut");

 addCategoryRow(model, "❓Help");
 addShortcutRow(model, shortcuts, "showHelp");
 addShortcutRow(model, shortcuts, "showShortcuts");

 JScrollPane scrollPane = new JScrollPane(table);
 add(scrollPane, BorderLayout.CENTER);

 JPanel buttonPanel = new JPanel(new FlowLayout());
 JButton closeButton = new JButton("Close");
 closeButton.addActionListener(e -> dispose());
 buttonPanel.add(closeButton);
 add(buttonPanel, BorderLayout.SOUTH);
 }

 private void addCategoryRow(DefaultTableModel model, String category) {
 model.addRow(new Object[]{category, "", ""});
 }

 private void addShortcutRow(DefaultTableModel model, Map<String, KeyboardShortcuts.ShortcutInfo> shortcuts, String key) {
 KeyboardShortcuts.ShortcutInfo info = shortcuts.get(key);
 if (info != null) {
 model.addRow(new Object[]{
 formatKeyStroke(info.key),
 key.replaceAll("([A-Z])", " $1").trim(),
 info.description
 });
 }
 }

 private String formatKeyStroke(String keyStroke) {
 return keyStroke.replace("ctrl", "Ctrl+")
 .replace("shift", "Shift+")
 .replace("alt", "Alt+")
 .replace("TAB", "Tab")
 .replace("ENTER", "Enter")
 .replace("ESCAPE", "Esc")
 .replace("PLUS", "+")
 .replace("MINUS", "-");
 }
}
