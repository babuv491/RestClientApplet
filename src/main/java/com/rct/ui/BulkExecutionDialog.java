package com.rct.ui;

import com.rct.manager.CollectionManager;
import com.rct.manager.EnvironmentManager;
import com.rct.model.RestResponse;
import com.rct.service.RestClientService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BulkExecutionDialog extends JDialog {
 private CollectionManager collectionManager;
 private JList<RequestItem> requestList;
 private DefaultListModel<RequestItem> listModel;
 private JTable resultsTable;
 private DefaultTableModel resultsModel;
 private JProgressBar progressBar;
 private JLabel statusLabel;
 private JButton executeBtn;
 private JButton pauseBtn;
 private JButton stopBtn;
 private JSpinner delaySpinner;
 private JSpinner threadsSpinner;
 private JCheckBox continueOnErrorCheck;
 private JCheckBox exportResultsCheck;
 private ExecutorService executorService;
 private volatile boolean isPaused = false;
 private volatile boolean isStopped = false;
 private int completedRequests = 0;
 private int totalRequests = 0;

 public static class RequestItem {
 private String collectionName;
 private String requestName;
 private String method;
 private String url;
 private CollectionManager.SavedRequest savedRequest;
 private boolean selected;

 public RequestItem(String collectionName, CollectionManager.SavedRequest request) {
 this.collectionName = collectionName;
 this.savedRequest = request;
 this.requestName = request.getName();
 this.method = request.getMethod();
 this.url = request.getUrl();
 this.selected = true;
  }

 @Override
 public String toString() {
 return String.format("[%s] %s → %s (%s)",
 selected ? "✓" : " ", collectionName, requestName, method);
 }

 // Getters
 public String getCollectionName() { return collectionName; }
 public String getRequestName() { return requestName; }
 public String getMethod() { return method; }
 public String getUrl() { return url; }
 public CollectionManager.SavedRequest getSavedRequest() { return savedRequest; }
 public boolean isSelected() { return selected; }
 public void setSelected(boolean selected) { this.selected = selected; }

 // Method to be overridden by subclasses for bulk request data
 public com.rct.util.BulkRequestParser.BulkRequest getBulkRequest() { return null; }
 }

 private EnvironmentManager environmentManager;

 public BulkExecutionDialog(Frame parent, CollectionManager collectionManager) {
 this(parent, collectionManager, null);
 }

 public BulkExecutionDialog(Frame parent, CollectionManager collectionManager, EnvironmentManager environmentManager) {
 super(parent, "Bulk Request Execution", true);
 this.collectionManager = collectionManager;
 this.environmentManager = environmentManager;
 initializeComponents();
 setupLayout();
 loadRequests();
 setSize(900, 700);
 setLocationRelativeTo(parent);
 }

 private void initializeComponents() {
 // Request selection components
 listModel = new DefaultListModel<>();
 requestList = new JList<>(listModel);
 requestList.setCellRenderer(new RequestListCellRenderer());
 requestList.addMouseListener(new java.awt.event.MouseAdapter() {
 @Override
 public void mouseClicked(java.awt.event.MouseEvent e) {
 if (e.getClickCount() == 1) {
 int index = requestList.locationToIndex(e.getPoint());
 if (index >= 0) {
 RequestItem item = listModel.getElementAt(index);
 item.setSelected(!item.isSelected());
 requestList.repaint();
 updateExecuteButton();
 }
 }
 }
 });

 // Results table
 String[] columns = {"Request", "Status", "Time (ms)", "Size (bytes)", "Result", "Error"};
 resultsModel = new DefaultTableModel(columns, 0) {
 @Override
 public boolean isCellEditable(int row, int column) { return false; }
 };
 resultsTable = new JTable(resultsModel);
 resultsTable.setDefaultRenderer(Object.class, new ResultsTableCellRenderer());
 resultsTable.setRowHeight(25);
 resultsTable.getColumnModel().getColumn(0).setPreferredWidth(200);
 resultsTable.getColumnModel().getColumn(1).setPreferredWidth(100);
 resultsTable.getColumnModel().getColumn(2).setPreferredWidth(80);
 resultsTable.getColumnModel().getColumn(3).setPreferredWidth(80);
 resultsTable.getColumnModel().getColumn(4).setPreferredWidth(80);
 resultsTable.getColumnModel().getColumn(5).setPreferredWidth(200);

 // Progress and status components
 progressBar = new JProgressBar(0, 100);
 progressBar.setStringPainted(true);
 progressBar.setString("Ready");

 statusLabel = new JLabel("Ready to execute requests");
 statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

 // Control components
 executeBtn = new JButton("🚀Execute Selected");
 executeBtn.setBackground(com.rct.util.UITheme.BUTTON_SUCCESS);
 executeBtn.setForeground(Color.WHITE);
 executeBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
 executeBtn.setFocusPainted(false);
 executeBtn.setBorderPainted(false);

 pauseBtn = new JButton("⏸️Pause");
 pauseBtn.setBackground(com.rct.util.UITheme.BUTTON_WARNING);
 pauseBtn.setForeground(Color.WHITE);
 pauseBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
 pauseBtn.setFocusPainted(false);
 pauseBtn.setBorderPainted(false);
 pauseBtn.setEnabled(false);

 stopBtn = new JButton("⏹️Stop");
 stopBtn.setBackground(com.rct.util.UITheme.BUTTON_DANGER);
 stopBtn.setForeground(Color.WHITE);
 stopBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
 stopBtn.setFocusPainted(false);
 stopBtn.setBorderPainted(false);
 stopBtn.setEnabled(false);

 // Configuration components
 delaySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 100));
 threadsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
 continueOnErrorCheck = new JCheckBox("Continue on error", true);
 exportResultsCheck = new JCheckBox("Export results to CSV", false);

 setupEventListeners();
 }

 private void setupEventListeners() {
 executeBtn.addActionListener(e -> executeSelectedRequests());
 pauseBtn.addActionListener(e -> togglePause());
 stopBtn.addActionListener(e -> stopExecution());
 }

 private void setupLayout() {
 setLayout(new BorderLayout());

 // Main content panel
 JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
 mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

 // Top panel with request selection
 JPanel topPanel = createRequestSelectionPanel();

 // Middle panel with configuration
 JPanel configPanel = createConfigurationPanel();

 // Bottom panel with results
 JPanel bottomPanel = createResultsPanel();

 // Progress panel
 JPanel progressPanel = createProgressPanel();

 // Control panel
 JPanel controlPanel = createControlPanel();

 // Layout
 JSplitPane topSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, configPanel);
 topSplit.setDividerLocation(300);
 topSplit.setResizeWeight(0.6);

 JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, bottomPanel);
 mainSplit.setDividerLocation(400);
 mainSplit.setResizeWeight(0.5);

 mainPanel.add(mainSplit, BorderLayout.CENTER);
 mainPanel.add(progressPanel, BorderLayout.SOUTH);

 add(mainPanel, BorderLayout.CENTER);
 add(controlPanel, BorderLayout.SOUTH);
 }

 private JPanel createRequestSelectionPanel() {
 JPanel panel = new JPanel(new BorderLayout());
 panel.setBorder(BorderFactory.createTitledBorder("📋Request Selection"));

 JScrollPane scrollPane = new JScrollPane(requestList);
 scrollPane.setPreferredSize(new Dimension(0, 250));
 panel.add(scrollPane, BorderLayout.CENTER);

 // Selection buttons
 JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

 JButton selectAllBtn = new JButton("✅Select All");
 selectAllBtn.addActionListener(e -> selectAll(true));
 
 JButton deselectAllBtn = new JButton("❌Deselect All");
 deselectAllBtn.addActionListener(e -> selectAll(false));

 JButton filterBtn = new JButton("🔍Filter");
 filterBtn.addActionListener(e -> showFilterDialog());

 JButton importBtn = new JButton("📁Import File");
 importBtn.setBackground(com.rct.util.UITheme.PRIMARY_BLUE);
 importBtn.setForeground(Color.WHITE);
 importBtn.setFocusPainted(false);
 importBtn.setBorderPainted(false);
 importBtn.addActionListener(e -> importFromFile());

 buttonPanel.add(selectAllBtn);
 buttonPanel.add(deselectAllBtn);
 buttonPanel.add(filterBtn);
 buttonPanel.add(importBtn);

 panel.add(buttonPanel, BorderLayout.SOUTH);

 return panel;
 }

 private JPanel createConfigurationPanel() {
 JPanel panel = new JPanel(new MigLayout("fillx", "[][grow][]", "[][]"));
 panel.setBorder(BorderFactory.createTitledBorder("⚙️Execution Configuration"));

 panel.add(new JLabel("Delay between requests (ms):"), "cell 0 0");
 panel.add(delaySpinner, "cell 1 0, growx");

 panel.add(new JLabel("Concurrent threads:"), "cell 2 0");
 panel.add(threadsSpinner, "cell 3 0");

 panel.add(continueOnErrorCheck, "cell 0 1, span 2");
 panel.add(exportResultsCheck, "cell 2 1, span 2");

 return panel;
 }

 private JPanel createResultsPanel() {
 JPanel panel = new JPanel(new BorderLayout());
 panel.setBorder(BorderFactory.createTitledBorder("📊Execution Results"));

 JScrollPane scrollPane = new JScrollPane(resultsTable);
 panel.add(scrollPane, BorderLayout.CENTER);

 // Results toolbar
 JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

 JButton clearBtn = new JButton("🗑️Clear");
 clearBtn.addActionListener(e -> resultsModel.setRowCount(0));

 JButton exportBtn = new JButton("📤Export CSV");
 exportBtn.addActionListener(e -> exportResults());

 JButton viewDetailsBtn = new JButton("🔍View Details");
 viewDetailsBtn.addActionListener(e -> viewSelectedResult());

 toolbarPanel.add(clearBtn);
 toolbarPanel.add(exportBtn);
 toolbarPanel.add(viewDetailsBtn);

 panel.add(toolbarPanel, BorderLayout.SOUTH);

 return panel;
 }
 
 private JPanel createProgressPanel() {
 JPanel panel = new JPanel(new BorderLayout(10, 5));
 panel.setBorder(new EmptyBorder(10, 0, 10, 0));

 panel.add(statusLabel, BorderLayout.NORTH);
 panel.add(progressBar, BorderLayout.CENTER);

 return panel;
 }

 private JPanel createControlPanel() {
 JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
 panel.setBorder(new EmptyBorder(10, 15, 15, 15));

 JButton closeBtn = new JButton("Close");
 closeBtn.addActionListener(e -> {
 if (executorService != null && !executorService.isShutdown()) {
 stopExecution();
 }
 dispose();
 });

 panel.add(executeBtn);
 panel.add(pauseBtn);
 panel.add(stopBtn);
 panel.add(Box.createHorizontalStrut(20));
 panel.add(closeBtn);

 return panel;
 }

 private void loadRequests() {
 listModel.clear();
 for (CollectionManager.Collection collection : collectionManager.getCollections()) {
 for (CollectionManager.SavedRequest request : collection.getRequests()) {
 listModel.addElement(new RequestItem(collection.getName(), request));
 }
 }
 updateExecuteButton();
 }

 private void selectAll(boolean selected) {
 for (int i = 0; i < listModel.getSize(); i++) {
 listModel.getElementAt(i).setSelected(selected);
 }
 requestList.repaint();
 updateExecuteButton();
 }

 private void updateExecuteButton() {
 int selectedCount = 0;
 for (int i = 0; i < listModel.getSize(); i++) {
 if (listModel.getElementAt(i).isSelected()) {
 selectedCount++;
 }
 }
 executeBtn.setText("🚀Execute Selected (" + selectedCount + ")");
 executeBtn.setEnabled(selectedCount > 0);
 }

 private void executeSelectedRequests() {
 List<RequestItem> selectedItems = new ArrayList<>();
 for (int i = 0; i < listModel.getSize(); i++) {
 RequestItem item = listModel.getElementAt(i);
 if (item.isSelected()) {
 selectedItems.add(item);
 }
 }

 if (selectedItems.isEmpty()) {
 JOptionPane.showMessageDialog(this, "Please select at least one request to execute.",
 "No Selection", JOptionPane.WARNING_MESSAGE);
 return;
 }

 // Clear previous results
 resultsModel.setRowCount(0);

 // Setup execution state
 totalRequests = selectedItems.size();
 completedRequests = 0;
 isPaused = false;
 isStopped = false;

 // Update UI
 executeBtn.setEnabled(false);
 pauseBtn.setEnabled(true);
 stopBtn.setEnabled(true);
 progressBar.setValue(0);
 progressBar.setString("Starting execution...");
 statusLabel.setText("Executing " + totalRequests + " requests...");

 // Create executor service
 int threadCount = (Integer) threadsSpinner.getValue();
 executorService = Executors.newFixedThreadPool(threadCount);

 // Execute requests
 executeRequestsAsync(selectedItems);
 }

 private void executeRequestsAsync(List<RequestItem> items) {
 int delay = (Integer) delaySpinner.getValue();
 boolean continueOnError = continueOnErrorCheck.isSelected();

 SwingWorker<Void, ExecutionResult> worker = new SwingWorker<Void, ExecutionResult>() {
 @Override
 protected Void doInBackground() throws Exception {
 RestClientService service = environmentManager != null ?
 new RestClientService(environmentManager) :
 new RestClientService();

 for (int i = 0; i < items.size() && !isStopped; i++) {
 // Handle pause
 while (isPaused && !isStopped) {
 Thread.sleep(100);
 }

 if (isStopped) break;

 RequestItem item = items.get(i);
 ExecutionResult result = new ExecutionResult();
 result.requestName = item.getRequestName();

 try {
 long startTime = System.currentTimeMillis();

 // Prepare request data with enhanced support
 Map<String, String> headers = parseHeaders(item.getSavedRequest().getHeaders());
 Map<String, String> params = parseParams(item.getSavedRequest().getParams());

 // Add authentication headers if needed
 addAuthenticationHeaders(headers, item);

 // Add cookies to headers
 addCookieHeaders(headers, item);

 // Execute request
 RestResponse response = service.sendRequest(
 item.getMethod(),
 item.getUrl(),
 headers,
 params,
 item.getSavedRequest().getBody()
 );

 long duration = System.currentTimeMillis() - startTime;

 // Process result
 result.status = response.getStatusCode() + " " + response.getStatusText();
 result.duration = duration;
 result.size = response.getBody() != null ? response.getBody().length() : 0;
 result.success = response.getStatusCode() >= 200 && response.getStatusCode() < 300;
 result.result = result.success ? "✅Success" : "❌Failed";
 result.error = result.success ? "" : "HTTP " + response.getStatusCode();

 } catch (Exception e) {
 result.status = "Error";
 result.duration = 0;
 result.size = 0;
 result.success = false;
 result.result = "❌Error";
 result.error = e.getMessage();

 if (!continueOnError) {
 publish(result);
 break;
 }
 }

 publish(result);

 // Add delay between requests
 if (delay > 0 && i < items.size() - 1) {
 Thread.sleep(delay);
 }
 }

 return null;
  }

 @Override
 protected void process(List<ExecutionResult> chunks) {
 for (ExecutionResult result : chunks) {
 resultsModel.addRow(new Object[]{
 result.requestName,
 result.status,
 result.duration,
 result.size,
 result.result,
 result.error
 });

  completedRequests++;
 int progress = (int) ((completedRequests * 100.0) / totalRequests);
 progressBar.setValue(progress);
 progressBar.setString(completedRequests + " / " + totalRequests + " completed");
 statusLabel.setText("Completed: " + completedRequests + " / " + totalRequests);
 }
 }

 @Override
 protected void done() {
 executionCompleted();
 }
 };

 worker.execute();
 }

 private void togglePause() {
 isPaused = !isPaused;
 if (isPaused) {
 pauseBtn.setText("▶️Resume");
 statusLabel.setText("Execution paused");
 } else {
 pauseBtn.setText("⏸️Pause");
 statusLabel.setText("Execution resumed");
 }
 }

 private void stopExecution() {
 isStopped = true;
 if (executorService != null) {
 executorService.shutdownNow();
 }
 executionCompleted();
 statusLabel.setText("Execution stopped by user");
 }

 private void executionCompleted() {
 executeBtn.setEnabled(true);
 pauseBtn.setEnabled(false);
 pauseBtn.setText("⏸️Pause");
 stopBtn.setEnabled(false);
 isPaused = false;

 if (executorService != null) {
 executorService.shutdown();
 }

 if (!isStopped) {
 progressBar.setString("Execution completed");
 statusLabel.setText("Execution completed successfully");

 if (exportResultsCheck.isSelected()) {
 exportResults();
 }
 }
 }

 private Map<String, String> parseHeaders(String headersStr) {
 Map<String, String> headers = new HashMap<>();
 if (headersStr != null && !headersStr.trim().isEmpty()) {
 String[] lines = headersStr.split("\n");
 for (String line : lines) {
 String[] parts = line.split(":", 2);
 if (parts.length == 2) {
 headers.put(parts[0].trim(), parts[1].trim());
 }
 }
 }
 return headers;
 }

  private Map<String, String> parseParams(String paramsStr) {
 Map<String, String> params = new HashMap<>();
 if (paramsStr != null && !paramsStr.trim().isEmpty()) {
 String[] lines = paramsStr.split("\n");
 for (String line : lines) {
 String[] parts = line.split("=", 2);
 if (parts.length == 2) {
 params.put(parts[0].trim(), parts[1].trim());
 }
 }
 }
 return params;
 }

 private void importFromFile() {
 JFileChooser fileChooser = new JFileChooser();
 fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
 "JSON and CSV files", "json", "csv"));

 if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
 try {
 File file = fileChooser.getSelectedFile();
 List<com.rct.util.BulkRequestParser.BulkRequest> bulkRequests =
 com.rct.util.BulkRequestParser.parseFromFile(file);

 // Convert to RequestItems and add to list
 for (com.rct.util.BulkRequestParser.BulkRequest bulkReq : bulkRequests) {
 RequestItem item = createRequestItemFromBulkRequest(bulkReq);
 listModel.addElement(item);
 }

 updateExecuteButton();
 JOptionPane.showMessageDialog(this,
 "Imported " + bulkRequests.size() + " requests successfully!",
 "Import Complete", JOptionPane.INFORMATION_MESSAGE);

 } catch (Exception e) {
 JOptionPane.showMessageDialog(this,
 "Error importing file: " + e.getMessage(),
 "Import Error", JOptionPane.ERROR_MESSAGE);
 }
 }
 }

 private RequestItem createRequestItemFromBulkRequest(com.rct.util.BulkRequestParser.BulkRequest bulkReq) {
 // Create a SavedRequest from BulkRequest
 CollectionManager.SavedRequest savedReq = new CollectionManager.SavedRequest(
 bulkReq.getName(),
 bulkReq.getMethod(),
 bulkReq.getUrl(),
 formatHeaders(bulkReq.getHeaders()),
 bulkReq.getBody(),
 formatParams(bulkReq.getQueryParams())
 );

 RequestItem item = new RequestItem("Imported", savedReq) {
 private com.rct.util.BulkRequestParser.BulkRequest originalBulkRequest = bulkReq;

 public com.rct.util.BulkRequestParser.BulkRequest getBulkRequest() {
 return originalBulkRequest;
 }
 };

 return item;
 }

 private String formatHeaders(Map<String, String> headers) {
 StringBuilder sb = new StringBuilder();
 for (Map.Entry<String, String> entry : headers.entrySet()) {
 sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
 }
 return sb.toString();
 }

 private String formatParams(Map<String, String> params) {
 StringBuilder sb = new StringBuilder();
 for (Map.Entry<String, String> entry : params.entrySet()) {
 sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
 }
 return sb.toString();
 }

 private void addAuthenticationHeaders(Map<String, String> headers, RequestItem item) {
 try {
 // Check if item has bulk request data with auth info
 if (item.getClass().getMethod("getBulkRequest") != null) {
 com.rct.util.BulkRequestParser.BulkRequest bulkReq =
 (com.rct.util.BulkRequestParser.BulkRequest) item.getClass().getMethod("getBulkRequest").invoke(item);

 String authType = bulkReq.getAuthType();
 if (authType != null && !authType.equals("none")) {
 switch (authType.toLowerCase()) {
 case "basic":
 String credentials = bulkReq.getAuthUsername() + ":" + bulkReq.getAuthPassword();
 String encoded = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
 headers.put("Authorization", "Basic " + encoded);
 break;
 case "bearer":
 headers.put("Authorization", "Bearer " + bulkReq.getAuthToken());
 break;
 case "apikey":
 headers.put(bulkReq.getAuthKey(), bulkReq.getAuthValue());
 break;
 }
  }
 }
 } catch (Exception e) {
 // Ignore if not a bulk request item
 }
 }

 private void addCookieHeaders(Map<String, String> headers, RequestItem item) {
 try {
 if (item.getClass().getMethod("getBulkRequest") != null) {
 com.rct.util.BulkRequestParser.BulkRequest bulkReq =
 (com.rct.util.BulkRequestParser.BulkRequest) item.getClass().getMethod("getBulkRequest").invoke(item);

 Map<String, String> cookies = bulkReq.getCookies();
 if (!cookies.isEmpty()) {
 StringBuilder cookieHeader = new StringBuilder();
 for (Map.Entry<String, String> entry : cookies.entrySet()) {
 if (cookieHeader.length() > 0) cookieHeader.append("; ");
 cookieHeader.append(entry.getKey()).append("=").append(entry.getValue());
 }
 headers.put("Cookie", cookieHeader.toString());
 }
 }
 } catch (Exception e) {
 // Ignore if not a bulk request item
 }
 }

 private void showFilterDialog() {
 JDialog filterDialog = new JDialog(this, "Filter Requests", true);
 filterDialog.setSize(400, 300);
 filterDialog.setLocationRelativeTo(this);

 JPanel panel = new JPanel(new MigLayout("fillx", "[][grow]", "[][][]"));

 JTextField methodFilter = new JTextField();
 JTextField collectionFilter = new JTextField();
 JTextField urlFilter = new JTextField();

 panel.add(new JLabel("Method:"), "cell 0 0");
 panel.add(methodFilter, "cell 1 0, growx");

 panel.add(new JLabel("Collection:"), "cell 0 1");
 panel.add(collectionFilter, "cell 1 1, growx");

 panel.add(new JLabel("URL contains:"), "cell 0 2");
 panel.add(urlFilter, "cell 1 2, growx");

 JPanel buttonPanel = new JPanel(new FlowLayout());
 JButton applyBtn = new JButton("Apply Filter");
 JButton clearBtn = new JButton("Clear Filter");
 JButton cancelBtn = new JButton("Cancel");

 applyBtn.addActionListener(e -> {
 applyFilter(methodFilter.getText(), collectionFilter.getText(), urlFilter.getText());
 filterDialog.dispose();
 });

 clearBtn.addActionListener(e -> {
 loadRequests();
 filterDialog.dispose();
 });

 cancelBtn.addActionListener(e -> filterDialog.dispose());

 buttonPanel.add(applyBtn);
 buttonPanel.add(clearBtn);
 buttonPanel.add(cancelBtn);

 panel.add(buttonPanel, "cell 0 3, span 2, center");

 filterDialog.add(panel);
 filterDialog.setVisible(true);
 }

 private void applyFilter(String methodFilter, String collectionFilter, String urlFilter) {
 DefaultListModel<RequestItem> filteredModel = new DefaultListModel<>();

 for (int i = 0; i < listModel.getSize(); i++) {
 RequestItem item = listModel.getElementAt(i);
 boolean matches = true;

 if (!methodFilter.trim().isEmpty() &&
  !item.getMethod().toLowerCase().contains(methodFilter.toLowerCase())) {
 matches = false;
 }

 if (!collectionFilter.trim().isEmpty() &&
 !item.getCollectionName().toLowerCase().contains(collectionFilter.toLowerCase())) {
 matches = false;
 }

 if (!urlFilter.trim().isEmpty() &&
 !item.getUrl().toLowerCase().contains(urlFilter.toLowerCase())) {
 matches = false;
 }

 if (matches) {
 filteredModel.addElement(item);
 }
 }

 requestList.setModel(filteredModel);
 listModel = filteredModel;
 updateExecuteButton();
 }

 private void exportResults() {
 JFileChooser fileChooser = new JFileChooser();
 fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV files", "csv"));
 fileChooser.setSelectedFile(new java.io.File("bulk_execution_results.csv"));

 if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
 try {
 java.io.FileWriter writer = new java.io.FileWriter(fileChooser.getSelectedFile());

 // Write header
 writer.write("Request,Status,Time (ms),Size (bytes),Result,Error\n");

 // Write data
 for (int i = 0; i < resultsModel.getRowCount(); i++) {
 for (int j = 0; j < resultsModel.getColumnCount(); j++) {
 Object value = resultsModel.getValueAt(i, j);
 writer.write("\"" + (value != null ? value.toString() : "") + "\"");
 if (j < resultsModel.getColumnCount() - 1) {
 writer.write(",");
 }
 }
 writer.write("\n");
 }

 writer.close();
 JOptionPane.showMessageDialog(this, "Results exported successfully!",
 "Export Complete", JOptionPane.INFORMATION_MESSAGE);

 } catch (Exception e) {
 JOptionPane.showMessageDialog(this, "Error exporting results: " + e.getMessage(),
 "Export Error", JOptionPane.ERROR_MESSAGE);
 }
 }
 }

 private void viewSelectedResult() {
 int selectedRow = resultsTable.getSelectedRow();
 if (selectedRow >= 0) {
 String request = (String) resultsModel.getValueAt(selectedRow, 0);
 String status = (String) resultsModel.getValueAt(selectedRow, 1);
 String time = resultsModel.getValueAt(selectedRow, 2).toString();
 String size = resultsModel.getValueAt(selectedRow, 3).toString();
 String result = (String) resultsModel.getValueAt(selectedRow, 4);
 String error = (String) resultsModel.getValueAt(selectedRow, 5);

 JDialog detailDialog = new JDialog(this, "Request Details", true);
 detailDialog.setSize(500, 400);
 detailDialog.setLocationRelativeTo(this);

 JTextArea textArea = new JTextArea();
 textArea.setEditable(false);
 textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

 StringBuilder details = new StringBuilder();
 details.append("Request: ").append(request).append("\n\n");
 details.append("Status: ").append(status).append("\n");
 details.append("Execution Time: ").append(time).append(" ms\n");
 details.append("Response Size: ").append(size).append(" bytes\n");
 details.append("Result: ").append(result).append("\n");
 if (!error.isEmpty()) {
 details.append("Error: ").append(error).append("\n");
 }

 textArea.setText(details.toString());

 JScrollPane scrollPane = new JScrollPane(textArea);
 detailDialog.add(scrollPane);
 detailDialog.setVisible(true);
 } else {
 JOptionPane.showMessageDialog(this, "Please select a result to view details.",
 "No Selection", JOptionPane.INFORMATION_MESSAGE);
 }
 }

 private static class ExecutionResult {
 String requestName;
 String status;
 long duration;
 int size;
 boolean success;
 String result;
 String error;
 }

 private static class RequestListCellRenderer extends DefaultListCellRenderer {
 @Override
 public Component getListCellRendererComponent(JList<?> list, Object value, int index,
 boolean isSelected, boolean cellHasFocus) {
 super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

 if (value instanceof RequestItem) {
 RequestItem item = (RequestItem) value;
 setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

 if (item.isSelected()) {
 setForeground(isSelected ? Color.WHITE : com.rct.util.UITheme.TEXT_PRIMARY);
 } else {
 setForeground(isSelected ? Color.WHITE : com.rct.util.UITheme.TEXT_SECONDARY);
 }
 }

 return this;
 }
 }

 private static class ResultsTableCellRenderer extends DefaultTableCellRenderer {
 @Override
 public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
 boolean hasFocus, int row, int column) {
 super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

 if (!isSelected) {
 if (column == 4 && value != null) { // Result column
 if (value.toString().contains("Success")) {
 setForeground(com.rct.util.UITheme.TEXT_SUCCESS);
 } else if (value.toString().contains("Failed") || value.toString().contains("Error")) {
 setForeground(com.rct.util.UITheme.TEXT_DANGER);
 } else {
 setForeground(com.rct.util.UITheme.TEXT_PRIMARY);
 }
 } else {
 setForeground(com.rct.util.UITheme.TEXT_PRIMARY);
 }

 setBackground(row % 2 == 0 ? com.rct.util.UITheme.TABLE_ROW_EVEN : com.rct.util.UITheme.TABLE_ROW_ODD);
 }

 return this;
 }
 }
}
