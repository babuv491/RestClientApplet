package com.rct.ui;

import com.rct.manager.CollectionManager;
import com.rct.model.RestResponse;
import com.rct.service.RestClientService;
import com.rct.util.UITheme;
import com.rct.util.LoadTestRunner;
import com.rct.util.PerformanceMetrics;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceTestDialog extends JDialog {
 private JTextField urlField;
 private JComboBox<String> methodCombo;
 private JSpinner concurrentUsersSpinner;
 private JSpinner requestsPerUserSpinner;
 private JSpinner rampUpTimeSpinner;
 private JSpinner testDurationSpinner;
 private JTextArea headersArea;
 private JTextArea bodyArea;
 private JButton startTestBtn;
 private JButton stopTestBtn;
 private JProgressBar progressBar;
 private JLabel statusLabel;
 private DefaultTableModel resultsModel;
 private JTable resultsTable;

 // Test execution
 private ExecutorService executorService;
 private volatile boolean testRunning = false;
 private AtomicInteger completedRequests = new AtomicInteger(0);
 private AtomicInteger successfulRequests = new AtomicInteger(0);
 private AtomicInteger failedRequests = new AtomicInteger(0);
 private AtomicLong totalResponseTime = new AtomicLong(0);
 private long testStartTime;
 private javax.swing.Timer updateTimer;

 // Results tracking
 private List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
 private Map<Integer, AtomicInteger> statusCounts = new ConcurrentHashMap<>();

 public PerformanceTestDialog(Frame parent) {
 super(parent, "Performance Testing - Load Test", true);
 initializeComponents();
 setupLayout();
 setSize(800, 700);
 setLocationRelativeTo(parent);
 }

 private void initializeComponents() {
 // Request configuration
 urlField = new JTextField("https://jsonplaceholder.typicode.com/posts/1");
 methodCombo = new JComboBox<>(new String[]{"GET", "POST", "PUT", "DELETE", "PATCH"});

 // Load test parameters
 concurrentUsersSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
 requestsPerUserSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
 rampUpTimeSpinner = new JSpinner(new SpinnerNumberModel(10, 0, 300, 1));
 testDurationSpinner = new JSpinner(new SpinnerNumberModel(60, 10, 3600, 10));

 // Request details
 headersArea = new JTextArea(3, 30);
 headersArea.setText("Content-Type: application/json\nAccept: application/json");
 bodyArea = new JTextArea(5, 30);
 bodyArea.setText("{\n \"title\": \"Test Post\",\n \"body\": \"Load test data\",\n \"userId\": 1\n}");

 // Control buttons
 startTestBtn = new JButton("🚀Start Load Test");
 startTestBtn.setBackground(UITheme.BUTTON_SUCCESS);
 startTestBtn.setForeground(Color.WHITE);
 startTestBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

 stopTestBtn = new JButton("⏹️Stop Test");
 stopTestBtn.setBackground(UITheme.BUTTON_DANGER);
 stopTestBtn.setForeground(Color.WHITE);
 stopTestBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
 stopTestBtn.setEnabled(false);

 // Progress and status
 progressBar = new JProgressBar(0, 100);
 progressBar.setStringPainted(true);
 progressBar.setString("Ready");
 statusLabel = new JLabel("Configure test parameters and click Start");

 // Results table
 String[] columns = {"Metric", "Value", "Unit"};
 resultsModel = new DefaultTableModel(columns, 0) {
 @Override
 public boolean isCellEditable(int row, int column) { return false; }
  };
 resultsTable = new JTable(resultsModel);
 resultsTable.setRowHeight(25);

 setupEventListeners();
 }

 private void setupEventListeners() {
 startTestBtn.addActionListener(e -> startLoadTest());
 stopTestBtn.addActionListener(e -> stopLoadTest());
 }

 private void setupLayout() {
 setLayout(new BorderLayout(10, 10));

 JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
 mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

 // Configuration panel
 JPanel configPanel = createConfigurationPanel();

 // Results panel
 JPanel resultsPanel = createResultsPanel();

 // Control panel
 JPanel controlPanel = createControlPanel();

 JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, configPanel, resultsPanel);
 splitPane.setDividerLocation(350);
 splitPane.setResizeWeight(0.5);

 mainPanel.add(splitPane, BorderLayout.CENTER);
 add(mainPanel, BorderLayout.CENTER);
 add(controlPanel, BorderLayout.SOUTH);
 }

 private JPanel createConfigurationPanel() {
 JPanel panel = new JPanel(new MigLayout("fillx", "[right][grow]", ""));
 panel.setBorder(BorderFactory.createTitledBorder("Load Test Configuration"));

 // Request configuration
 panel.add(new JLabel("URL:"), "");
 panel.add(urlField, "growx, wrap");

 panel.add(new JLabel("Method:"), "");
 panel.add(methodCombo, "growx, wrap");

 // Load test parameters
 panel.add(new JLabel("Concurrent Users:"), "");
 panel.add(concurrentUsersSpinner, "growx, wrap");

 panel.add(new JLabel("Requests per User:"), "");
 panel.add(requestsPerUserSpinner, "growx, wrap");

 panel.add(new JLabel("Ramp-up Time (sec):"), "");
 panel.add(rampUpTimeSpinner, "growx, wrap");

 panel.add(new JLabel("Test Duration (sec):"), "");
 panel.add(testDurationSpinner, "growx, wrap");

 // Headers
 panel.add(new JLabel("Headers:"), "top");
 panel.add(new JScrollPane(headersArea), "growx, wrap");

 // Body
 panel.add(new JLabel("Body:"), "top");
 panel.add(new JScrollPane(bodyArea), "growx, wrap");

 return panel;
 }

 private JPanel createResultsPanel() {
 JPanel panel = new JPanel(new BorderLayout());
 panel.setBorder(BorderFactory.createTitledBorder("Test Results"));

 JScrollPane scrollPane = new JScrollPane(resultsTable);
 panel.add(scrollPane, BorderLayout.CENTER);

 return panel;
 }

 private JPanel createControlPanel() {
 JPanel panel = new JPanel(new MigLayout("fillx", "[grow][][][]", ""));

 panel.add(statusLabel, "growx");
 panel.add(startTestBtn, "");
 panel.add(stopTestBtn, "");

 JButton exportBtn = new JButton("📊Export Results");
 exportBtn.addActionListener(e -> exportResults());
 panel.add(exportBtn, "wrap");

 panel.add(progressBar, "growx, span 4");

 return panel;
 }

 private void startLoadTest() {
 if (testRunning) return;

 // Reset counters
 completedRequests.set(0);
 successfulRequests.set(0);
 failedRequests.set(0);
 totalResponseTime.set(0);
 responseTimes.clear();
 statusCounts.clear();
 resultsModel.setRowCount(0);

 testRunning = true;
 testStartTime = System.currentTimeMillis();
 startTestBtn.setEnabled(false);
 stopTestBtn.setEnabled(true);

  int concurrentUsers = (Integer) concurrentUsersSpinner.getValue();
 int requestsPerUser = (Integer) requestsPerUserSpinner.getValue();
 int rampUpTime = (Integer) rampUpTimeSpinner.getValue();
 int testDuration = (Integer) testDurationSpinner.getValue();

 int totalRequests = concurrentUsers * requestsPerUser;
 progressBar.setMaximum(totalRequests);

 statusLabel.setText("Starting load test...");

 // Start update timer
 updateTimer = new javax.swing.Timer(1000, e -> updateResults());
 updateTimer.start();

 // Create load test configuration
 LoadTestRunner.LoadTestConfig config = new LoadTestRunner.LoadTestConfig(
 urlField.getText(),
 (String) methodCombo.getSelectedItem(),
 parseHeaders(headersArea.getText()),
 bodyArea.getText(),
 concurrentUsers,
 requestsPerUser,
 rampUpTime,
 testDuration
 );
 
 // Run load test in background
 SwingWorker<LoadTestRunner.LoadTestResult, Void> worker = new SwingWorker<LoadTestRunner.LoadTestResult, Void>() {
 @Override
 protected LoadTestRunner.LoadTestResult doInBackground() throws Exception {
 return LoadTestRunner.runLoadTest(config, new LoadTestRunner.LoadTestProgressCallback() {
 @Override
 public void onUserCompleted(int userIndex, int completedUsers, int totalUsers) {
 SwingUtilities.invokeLater(() -> {
 statusLabel.setText(String.format("Users completed: %d/%d", completedUsers, totalUsers));
 });
 }

  @Override
 public void onRequestCompleted(int userIndex, int completedRequests, int totalRequests,
 long responseTime, int statusCode, boolean success) {
 SwingUtilities.invokeLater(() -> {
 PerformanceTestDialog.this.completedRequests.incrementAndGet();
 PerformanceTestDialog.this.totalResponseTime.addAndGet(responseTime);
 PerformanceTestDialog.this.responseTimes.add(responseTime);

 if (success) {
 PerformanceTestDialog.this.successfulRequests.incrementAndGet();
 } else {
  PerformanceTestDialog.this.failedRequests.incrementAndGet();
 }

 statusCounts.computeIfAbsent(statusCode, k -> new AtomicInteger(0))
 .incrementAndGet();
 });
 }

 @Override
 public void onTestCompleted(PerformanceMetrics metrics) {
  SwingUtilities.invokeLater(() -> {
 statusLabel.setText("Load test completed - " + metrics.generateReport().split("\n")[0]);
 });
 }
 });
 }

 @Override
 protected void done() {
 stopLoadTest();
 }
 };

 worker.execute();
 }



 private Map<String, String> parseHeaders(String headersText) {
 Map<String, String> headers = new HashMap<>();
 if (headersText != null && !headersText.trim().isEmpty()) {
 String[] lines = headersText.split("\n");
 for (String line : lines) {
 String[] parts = line.split(":", 2);
  if (parts.length == 2) {
 headers.put(parts[0].trim(), parts[1].trim());
 }
 }
 }
 return headers;
 }

 private void updateResults() {
 if (!testRunning && completedRequests.get() == 0) return;

 int completed = completedRequests.get();
 int successful = successfulRequests.get();
 int failed = failedRequests.get();

 // Update progress
 progressBar.setValue(completed);
 progressBar.setString(String.format("%d/%d requests", completed, progressBar.getMaximum()));

 // Update status
 long elapsedTime = (System.currentTimeMillis() - testStartTime) / 1000;
 double requestsPerSecond = completed > 0 ? (double) completed / elapsedTime : 0;
 statusLabel.setText(String.format("Elapsed: %ds | RPS: %.2f | Success: %d | Failed: %d",
 elapsedTime, requestsPerSecond, successful, failed));

 // Update results table
 updateResultsTable(completed, successful, failed, elapsedTime, requestsPerSecond);
 }

 private void updateResultsTable(int completed, int successful, int failed, long elapsedTime, double rps) {
 resultsModel.setRowCount(0);

 // Basic metrics
 resultsModel.addRow(new Object[]{"Total Requests", completed, "count"});
 resultsModel.addRow(new Object[]{"Successful Requests", successful, "count"});
 resultsModel.addRow(new Object[]{"Failed Requests", failed, "count"});
 resultsModel.addRow(new Object[]{"Success Rate", String.format("%.2f%%", (double)successful/completed*100), "%"});
 resultsModel.addRow(new Object[]{"Requests per Second", String.format("%.2f", rps), "req/sec"});
 resultsModel.addRow(new Object[]{"Test Duration", elapsedTime, "seconds"});

 // Response time statistics
 if (!responseTimes.isEmpty()) {
 List<Long> sortedTimes = new ArrayList<>(responseTimes);
 Collections.sort(sortedTimes);

 long avgTime = totalResponseTime.get() / completed;
 long minTime = sortedTimes.get(0);
 long maxTime = sortedTimes.get(sortedTimes.size() - 1);
 long p50 = sortedTimes.get((int)(sortedTimes.size() * 0.5));
 long p95 = sortedTimes.get((int)(sortedTimes.size() * 0.95));
 long p99 = sortedTimes.get((int)(sortedTimes.size() * 0.99));

 resultsModel.addRow(new Object[]{"", "", ""});
 resultsModel.addRow(new Object[]{"Avg Response Time", avgTime, "ms"});
 resultsModel.addRow(new Object[]{"Min Response Time", minTime, "ms"});
 resultsModel.addRow(new Object[]{"Max Response Time", maxTime, "ms"});
 resultsModel.addRow(new Object[]{"50th Percentile", p50, "ms"});
 resultsModel.addRow(new Object[]{"95th Percentile", p95, "ms"});
 resultsModel.addRow(new Object[]{"99th Percentile", p99, "ms"});
 }

 // Status code distribution
 if (!statusCounts.isEmpty()) {
 resultsModel.addRow(new Object[]{"", "", ""});
 for (Map.Entry<Integer, AtomicInteger> entry : statusCounts.entrySet()) {
 resultsModel.addRow(new Object[]{
 "Status " + entry.getKey(),
 entry.getValue().get(),
 "count"
 });
 }
 }
 }

 private void stopLoadTest() {
 if (!testRunning) return;

 testRunning = false;
 startTestBtn.setEnabled(true);
 stopTestBtn.setEnabled(false);

 if (executorService != null) {
 executorService.shutdown();
 try {
 if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
 executorService.shutdownNow();
 }
 } catch (InterruptedException e) {
 executorService.shutdownNow();
 }
 }

 if (updateTimer != null) {
 updateTimer.stop();
 }

 // Final update
 updateResults();
 statusLabel.setText("Load test completed");
 progressBar.setString("Test Completed");
 }

 private void exportResults() {
 if (completedRequests.get() == 0) {
 JOptionPane.showMessageDialog(this, "No test results to export", "No Data", JOptionPane.WARNING_MESSAGE);
 return;
 }

 JFileChooser fileChooser = new JFileChooser();
 fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV files", "csv"));
 fileChooser.setSelectedFile(new java.io.File("load_test_results.csv"));

 if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
 try {
 java.io.File file = fileChooser.getSelectedFile();
 if (!file.getName().endsWith(".csv")) {
 file = new java.io.File(file.getAbsolutePath() + ".csv");
 }

 try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
 // Write CSV header
 writer.println("Metric,Value,Unit");

 // Write all table data
 for (int i = 0; i < resultsModel.getRowCount(); i++) {
 writer.printf("%s,%s,%s%n",
 resultsModel.getValueAt(i, 0),
 resultsModel.getValueAt(i, 1),
 resultsModel.getValueAt(i, 2)
 );
 }

 // Write raw response times
 writer.println("\nResponse Times (ms)");
 for (Long time : responseTimes) {
 writer.println(time);
 }
 }

 JOptionPane.showMessageDialog(this, "Results exported successfully!", "Export Complete", JOptionPane.INFORMATION_MESSAGE);

 } catch (Exception e) {
 JOptionPane.showMessageDialog(this, "Error exporting results: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
 }
 }
 }
}

