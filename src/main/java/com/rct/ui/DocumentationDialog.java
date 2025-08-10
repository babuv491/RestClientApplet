package com.rct.ui;

import com.rct.manager.CollectionManager;
import com.rct.util.DocumentationGenerator;
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

public class DocumentationDialog extends JDialog {
 private CollectionManager collectionManager;
 private JComboBox<CollectionItem> collectionCombo;
 private JComboBox<DocumentationGenerator.Format> formatCombo;
 private RSyntaxTextArea docArea;
 private JButton generateBtn;
 private JButton copyBtn;
 private JButton saveBtn;
 private JButton previewBtn;

 public static class CollectionItem {
 private CollectionManager.Collection collection;

 public CollectionItem(CollectionManager.Collection collection) {
 this.collection = collection;
 }

 @Override
 public String toString() {
 return collection.getName() + " (" + collection.getRequests().size() + " endpoints)";
 }

 public CollectionManager.Collection getCollection() { return collection; }
 }

 public DocumentationDialog(Frame parent, CollectionManager collectionManager) {
 super(parent, "API Documentation Generator", true);
 this.collectionManager = collectionManager;
 initializeComponents();
 setupLayout();
 loadCollections();
 setSize(1000, 800);
 setLocationRelativeTo(parent);
 }

 private void initializeComponents() {
 // Collection selection
 collectionCombo = new JComboBox<>();
 collectionCombo.setPreferredSize(new Dimension(300, 25));

 // Format selection
 formatCombo = new JComboBox<>(DocumentationGenerator.Format.values());
 formatCombo.setPreferredSize(new Dimension(150, 25));

 // Documentation display area
 docArea = new RSyntaxTextArea(30, 90);
 docArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MARKDOWN);
 docArea.setCodeFoldingEnabled(true);
 docArea.setAntiAliasingEnabled(true);
 docArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
 docArea.setEditable(false);
 docArea.setText("Select a collection and format, then click Generate to create documentation.");

 // Buttons
 generateBtn = new JButton("📖Generate Documentation");
 generateBtn.setBackground(new Color(0, 123, 255));
 generateBtn.setForeground(Color.WHITE);
 generateBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

 copyBtn = new JButton("📋Copy");
 copyBtn.setBackground(new Color(40, 167, 69));
 copyBtn.setForeground(Color.WHITE);
 copyBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
 copyBtn.setEnabled(false);
 
 saveBtn = new JButton("💾Save");
 saveBtn.setBackground(new Color(108, 117, 125));
 saveBtn.setForeground(Color.WHITE);
 saveBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
 saveBtn.setEnabled(false);

 previewBtn = new JButton("👁️Preview");
 previewBtn.setBackground(new Color(255, 193, 7));
 previewBtn.setForeground(Color.BLACK);
 previewBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
 previewBtn.setEnabled(false);

 setupEventListeners();
 }

 private void setupEventListeners() {
 generateBtn.addActionListener(e -> generateDocumentation());
 copyBtn.addActionListener(e -> copyToClipboard());
 saveBtn.addActionListener(e -> saveToFile());
 previewBtn.addActionListener(e -> previewDocumentation());

 formatCombo.addActionListener(e -> {
 DocumentationGenerator.Format selected = (DocumentationGenerator.Format) formatCombo.getSelectedItem();
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

 // Documentation display panel
 JPanel docPanel = createDocumentationPanel();

 // Button panel
 JPanel buttonPanel = createButtonPanel();

 mainPanel.add(controlPanel, BorderLayout.NORTH);
 mainPanel.add(docPanel, BorderLayout.CENTER);
 mainPanel.add(buttonPanel, BorderLayout.SOUTH);

 add(mainPanel, BorderLayout.CENTER);
 }

 private JPanel createControlPanel() {
 JPanel panel = new JPanel(new MigLayout("fillx", "[][grow][]", ""));
 panel.setBorder(BorderFactory.createTitledBorder("Documentation Settings"));

 panel.add(new JLabel("Collection:"), "");
 panel.add(collectionCombo, "growx");
 panel.add(new JLabel("Format:"), "gapleft 20");
 panel.add(formatCombo, "wrap");

 return panel;
 }

 private JPanel createDocumentationPanel() {
 JPanel panel = new JPanel(new BorderLayout());
 panel.setBorder(BorderFactory.createTitledBorder("Generated Documentation"));

 RTextScrollPane scrollPane = new RTextScrollPane(docArea);
 scrollPane.setFoldIndicatorEnabled(true);
 panel.add(scrollPane, BorderLayout.CENTER);

 return panel;
 }

 private JPanel createButtonPanel() {
 JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

 panel.add(generateBtn);
 panel.add(copyBtn);
 panel.add(saveBtn);
 panel.add(previewBtn);

 JButton closeBtn = new JButton("Close");
 closeBtn.addActionListener(e -> dispose());
 panel.add(closeBtn);

 return panel;
 }

 private void loadCollections() {
 collectionCombo.removeAllItems();

 List<CollectionManager.Collection> collections = collectionManager.getCollections();
 for (CollectionManager.Collection collection : collections) {
 if (!collection.getRequests().isEmpty()) {
 collectionCombo.addItem(new CollectionItem(collection));
 }
 }

 if (collectionCombo.getItemCount() == 0) {
 generateBtn.setEnabled(false);
 docArea.setText("No collections with requests found. Please create some collections with saved requests first.");
 }
 }

 private void generateDocumentation() {
 CollectionItem selectedItem = (CollectionItem) collectionCombo.getSelectedItem();
 DocumentationGenerator.Format selectedFormat = (DocumentationGenerator.Format) formatCombo.getSelectedItem();

 if (selectedItem == null || selectedFormat == null) {
 JOptionPane.showMessageDialog(this, "Please select a collection and format",
 "Selection Required", JOptionPane.WARNING_MESSAGE);
 return;
 }

 try {
 String documentation = DocumentationGenerator.generateDocumentation(
 selectedItem.getCollection(), selectedFormat);
 docArea.setText(documentation);
 copyBtn.setEnabled(true);
 saveBtn.setEnabled(true);
 previewBtn.setEnabled(selectedFormat == DocumentationGenerator.Format.HTML);

 updateSyntaxHighlighting(selectedFormat);

 } catch (Exception e) {
 JOptionPane.showMessageDialog(this, "Error generating documentation: " + e.getMessage(),
 "Generation Error", JOptionPane.ERROR_MESSAGE);
 }
 }

 private void updateSyntaxHighlighting(DocumentationGenerator.Format format) {
 switch (format) {
 case MARKDOWN:
 docArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MARKDOWN);
 break;
 case HTML:
 docArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HTML);
 break;
 case JSON:
 case POSTMAN:
 docArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
 break;
 default:
 docArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
 }
 }

 private void copyToClipboard() {
 String documentation = docArea.getText();
 if (documentation != null && !documentation.trim().isEmpty()) {
 StringSelection selection = new StringSelection(documentation);
 Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
 JOptionPane.showMessageDialog(this, "Documentation copied to clipboard!",
 "Copy Successful", JOptionPane.INFORMATION_MESSAGE);
 }
 }

 private void saveToFile() {
 CollectionItem selectedItem = (CollectionItem) collectionCombo.getSelectedItem();
 DocumentationGenerator.Format selectedFormat = (DocumentationGenerator.Format) formatCombo.getSelectedItem();

 if (selectedItem == null || selectedFormat == null) return;

 JFileChooser fileChooser = new JFileChooser();
 String fileName = selectedItem.getCollection().getName().replaceAll("[^a-zA-Z0-9]", "_")
  + "_docs." + selectedFormat.getExtension();
 fileChooser.setSelectedFile(new File(fileName));

 if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
 try {
 File file = fileChooser.getSelectedFile();
 try (FileWriter writer = new FileWriter(file)) {
 writer.write(docArea.getText());
 }
 JOptionPane.showMessageDialog(this, "Documentation saved to: " + file.getAbsolutePath(),
 "Save Successful", JOptionPane.INFORMATION_MESSAGE);
 } catch (Exception e) {
 JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(),
 "Save Error", JOptionPane.ERROR_MESSAGE);
 }
 }
 }

 private void previewDocumentation() {
 DocumentationGenerator.Format selectedFormat = (DocumentationGenerator.Format) formatCombo.getSelectedItem();

 if (selectedFormat == DocumentationGenerator.Format.HTML) {
 try {
 // Create temporary HTML file
 File tempFile = File.createTempFile("api_docs", ".html");
 try (FileWriter writer = new FileWriter(tempFile)) {
 writer.write(docArea.getText());
 }
 
 // Open in default browser
 if (Desktop.isDesktopSupported()) {
 Desktop.getDesktop().browse(tempFile.toURI());
 } else {
 JOptionPane.showMessageDialog(this,
 "Desktop not supported. File saved to: " + tempFile.getAbsolutePath(),
 "Preview", JOptionPane.INFORMATION_MESSAGE);
 }

 } catch (Exception e) {
 JOptionPane.showMessageDialog(this, "Error creating preview: " + e.getMessage(),
 "Preview Error", JOptionPane.ERROR_MESSAGE);
 }
 } else {
 JOptionPane.showMessageDialog(this, "Preview is only available for HTML format",
 "Preview Not Available", JOptionPane.INFORMATION_MESSAGE);
 }
 }
}
