package com.rct.ui;

import com.rct.manager.CollectionManager;
import com.rct.util.CodeGenerator;
import net.miginfocom.swing.MigLayout;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class CodeGenerationDialog extends JDialog {
 private CollectionManager collectionManager;
 private JComboBox<RequestItem> requestCombo;
 private JComboBox<CodeGenerator.Language> languageCombo;
 private RSyntaxTextArea codeArea;
 private JButton generateBtn;
 private JButton copyBtn;
 private JButton saveBtn;

 public static class RequestItem {
 private String collectionName;
 private CollectionManager.SavedRequest request;

 public RequestItem(String collectionName, CollectionManager.SavedRequest request) {
 this.collectionName = collectionName;
 this.request = request;
 }

 @Override
 public String toString() {
 return collectionName + " → " + request.getName() + " (" + request.getMethod() + ")";
 }

 public CollectionManager.SavedRequest getRequest() { return request; }
 public String getCollectionName() { return collectionName; }
 }

 public CodeGenerationDialog(Frame parent, CollectionManager collectionManager) {
 super(parent, "Code Generation - SDK Generator", true);
 this.collectionManager = collectionManager;
 initializeComponents();
 setupLayout();
 loadRequests();
 setSize(900, 700);
 setLocationRelativeTo(parent);
 }

 private void initializeComponents() {
 // Request selection
 requestCombo = new JComboBox<>();
 requestCombo.setPreferredSize(new Dimension(300, 25));

 // Language selection
 languageCombo = new JComboBox<>(CodeGenerator.Language.values());
 languageCombo.setPreferredSize(new Dimension(150, 25));

 // Code display area
 codeArea = new RSyntaxTextArea(25, 80);
 codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
 codeArea.setCodeFoldingEnabled(true);
 codeArea.setAntiAliasingEnabled(true);
 codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
 codeArea.setEditable(false);

 // Buttons
 generateBtn = new JButton("🔧Generate Code");
 generateBtn.setBackground(new Color(0, 123, 255));
 generateBtn.setForeground(Color.WHITE);
 generateBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

 copyBtn = new JButton("📋Copy to Clipboard");
 copyBtn.setBackground(new Color(40, 167, 69));
 copyBtn.setForeground(Color.WHITE);
 copyBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
 copyBtn.setEnabled(false);

 saveBtn = new JButton("💾Save to File");
 saveBtn.setBackground(new Color(108, 117, 125));
 saveBtn.setForeground(Color.WHITE);
 saveBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
 saveBtn.setEnabled(false);

 setupEventListeners();
 }

 private void setupEventListeners() {
 generateBtn.addActionListener(e -> generateCode());
 copyBtn.addActionListener(e -> copyToClipboard());
 saveBtn.addActionListener(e -> saveToFile());

 languageCombo.addActionListener(e -> {
 CodeGenerator.Language selected = (CodeGenerator.Language) languageCombo.getSelectedItem();
 if (selected != null) {
  updateSyntaxHighlighting(selected);
 }
 });
 }

 private void setupLayout() {
 setLayout(new BorderLayout(10, 10));

 JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
 mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

 // Top panel with controls
 JPanel controlPanel = createControlPanel();

 // Code display panel
 JPanel codePanel = createCodePanel();

 // Button panel
 JPanel buttonPanel = createButtonPanel();

 mainPanel.add(controlPanel, BorderLayout.NORTH);
 mainPanel.add(codePanel, BorderLayout.CENTER);
 mainPanel.add(buttonPanel, BorderLayout.SOUTH);

 add(mainPanel, BorderLayout.CENTER);
 }

 private JPanel createControlPanel() {
 JPanel panel = new JPanel(new MigLayout("fillx", "[][grow][]", ""));
 panel.setBorder(BorderFactory.createTitledBorder("Code Generation Settings"));

 panel.add(new JLabel("Request:"), "");
 panel.add(requestCombo, "growx");
 panel.add(new JLabel("Language:"), "gapleft 20");
 panel.add(languageCombo, "wrap");

 return panel;
 }

 private JPanel createCodePanel() {
 JPanel panel = new JPanel(new BorderLayout());
 panel.setBorder(BorderFactory.createTitledBorder("Generated Code"));

 RTextScrollPane scrollPane = new RTextScrollPane(codeArea);
 scrollPane.setFoldIndicatorEnabled(true);
 panel.add(scrollPane, BorderLayout.CENTER);

 return panel;
 }

 private JPanel createButtonPanel() {
 JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

 panel.add(generateBtn);
 panel.add(copyBtn);
 panel.add(saveBtn);

 JButton closeBtn = new JButton("Close");
 closeBtn.addActionListener(e -> dispose());
 panel.add(closeBtn);

 return panel;
 }

 private void loadRequests() {
 requestCombo.removeAllItems();

 List<CollectionManager.Collection> collections = collectionManager.getCollections();
 for (CollectionManager.Collection collection : collections) {
  for (CollectionManager.SavedRequest request : collection.getRequests()) {
 requestCombo.addItem(new RequestItem(collection.getName(), request));
 }
 }

 if (requestCombo.getItemCount() == 0) {
 generateBtn.setEnabled(false);
 codeArea.setText("No saved requests found. Please save some requests first.");
 }
 }

 private void generateCode() {
 RequestItem selectedItem = (RequestItem) requestCombo.getSelectedItem();
 CodeGenerator.Language selectedLanguage = (CodeGenerator.Language) languageCombo.getSelectedItem();

 if (selectedItem == null || selectedLanguage == null) {
 JOptionPane.showMessageDialog(this, "Please select a request and language",
 "Selection Required", JOptionPane.WARNING_MESSAGE);
 return;
 }

 try {
 String generatedCode = CodeGenerator.generateCode(selectedItem.getRequest(), selectedLanguage);
 codeArea.setText(generatedCode);
 copyBtn.setEnabled(true);
 saveBtn.setEnabled(true);

 updateSyntaxHighlighting(selectedLanguage);

 } catch (Exception e) {
 JOptionPane.showMessageDialog(this, "Error generating code: " + e.getMessage(),
 "Generation Error", JOptionPane.ERROR_MESSAGE);
 }
 }

 private void updateSyntaxHighlighting(CodeGenerator.Language language) {
 switch (language) {
 case JAVA:
 codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
 break;
 case PYTHON:
 codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
 break;
 case JAVASCRIPT:
 codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
 break;
 case CURL:
 codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL);
 break;
 case CSHARP:
 codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CSHARP);
 break;
 case GO:
 codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GO);
 break;
 default:
 codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
 }
 }

 private void copyToClipboard() {
 String code = codeArea.getText();
 if (code != null && !code.trim().isEmpty()) {
 StringSelection selection = new StringSelection(code);
 Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
 JOptionPane.showMessageDialog(this, "Code copied to clipboard!",
 "Copy Successful", JOptionPane.INFORMATION_MESSAGE);
 }
 }

 private void saveToFile() {
 RequestItem selectedItem = (RequestItem) requestCombo.getSelectedItem();
 CodeGenerator.Language selectedLanguage = (CodeGenerator.Language) languageCombo.getSelectedItem();

 if (selectedItem == null || selectedLanguage == null) return;

 JFileChooser fileChooser = new JFileChooser();
 String fileName = selectedItem.getRequest().getName().replaceAll("[^a-zA-Z0-9]", "_")
 + "_client." + selectedLanguage.getExtension();
 fileChooser.setSelectedFile(new File(fileName));

 if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
 try {
 File file = fileChooser.getSelectedFile();
 try (FileWriter writer = new FileWriter(file)) {
 writer.write(codeArea.getText());
 }
 JOptionPane.showMessageDialog(this, "Code saved to: " + file.getAbsolutePath(),
 "Save Successful", JOptionPane.INFORMATION_MESSAGE);
 } catch (Exception e) {
 JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(),
 "Save Error", JOptionPane.ERROR_MESSAGE);
 }
 }
 }
}
