package com.rct.ui;

import com.rct.manager.CollectionManager;
import com.rct.util.TestGenerator;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TestGenerationDialog extends JDialog {
 private RSyntaxTextArea testCodeArea;
 private JTextField classNameField;
 private JTextField packageNameField;
 private JComboBox<String> frameworkCombo;
 private CollectionManager.SavedRequest currentRequest;
 private RequestTab currentTab;

 public TestGenerationDialog(Frame parent, RequestTab requestTab) {
  super(parent, "Generate Test Cases", true);
  this.currentTab = requestTab;
  initializeComponents();
  setupLayout();
  generateTestFromCurrentTab();
 }

 public TestGenerationDialog(Frame parent, CollectionManager.SavedRequest request) {
  super(parent, "Generate Test Cases", true);
  this.currentRequest = request;
  initializeComponents();
  setupLayout();
  generateTestFromRequest();
 }

 private void initializeComponents() {
  setSize(800, 600);
  setLocationRelativeTo(getParent());
  setDefaultCloseOperation(DISPOSE_ON_CLOSE);

  // Configuration panel
  JPanel configPanel = new JPanel();
  configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));
  configPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

  // Framework selection
  JPanel frameworkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
  frameworkPanel.add(new JLabel("Test Framework:"));
  frameworkCombo = new JComboBox<>(new String[]{"RestAssured + TestNG"});
  frameworkCombo.setSelectedIndex(0);
  frameworkPanel.add(frameworkCombo);

  // Package name
  JPanel packagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
  packagePanel.add(new JLabel("Package:"));
  packageNameField = new JTextField("com.api.tests", 20);
  packagePanel.add(packageNameField);

  // Class name
  JPanel classPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
  classPanel.add(new JLabel("Class Name:"));
  classNameField = new JTextField("ApiTest", 20);
  classPanel.add(classNameField);

  configPanel.add(frameworkPanel);
  configPanel.add(packagePanel);
  configPanel.add(classPanel);

  // Code area
  testCodeArea = new RSyntaxTextArea(25, 80);
  testCodeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
  testCodeArea.setCodeFoldingEnabled(true);
  testCodeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
  RTextScrollPane scrollPane = new RTextScrollPane(testCodeArea);

  // Button panel
  JPanel buttonPanel = new JPanel(new FlowLayout());

  JButton generateButton = new JButton("🔄Regenerate");
  generateButton.addActionListener(e -> regenerateTest());

  JButton copyButton = new JButton("📋Copy");
  copyButton.addActionListener(e -> copyToClipboard());

  JButton saveButton = new JButton("💾Save to File");
  saveButton.addActionListener(e -> saveToFile());

  JButton closeButton = new JButton("❌Close");
  closeButton.addActionListener(e -> dispose());

  buttonPanel.add(generateButton);
  buttonPanel.add(copyButton);
  buttonPanel.add(saveButton);
  buttonPanel.add(closeButton);

  // Layout
  setLayout(new BorderLayout());
  add(configPanel, BorderLayout.NORTH);
  add(scrollPane, BorderLayout.CENTER);
  add(buttonPanel, BorderLayout.SOUTH);
 }

 private void setupLayout() {
  // Add listeners for auto-regeneration
  packageNameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
   public void insertUpdate(javax.swing.event.DocumentEvent e) { regenerateTest(); }
   public void removeUpdate(javax.swing.event.DocumentEvent e) { regenerateTest(); }
   public void changedUpdate(javax.swing.event.DocumentEvent e) { regenerateTest(); }
  });

  classNameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
   public void insertUpdate(javax.swing.event.DocumentEvent e) { regenerateTest(); }
   public void removeUpdate(javax.swing.event.DocumentEvent e) { regenerateTest(); }
   public void changedUpdate(javax.swing.event.DocumentEvent e) { regenerateTest(); }
  });
 }

 private void generateTestFromCurrentTab() {
  if (currentTab == null) return;

  // Create a SavedRequest from current tab data
  CollectionManager.SavedRequest request = createRequestFromTab();
  generateTest(request);
 }

 private void generateTestFromRequest() {
  if (currentRequest == null) return;
  generateTest(currentRequest);
 }

 private CollectionManager.SavedRequest createRequestFromTab() {
  // Export session data from current tab to create SavedRequest
  com.rct.manager.SessionManager.TabSession session = currentTab.exportSession();

  return new CollectionManager.SavedRequest(
          session.getName(),
          session.getMethod(),
          session.getUrl(),
          session.getHeaders(),
          session.getBody(),
          session.getParams()
  );
 }

 private void generateTest(CollectionManager.SavedRequest request) {
  String testCode = TestGenerator.generateRestAssuredTest(request);

  // Update package and class name in the generated code
  String packageName = packageNameField.getText().trim();
  String className = classNameField.getText().trim();

  if (!packageName.isEmpty()) {
   testCode = testCode.replaceFirst("package com\\.api\\.tests;", "package " + packageName + ";");
  }

  if (!className.isEmpty()) {
   // Find and replace the class name
   testCode = testCode.replaceAll("public class \\w+Test", "public class " + className);
  }

  testCodeArea.setText(testCode);
  testCodeArea.setCaretPosition(0);
 }

 private void regenerateTest() {
  if (currentRequest != null) {
   generateTest(currentRequest);
  } else if (currentTab != null) {
   generateTestFromCurrentTab();
  }
 }

 private void copyToClipboard() {
  try {
   String code = testCodeArea.getText();
   java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(code);
   java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);

   JOptionPane.showMessageDialog(this,
           "Test code copied to clipboard! 📋",
           "Success",
           JOptionPane.INFORMATION_MESSAGE);
  } catch (Exception e) {
   JOptionPane.showMessageDialog(this,
           "Error copying to clipboard: " + e.getMessage(),
           "Error",
           JOptionPane.ERROR_MESSAGE);
  }
 }

 private void saveToFile() {
  JFileChooser fileChooser = new JFileChooser();
  fileChooser.setDialogTitle("Save Test File");

  String className = classNameField.getText().trim();
  if (className.isEmpty()) {
   className = "ApiTest";
  }

  fileChooser.setSelectedFile(new File(className + ".java"));
  fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Java Files", "java"));

  int result = fileChooser.showSaveDialog(this);
  if (result == JFileChooser.APPROVE_OPTION) {
   File file = fileChooser.getSelectedFile();

   // Ensure .java extension
   if (!file.getName().toLowerCase().endsWith(".java")) {
    file = new File(file.getAbsolutePath() + ".java");
   }

   try (FileWriter writer = new FileWriter(file)) {
    writer.write(testCodeArea.getText());

    JOptionPane.showMessageDialog(this,
            "Test file saved successfully:\n" + file.getAbsolutePath(),
            "Success",
            JOptionPane.INFORMATION_MESSAGE);

   } catch (IOException e) {
    JOptionPane.showMessageDialog(this,
            "Error saving file: " + e.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
   }
  }
 }
}