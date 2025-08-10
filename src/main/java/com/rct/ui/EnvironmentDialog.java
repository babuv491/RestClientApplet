package com.rct.ui;

import com.rct.manager.EnvironmentManager;
import com.rct.util.UITheme;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.Map;

public class EnvironmentDialog extends JDialog {
 private EnvironmentManager environmentManager;
 private JComboBox<String> environmentCombo;
 private JTable variablesTable;
 private DefaultTableModel tableModel;
 private JLabel statusLabel;

 public EnvironmentDialog(Frame parent, EnvironmentManager environmentManager) {
 super(parent, "🌍Environment Variables", true);
 this.environmentManager = environmentManager;
 initializeComponents();
 setupLayout();
 loadEnvironments();
 setSize(700, 500);
 setLocationRelativeTo(parent);
 }

 private void initializeComponents() {
 environmentCombo = new JComboBox<>();
 environmentCombo.addActionListener(e -> loadSelectedEnvironment());

 String[] columns = {"Variable Name", "Value"};
 tableModel = new DefaultTableModel(columns, 0) {
 @Override
 public boolean isCellEditable(int row, int column) { return true; }
 };

 variablesTable = new JTable(tableModel);
 variablesTable.setRowHeight(25);
 variablesTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
 @Override
 public Component getTableCellRendererComponent(JTable table, Object value,
 boolean isSelected, boolean hasFocus, int row, int column) {
 Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
 if (!isSelected) {
 c.setBackground(row % 2 == 0 ? new Color(248, 249, 250) : Color.WHITE);
 }
 return c;
 }
 });

 tableModel.addTableModelListener(e -> {
 if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
 saveCurrentEnvironment();
 }
 });

 statusLabel = new JLabel("Ready");
 statusLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
 statusLabel.setForeground(Color.GRAY);
 }

 private void setupLayout() {
 setLayout(new BorderLayout());

 // Header panel
 JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
 headerPanel.add(new JLabel("Environment:"));
 headerPanel.add(environmentCombo);

 JButton newEnvBtn = new JButton("New");
 newEnvBtn.addActionListener(e -> createNewEnvironment());
 JButton deleteEnvBtn = new JButton("Delete");
 deleteEnvBtn.addActionListener(e -> deleteEnvironment());

 headerPanel.add(newEnvBtn);
 headerPanel.add(deleteEnvBtn);

 // Variables panel
 JPanel variablesPanel = new JPanel(new BorderLayout());
 variablesPanel.setBorder(BorderFactory.createTitledBorder("Variables"));

 JScrollPane tableScrollPane = new JScrollPane(variablesTable);
 variablesPanel.add(tableScrollPane, BorderLayout.CENTER);

 // Variable buttons
 JPanel varButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
 JButton addVarBtn = new JButton("+ Add Variable");
 addVarBtn.addActionListener(e -> addEmptyRow());
 JButton removeVarBtn = new JButton("- Remove");
 removeVarBtn.addActionListener(e -> removeVariable());

 varButtonPanel.add(addVarBtn);
 varButtonPanel.add(removeVarBtn);
 variablesPanel.add(varButtonPanel, BorderLayout.SOUTH);

 // Bottom panel
 JPanel bottomPanel = new JPanel(new BorderLayout());
 bottomPanel.add(statusLabel, BorderLayout.WEST);

 JButton closeBtn = new JButton("Close");
 closeBtn.addActionListener(e -> dispose());
 JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
 closePanel.add(closeBtn);
 bottomPanel.add(closePanel, BorderLayout.EAST);

 add(headerPanel, BorderLayout.NORTH);
 add(variablesPanel, BorderLayout.CENTER);
 add(bottomPanel, BorderLayout.SOUTH);
 }

 private void loadEnvironments() {
 environmentCombo.removeAllItems();
 for (String envName : environmentManager.getEnvironmentNames()) {
 environmentCombo.addItem(envName);
 }

 EnvironmentManager.Environment activeEnv = environmentManager.getActiveEnvironment();
 if (activeEnv != null) {
 environmentCombo.setSelectedItem(activeEnv.getName());
 }

 loadSelectedEnvironment();
 }

 private void loadSelectedEnvironment() {
 String selectedEnv = (String) environmentCombo.getSelectedItem();
 if (selectedEnv != null) {
 EnvironmentManager.Environment env = environmentManager.getEnvironment(selectedEnv);
  if (env != null) {
 tableModel.setRowCount(0);
 for (Map.Entry<String, String> entry : env.getVariables().entrySet()) {
 tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
 }
 environmentManager.setActiveEnvironment(selectedEnv);
 updateStatus("Loaded environment: " + selectedEnv);
 }
 }
 }

 private void saveCurrentEnvironment() {
 String selectedEnv = (String) environmentCombo.getSelectedItem();
 if (selectedEnv != null) {
 EnvironmentManager.Environment env = environmentManager.getEnvironment(selectedEnv);
 if (env != null) {
 env.getVariables().clear();
 for (int i = 0; i < tableModel.getRowCount(); i++) {
 String key = (String) tableModel.getValueAt(i, 0);
 String value = (String) tableModel.getValueAt(i, 1);
 if (key != null && !key.trim().isEmpty()) {
 env.addVariable(key.trim(), value != null ? value.trim() : "");
 }
 }
 updateStatus("Saved changes to " + selectedEnv);
 }
 }
 }

 private void addVariable() {
 String varName = JOptionPane.showInputDialog(this, "Variable Name:", "Add Variable", JOptionPane.PLAIN_MESSAGE);
 if (varName != null && !varName.trim().isEmpty()) {
 String varValue = JOptionPane.showInputDialog(this, "Variable Value:", "Add Variable", JOptionPane.PLAIN_MESSAGE);
 tableModel.addRow(new Object[]{varName.trim(), varValue != null ? varValue.trim() : ""});
 saveCurrentEnvironment();
 }
 }

 private void addEmptyRow() {
 tableModel.addRow(new Object[]{"", ""});
 int newRow = tableModel.getRowCount() - 1;
 variablesTable.setRowSelectionInterval(newRow, newRow);
 variablesTable.editCellAt(newRow, 0);
 if (variablesTable.getEditorComponent() != null) {
 variablesTable.getEditorComponent().requestFocus();
 }
 }

 private void removeVariable() {
 int selectedRow = variablesTable.getSelectedRow();
 if (selectedRow >= 0) {
 tableModel.removeRow(selectedRow);
 saveCurrentEnvironment();
 } else {
 JOptionPane.showMessageDialog(this, "Please select a variable to remove.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
 }
 }

 private void createNewEnvironment() {
 String envName = JOptionPane.showInputDialog(this, "Environment Name:", "New Environment", JOptionPane.PLAIN_MESSAGE);
 if (envName != null && !envName.trim().isEmpty()) {
 environmentManager.addEnvironment(envName.trim());
 loadEnvironments();
 environmentCombo.setSelectedItem(envName.trim());
 updateStatus("Created new environment: " + envName.trim());
 }
 }

 private void deleteEnvironment() {
 String selectedEnv = (String) environmentCombo.getSelectedItem();
 if (selectedEnv != null) {
 if (environmentManager.getAllEnvironments().size() <= 1) {
 JOptionPane.showMessageDialog(this, "Cannot delete the last environment.", "Cannot Delete", JOptionPane.WARNING_MESSAGE);
 return;
 }

 int result = JOptionPane.showConfirmDialog(this, "Delete environment '" + selectedEnv + "'?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
 if (result == JOptionPane.YES_OPTION) {
 environmentManager.removeEnvironment(selectedEnv);
 loadEnvironments();
 updateStatus("Deleted environment: " + selectedEnv);
 }
 }
 }

 private void updateStatus(String message) {
 statusLabel.setText(message);
 Timer timer = new Timer(3000, e -> statusLabel.setText("Ready"));
 timer.setRepeats(false);
 timer.start();
 }
}

