package com.rct.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.intellijthemes.FlatArcOrangeIJTheme;
import com.rct.manager.CollectionManager;
import com.rct.manager.EnvironmentManager;
import com.rct.manager.HistoryManager;
import com.rct.util.CurlImporter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class RestClientApp extends JPanel implements RequestTab.TabUpdateCallback, EnhancedTabbedPane.TabCloseListener, EnhancedTabbedPane.NewTabListener, com.rct.util.KeyboardShortcuts.GlobalShortcutHandler {
 private EnhancedTabbedPane requestTabs;
 private JPanel collectionsPanel;
 private JPanel historyPanel;
 private JSplitPane mainSplitPane;
 private JSplitPane leftSplitPane;
 private List<RequestTab> requestTabsList;
 private CollectionManager collectionManager;
 private HistoryManager historyManager;
 private EnvironmentManager environmentManager;
 private com.rct.manager.SessionManager sessionManager;
 private int tabCounter = 1;

 public RestClientApp() {
 setLayout(new BorderLayout());
 requestTabsList = new ArrayList<>();
 collectionManager = new CollectionManager();
 historyManager = new HistoryManager();
 environmentManager = new EnvironmentManager();
 sessionManager = new com.rct.manager.SessionManager();

 initializeComponents();
 setupLayout();
 loadSavedSessions();
 }

 private void initializeComponents() {
 JPanel topPanel = new JPanel(new BorderLayout());

 JMenuBar menuBar = createMenuBar();
 topPanel.add(menuBar, BorderLayout.CENTER);

 // Add environment dropdown in top right
 JPanel envPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
 envPanel.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);

 JLabel envLabel = new JLabel("🌍Environment:");
 envLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));

 JComboBox<String> envDropdown = new JComboBox<>();
 envDropdown.setPreferredSize(new Dimension(150, 25));
 for (String envName : environmentManager.getEnvironmentNames()) {
 envDropdown.addItem(envName);
 }

 EnvironmentManager.Environment activeEnv = environmentManager.getActiveEnvironment();
 if (activeEnv != null) {
 envDropdown.setSelectedItem(activeEnv.getName());
 }

 envDropdown.addActionListener(e -> {
 String selected = (String) envDropdown.getSelectedItem();
 if (selected != null) {
 environmentManager.setActiveEnvironment(selected);
 }
 });

 JButton envSettingsBtn = new JButton("⚙️");
 envSettingsBtn.setPreferredSize(new Dimension(30, 25));
 envSettingsBtn.setToolTipText("Environment Settings");
 envSettingsBtn.addActionListener(e -> showEnvironmentDialog());

 envPanel.add(envLabel);
 envPanel.add(envDropdown);
 envPanel.add(envSettingsBtn);

 topPanel.add(envPanel, BorderLayout.EAST);

 add(topPanel, BorderLayout.NORTH);

 requestTabs = new EnhancedTabbedPane();
 requestTabs.addTabCloseListener(this);

 collectionsPanel = createCollectionsPanel();
 historyPanel = createHistoryPanel();

 JTabbedPane leftTabs = new JTabbedPane();
 leftTabs.addTab("<html><font color='" + com.rct.util.UITheme.ICON_COLLECTION + "'>📁</font> Collections</html>", collectionsPanel);
 leftTabs.addTab("<html><font color='" + com.rct.util.UITheme.ICON_HISTORY + "'>📜</font> History</html>", historyPanel);
 leftTabs.setPreferredSize(new Dimension(350, 0));

 leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, leftTabs, new JPanel());
 leftSplitPane.setDividerLocation(1.0);
 leftSplitPane.setResizeWeight(1.0);

 mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplitPane, requestTabs);
 mainSplitPane.setDividerLocation(350);
 mainSplitPane.setResizeWeight(0.0);
 }

 private void setupLayout() {
 add(mainSplitPane, BorderLayout.CENTER);

 // Add file drop zone to collections panel
 com.rct.util.FileDropZone dropZone = new com.rct.util.FileDropZone(files -> {
 for (java.io.File file : files) {
 try {
 if (file.getName().toLowerCase().endsWith(".json")) {
 com.rct.util.LogManager.getInstance().log("Drag & drop import from: " + file.getName());
 collectionManager.importFromFile(file);
 JOptionPane.showMessageDialog(this,
  "Collection imported successfully:\n" + file.getName(),
 "Import Success", JOptionPane.INFORMATION_MESSAGE);
 } else {
 JOptionPane.showMessageDialog(this,
 "Only JSON files are supported for import:\n" + file.getName(),
 "Invalid File Type", JOptionPane.WARNING_MESSAGE);
 }
 } catch (Exception e) {
 com.rct.util.LogManager.getInstance().log("Drag & drop import error: " + e.getMessage());
 JOptionPane.showMessageDialog(this,
 "Import error for file:\n" + file.getName() + "\n\nError: " + e.getMessage(),
 "Import Error", JOptionPane.ERROR_MESSAGE);
 }
 }
 });
 collectionsPanel.add(dropZone, BorderLayout.SOUTH);

 setupKeyboardShortcuts();
 com.rct.util.KeyboardShortcuts.setupGlobalShortcuts(this, this);
 }

 private JMenuBar createMenuBar() {
 JMenuBar menuBar = new JMenuBar();

 JMenu fileMenu = new JMenu("File");
 fileMenu.add(createMenuItem("New Request", "Ctrl+N", e -> createNewTab()));
 fileMenu.add(createMenuItem("Duplicate Request", "Ctrl+D", e -> duplicateCurrentTab()));
 fileMenu.add(createMenuItem("Import cURL", "Ctrl+I", e -> importCurlDialog()));
 fileMenu.addSeparator();
 fileMenu.add(createMenuItem("Bulk Request Execution", "Ctrl+B", e -> showEnhancedBulkExecutionDialog()));
 fileMenu.addSeparator();
 fileMenu.add(createMenuItem("Export Collection", "", e -> exportCollectionDialog()));
 fileMenu.add(createMenuItem("Import Collection", "", e -> importCollection()));

 JMenu viewMenu = new JMenu("View");
 viewMenu.add(createMenuItem("Light Theme", "", e -> setTheme("light")));
 viewMenu.add(createMenuItem("Dark Theme", "", e -> setTheme("dark")));
 viewMenu.add(createMenuItem("Arc Orange Theme", "", e -> setTheme("arc")));

 JMenu toolsMenu = new JMenu("Tools");
 toolsMenu.add(createMenuItem("Environment Variables", "Ctrl+E", e -> showEnvironmentDialog()));
 toolsMenu.add(createMenuItem("Performance Testing", "Ctrl+P", e -> showPerformanceTestDialog()));
 toolsMenu.add(createMenuItem("Code Generation", "Ctrl+G", e -> showCodeGenerationDialog()));
 toolsMenu.add(createMenuItem("Test Generation", "Ctrl+Shift+T", e -> showTestGenerationDialog()));
 toolsMenu.add(createMenuItem("API Documentation", "Ctrl+Shift+D", e -> showDocumentationDialog()));
 toolsMenu.addSeparator();
 toolsMenu.add(createMenuItem("Console Logs", "F12", e -> com.rct.util.LogManager.getInstance().showConsole(this)));
 toolsMenu.add(createMenuItem("Settings", "", e -> showSettingsDialog()));
 toolsMenu.addSeparator();
 toolsMenu.add(createMenuItem("Keyboard Shortcuts", "Ctrl+Shift+K", e -> showShortcutsDialog()));

 JMenu helpMenu = new JMenu("Help");
 helpMenu.add(createMenuItem("Help", "F1", e -> showHelpDialog()));
 helpMenu.add(createMenuItem("Keyboard Shortcuts", "Ctrl+Shift+K", e -> showShortcutsDialog()));

 menuBar.add(fileMenu);
 menuBar.add(viewMenu);
 menuBar.add(toolsMenu);
 menuBar.add(helpMenu);

 return menuBar;
 }

 private JMenuItem createMenuItem(String text, String accelerator, ActionListener action) {
 JMenuItem item = new JMenuItem(text);
 if (!accelerator.isEmpty()) {
 item.setAccelerator(KeyStroke.getKeyStroke(accelerator));
 }
 item.addActionListener(action);
 return item;
 }

 private JPanel createCollectionsPanel() {
 JPanel panel = new JPanel(new BorderLayout());
 panel.setBorder(new EmptyBorder(10, 10, 10, 10));

 JLabel title = new JLabel("<html><font color='" + com.rct.util.UITheme.ICON_COLLECTION + "'>📂</font> Collections</html>");
 title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
 panel.add(title, BorderLayout.NORTH);

 JTree collectionsTree = collectionManager.getCollectionsTree();

 // Enable drag and drop for reordering requests
 com.rct.util.DragDropHandler.enableTreeDragDrop(collectionsTree, collectionManager);

 collectionsTree.addMouseListener(new java.awt.event.MouseAdapter() {
 @Override
 public void mouseClicked(java.awt.event.MouseEvent e) {
 javax.swing.tree.TreePath path = collectionsTree.getPathForLocation(e.getX(), e.getY());
 if (path != null) {
 collectionsTree.setSelectionPath(path);
 if (e.getClickCount() == 2 && path.getPathCount() == 3) {
 loadSelectedRequest(collectionsTree);
 } else if (e.getButton() == java.awt.event.MouseEvent.BUTTON3) {
 showContextMenu(collectionsTree, e.getX(), e.getY());
 }
 }
 }
 });
 JScrollPane scrollPane = new JScrollPane(collectionsTree);
 panel.add(scrollPane, BorderLayout.CENTER);

 JPanel buttonPanel = new JPanel(new FlowLayout());
 JButton newCollectionBtn = new JButton("➕New");
 JButton deleteBtn = new JButton("🗑️Delete");

 newCollectionBtn.addActionListener(e -> {
 String name = JOptionPane.showInputDialog(this, "Collection Name:", "New Collection", JOptionPane.PLAIN_MESSAGE);
 if (name != null && !name.trim().isEmpty()) {
 collectionManager.addCollection(new CollectionManager.Collection(name.trim()));
 }
 });

 deleteBtn.addActionListener(e -> deleteSelectedCollection(collectionsTree));

 buttonPanel.add(newCollectionBtn);
 buttonPanel.add(deleteBtn);
 panel.add(buttonPanel, BorderLayout.SOUTH);

 return panel;
 }

 private JPanel createHistoryPanel() {
 JPanel panel = new JPanel(new BorderLayout());
 panel.setBorder(new EmptyBorder(10, 10, 10, 10));

 JLabel title = new JLabel("<html><font color='" + com.rct.util.UITheme.ICON_HISTORY + "'>📜</font> History</html>");
 title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
 panel.add(title, BorderLayout.NORTH);

 JList<String> historyList = historyManager.getHistoryList();

 // Enable drag and drop reordering for history
 com.rct.util.ReorderableListModel.enableReordering(historyList);

 historyList.addMouseListener(new java.awt.event.MouseAdapter() {
 @Override
 public void mouseClicked(java.awt.event.MouseEvent e) {
 if (e.getClickCount() == 2) {
 loadSelectedHistoryItem(historyList);
 } else if (e.getButton() == java.awt.event.MouseEvent.BUTTON3) {
 showHistoryContextMenu(historyList, e.getX(), e.getY());
 }
 }
 });
 JScrollPane scrollPane = new JScrollPane(historyList);
 panel.add(scrollPane, BorderLayout.CENTER);

 JPanel buttonPanel = new JPanel(new FlowLayout());
 JButton deleteBtn = new JButton("🗑️Delete");
 JButton clearBtn = new JButton("🗑️Clear All");

 deleteBtn.addActionListener(e -> {
 int selectedIndex = historyList.getSelectedIndex();
 if (selectedIndex >= 0) {
 int result = JOptionPane.showConfirmDialog(this, "Delete selected history entry?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
 if (result == JOptionPane.YES_OPTION) {
 historyManager.removeHistoryEntry(selectedIndex);
 }
 } else {
 JOptionPane.showMessageDialog(this, "Please select a history entry to delete.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
 }
 });

 clearBtn.addActionListener(e -> {
 int result = JOptionPane.showConfirmDialog(this, "Clear all history?", "Confirm", JOptionPane.YES_NO_OPTION);
 if (result == JOptionPane.YES_OPTION) {
 historyManager.clearHistory();
 }
 });

 buttonPanel.add(deleteBtn);
 buttonPanel.add(clearBtn);
 panel.add(buttonPanel, BorderLayout.SOUTH);

 return panel;
 }

 private void createInitialTab() {
 createNewTab();
 }

 private void createNewTab() {
 RequestTab tab = new RequestTab("Request " + tabCounter++, historyManager, collectionManager, environmentManager, this);
 requestTabsList.add(tab);

 String tooltip = "New request tab - Click to configure";
 requestTabs.addEnhancedTab(tab.getDisplayName(), tab.getPanel(), tooltip, true, tab.getStatusIndicator());
 requestTabs.setSelectedIndex(requestTabs.getTabCount() - 1);

 // Save sessions after creating new tab
 saveSessions();
 }

 private void closeTab(RequestTab tab) {
 int index = requestTabsList.indexOf(tab);
 if (index >= 0) {
 requestTabs.removeTabAt(index);
 requestTabsList.remove(tab);

 if (requestTabsList.isEmpty()) {
 createNewTab();
 }

 // Save sessions after closing tab
 saveSessions();
 }
 }

 private void importCurlDialog() {
 JTextArea textArea = new JTextArea(10, 50);
 textArea.setLineWrap(true);
 textArea.setWrapStyleWord(true);
 JScrollPane scrollPane = new JScrollPane(textArea);

 int result = JOptionPane.showConfirmDialog(this, scrollPane, "Import cURL Command",
 JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

 if (result == JOptionPane.OK_OPTION) {
 String curl = textArea.getText();
 if (curl != null && !curl.trim().isEmpty()) {
 try {
 CurlImporter.CurlRequest curlRequest = CurlImporter.parseCurl(curl);
 createNewTabFromCurl(curlRequest);
 } catch (Exception e) {
 JOptionPane.showMessageDialog(this, "Error parsing cURL: " + e.getMessage(),
 "Error", JOptionPane.ERROR_MESSAGE);
 }
 }
 }
 }

 public void exportCollectionDialog() {
 if (collectionManager.getCollections().isEmpty()) {
 JOptionPane.showMessageDialog(this, "No collections to export.", "Export", JOptionPane.INFORMATION_MESSAGE);
 return;
 }

 JFileChooser fileChooser = new JFileChooser();
 fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files", "json"));
 fileChooser.setDialogTitle("Export Collections");
 fileChooser.setSelectedFile(new java.io.File("collections.json"));

 if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
 java.io.File selectedFile = fileChooser.getSelectedFile();
 if (!selectedFile.getName().toLowerCase().endsWith(".json")) {
 selectedFile = new java.io.File(selectedFile.getAbsolutePath() + ".json");
 }

 try {
 com.rct.util.LogManager.getInstance().log("Starting export to: " + selectedFile.getAbsolutePath());
 collectionManager.exportToFile(selectedFile);
 JOptionPane.showMessageDialog(this,
 "Collections exported successfully to:\n" + selectedFile.getName(),
 "Export Success", JOptionPane.INFORMATION_MESSAGE);
 } catch (Exception e) {
 com.rct.util.LogManager.getInstance().log("Error during export: " + e.getMessage());
 e.printStackTrace();
 JOptionPane.showMessageDialog(this,
 "Error exporting collections to:\n" + selectedFile.getName() + "\n\nError: " + e.getMessage(),
 "Export Error", JOptionPane.ERROR_MESSAGE);
 }
 }
 }

 public void importCollection() {
 JFileChooser fileChooser = new JFileChooser();
 fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files", "json"));
 fileChooser.setDialogTitle("Import Collection");

 if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
 java.io.File selectedFile = fileChooser.getSelectedFile();
 try {
 com.rct.util.LogManager.getInstance().log("Starting import from: " + selectedFile.getAbsolutePath());
 collectionManager.importFromFile(selectedFile);
 JOptionPane.showMessageDialog(this,
 "Collection imported successfully from:\n" + selectedFile.getName(),
 "Import Success", JOptionPane.INFORMATION_MESSAGE);
 } catch (com.fasterxml.jackson.core.JsonParseException e) {
 com.rct.util.LogManager.getInstance().log("JSON parse error during import: " + e.getMessage());
 JOptionPane.showMessageDialog(this,
 "Invalid JSON format in file:\n" + selectedFile.getName() + "\n\nError: " + e.getMessage(),
 "Import Error", JOptionPane.ERROR_MESSAGE);
 } catch (java.io.FileNotFoundException e) {
 com.rct.util.LogManager.getInstance().log("File not found during import: " + e.getMessage());
 JOptionPane.showMessageDialog(this,
  "File not found:\n" + selectedFile.getName(),
 "Import Error", JOptionPane.ERROR_MESSAGE);
 } catch (Exception e) {
 com.rct.util.LogManager.getInstance().log("Unexpected error during import: " + e.getMessage());
 e.printStackTrace();
 JOptionPane.showMessageDialog(this,
 "Error importing collection from:\n" + selectedFile.getName() + "\n\nError: " + e.getMessage(),
 "Import Error", JOptionPane.ERROR_MESSAGE);
 }
 }
 }

 private void setTheme(String theme) {
 try {
 switch (theme) {
 case "light": FlatLightLaf.setup(); break;
 case "dark": FlatDarkLaf.setup(); break;
 case "arc": FlatArcOrangeIJTheme.setup(); break;
 }
 SwingUtilities.updateComponentTreeUI(SwingUtilities.getWindowAncestor(this));
 } catch (Exception e) {
 e.printStackTrace();
 }
 }

 private void showEnvironmentDialog() {
 EnvironmentDialog dialog = new EnvironmentDialog(
 (Frame) SwingUtilities.getWindowAncestor(this), environmentManager);
 dialog.setVisible(true);
 }

 private void showPerformanceTestDialog() {
 PerformanceTestDialog dialog = new PerformanceTestDialog(
 (Frame) SwingUtilities.getWindowAncestor(this));
 dialog.setVisible(true);
 }

 private void showCodeGenerationDialog() {
 CodeGenerationDialog dialog = new CodeGenerationDialog(
 (Frame) SwingUtilities.getWindowAncestor(this), collectionManager);
 dialog.setVisible(true);
 }

 private void showDocumentationDialog() {
 DocumentationDialog dialog = new DocumentationDialog(
 (Frame) SwingUtilities.getWindowAncestor(this), collectionManager);
 dialog.setVisible(true);
 }

 private void showTestGenerationDialog() {
 RequestTab currentTab = getCurrentTab().orElse(null);
 if (currentTab != null) {
 TestGenerationDialog dialog = new TestGenerationDialog(
 (Frame) SwingUtilities.getWindowAncestor(this), currentTab);
 dialog.setVisible(true);
 } else {
 JOptionPane.showMessageDialog(this,
 "No active request tab to generate tests from.",
 "No Request",
 JOptionPane.WARNING_MESSAGE);
 }
 }

 private void generateTestForRequest(CollectionManager.RequestTreeNode reqNode) {
 String collectionName = reqNode.getCollectionName();
 String method = reqNode.getMethod();
 String requestName = reqNode.getRequestName();

 for (CollectionManager.Collection collection : collectionManager.getCollections()) {
 if (collection.getName().equals(collectionName)) {
 for (CollectionManager.SavedRequest request : collection.getRequests()) {
 if (request.getName().equals(requestName) && request.getMethod().equals(method)) {
 TestGenerationDialog dialog = new TestGenerationDialog(
 (Frame) SwingUtilities.getWindowAncestor(this), request);
 dialog.setVisible(true);
 return;
 }
 }
 }
 }
 }

 private void createNewTabFromCurl(CurlImporter.CurlRequest curlRequest) {
 RequestTab tab = new RequestTab("Imported Request " + tabCounter++, historyManager, collectionManager, environmentManager, this);
 requestTabsList.add(tab);

 tab.setRequestData(curlRequest.getMethod(), curlRequest.getUrl(),
  curlRequest.getHeaders(), curlRequest.getBody());

 String tooltip = "Imported from cURL: " + curlRequest.getMethod() + " " + curlRequest.getUrl();
 requestTabs.addEnhancedTab(tab.getDisplayName(), tab.getPanel(), tooltip, true, tab.getStatusIndicator());
 requestTabs.setSelectedIndex(requestTabs.getTabCount() - 1);
 }

 private void setupKeyboardShortcuts() {
 // Legacy method kept for compatibility - actual shortcuts now handled by KeyboardShortcuts class
 }

 private void showEnhancedBulkExecutionDialog() {
 BulkExecutionDialog dialog = new BulkExecutionDialog(
 (Frame) SwingUtilities.getWindowAncestor(this), collectionManager, environmentManager);
 dialog.setVisible(true);
 }

 private void showBulkExecutionDialog() {
 JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Bulk Request Execution", true);
 dialog.setSize(600, 500);
 dialog.setLocationRelativeTo(this);

 JPanel panel = new JPanel(new BorderLayout());

 // Request selection panel
 JPanel selectionPanel = new JPanel(new BorderLayout());
 selectionPanel.setBorder(BorderFactory.createTitledBorder("Select Requests to Execute"));

 DefaultListModel<String> listModel = new DefaultListModel<>();
 JList<String> requestList = new JList<>(listModel);
 requestList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

 // Populate with all saved requests
 for (CollectionManager.Collection collection : collectionManager.getCollections()) {
 for (CollectionManager.SavedRequest request : collection.getRequests()) {
 listModel.addElement(collection.getName() + " → " + request.getName() + " (" + request.getMethod() + ")");
 }
 }

 JScrollPane listScrollPane = new JScrollPane(requestList);
 selectionPanel.add(listScrollPane, BorderLayout.CENTER);

 JPanel listButtonPanel = new JPanel(new FlowLayout());
 JButton selectAllBtn = new JButton("Select All");
 JButton clearSelectionBtn = new JButton("Clear Selection");

 selectAllBtn.addActionListener(e -> requestList.setSelectionInterval(0, listModel.getSize() - 1));
 clearSelectionBtn.addActionListener(e -> requestList.clearSelection());

 listButtonPanel.add(selectAllBtn);
 listButtonPanel.add(clearSelectionBtn);
 selectionPanel.add(listButtonPanel, BorderLayout.SOUTH);

 // Results panel
 JPanel resultsPanel = new JPanel(new BorderLayout());
 resultsPanel.setBorder(BorderFactory.createTitledBorder("Execution Results"));

 String[] columns = {"Request", "Status", "Time (ms)", "Result"};
 DefaultTableModel resultsModel = new DefaultTableModel(columns, 0) {
 @Override
 public boolean isCellEditable(int row, int column) { return false; }
 };
 JTable resultsTable = new JTable(resultsModel);
 JScrollPane resultsScrollPane = new JScrollPane(resultsTable);
 resultsPanel.add(resultsScrollPane, BorderLayout.CENTER);

 // Control panel
 JPanel controlPanel = new JPanel(new FlowLayout());
 JButton executeBtn = new JButton("🚀Execute Selected");
 JButton closeBtn = new JButton("Close");

 executeBtn.addActionListener(e -> {
 int[] selectedIndices = requestList.getSelectedIndices();
 if (selectedIndices.length == 0) {
 JOptionPane.showMessageDialog(dialog, "Please select at least one request to execute.", "No Selection", JOptionPane.WARNING_MESSAGE);
 return;
 }

 resultsModel.setRowCount(0);
 executeBtn.setEnabled(false);

 SwingWorker<Void, String[]> worker = new SwingWorker<Void, String[]>() {
 @Override
 protected Void doInBackground() throws Exception {
 for (int index : selectedIndices) {
 String requestInfo = listModel.getElementAt(index);
  String[] parts = requestInfo.split(" → ");
 String collectionName = parts[0];
 String requestPart = parts[1];
 String requestName = requestPart.substring(0, requestPart.lastIndexOf(" ("));
 String method = requestPart.substring(requestPart.lastIndexOf("(") + 1, requestPart.lastIndexOf(")"));

 CollectionManager.SavedRequest request = findRequest(collectionName, requestName, method);
 if (request != null) {
 try {
 long startTime = System.currentTimeMillis();
 com.rct.model.RestResponse response = executeRequest(request);
 long duration = System.currentTimeMillis() - startTime;

 String status = response.getStatusCode() + " " + response.getStatusText();
 String result = response.getStatusCode() >= 200 && response.getStatusCode() < 300 ? "✅Success" : "❌Failed";

 publish(new String[]{requestName, status, String.valueOf(duration), result});
 } catch (Exception ex) {
 publish(new String[]{requestName, "Error", "-", "❌" + ex.getMessage()});
 }
 }
 }
 return null;
 }

 @Override
 protected void process(java.util.List<String[]> chunks) {
 for (String[] result : chunks) {
 resultsModel.addRow(result);
 }
 }

 @Override
 protected void done() {
 executeBtn.setEnabled(true);
 JOptionPane.showMessageDialog(dialog, "Bulk execution completed!", "Execution Complete", JOptionPane.INFORMATION_MESSAGE);
 }
 };

 worker.execute();
 });

 closeBtn.addActionListener(e -> dialog.dispose());

 controlPanel.add(executeBtn);
 controlPanel.add(closeBtn);

 // Layout
 JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, selectionPanel, resultsPanel);
 splitPane.setDividerLocation(250);
 splitPane.setResizeWeight(0.5);

 panel.add(splitPane, BorderLayout.CENTER);
 panel.add(controlPanel, BorderLayout.SOUTH);

 dialog.add(panel);
 dialog.setVisible(true);
 }

 private CollectionManager.SavedRequest findRequest(String collectionName, String requestName, String method) {
 for (CollectionManager.Collection collection : collectionManager.getCollections()) {
 if (collection.getName().equals(collectionName)) {
 for (CollectionManager.SavedRequest request : collection.getRequests()) {
 if (request.getName().equals(requestName) && request.getMethod().equals(method)) {
 return request;
  }
 }
 }
 }
 return null;
 }

 private com.rct.model.RestResponse executeRequest(CollectionManager.SavedRequest request) throws Exception {
 com.rct.service.RestClientService service = new com.rct.service.RestClientService();

 java.util.Map<String, String> headers = new java.util.HashMap<>();
 if (request.getHeaders() != null && !request.getHeaders().trim().isEmpty()) {
 String[] headerLines = request.getHeaders().split("\n");
 for (String line : headerLines) {
 String[] parts = line.split(":", 2);
 if (parts.length == 2) {
 headers.put(parts[0].trim(), parts[1].trim());
 }
 }
 }

 java.util.Map<String, String> params = new java.util.HashMap<>();
 if (request.getParams() != null && !request.getParams().trim().isEmpty()) {
 String[] paramLines = request.getParams().split("\n");
 for (String line : paramLines) {
 String[] parts = line.split("=", 2);
 if (parts.length == 2) {
 params.put(parts[0].trim(), parts[1].trim());
 }
 }
 }

 return service.sendRequest(request.getMethod(), request.getUrl(), headers, params, request.getBody());
 }

 private void showSettingsDialog() {
 JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Settings", true);
 dialog.setSize(400, 300);
 dialog.setLocationRelativeTo(this);

 JPanel panel = new JPanel(new MigLayout("fillx", "[][grow]", "[][][][][]"));

 panel.add(new JLabel("Request Timeout (ms):"), "cell 0 0");
 JTextField timeoutField = new JTextField("30000");
 panel.add(timeoutField, "cell 1 0, growx");

 panel.add(new JLabel("Max Redirects:"), "cell 0 1");
 JTextField redirectsField = new JTextField("5");
 panel.add(redirectsField, "cell 1 1, growx");

 JCheckBox followRedirects = new JCheckBox("Follow Redirects", true);
 panel.add(followRedirects, "cell 0 2, span 2");

 JCheckBox validateSSL = new JCheckBox("Validate SSL Certificates", true);
 panel.add(validateSSL, "cell 0 3, span 2");

 JPanel buttonPanel = new JPanel(new FlowLayout());
 JButton saveBtn = new JButton("Save");
 JButton cancelBtn = new JButton("Cancel");

 saveBtn.addActionListener(e -> {
 JOptionPane.showMessageDialog(dialog, "Settings saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
 dialog.dispose();
 });
 cancelBtn.addActionListener(e -> dialog.dispose());

 buttonPanel.add(saveBtn);
 buttonPanel.add(cancelBtn);

 panel.add(buttonPanel, "cell 0 4, span 2, center");

 dialog.add(panel);
 dialog.setVisible(true);
 }

 private void loadSelectedRequest(JTree tree) {
 javax.swing.tree.TreePath path = tree.getSelectionPath();

 if (path != null && path.getPathCount() == 3) {
 javax.swing.tree.DefaultMutableTreeNode requestNode = (javax.swing.tree.DefaultMutableTreeNode) path.getLastPathComponent();

 if (requestNode instanceof CollectionManager.RequestTreeNode) {
 CollectionManager.RequestTreeNode reqNode = (CollectionManager.RequestTreeNode) requestNode;
 String collectionName = reqNode.getCollectionName();
 String method = reqNode.getMethod();
 String requestName = reqNode.getRequestName();
 
 for (CollectionManager.Collection collection : collectionManager.getCollections()) {
 if (collection.getName().equals(collectionName)) {
 for (CollectionManager.SavedRequest request : collection.getRequests()) {
 if (request.getName().equals(requestName) && request.getMethod().equals(method)) {
 createNewTabFromSavedRequest(request, collectionName);
 return;
 }
 }
 }
 }
 }
 }
 }

 private void showContextMenu(JTree tree, int x, int y) {
 javax.swing.tree.TreePath path = tree.getPathForLocation(x, y);
 if (path != null && path.getPathCount() == 3) {
 javax.swing.tree.DefaultMutableTreeNode requestNode = (javax.swing.tree.DefaultMutableTreeNode) path.getLastPathComponent();

 if (requestNode instanceof CollectionManager.RequestTreeNode) {
 tree.setSelectionPath(path);
 CollectionManager.RequestTreeNode reqNode = (CollectionManager.RequestTreeNode) requestNode;
 
 JPopupMenu contextMenu = new JPopupMenu();

 JMenuItem renameItem = new JMenuItem("<html><font color='" + com.rct.util.UITheme.ICON_PATCH + "'>✏️</font> Rename</html>");
 renameItem.addActionListener(e -> renameRequest(reqNode));

 JMenuItem copyAsCurlItem = new JMenuItem("<html><font color='" + com.rct.util.UITheme.ICON_GET + "'>📋</font> Copy as cURL</html>");
 copyAsCurlItem.addActionListener(e -> copyAsCurl(reqNode));

 JMenuItem generateTestItem = new JMenuItem("<html><font color='" + com.rct.util.UITheme.ICON_POST + "'>🧪</font> Generate Test</html>");
 generateTestItem.addActionListener(e -> generateTestForRequest(reqNode));

 JMenuItem deleteItem = new JMenuItem("<html><font color='" + com.rct.util.UITheme.ICON_DELETE + "'>🗑️</font> Delete</html>");
 deleteItem.addActionListener(e -> {
 int result = JOptionPane.showConfirmDialog(this,
 "Delete request '" + reqNode.getRequestName() + "'?", "Confirm Delete",
 JOptionPane.YES_NO_OPTION);
 if (result == JOptionPane.YES_OPTION) {
 collectionManager.removeRequest(reqNode.getCollectionName(), reqNode.getRequestName(), reqNode.getMethod());
 }
 });

 contextMenu.add(renameItem);
 contextMenu.add(copyAsCurlItem);
 contextMenu.add(generateTestItem);
 contextMenu.add(deleteItem);

 contextMenu.show(tree, x, y);
 }
 }
 }

 private void renameRequest(CollectionManager.RequestTreeNode reqNode) {
 String newName = JOptionPane.showInputDialog(this, "Enter new name:", reqNode.getRequestName());
 if (newName != null && !newName.trim().isEmpty()) {
 String collectionName = reqNode.getCollectionName();
 String method = reqNode.getMethod();
 String oldName = reqNode.getRequestName();

 for (CollectionManager.Collection collection : collectionManager.getCollections()) {
 if (collection.getName().equals(collectionName)) {
 for (CollectionManager.SavedRequest request : collection.getRequests()) {
 if (request.getName().equals(oldName) && request.getMethod().equals(method)) {
 request.setName(newName.trim());
 collectionManager.refreshTree();

 // Save the changes to file
  try {
 java.lang.reflect.Method saveMethod = collectionManager.getClass().getDeclaredMethod("saveCollections");
 saveMethod.setAccessible(true);
 saveMethod.invoke(collectionManager);
 } catch (Exception ex) {
 com.rct.util.LogManager.getInstance().log("Error saving collections after rename: " + ex.getMessage());
 }

 // Update any open tabs with the old name
 String oldTabName = collectionName + " → " + oldName;
 String newTabName = collectionName + " → " + newName.trim();
 for (RequestTab tab : requestTabsList) {
 if (tab.getName().equals(oldTabName)) {
 tab.setName(newTabName);
 updateTabTitle(tab);
 }
 }
 return;
  }
 }
 }
 }
 }
 }

 private void copyAsCurl(CollectionManager.RequestTreeNode reqNode) {
 try {
 String collectionName = reqNode.getCollectionName();
 String method = reqNode.getMethod();
 String requestName = reqNode.getRequestName();

 for (CollectionManager.Collection collection : collectionManager.getCollections()) {
 if (collection.getName().equals(collectionName)) {
 for (CollectionManager.SavedRequest request : collection.getRequests()) {
 if (request.getName().equals(requestName) && request.getMethod().equals(method)) {
 StringBuilder curl = new StringBuilder();
 curl.append("curl -X ").append(request.getMethod());
 
 // Build URL with query parameters
 String baseUrl = request.getUrl() != null ? request.getUrl().trim() : "";
 if (!baseUrl.isEmpty()) {
 StringBuilder urlWithParams = new StringBuilder(baseUrl);

 // Add query parameters if they exist
 if (request.getParams() != null && !request.getParams().trim().isEmpty()) {
 String[] paramLines = request.getParams().split("\n");
 boolean hasParams = false;
 for (String line : paramLines) {
  if (line.trim().isEmpty()) continue;
 String[] parts = line.split("=", 2);
 if (parts.length == 2) {
 if (!hasParams) {
 urlWithParams.append(baseUrl.contains("?") ? "&" : "?");
 hasParams = true;
 } else {
  urlWithParams.append("&");
 }
 urlWithParams.append(parts[0].trim()).append("=").append(parts[1].trim());
 }
 }
 }
 curl.append(" \"").append(urlWithParams.toString()).append("\"");
 }

  // Add headers
 if (request.getHeaders() != null && !request.getHeaders().trim().isEmpty()) {
 String[] headerLines = request.getHeaders().split("\n");
 for (String line : headerLines) {
 if (line.trim().isEmpty()) continue;
 String[] parts = line.split(":", 2);
 if (parts.length == 2) {
 curl.append(" \\").append("\n -H \"").append(parts[0].trim())
 .append(": ").append(parts[1].trim()).append("\"");
 }
 }
 }

 // Add body
 if (request.getBody() != null && !request.getBody().trim().isEmpty()) {
 String body = request.getBody().replace("\"", "\\\"").replace("\n", "\\n");
 curl.append(" \\").append("\n -d \"").append(body).append("\"");
 }

 java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(curl.toString());
 java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
 JOptionPane.showMessageDialog(this, "cURL command copied to clipboard!", "Success", JOptionPane.INFORMATION_MESSAGE);
 return;
 }
 }
 }
 }
 JOptionPane.showMessageDialog(this, "Request not found!", "Error", JOptionPane.ERROR_MESSAGE);
 } catch (Exception e) {
 JOptionPane.showMessageDialog(this, "Error generating cURL: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
 }
 }

 private void showHistoryContextMenu(JList<String> list, int x, int y) {
 int selectedIndex = list.locationToIndex(new java.awt.Point(x, y));
 if (selectedIndex >= 0) {
 list.setSelectedIndex(selectedIndex);
  java.util.List<HistoryManager.HistoryEntry> history = historyManager.getHistory();
 if (selectedIndex < history.size()) {
 HistoryManager.HistoryEntry entry = history.get(selectedIndex);

 JPopupMenu contextMenu = new JPopupMenu();

 JMenuItem copyAsCurlItem = new JMenuItem("<html><font color='" + com.rct.util.UITheme.ICON_GET + "'>📋</font> Copy as cURL</html>");
 copyAsCurlItem.addActionListener(e -> copyHistoryAsCurl(entry));

 contextMenu.add(copyAsCurlItem);
 contextMenu.show(list, x, y);
 }
 }
 }

 private void copyHistoryAsCurl(HistoryManager.HistoryEntry entry) {
 try {
 StringBuilder curl = new StringBuilder();
 curl.append("curl -X ").append(entry.getMethod());

 String url = entry.getUrl() != null ? entry.getUrl().trim() : "";
 if (!url.isEmpty()) {
  curl.append(" \"").append(url).append("\"");
 }

 // Add headers
 if (entry.getHeaders() != null && !entry.getHeaders().isEmpty()) {
 for (java.util.Map.Entry<String, String> headerEntry : entry.getHeaders().entrySet()) {
 curl.append(" \\").append("\n -H \"").append(headerEntry.getKey())
 .append(": ").append(headerEntry.getValue()).append("\"");
 }
 }

 // Add body
 if (entry.getBody() != null && !entry.getBody().trim().isEmpty()) {
 String body = entry.getBody().replace("\"", "\\\"").replace("\n", "\\n");
 curl.append(" \\").append("\n -d \"").append(body).append("\"");
 }

 java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(curl.toString());
 java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
 JOptionPane.showMessageDialog(this, "cURL command copied to clipboard!", "Success", JOptionPane.INFORMATION_MESSAGE);

 } catch (Exception e) {
 JOptionPane.showMessageDialog(this, "Error generating cURL: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
 }
 }

 private void loadSelectedHistoryItem(JList<String> list) {
 int selectedIndex = list.getSelectedIndex();
 if (selectedIndex >= 0) {
 java.util.List<HistoryManager.HistoryEntry> history = historyManager.getHistory();
 if (selectedIndex < history.size()) {
 HistoryManager.HistoryEntry entry = history.get(selectedIndex);
 createNewTabFromHistoryEntry(entry);
 }
 }
 }

 private void deleteSelectedCollection(JTree tree) {
 javax.swing.tree.TreePath path = tree.getSelectionPath();
 if (path != null) {
 if (path.getPathCount() == 2) {
 // Deleting a collection
 javax.swing.tree.DefaultMutableTreeNode node = (javax.swing.tree.DefaultMutableTreeNode) path.getLastPathComponent();
 String collectionName = node.toString().replaceAll("<[^>]*>", "").trim(); // Remove HTML tags

 int result = JOptionPane.showConfirmDialog(this,
 "Delete collection '" + collectionName + "'?", "Confirm Delete",
 JOptionPane.YES_NO_OPTION);

  if (result == JOptionPane.YES_OPTION) {
 collectionManager.removeCollection(collectionName);
 }
 } else if (path.getPathCount() == 3) {
 // Deleting a request
 javax.swing.tree.DefaultMutableTreeNode requestNode = (javax.swing.tree.DefaultMutableTreeNode) path.getLastPathComponent();
 if (requestNode instanceof CollectionManager.RequestTreeNode) {
 CollectionManager.RequestTreeNode reqNode = (CollectionManager.RequestTreeNode) requestNode;
 String collectionName = reqNode.getCollectionName();
 String requestName = reqNode.getRequestName();
 String method = reqNode.getMethod();

 int result = JOptionPane.showConfirmDialog(this,
 "Delete request '" + requestName + "'?", "Confirm Delete",
 JOptionPane.YES_NO_OPTION);

  if (result == JOptionPane.YES_OPTION) {
 collectionManager.removeRequest(collectionName, requestName, method);
 }
 }
 }
 } else {
 JOptionPane.showMessageDialog(this, "Please select a collection or request to delete.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
 }
 }

 private void createNewTabFromSavedRequest(CollectionManager.SavedRequest request, String collectionName) {
 com.rct.util.LogManager.getInstance().log("Creating new tab from saved request: " + request.getName());
 com.rct.util.LogManager.getInstance().log("Request details - Method: " + request.getMethod() + ", URL: " + request.getUrl());

 String uniqueTabName = collectionName + " → " + request.getName();

 for (int i = 0; i < requestTabsList.size(); i++) {
 RequestTab existingTab = requestTabsList.get(i);
 if (existingTab.getName().equals(uniqueTabName)) {
  requestTabs.setSelectedIndex(i);
 com.rct.util.LogManager.getInstance().log("Tab already exists, switching to it");
 return;
 }
 }

 RequestTab tab = new RequestTab(uniqueTabName, historyManager, collectionManager, environmentManager, this);
 requestTabsList.add(tab);

 java.util.Map<String, String> headers = new java.util.HashMap<>();
 if (request.getHeaders() != null && !request.getHeaders().trim().isEmpty()) {
 String[] headerLines = request.getHeaders().split("\n");
 for (String line : headerLines) {
 String[] parts = line.split(":", 2);
 if (parts.length == 2) {
 headers.put(parts[0].trim(), parts[1].trim());
 }
 }
 }

 com.rct.util.LogManager.getInstance().log("Setting request data on tab");
 tab.setRequestData(request.getMethod(), request.getUrl(), headers, request.getBody(), request.getParams());
 tab.setSavedInfo(collectionName, request.getName());

 String tooltip = "Collection: " + collectionName + "\nMethod: " + request.getMethod() + "\nURL: " + request.getUrl();
 requestTabs.addEnhancedTab(tab.getDisplayName(), tab.getPanel(), tooltip, true, tab.getStatusIndicator());
 requestTabs.setSelectedIndex(requestTabs.getTabCount() - 1);

 com.rct.util.LogManager.getInstance().log("New tab created and selected successfully");
 }

 private void createNewTabFromHistoryEntry(HistoryManager.HistoryEntry entry) {
 String tabName = "History: " + entry.getMethod() + " Request";

 for (int i = 0; i < requestTabsList.size(); i++) {
 RequestTab existingTab = requestTabsList.get(i);
 if (existingTab.getName().equals(tabName)) {
 requestTabs.setSelectedIndex(i);
 return;
 }
 }

  RequestTab tab = new RequestTab(tabName, historyManager, collectionManager, environmentManager, this);
 requestTabsList.add(tab);

 tab.setRequestData(entry.getMethod(), entry.getUrl(), entry.getHeaders(), entry.getBody());

 String tooltip = "From History\nMethod: " + entry.getMethod() + "\nURL: " + entry.getUrl() + "\nTime: " + entry.getTimestamp();
 requestTabs.addEnhancedTab(tab.getDisplayName(), tab.getPanel(), tooltip, true, tab.getStatusIndicator());
 requestTabs.setSelectedIndex(requestTabs.getTabCount() - 1);
 }

 private void duplicateCurrentTab() {
 int selectedIndex = requestTabs.getSelectedIndex();
 if (selectedIndex >= 0 && selectedIndex < requestTabsList.size()) {
 RequestTab currentTab = requestTabsList.get(selectedIndex);
 RequestTab duplicatedTab = currentTab.duplicate("Copy of " + currentTab.getName(), historyManager, collectionManager, environmentManager, this);
 requestTabsList.add(duplicatedTab);

 String tooltip = "Duplicated from: " + currentTab.getName();
 requestTabs.addEnhancedTab(duplicatedTab.getDisplayName(), duplicatedTab.getPanel(), tooltip, true, duplicatedTab.getStatusIndicator());
 requestTabs.setSelectedIndex(requestTabs.getTabCount() - 1);
 }
 }

 @Override
 public void updateTabTitle(RequestTab tab) {
 int index = requestTabsList.indexOf(tab);
 if (index >= 0) {
 requestTabs.updateTabTitle(index, tab.getDisplayName());
 }
 }

 // Enhanced tab close listener implementations
 @Override
 public void tabClosed(int index) {
 if (index >= 0 && index < requestTabsList.size()) {
 RequestTab tab = requestTabsList.get(index);
 closeTab(tab);
 }
 }

 @Override
 public void tabCloseAll() {
 while (!requestTabsList.isEmpty()) {
 closeTab(requestTabsList.get(0));
 }
 createNewTab(); // Always keep at least one tab
 }

 @Override
 public void tabCloseOthers(int keepIndex) {
 if (keepIndex >= 0 && keepIndex < requestTabsList.size()) {
 RequestTab keepTab = requestTabsList.get(keepIndex);
 List<RequestTab> tabsToClose = new ArrayList<>(requestTabsList);
 tabsToClose.remove(keepTab);

 for (RequestTab tab : tabsToClose) {
 closeTab(tab);
 }
 }
 }

 @Override
 public void tabDuplicate(int index) {
 if (index >= 0 && index < requestTabsList.size()) {
 RequestTab currentTab = requestTabsList.get(index);
 RequestTab duplicatedTab = currentTab.duplicate("Copy of " + currentTab.getName(), historyManager, collectionManager, environmentManager, this);
 requestTabsList.add(duplicatedTab);

 String tooltip = "Duplicated from: " + currentTab.getName();
 requestTabs.addEnhancedTab(duplicatedTab.getDisplayName(), duplicatedTab.getPanel(), tooltip, true, duplicatedTab.getStatusIndicator());
 requestTabs.setSelectedIndex(requestTabs.getTabCount() - 1);
 }
 }

 @Override
 public void newTabRequested() {
 createNewTab();
 }

 private void loadSavedSessions() {
 java.util.List<com.rct.manager.SessionManager.TabSession> sessions = sessionManager.loadSessions();

 if (sessions.isEmpty()) {
 createNewTab();
 } else {
 for (com.rct.manager.SessionManager.TabSession session : sessions) {
 RequestTab tab = new RequestTab(session.getName(), historyManager, collectionManager, environmentManager, this);
 tab.importSession(session);
 requestTabsList.add(tab);

 String tooltip = "Restored session";
 requestTabs.addEnhancedTab(tab.getDisplayName(), tab.getPanel(), tooltip, true, tab.getStatusIndicator());
 }
 requestTabs.setSelectedIndex(0);
 }
 }

 public void saveSessions() {
 java.util.List<com.rct.manager.SessionManager.TabSession> sessions = new java.util.ArrayList<>();
 for (RequestTab tab : requestTabsList) {
 sessions.add(tab.exportSession());
 }
 sessionManager.saveSessions(sessions);
 }

 // GlobalShortcutHandler implementation
 @Override public void newRequest() { createNewTab(); }
 @Override public void openRequest() { /* TODO: Implement open request */ }
 @Override public void saveRequest() { getCurrentTab().ifPresent(tab -> tab.saveToCollection()); }
 @Override public void saveAs() { getCurrentTab().ifPresent(tab -> tab.saveAsToCollection()); }
 @Override public void importCurl() { importCurlDialog(); }
 @Override public void exportCollection() { exportCollectionDialog(); }

 @Override public void newTab() { createNewTab(); }
 @Override public void closeTab() {
 int selectedIndex = requestTabs.getSelectedIndex();
 if (selectedIndex >= 0 && !requestTabsList.isEmpty()) {
 closeTab(requestTabsList.get(selectedIndex));
 }
 }
 @Override public void closeAllTabs() { tabCloseAll(); }
 @Override public void duplicateTab() { duplicateCurrentTab(); }
 @Override public void nextTab() {
 int current = requestTabs.getSelectedIndex();
 int next = (current + 1) % requestTabs.getTabCount();
 requestTabs.setSelectedIndex(next);
 }
 @Override public void prevTab() {
 int current = requestTabs.getSelectedIndex();
 int prev = (current - 1 + requestTabs.getTabCount()) % requestTabs.getTabCount();
 requestTabs.setSelectedIndex(prev);
 }
 @Override public void goToTab(int index) {
 if (index >= 0 && index < requestTabs.getTabCount()) {
 requestTabs.setSelectedIndex(index);
 }
 }

 @Override public void sendRequest() { getCurrentTab().ifPresent(tab -> tab.sendRequest()); }
 @Override public void sendRequestNewTab() { /* TODO: Implement send in new tab */ }
 @Override public void refreshRequest() { getCurrentTab().ifPresent(tab -> tab.sendRequest()); }
 @Override public void cancelRequest() { /* TODO: Implement cancel request */ }

 @Override public void focusUrl() { getCurrentTab().ifPresent(tab -> tab.focusUrl()); }
 @Override public void focusParams() { getCurrentTab().ifPresent(tab -> tab.focusParams()); }
 @Override public void focusHeaders() { getCurrentTab().ifPresent(tab -> tab.focusHeaders()); }
 @Override public void focusBody() { getCurrentTab().ifPresent(tab -> tab.focusBody()); }
 @Override public void focusAuth() { getCurrentTab().ifPresent(tab -> tab.focusAuth()); }

 @Override public void bulkExecution() { showEnhancedBulkExecutionDialog(); }
 @Override public void performanceTest() { showPerformanceTestDialog(); }
 @Override public void codeGeneration() { showCodeGenerationDialog(); }
 @Override public void testGeneration() { showTestGenerationDialog(); }
 @Override public void documentation() { showDocumentationDialog(); }
 @Override public void showConsole() { com.rct.util.LogManager.getInstance().showConsole(this); }

 @Override public void formatJson() { getCurrentTab().ifPresent(tab -> tab.formatJson()); }
 @Override public void minifyJson() { getCurrentTab().ifPresent(tab -> tab.minifyJson()); }
 @Override public void validateJson() { getCurrentTab().ifPresent(tab -> tab.validateJson()); }

 @Override public void copyAsCurl() { getCurrentTab().ifPresent(tab -> tab.copyAsCurl()); }
 @Override public void copyResponse() { getCurrentTab().ifPresent(tab -> tab.copyResponse()); }

 @Override public void toggleFullscreen() { /* TODO: Implement fullscreen */ }
 @Override public void zoomIn() { /* TODO: Implement zoom */ }
 @Override public void zoomOut() { /* TODO: Implement zoom */ }
 @Override public void resetZoom() { /* TODO: Implement zoom */ }

 @Override public void showHelp() { showHelpDialog(); }
 @Override public void showShortcuts() { showShortcutsDialog(); }

 private java.util.Optional<RequestTab> getCurrentTab() {
 int selectedIndex = requestTabs.getSelectedIndex();
 if (selectedIndex >= 0 && selectedIndex < requestTabsList.size()) {
 return java.util.Optional.of(requestTabsList.get(selectedIndex));
 }
 return java.util.Optional.empty();
 }

 private void showShortcutsDialog() {
 ShortcutsDialog dialog = new ShortcutsDialog((Frame) SwingUtilities.getWindowAncestor(this));
 dialog.setVisible(true);
 }

 private void showHelpDialog() {
 JOptionPane.showMessageDialog(this,
 "RestClientTool - Comprehensive API Testing Tool\n\n" +
 "Press Ctrl+Shift+K to view all keyboard shortcuts\n" +
 "Press F12 to open console logs\n" +
 "Visit our documentation for more help",
 "Help", JOptionPane.INFORMATION_MESSAGE);
 }
}