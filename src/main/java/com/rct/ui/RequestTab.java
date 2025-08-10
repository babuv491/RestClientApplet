package com.rct.ui;

import com.rct.manager.CollectionManager;
import com.rct.manager.EnvironmentManager;
import com.rct.manager.HistoryManager;
import com.rct.model.RestResponse;
import com.rct.service.RestClientService;
import com.rct.util.JsonFormatter;
import com.rct.util.LogManager;
import net.miginfocom.swing.MigLayout;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

public class RequestTab {
 private String name;
 private JPanel mainPanel;
 private JComboBox<String> methodCombo;
 private JTextField urlField;
 private JTable headersTable;
 private JTable paramsTable;
 private RSyntaxTextArea bodyArea;
 private JTabbedPane requestTabs;
 private JTabbedPane responsePanel;
 private RSyntaxTextArea responseArea;
 private JLabel statusLabel;
 private JLabel timeLabel;
 private JTable responseHeadersTable;
 private JPanel statusTimeInfoPanel;
 private RestClientService restService;
 private HistoryManager historyManager;
 private CollectionManager collectionManager;
 private EnvironmentManager environmentManager;
 private TabUpdateCallback tabUpdateCallback;
 private TabStatusIndicator statusIndicator;
 private boolean isModified = false;
 private String savedCollectionName = null;
 private String savedRequestName = null;

 private JComboBox<String> authTypeCombo;
 private JTextField authUsernameField;
 private JPasswordField authPasswordField;
 private JTextField authTokenField;
 private JTextField authKeyField;
 private JTextField authValueField;
 private JTextField oauthTokenUrlField;
 private JTextField oauthClientIdField;
 private JTextField oauthClientSecretField;
 private JTextField oauthScopeField;
 private JPanel authFieldsPanel;

 public interface TabUpdateCallback {
 void updateTabTitle(RequestTab tab);
 }

 public RequestTab(String name, HistoryManager historyManager, CollectionManager collectionManager, EnvironmentManager environmentManager, TabUpdateCallback callback) {
 this.name = name;
 this.historyManager = historyManager;
 this.collectionManager = collectionManager;
 this.environmentManager = environmentManager;
 this.tabUpdateCallback = callback;
  this.restService = new RestClientService(environmentManager);
 this.statusIndicator = new TabStatusIndicator();
 initializeComponents();
 setupChangeListeners();
 }

 private void initializeComponents() {
 mainPanel = new JPanel(new BorderLayout());

 JPanel urlPanel = createUrlPanel();
 mainPanel.add(urlPanel, BorderLayout.NORTH);

 JPanel requestSection = createRequestSection();
 JPanel responseSection = createResponseSection();

 JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, requestSection, responseSection);
 splitPane.setResizeWeight(0.5);
 splitPane.setDividerLocation(0.5);
 splitPane.setDividerSize(8);
 splitPane.setBorder(null);
 splitPane.setOpaque(false);

 mainPanel.add(splitPane, BorderLayout.CENTER);
 }

 private JPanel createRequestSection() {
 JPanel panel = new JPanel(new BorderLayout());
 panel.setBorder(new EmptyBorder(5, 10, 5, 15));

 JLabel requestTitle = new JLabel("📝Request");
 requestTitle.setFont(requestTitle.getFont().deriveFont(Font.BOLD, 16f));
 requestTitle.setBorder(new EmptyBorder(0, 0, 10, 0));
 requestTitle.setForeground(com.rct.util.UITheme.TEXT_PRIMARY);
 panel.add(requestTitle, BorderLayout.NORTH);

 requestTabs = new JTabbedPane();
 requestTabs.addTab("<html><font color='" + com.rct.util.UITheme.ICON_POST + "'>📋</font> Params</html>", createParamsPanel());
 requestTabs.addTab("<html><font color='" + com.rct.util.UITheme.ICON_PATCH + "'>📄</font> Headers</html>", createHeadersPanel());
 requestTabs.addTab("<html><font color='" + com.rct.util.UITheme.ICON_PUT + "'>📝</font> Body</html>", createBodyPanel());
 requestTabs.addTab("<html><font color='" + com.rct.util.UITheme.ICON_DELETE + "'>🔐</font> Auth</html>", createAuthPanel());

 panel.add(requestTabs, BorderLayout.CENTER);

 return panel;
 }

 private JPanel createUrlPanel() {
 JPanel panel = new JPanel(new MigLayout("fillx", "[100][grow][100]", "[]"));
 panel.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);
 panel.setBorder(new EmptyBorder(10, 10, 10, 10));

 String[] methods = {"GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"};
 methodCombo = new JComboBox<>(methods) {
 @Override
 public void setSelectedItem(Object item) {
 super.setSelectedItem(item);
 String method = (String) item;
 switch (method) {
 case "GET": setBackground(com.rct.util.UITheme.BUTTON_SUCCESS); break;
 case "POST": setBackground(com.rct.util.UITheme.PRIMARY_BLUE); break;
 case "PUT": setBackground(com.rct.util.UITheme.PRIMARY_ORANGE); break;
 case "DELETE": setBackground(com.rct.util.UITheme.BUTTON_DANGER); break;
 default: setBackground(com.rct.util.UITheme.PRIMARY_PURPLE); break;
 }
 setForeground(Color.WHITE);
 }
 };
 methodCombo.setPreferredSize(new Dimension(100, 38));
 methodCombo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
 methodCombo.setFocusable(false);
 methodCombo.setSelectedItem("GET");

 urlField = new JTextField();
 urlField.setPreferredSize(new Dimension(0, 38));
 urlField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
 urlField.setBorder(BorderFactory.createCompoundBorder(
 BorderFactory.createLineBorder(com.rct.util.UITheme.TABLE_BORDER, 1),
 BorderFactory.createEmptyBorder(8, 12, 8, 12)));
 urlField.addKeyListener(new KeyAdapter() {
 @Override
 public void keyPressed(KeyEvent e) {
 if (e.getKeyCode() == KeyEvent.VK_ENTER) {
 sendRequest();
 }
 }
 });

 urlField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
 @Override
 public void insertUpdate(javax.swing.event.DocumentEvent e) {
 syncParamsFromUrl();
 markAsModified();
 updateVariableHighlighting();
 }
 @Override
 public void removeUpdate(javax.swing.event.DocumentEvent e) {
 syncParamsFromUrl();
 markAsModified();
 updateVariableHighlighting();
 }
 @Override
 public void changedUpdate(javax.swing.event.DocumentEvent e) {
 syncParamsFromUrl();
 markAsModified();
 updateVariableHighlighting();
 }
 });

 JButton sendButton = new JButton("🚀Send");
 sendButton.setPreferredSize(new Dimension(90, 38));
 sendButton.setBackground(com.rct.util.UITheme.PRIMARY_BLUE);
 sendButton.setForeground(Color.WHITE);
 sendButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
 sendButton.setFocusPainted(false);
 sendButton.setBorderPainted(false);
 sendButton.addActionListener(e -> sendRequest());

 JButton saveButton = new JButton("💾");
 saveButton.setPreferredSize(new Dimension(38, 38));
 saveButton.setBackground(com.rct.util.UITheme.BUTTON_SUCCESS);
 saveButton.setForeground(Color.WHITE);
 saveButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
 saveButton.setToolTipText("Save to Collection");
 saveButton.setFocusPainted(false);
 saveButton.setBorderPainted(false);
 saveButton.addActionListener(e -> saveToCollection());

 panel.add(methodCombo, "cell 0 0");
 panel.add(urlField, "cell 1 0, growx");
 panel.add(sendButton, "cell 2 0, split 2");
 panel.add(saveButton, "cell 2 0");

 return panel;
 }

 private JPanel createParamsPanel() {
 JPanel panel = new JPanel(new BorderLayout());
 panel.setBorder(new EmptyBorder(10, 10, 10, 10));
 panel.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);

 String[] columns = {"Key", "Value", "Description"};
 DefaultTableModel model = new DefaultTableModel(columns, 0) {
 @Override
 public boolean isCellEditable(int row, int column) { return true; }
 };

 paramsTable = new JTable(model) {
 @Override
 public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
 Component c = super.prepareRenderer(renderer, row, column);
 if (!isRowSelected(row)) {
 c.setBackground(row % 2 == 0 ? com.rct.util.UITheme.TABLE_ROW_EVEN : com.rct.util.UITheme.TABLE_ROW_ODD);
 c.setForeground(com.rct.util.UITheme.TEXT_PRIMARY);
 } else {
 c.setBackground(new Color(74, 144, 226));
 c.setForeground(Color.WHITE);
 }
 return c;
 }
 };

 paramsTable.setRowHeight(32);
 paramsTable.setShowGrid(true);
  paramsTable.setGridColor(com.rct.util.UITheme.TABLE_BORDER);
 paramsTable.setSelectionBackground(new Color(74, 144, 226));
 paramsTable.setSelectionForeground(Color.WHITE);
 paramsTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
 paramsTable.getTableHeader().setBackground(com.rct.util.UITheme.TABLE_HEADER);
 paramsTable.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
 paramsTable.getTableHeader().setForeground(com.rct.util.UITheme.TEXT_PRIMARY);

 model.addTableModelListener(e -> syncUrlFromParams());

 JScrollPane scrollPane = new JScrollPane(paramsTable);
 scrollPane.setBorder(BorderFactory.createLineBorder(com.rct.util.UITheme.TABLE_BORDER));
 scrollPane.getViewport().setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);

 JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
 buttonPanel.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);

 JButton addButton = new JButton("<html><font color='" + com.rct.util.UITheme.ICON_GET + "'>➕</font> Add Parameter</html>");
 addButton.setBackground(com.rct.util.UITheme.BUTTON_SUCCESS);
 addButton.setForeground(Color.WHITE);
 addButton.setFocusPainted(false);
 addButton.setBorderPainted(false);
 addButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));

 JButton removeButton = new JButton("🗑️Remove");
 removeButton.setBackground(com.rct.util.UITheme.BUTTON_DANGER);
 removeButton.setForeground(Color.WHITE);
 removeButton.setFocusPainted(false);
 removeButton.setBorderPainted(false);
 removeButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));

 addButton.addActionListener(e -> {
 model.addRow(new Object[]{"", "", ""});
 int newRow = model.getRowCount() - 1;
 paramsTable.setRowSelectionInterval(newRow, newRow);
 paramsTable.editCellAt(newRow, 0);
 if (paramsTable.getEditorComponent() != null) {
 paramsTable.getEditorComponent().requestFocus();
 }
 });

 removeButton.addActionListener(e -> {
 int selectedRow = paramsTable.getSelectedRow();
 if (selectedRow >= 0) model.removeRow(selectedRow);
 });

 buttonPanel.add(addButton);
 buttonPanel.add(removeButton);

 panel.add(scrollPane, BorderLayout.CENTER);
 panel.add(buttonPanel, BorderLayout.SOUTH);

 return panel;
 }

 private JPanel createHeadersPanel() {
 JPanel panel = new JPanel(new BorderLayout());
 panel.setBorder(new EmptyBorder(10, 10, 10, 10));
 panel.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);

 String[] columns = {"Key", "Value", "Description"};
 DefaultTableModel model = new DefaultTableModel(columns, 0) {
 @Override
 public boolean isCellEditable(int row, int column) { return true; }
 };

 headersTable = new JTable(model) {
 @Override
 public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
 Component c = super.prepareRenderer(renderer, row, column);
 if (!isRowSelected(row)) {
 c.setBackground(row % 2 == 0 ? com.rct.util.UITheme.TABLE_ROW_EVEN : com.rct.util.UITheme.TABLE_ROW_ODD);
 c.setForeground(com.rct.util.UITheme.TEXT_PRIMARY);
 } else {
 c.setBackground(new Color(74, 144, 226));
 c.setForeground(Color.WHITE);
 }
 return c;
  }
 };

 headersTable.setRowHeight(32);
 headersTable.setShowGrid(true);
 headersTable.setGridColor(com.rct.util.UITheme.TABLE_BORDER);
 headersTable.setSelectionBackground(new Color(74, 144, 226));
 headersTable.setSelectionForeground(Color.WHITE);
 headersTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
 headersTable.getTableHeader().setBackground(com.rct.util.UITheme.TABLE_HEADER);
 headersTable.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
 headersTable.getTableHeader().setForeground(com.rct.util.UITheme.TEXT_PRIMARY);

 JScrollPane scrollPane = new JScrollPane(headersTable);
 scrollPane.setBorder(BorderFactory.createLineBorder(com.rct.util.UITheme.TABLE_BORDER));
 scrollPane.getViewport().setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);

 JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
 buttonPanel.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);

 JButton addButton = new JButton("<html><font color='" + com.rct.util.UITheme.ICON_GET + "'>➕</font> Add Header</html>");
 addButton.setBackground(com.rct.util.UITheme.BUTTON_SUCCESS);
 addButton.setForeground(Color.WHITE);
 addButton.setFocusPainted(false);
 addButton.setBorderPainted(false);
 addButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));

 JButton removeButton = new JButton("🗑️Remove");
 removeButton.setBackground(com.rct.util.UITheme.BUTTON_DANGER);
 removeButton.setForeground(Color.WHITE);
 removeButton.setFocusPainted(false);
 removeButton.setBorderPainted(false);
 removeButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));

 addButton.addActionListener(e -> {
 model.addRow(new Object[]{"", "", ""});
 int newRow = model.getRowCount() - 1;
 headersTable.setRowSelectionInterval(newRow, newRow);
 headersTable.editCellAt(newRow, 0);
 if (headersTable.getEditorComponent() != null) {
 headersTable.getEditorComponent().requestFocus();
 }
 });

 removeButton.addActionListener(e -> {
 int selectedRow = headersTable.getSelectedRow();
 if (selectedRow >= 0) model.removeRow(selectedRow);
 });

 buttonPanel.add(addButton);
 buttonPanel.add(removeButton);

 panel.add(scrollPane, BorderLayout.CENTER);
 panel.add(buttonPanel, BorderLayout.SOUTH);

 return panel;
 }

 private JPanel createBodyPanel() {
 JPanel panel = new JPanel(new BorderLayout());
 panel.setBorder(new EmptyBorder(10, 10, 10, 10));
 panel.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);

 JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
 typePanel.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);

 JLabel typeLabel = new JLabel("📝Body Type:");
 typeLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
 typeLabel.setForeground(com.rct.util.UITheme.TEXT_PRIMARY);

 JRadioButton jsonRadio = new JRadioButton("JSON", true);
 JRadioButton xmlRadio = new JRadioButton("XML");
 JRadioButton textRadio = new JRadioButton("Text");

 jsonRadio.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);
 xmlRadio.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);
 textRadio.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);

 ButtonGroup group = new ButtonGroup();
 group.add(jsonRadio);
 group.add(xmlRadio);
 group.add(textRadio);

 typePanel.add(typeLabel);
 typePanel.add(jsonRadio);
 typePanel.add(xmlRadio);
 typePanel.add(textRadio);

 bodyArea = new RSyntaxTextArea(20, 60);
 bodyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
 bodyArea.setCodeFoldingEnabled(true);
 bodyArea.setBackground(com.rct.util.UITheme.BACKGROUND_LIGHT);
 bodyArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
 RTextScrollPane scrollPane = new RTextScrollPane(bodyArea);

 JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
 buttonPanel.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);

 JButton formatButton = new JButton("🎨Format");
 formatButton.setBackground(com.rct.util.UITheme.BUTTON_INFO);
 formatButton.setForeground(Color.WHITE);
 formatButton.setFocusPainted(false);
 formatButton.setBorderPainted(false);
 formatButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));

 JButton minifyButton = new JButton("📦Minify");
 minifyButton.setBackground(com.rct.util.UITheme.BUTTON_WARNING);
 minifyButton.setForeground(Color.WHITE);
 minifyButton.setFocusPainted(false);
 minifyButton.setBorderPainted(false);
 minifyButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));

 JButton validateButton = new JButton("✅Validate");
 validateButton.setBackground(com.rct.util.UITheme.BUTTON_SUCCESS);
 validateButton.setForeground(Color.WHITE);
 validateButton.setFocusPainted(false);
 validateButton.setBorderPainted(false);
 validateButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));

 formatButton.addActionListener(e -> formatJsonInternal());
 minifyButton.addActionListener(e -> minifyJsonInternal());
 validateButton.addActionListener(e -> validateJsonInternal());

 buttonPanel.add(formatButton);
 buttonPanel.add(minifyButton);
 buttonPanel.add(validateButton);

 panel.add(typePanel, BorderLayout.NORTH);
 panel.add(scrollPane, BorderLayout.CENTER);
 panel.add(buttonPanel, BorderLayout.SOUTH);

 return panel;
 }

 private JPanel createAuthPanel() {
 JPanel panel = new JPanel(new BorderLayout());
 panel.setBorder(new EmptyBorder(10, 10, 10, 10));
 panel.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);

 JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
 typePanel.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);

 JLabel authLabel = new JLabel("🔐Auth Type:");
 authLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
 authLabel.setForeground(com.rct.util.UITheme.TEXT_PRIMARY);

 authTypeCombo = new JComboBox<>(new String[]{"No Auth", "Basic Auth", "Bearer Token", "API Key", "OAuth 2.0"});
 authTypeCombo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
 authTypeCombo.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);

 typePanel.add(authLabel);
 typePanel.add(authTypeCombo);

 authFieldsPanel = new JPanel(new MigLayout("fillx", "[][grow]", "[][][][]"));
 authFieldsPanel.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);

 authUsernameField = new JTextField();
 authPasswordField = new JPasswordField();
 authTokenField = new JTextField();
 authKeyField = new JTextField();
 authValueField = new JTextField();
 oauthTokenUrlField = new JTextField();
 oauthClientIdField = new JTextField();
 oauthClientSecretField = new JTextField();
 oauthScopeField = new JTextField();

 // Style text fields
 JTextField[] fields = {authUsernameField, authTokenField, authKeyField, authValueField,
 oauthTokenUrlField, oauthClientIdField, oauthClientSecretField, oauthScopeField};
 for (JTextField field : fields) {
 field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
 field.setBorder(BorderFactory.createCompoundBorder(
 BorderFactory.createLineBorder(com.rct.util.UITheme.TABLE_BORDER, 1),
 BorderFactory.createEmptyBorder(5, 8, 5, 8)));
 }
 authPasswordField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
 authPasswordField.setBorder(BorderFactory.createCompoundBorder(
 BorderFactory.createLineBorder(com.rct.util.UITheme.TABLE_BORDER, 1),
 BorderFactory.createEmptyBorder(5, 8, 5, 8)));

 authTypeCombo.addActionListener(e -> updateAuthFields());

 panel.add(typePanel, BorderLayout.NORTH);
 panel.add(authFieldsPanel, BorderLayout.CENTER);

 updateAuthFields();

 return panel;
 }

 private void updateAuthFields() {
 authFieldsPanel.removeAll();

 String authType = (String) authTypeCombo.getSelectedItem();
 switch (authType) {
 case "Basic Auth":
 JLabel userLabel = new JLabel("<html><font color='" + com.rct.util.UITheme.ICON_POST + "'>👤</font> Username:</html>");
 userLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
 userLabel.setForeground(com.rct.util.UITheme.TEXT_PRIMARY);

 JLabel passLabel = new JLabel("<html><font color='" + com.rct.util.UITheme.ICON_DELETE + "'>🔑</font> Password:</html>");
 passLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
 passLabel.setForeground(com.rct.util.UITheme.TEXT_PRIMARY);

 authFieldsPanel.add(userLabel, "cell 0 0");
 authFieldsPanel.add(authUsernameField, "cell 1 0, growx");
 authFieldsPanel.add(passLabel, "cell 0 1");
 authFieldsPanel.add(authPasswordField, "cell 1 1, growx");
 break;
 case "Bearer Token":
 JLabel tokenLabel = new JLabel("<html><font color='" + com.rct.util.UITheme.ICON_PATCH + "'>🎫</font> Token:</html>");
 tokenLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
 tokenLabel.setForeground(com.rct.util.UITheme.TEXT_PRIMARY);

 authFieldsPanel.add(tokenLabel, "cell 0 0");
 authFieldsPanel.add(authTokenField, "cell 1 0, growx");
 break;
 case "API Key":
 JLabel keyLabel = new JLabel("<html><font color='" + com.rct.util.UITheme.ICON_DELETE + "'>🔑</font> Key:</html>");
 keyLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
 keyLabel.setForeground(com.rct.util.UITheme.TEXT_PRIMARY);

 JLabel valueLabel = new JLabel("<html><font color='" + com.rct.util.UITheme.ICON_PUT + "'>📝</font> Value:</html>");
 valueLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
 valueLabel.setForeground(com.rct.util.UITheme.TEXT_PRIMARY);

 authFieldsPanel.add(keyLabel, "cell 0 0");
 authFieldsPanel.add(authKeyField, "cell 1 0, growx");
 authFieldsPanel.add(valueLabel, "cell 0 1");
 authFieldsPanel.add(authValueField, "cell 1 1, growx");
 break;
 case "OAuth 2.0":
 JLabel tokenUrlLabel = new JLabel("<html><font color='" + com.rct.util.UITheme.ICON_POST + "'>🌐</font> Token URL:</html>");
 tokenUrlLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
 tokenUrlLabel.setForeground(com.rct.util.UITheme.TEXT_PRIMARY);

 JLabel clientIdLabel = new JLabel("<html><font color='" + com.rct.util.UITheme.ICON_PATCH + "'>🆔</font> Client ID:</html>");
 clientIdLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
 clientIdLabel.setForeground(com.rct.util.UITheme.TEXT_PRIMARY);

 JLabel clientSecretLabel = new JLabel("<html><font color='" + com.rct.util.UITheme.ICON_DELETE + "'>🔐</font> Client Secret:</html>");
 clientSecretLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
 clientSecretLabel.setForeground(com.rct.util.UITheme.TEXT_PRIMARY);

 JLabel scopeLabel = new JLabel("<html><font color='" + com.rct.util.UITheme.ICON_GET + "'>📋</font> Scope:</html>");
 scopeLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
 scopeLabel.setForeground(com.rct.util.UITheme.TEXT_PRIMARY);

 authFieldsPanel.add(tokenUrlLabel, "cell 0 0");
 authFieldsPanel.add(oauthTokenUrlField, "cell 1 0, growx");
 authFieldsPanel.add(clientIdLabel, "cell 0 1");
 authFieldsPanel.add(oauthClientIdField, "cell 1 1, growx");
  authFieldsPanel.add(clientSecretLabel, "cell 0 2");
 authFieldsPanel.add(oauthClientSecretField, "cell 1 2, growx");
 authFieldsPanel.add(scopeLabel, "cell 0 3");
 authFieldsPanel.add(oauthScopeField, "cell 1 3, growx");
 break;
 default:
 JLabel noAuthLabel = new JLabel("<html><font color='" + com.rct.util.UITheme.ICON_GET + "'>✅</font> No authentication required</html>");
 noAuthLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
 noAuthLabel.setForeground(com.rct.util.UITheme.TEXT_SUCCESS);
 authFieldsPanel.add(noAuthLabel, "cell 0 0");
 break;
 }

 authFieldsPanel.revalidate();
 authFieldsPanel.repaint();
 }

 private JPanel createResponseSection() {
 JPanel panel = new JPanel(new BorderLayout());
 panel.setBorder(new EmptyBorder(5, 15, 5, 10));
 panel.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);

 JPanel responseHeader = new JPanel(new BorderLayout());
 responseHeader.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);
 responseHeader.setBorder(new EmptyBorder(0, 0, 10, 0));

 JLabel responseTitle = new JLabel("📊Response");
 responseTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
 responseTitle.setForeground(com.rct.util.UITheme.TEXT_PRIMARY);

 JPanel statusTimePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
 statusTimePanel.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);

 statusLabel = new JLabel("Status: Ready");
 statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
 statusLabel.setForeground(com.rct.util.UITheme.TEXT_SECONDARY);

 timeLabel = new JLabel("<html><font color='" + com.rct.util.UITheme.ICON_HISTORY + "'>⏱</font> Time: -</html>");
 timeLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
 timeLabel.setForeground(com.rct.util.UITheme.TEXT_SECONDARY);

 statusTimePanel.add(statusLabel);
 statusTimePanel.add(timeLabel);

 responseHeader.add(responseTitle, BorderLayout.WEST);
 responseHeader.add(statusTimePanel, BorderLayout.EAST);
 panel.add(responseHeader, BorderLayout.NORTH);

 responsePanel = new JTabbedPane();
 responsePanel.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);

  // Add status and time panel above the tabs
 JPanel responseWithStatus = new JPanel(new BorderLayout());
 responseWithStatus.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);

 statusTimeInfoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
 statusTimeInfoPanel.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);
 statusTimeInfoPanel.setBorder(new EmptyBorder(0, 0, 5, 0));
 statusTimeInfoPanel.add(statusLabel);
 statusTimeInfoPanel.add(timeLabel);

 responseWithStatus.add(statusTimeInfoPanel, BorderLayout.NORTH);
 responseWithStatus.add(responsePanel, BorderLayout.CENTER);

 responseArea = new RSyntaxTextArea(20, 60);
 responseArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
 responseArea.setEditable(false);
 responseArea.setCodeFoldingEnabled(true);
 responseArea.setBackground(com.rct.util.UITheme.BACKGROUND_LIGHT);
 RTextScrollPane responseScrollPane = new RTextScrollPane(responseArea);

 JPanel responseBodyPanel = new JPanel(new BorderLayout());
 responseBodyPanel.add(responseScrollPane, BorderLayout.CENTER);

 JPanel responseButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
 responseButtonPanel.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);

 JButton formatResponseBtn = new JButton("🎨Format");
 formatResponseBtn.setBackground(com.rct.util.UITheme.BUTTON_INFO);
 formatResponseBtn.setForeground(Color.WHITE);
 formatResponseBtn.setFocusPainted(false);
 formatResponseBtn.setBorderPainted(false);
 formatResponseBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
 formatResponseBtn.addActionListener(e -> formatResponseJson());

 JButton minifyResponseBtn = new JButton("📦Minify");
 minifyResponseBtn.setBackground(com.rct.util.UITheme.BUTTON_WARNING);
 minifyResponseBtn.setForeground(Color.WHITE);
 minifyResponseBtn.setFocusPainted(false);
 minifyResponseBtn.setBorderPainted(false);
 minifyResponseBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
 minifyResponseBtn.addActionListener(e -> minifyResponseJson());

 JButton validateResponseBtn = new JButton("✅Validate");
 validateResponseBtn.setBackground(com.rct.util.UITheme.BUTTON_SUCCESS);
 validateResponseBtn.setForeground(Color.WHITE);
 validateResponseBtn.setFocusPainted(false);
 validateResponseBtn.setBorderPainted(false);
 validateResponseBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
 validateResponseBtn.addActionListener(e -> validateResponseJson());

 JButton copyResponseBtn = new JButton("📋Copy");
 copyResponseBtn.setBackground(com.rct.util.UITheme.PRIMARY_BLUE);
 copyResponseBtn.setForeground(Color.WHITE);
 copyResponseBtn.setFocusPainted(false);
 copyResponseBtn.setBorderPainted(false);
 copyResponseBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
 copyResponseBtn.addActionListener(e -> copyResponseToClipboard());

 responseButtonPanel.add(formatResponseBtn);
 responseButtonPanel.add(minifyResponseBtn);
 responseButtonPanel.add(validateResponseBtn);
 responseButtonPanel.add(copyResponseBtn);

 responseBodyPanel.add(responseButtonPanel, BorderLayout.SOUTH);

 String[] headerColumns = {"Header", "Value"};
 DefaultTableModel headerModel = new DefaultTableModel(headerColumns, 0) {
 @Override
 public boolean isCellEditable(int row, int column) { return false; }
 };
 responseHeadersTable = new JTable(headerModel) {
 @Override
 public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
 Component c = super.prepareRenderer(renderer, row, column);
 if (!isRowSelected(row)) {
 c.setBackground(row % 2 == 0 ? com.rct.util.UITheme.TABLE_ROW_EVEN : com.rct.util.UITheme.TABLE_ROW_ODD);
 c.setForeground(com.rct.util.UITheme.TEXT_PRIMARY);
 } else {
  c.setBackground(new Color(74, 144, 226));
 c.setForeground(Color.WHITE);
 }
 return c;
 }
 };
 responseHeadersTable.setSelectionBackground(new Color(74, 144, 226));
 responseHeadersTable.setSelectionForeground(Color.WHITE);
 responseHeadersTable.setRowHeight(28);
 responseHeadersTable.getTableHeader().setBackground(com.rct.util.UITheme.TABLE_HEADER);
 JScrollPane headerScrollPane = new JScrollPane(responseHeadersTable);

 responsePanel.addTab("<html><font color='" + com.rct.util.UITheme.ICON_GET + "'>📄</font> Response</html>", responseBodyPanel);
 responsePanel.addTab("<html><font color='" + com.rct.util.UITheme.ICON_PATCH + "'>📋</font> Headers</html>", headerScrollPane);

 panel.add(responseWithStatus, BorderLayout.CENTER);

 JPanel consolePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
 consolePanel.setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);
 consolePanel.setBorder(new EmptyBorder(10, 0, 0, 0));

 JButton consoleBtn = new JButton("🖥️Console Logs");
 consoleBtn.setBackground(com.rct.util.UITheme.BACKGROUND_DARK);
 consoleBtn.setForeground(Color.WHITE);
 consoleBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
 consoleBtn.setFocusPainted(false);
 consoleBtn.setBorderPainted(false);
 consoleBtn.addActionListener(e -> LogManager.getInstance().showConsole(mainPanel));

 consolePanel.add(consoleBtn);
 panel.add(consolePanel, BorderLayout.SOUTH);

 return panel;
 }

 public void sendRequest() {
 LogManager logger = LogManager.getInstance();
 logger.log("======================================");
 logger.log("=== NEW REQUEST INITIATED ===");
 logger.log("======================================");

 try {
 String method = (String) methodCombo.getSelectedItem();
 String url = urlField.getText().trim();

 if (url.isEmpty()) {
 logger.log("ERROR: URL is empty");
 JOptionPane.showMessageDialog(mainPanel, "Please enter a URL", "Error", JOptionPane.ERROR_MESSAGE);
 return;
 }

 String authType = (String) authTypeCombo.getSelectedItem();
 logger.log("Authentication Type: " + authType);

 Map<String, String> headers = new HashMap<>();
 DefaultTableModel headerModel = (DefaultTableModel) headersTable.getModel();
 for (int i = 0; i < headerModel.getRowCount(); i++) {
 String key = (String) headerModel.getValueAt(i, 0);
 String value = (String) headerModel.getValueAt(i, 1);
 if (key != null && !key.trim().isEmpty() && value != null) {
 headers.put(key.trim(), value.trim());
 }
 }
 
 addAuthHeaders(headers);

 Map<String, String> params = new HashMap<>();
 DefaultTableModel paramModel = (DefaultTableModel) paramsTable.getModel();
 for (int i = 0; i < paramModel.getRowCount(); i++) {
 String key = (String) paramModel.getValueAt(i, 0);
 String value = (String) paramModel.getValueAt(i, 1);
 if (key != null && !key.trim().isEmpty() && value != null) {
 params.put(key.trim(), value.trim());
 }
 }

 String body = bodyArea.getText();

 statusLabel.setText("Status: Sending...");
 timeLabel.setText("Time: -");
 statusIndicator.setStatus(TabStatusIndicator.Status.SENDING);

 SwingWorker<RestResponse, Void> worker = new SwingWorker<RestResponse, Void>() {
 private boolean historyAdded = false;

 @Override
 protected RestResponse doInBackground() throws Exception {
 long startTime = System.currentTimeMillis();
 RestResponse response = restService.sendRequest(method, url, headers, params, body);
  response.setResponseTime(System.currentTimeMillis() - startTime);
 return response;
 }

 @Override
 protected void done() {
 try {
 RestResponse response = get();
 updateResponse(response);

 // Update status indicator based on response
 if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
 statusIndicator.setStatus(TabStatusIndicator.Status.SUCCESS);
 } else {
 statusIndicator.setStatus(TabStatusIndicator.Status.ERROR);
 }

 // Add to history only once
 if (historyManager != null && !historyAdded) {
 historyManager.addHistoryEntry(method, url, response.getStatusCode(), response.getResponseTime(), headers, body);
 historyAdded = true;
 }
 logger.log("=== REQUEST CYCLE COMPLETED ===");
 } catch (Exception e) {
 logger.log("ERROR: " + e.getMessage());
 statusLabel.setText("Status: Error - " + e.getMessage());
 responseArea.setText("Error: " + e.getMessage());
 statusIndicator.setStatus(TabStatusIndicator.Status.ERROR);
 }
 }
 };

 worker.execute();

 } catch (Exception e) {
 logger.log("FATAL ERROR: " + e.getMessage());
 statusIndicator.setStatus(TabStatusIndicator.Status.ERROR);
 JOptionPane.showMessageDialog(mainPanel, "Error sending request: " + e.getMessage(),
 "Error", JOptionPane.ERROR_MESSAGE);
 }
 }

 private void updateResponse(RestResponse response) {
 int statusCode = response.getStatusCode();
 String statusText = statusCode + " " + response.getStatusText();
 String timeText = "<html><font color='" + com.rct.util.UITheme.ICON_HISTORY + "'>⏱</font> Time: " + response.getResponseTime() + "ms</html>";

 statusLabel.setText(statusText);
 timeLabel.setText(timeText);

 // Color code status based on HTTP status
 if (statusCode >= 200 && statusCode < 300) {
 statusLabel.setForeground(com.rct.util.UITheme.TEXT_SUCCESS);
 } else if (statusCode >= 400) {
 statusLabel.setForeground(com.rct.util.UITheme.TEXT_DANGER);
 } else {
 statusLabel.setForeground(com.rct.util.UITheme.TEXT_SECONDARY);
 }

 responseArea.setText(response.getBody());

 DefaultTableModel headerModel = (DefaultTableModel) responseHeadersTable.getModel();
 headerModel.setRowCount(0);
 if (response.getHeaders() != null) {
 for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
 headerModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
 }
 }

 // Repaint the status panel to update the display
 if (statusTimeInfoPanel != null) {
 statusTimeInfoPanel.repaint();
 }
 }

 private String getOAuthAccessToken(String tokenUrl, String clientId, String clientSecret, String scope) throws Exception {
 Map<String, String> tokenHeaders = new HashMap<>();
 tokenHeaders.put("Content-Type", "application/x-www-form-urlencoded");

 StringBuilder body = new StringBuilder();
 body.append("grant_type=client_credentials");
 body.append("&client_id=").append(java.net.URLEncoder.encode(clientId, "UTF-8"));
 body.append("&client_secret=").append(java.net.URLEncoder.encode(clientSecret, "UTF-8"));
 if (scope != null && !scope.trim().isEmpty()) {
 body.append("&scope=").append(java.net.URLEncoder.encode(scope, "UTF-8"));
 }

 RestResponse response = restService.sendRequest("POST", tokenUrl, tokenHeaders, new HashMap<>(), body.toString());

 if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
 try {
 org.json.JSONObject jsonResponse = new org.json.JSONObject(response.getBody());
 return jsonResponse.optString("access_token");
 } catch (Exception e) {
  throw new Exception("Failed to parse OAuth token response: " + e.getMessage());
 }
 } else {
 throw new Exception("OAuth token request failed with status: " + response.getStatusCode());
 }
 }

 private void addAuthHeaders(Map<String, String> headers) {
 LogManager logger = LogManager.getInstance();
 String authType = (String) authTypeCombo.getSelectedItem();

 switch (authType) {
 case "Basic Auth":
  String username = authUsernameField.getText();
 String password = new String(authPasswordField.getPassword());
 if (!username.isEmpty() && !password.isEmpty()) {
 String credentials = username + ":" + password;
 String encoded = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
 headers.put("Authorization", "Basic " + encoded);
 logger.log("Added Basic Auth header for user: " + username);
 }
 break;
 case "Bearer Token":
 String token = authTokenField.getText();
 if (!token.isEmpty()) {
 headers.put("Authorization", "Bearer " + token);
 logger.log("Added Bearer Token authorization header");
 }
 break;
 case "API Key":
 String key = authKeyField.getText();
 String value = authValueField.getText();
  if (!key.isEmpty() && !value.isEmpty()) {
 headers.put(key, value);
 logger.log("Added API Key header: " + key);
 }
 break;
 case "OAuth 2.0":
 String tokenUrl = oauthTokenUrlField.getText();
 String clientId = oauthClientIdField.getText();
 String clientSecret = oauthClientSecretField.getText();
 String scope = oauthScopeField.getText();

  if (!tokenUrl.isEmpty() && !clientId.isEmpty() && !clientSecret.isEmpty()) {
 try {
 String accessToken = getOAuthAccessToken(tokenUrl, clientId, clientSecret, scope);
 if (accessToken != null && !accessToken.isEmpty()) {
 headers.put("Authorization", "Bearer " + accessToken);
 logger.log("Added OAuth 2.0 Bearer token authorization header");
 } else {
  logger.log("Failed to obtain OAuth 2.0 access token");
 }
 } catch (Exception e) {
 logger.log("OAuth 2.0 error: " + e.getMessage());
 }
 }
 break;
 }
 }

 private void formatJsonInternal() {
 try {
 String text = bodyArea.getText();
 if (!text.trim().isEmpty()) {
 String formatted = JsonFormatter.formatJson(text);
  bodyArea.setText(formatted);
 }
 } catch (Exception e) {
 JOptionPane.showMessageDialog(mainPanel, "Error formatting JSON: " + e.getMessage(),
 "Error", JOptionPane.ERROR_MESSAGE);
 }
 }

 private void minifyJsonInternal() {
 try {
 String text = bodyArea.getText();
 if (!text.trim().isEmpty()) {
 String minified = JsonFormatter.minifyJson(text);
 bodyArea.setText(minified);
 }
 } catch (Exception e) {
 JOptionPane.showMessageDialog(mainPanel, "Error minifying JSON: " + e.getMessage(),
 "Error", JOptionPane.ERROR_MESSAGE);
 }
 }
 
 private void validateJsonInternal() {
 try {
 String text = bodyArea.getText();
 if (text.trim().isEmpty()) {
 JOptionPane.showMessageDialog(mainPanel, "No JSON content to validate",
 "Info", JOptionPane.INFORMATION_MESSAGE);
 return;
 }

 boolean isValid = JsonFormatter.isValidJson(text);
 String message = isValid ? "JSON is valid! ✅" : "JSON is invalid! ❌";
 int messageType = isValid ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE;
 JOptionPane.showMessageDialog(mainPanel, message, "Validation Result", messageType);
 } catch (Exception e) {
 JOptionPane.showMessageDialog(mainPanel, "Error validating JSON: " + e.getMessage(),
 "Error", JOptionPane.ERROR_MESSAGE);
 }
 }

 private void formatResponseJson() {
 try {
 String text = responseArea.getText();
  if (!text.trim().isEmpty()) {
 String formatted = JsonFormatter.formatJson(text);
 responseArea.setText(formatted);
 }
 } catch (Exception e) {
 JOptionPane.showMessageDialog(mainPanel, "Error formatting JSON: " + e.getMessage(),
 "Error", JOptionPane.ERROR_MESSAGE);
 }
 }

 private void minifyResponseJson() {
 try {
 String text = responseArea.getText();
 if (!text.trim().isEmpty()) {
 String minified = JsonFormatter.minifyJson(text);
 responseArea.setText(minified);
 }
 } catch (Exception e) {
 JOptionPane.showMessageDialog(mainPanel, "Error minifying JSON: " + e.getMessage(),
 "Error", JOptionPane.ERROR_MESSAGE);
 }
 }

 private void validateResponseJson() {
 try {
 String text = responseArea.getText();
 if (text.trim().isEmpty()) {
 JOptionPane.showMessageDialog(mainPanel, "No response content to validate",
 "Info", JOptionPane.INFORMATION_MESSAGE);
 return;
 }

 boolean isValid = JsonFormatter.isValidJson(text);
 String message = isValid ? "Response JSON is valid! ✅" : "Response JSON is invalid! ❌";
 int messageType = isValid ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE;
 JOptionPane.showMessageDialog(mainPanel, message, "Validation Result", messageType);
 } catch (Exception e) {
 JOptionPane.showMessageDialog(mainPanel, "Error validating JSON: " + e.getMessage(),
 "Error", JOptionPane.ERROR_MESSAGE);
 }
 }

 private void copyResponseToClipboard() {
 try {
 String text = responseArea.getText();
 if (!text.trim().isEmpty()) {
 java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(text);
 java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
 JOptionPane.showMessageDialog(mainPanel, "Response copied to clipboard! 📋",
 "Success", JOptionPane.INFORMATION_MESSAGE);
 } else {
 JOptionPane.showMessageDialog(mainPanel, "No response content to copy",
 "Info", JOptionPane.INFORMATION_MESSAGE);
 }
 } catch (Exception e) {
 JOptionPane.showMessageDialog(mainPanel, "Error copying to clipboard: " + e.getMessage(),
 "Error", JOptionPane.ERROR_MESSAGE);
 }
 }

 public void saveToCollection() {
 saveToCollectionInternal(false);
 }

 public void saveAsToCollection() {
 saveToCollectionInternal(true);
 }

 private void saveToCollectionInternal(boolean forceDialog) {
 if (collectionManager == null) {
 JOptionPane.showMessageDialog(mainPanel, "Collection manager not available", "Error", JOptionPane.ERROR_MESSAGE);
 return;
 }

 // If request is already saved and not forcing dialog, save directly
 if (!forceDialog && savedCollectionName != null && savedRequestName != null) {
 saveRequestDirectly(savedCollectionName, savedRequestName);
 return;
 }

 JDialog saveDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel), "Save Request", true);
 saveDialog.setSize(400, 350);
 saveDialog.setLocationRelativeTo(mainPanel);

 JPanel panel = new JPanel(new BorderLayout());

 JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
 namePanel.add(new JLabel("Request Name:"));
 JTextField nameField = new JTextField(20);
 nameField.setText(name);
 namePanel.add(nameField);

 JPanel collectionPanel = new JPanel(new BorderLayout());
 collectionPanel.setBorder(BorderFactory.createTitledBorder("Select Collection"));

 DefaultListModel<String> listModel = new DefaultListModel<>();
 for (CollectionManager.Collection collection : collectionManager.getCollections()) {
 listModel.addElement(collection.getName());
 }

 JList<String> collectionList = new JList<>(listModel);
 if (!listModel.isEmpty()) {
 collectionList.setSelectedIndex(0);
 }

 JScrollPane scrollPane = new JScrollPane(collectionList);
 scrollPane.setPreferredSize(new Dimension(350, 200));

 JPanel collectionButtons = new JPanel(new FlowLayout());
 JButton newCollectionBtn = new JButton("New Collection");
 newCollectionBtn.addActionListener(e -> {
 String newName = JOptionPane.showInputDialog(saveDialog, "Collection Name:", "New Collection", JOptionPane.PLAIN_MESSAGE);
 if (newName != null && !newName.trim().isEmpty()) {
 listModel.addElement(newName.trim());
 collectionList.setSelectedIndex(listModel.getSize() - 1);
 }
 });
 collectionButtons.add(newCollectionBtn);

 collectionPanel.add(scrollPane, BorderLayout.CENTER);
  collectionPanel.add(collectionButtons, BorderLayout.SOUTH);

 JPanel buttonPanel = new JPanel(new FlowLayout());
 JButton saveBtn = new JButton("Save");
 JButton cancelBtn = new JButton("Cancel");

 saveBtn.addActionListener(e -> {
 String requestName = nameField.getText().trim();
 String selectedCollection = collectionList.getSelectedValue();

 if (requestName.isEmpty()) {
 JOptionPane.showMessageDialog(saveDialog, "Please enter a request name", "Error", JOptionPane.ERROR_MESSAGE);
 return;
 }

 if (selectedCollection == null) {
 JOptionPane.showMessageDialog(saveDialog, "Please select a collection", "Error", JOptionPane.ERROR_MESSAGE);
 return;
 }

 String method = (String) methodCombo.getSelectedItem();
 String url = urlField.getText().trim();

 StringBuilder headersStr = new StringBuilder();
 DefaultTableModel headerModel = (DefaultTableModel) headersTable.getModel();
 for (int i = 0; i < headerModel.getRowCount(); i++) {
 String key = (String) headerModel.getValueAt(i, 0);
 String value = (String) headerModel.getValueAt(i, 1);
 if (key != null && !key.trim().isEmpty() && value != null) {
 headersStr.append(key.trim()).append(": ").append(value.trim()).append("\n");
  }
 }

 StringBuilder paramsStr = new StringBuilder();
 DefaultTableModel paramModel = (DefaultTableModel) paramsTable.getModel();
 for (int i = 0; i < paramModel.getRowCount(); i++) {
 String key = (String) paramModel.getValueAt(i, 0);
 String value = (String) paramModel.getValueAt(i, 1);
 if (key != null && !key.trim().isEmpty() && value != null) {
 paramsStr.append(key.trim()).append("=").append(value.trim()).append("\n");
 }
 }

 String body = bodyArea.getText();

 CollectionManager.SavedRequest savedRequest = new CollectionManager.SavedRequest(
 requestName, method, url, headersStr.toString(), body, paramsStr.toString());

 CollectionManager.Collection targetCollection = null;
 for (CollectionManager.Collection collection : collectionManager.getCollections()) {
 if (collection.getName().equals(selectedCollection)) {
 targetCollection = collection;
 break;
 }
 }

 if (targetCollection == null) {
 targetCollection = new CollectionManager.Collection(selectedCollection);
 collectionManager.addCollection(targetCollection);
 }

 // Check if request with same name already exists and replace it
 boolean replaced = false;
 for (int i = 0; i < targetCollection.getRequests().size(); i++) {
 CollectionManager.SavedRequest existingRequest = targetCollection.getRequests().get(i);
 if (existingRequest.getName().equals(requestName)) {
 targetCollection.getRequests().set(i, savedRequest);
 replaced = true;
 break;
 }
 }

 if (!replaced) {
 targetCollection.addRequest(savedRequest);
 }
 collectionManager.refreshTree();

 // Force save after adding request
 try {
 java.lang.reflect.Method saveMethod = collectionManager.getClass().getDeclaredMethod("saveCollections");
 saveMethod.setAccessible(true);
 saveMethod.invoke(collectionManager);
 } catch (Exception ex) {
 com.rct.util.LogManager.getInstance().log("Error saving collections: " + ex.getMessage());
 }

 String newTabName = selectedCollection + " → " + requestName;
 setName(newTabName);
  savedCollectionName = selectedCollection;
 savedRequestName = requestName;
 markAsSaved(); // Mark as saved after successful save
 if (tabUpdateCallback != null) {
 tabUpdateCallback.updateTabTitle(this);
 }

 JOptionPane.showMessageDialog(saveDialog, "Request saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
 saveDialog.dispose();
 });

 cancelBtn.addActionListener(e -> saveDialog.dispose());

 buttonPanel.add(saveBtn);
 buttonPanel.add(cancelBtn);

 panel.add(namePanel, BorderLayout.NORTH);
 panel.add(collectionPanel, BorderLayout.CENTER);
 panel.add(buttonPanel, BorderLayout.SOUTH);

 saveDialog.add(panel);
 saveDialog.setVisible(true);
 }

 private void saveRequestDirectly(String collectionName, String requestName) {
 String method = (String) methodCombo.getSelectedItem();
 String url = urlField.getText().trim();

 StringBuilder headersStr = new StringBuilder();
 DefaultTableModel headerModel = (DefaultTableModel) headersTable.getModel();
 for (int i = 0; i < headerModel.getRowCount(); i++) {
 String key = (String) headerModel.getValueAt(i, 0);
 String value = (String) headerModel.getValueAt(i, 1);
 if (key != null && !key.trim().isEmpty() && value != null) {
 headersStr.append(key.trim()).append(": ").append(value.trim()).append("\n");
 }
 }

 StringBuilder paramsStr = new StringBuilder();
 DefaultTableModel paramModel = (DefaultTableModel) paramsTable.getModel();
 for (int i = 0; i < paramModel.getRowCount(); i++) {
 String key = (String) paramModel.getValueAt(i, 0);
 String value = (String) paramModel.getValueAt(i, 1);
 if (key != null && !key.trim().isEmpty() && value != null) {
 paramsStr.append(key.trim()).append("=").append(value.trim()).append("\n");
 }
 }

 String body = bodyArea.getText();

 CollectionManager.SavedRequest savedRequest = new CollectionManager.SavedRequest(
 requestName, method, url, headersStr.toString(), body, paramsStr.toString());

 CollectionManager.Collection targetCollection = null;
 for (CollectionManager.Collection collection : collectionManager.getCollections()) {
 if (collection.getName().equals(collectionName)) {
 targetCollection = collection;
 break;
 }
 }

 if (targetCollection != null) {
 // Replace existing request
 for (int i = 0; i < targetCollection.getRequests().size(); i++) {
 CollectionManager.SavedRequest existingRequest = targetCollection.getRequests().get(i);
 if (existingRequest.getName().equals(requestName)) {
 targetCollection.getRequests().set(i, savedRequest);
 break;
 }
 }

 collectionManager.refreshTree();

 // Force save after updating request
 try {
 java.lang.reflect.Method saveMethod = collectionManager.getClass().getDeclaredMethod("saveCollections");
 saveMethod.setAccessible(true);
 saveMethod.invoke(collectionManager);
 } catch (Exception ex) {
 com.rct.util.LogManager.getInstance().log("Error saving collections: " + ex.getMessage());
 }

 markAsSaved();
 if (tabUpdateCallback != null) {
 tabUpdateCallback.updateTabTitle(this);
 }

 JOptionPane.showMessageDialog(mainPanel, "Request updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
 }
 }

 public void setRequestData(String method, String url, Map<String, String> headers, String body) {
 setRequestData(method, url, headers, body, null);
 }

 public void setRequestData(String method, String url, Map<String, String> headers, String body, String params) {
 com.rct.util.LogManager.getInstance().log("Setting request data - Method: " + method + ", URL: " + url);

 methodCombo.setSelectedItem(method);
 urlField.setText(url);

 if (headers != null && !headers.isEmpty()) {
 DefaultTableModel headerModel = (DefaultTableModel) headersTable.getModel();
 headerModel.setRowCount(0);
 for (Map.Entry<String, String> entry : headers.entrySet()) {
 headerModel.addRow(new Object[]{entry.getKey(), entry.getValue(), ""});
 com.rct.util.LogManager.getInstance().log("Added header: " + entry.getKey() + " = " + entry.getValue());
 }
 }

 if (params != null && !params.trim().isEmpty()) {
 DefaultTableModel paramModel = (DefaultTableModel) paramsTable.getModel();
 paramModel.setRowCount(0);
 String[] paramLines = params.split("\n");
 for (String line : paramLines) {
 if (line.trim().isEmpty()) continue;
 String[] parts = line.split("=", 2);
 if (parts.length == 2) {
 paramModel.addRow(new Object[]{parts[0].trim(), parts[1].trim(), ""});
 com.rct.util.LogManager.getInstance().log("Added param: " + parts[0].trim() + " = " + parts[1].trim());
  }
 }
 }

 if (body != null && !body.trim().isEmpty()) {
 bodyArea.setText(body);
 requestTabs.setSelectedIndex(2);
 com.rct.util.LogManager.getInstance().log("Set body content and switched to Body tab");
 }

 com.rct.util.LogManager.getInstance().log("Request data set successfully");
 }

 public RequestTab duplicate(String newName, HistoryManager historyManager, CollectionManager collectionManager, EnvironmentManager environmentManager, TabUpdateCallback callback) {
 RequestTab duplicate = new RequestTab(newName, historyManager, collectionManager, environmentManager, callback);

 duplicate.methodCombo.setSelectedItem(this.methodCombo.getSelectedItem());
 duplicate.urlField.setText(this.urlField.getText());
 duplicate.bodyArea.setText(this.bodyArea.getText());

 return duplicate;
 }

 // Keyboard shortcut methods
 public void focusUrl() {
 urlField.requestFocus();
 }

 public void focusParams() {
 requestTabs.setSelectedIndex(0);
 paramsTable.requestFocus();
 }

 public void focusHeaders() {
 requestTabs.setSelectedIndex(1);
 headersTable.requestFocus();
 }

 public void focusBody() {
 requestTabs.setSelectedIndex(2);
 bodyArea.requestFocus();
 }

 public void focusAuth() {
 requestTabs.setSelectedIndex(3);
 authTypeCombo.requestFocus();
 }

 public void formatJson() {
 if (requestTabs.getSelectedIndex() == 2) {
 formatJsonInternal();
 } else if (responsePanel.getSelectedIndex() == 0) {
 formatResponseJson();
 }
 }

 public void minifyJson() {
 if (requestTabs.getSelectedIndex() == 2) {
 minifyJsonInternal();
 } else if (responsePanel.getSelectedIndex() == 0) {
 minifyResponseJson();
 }
 }

 public void validateJson() {
 if (requestTabs.getSelectedIndex() == 2) {
 validateJsonInternal();
 } else if (responsePanel.getSelectedIndex() == 0) {
 validateResponseJson();
 }
 }

 public void copyAsCurl() {
 try {
 StringBuilder curl = new StringBuilder();
 curl.append("curl -X ").append(methodCombo.getSelectedItem());

 String url = urlField.getText().trim();
 if (!url.isEmpty()) {
 curl.append(" \"").append(url).append("\"");
 }

 DefaultTableModel headerModel = (DefaultTableModel) headersTable.getModel();
 for (int i = 0; i < headerModel.getRowCount(); i++) {
 String key = (String) headerModel.getValueAt(i, 0);
 String value = (String) headerModel.getValueAt(i, 1);
 if (key != null && !key.trim().isEmpty() && value != null) {
 curl.append(" \\").append("\n -H \"").append(key.trim())
 .append(": ").append(value.trim()).append("\"");
 }
 }

 String body = bodyArea.getText();
 if (!body.trim().isEmpty()) {
 String escapedBody = body.replace("\"", "\\\"").replace("\n", "\\n");
 curl.append(" \\").append("\n -d \"").append(escapedBody).append("\"");
 }

 java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(curl.toString());
 java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
 JOptionPane.showMessageDialog(mainPanel, "cURL command copied to clipboard!", "Success", JOptionPane.INFORMATION_MESSAGE);
 } catch (Exception e) {
 JOptionPane.showMessageDialog(mainPanel, "Error generating cURL: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
 }
 }

 public void copyResponse() {
 copyResponseToClipboard();
 }

 private void syncParamsFromUrl() {
 String url = urlField.getText();
 if (url.contains("?")) {
 String[] parts = url.split("\\?", 2);
 if (parts.length == 2) {
 DefaultTableModel model = (DefaultTableModel) paramsTable.getModel();
 model.setRowCount(0);

 String[] params = parts[1].split("&");
 for (String param : params) {
 String[] keyValue = param.split("=", 2);
 if (keyValue.length >= 1) {
 String key = keyValue[0];
 String value = keyValue.length == 2 ? keyValue[1] : "";
 model.addRow(new Object[]{key, value, ""});
 }
 }
 }
 }
 }

 private void syncUrlFromParams() {
 SwingUtilities.invokeLater(() -> {
 String currentUrl = urlField.getText();
 String baseUrl = currentUrl.contains("?") ? currentUrl.split("\\?")[0] : currentUrl;

 DefaultTableModel model = (DefaultTableModel) paramsTable.getModel();
 StringBuilder params = new StringBuilder();

 for (int i = 0; i < model.getRowCount(); i++) {
 String key = (String) model.getValueAt(i, 0);
 String value = (String) model.getValueAt(i, 1);
 if (key != null && !key.trim().isEmpty()) {
 if (params.length() > 0) params.append("&");
 params.append(key.trim());
 if (value != null && !value.trim().isEmpty()) {
 params.append("=").append(value.trim());
 }
 }
  }

 String newUrl = baseUrl;
 if (params.length() > 0) {
 newUrl += "?" + params.toString();
 }

 if (!newUrl.equals(currentUrl)) {
 urlField.setText(newUrl);
 }
 });
 }

 private void setupChangeListeners() {
 // Add change listeners to detect modifications
 methodCombo.addActionListener(e -> markAsModified());

 bodyArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
 @Override
 public void insertUpdate(javax.swing.event.DocumentEvent e) {
 markAsModified();
 updateVariableHighlighting();
 }
 @Override
 public void removeUpdate(javax.swing.event.DocumentEvent e) {
 markAsModified();
 updateVariableHighlighting();
 }
 @Override
 public void changedUpdate(javax.swing.event.DocumentEvent e) {
 markAsModified();
 updateVariableHighlighting();
 }
 });
 }

 private void markAsModified() {
 if (!isModified) {
 isModified = true;
 statusIndicator.setModified(true);
 if (tabUpdateCallback != null) {
 tabUpdateCallback.updateTabTitle(this);
 // Auto-save session when modified
 if (tabUpdateCallback instanceof com.rct.ui.RestClientApp) {
 ((com.rct.ui.RestClientApp) tabUpdateCallback).saveSessions();
 }
 }
 }
 }

 public void markAsSaved() {
 isModified = false;
 statusIndicator.setModified(false);
 statusIndicator.setStatus(TabStatusIndicator.Status.READY);
 if (tabUpdateCallback != null) {
 tabUpdateCallback.updateTabTitle(this);
 // Auto-save session when saved
 if (tabUpdateCallback instanceof com.rct.ui.RestClientApp) {
 ((com.rct.ui.RestClientApp) tabUpdateCallback).saveSessions();
 }
 }
 }

 public String getDisplayName() {
 return isModified ? name + " *" : name;
 }

 public String getName() { return name; }
 public void setName(String newName) { this.name = newName; }
 public JPanel getPanel() { return mainPanel; }
 public TabStatusIndicator getStatusIndicator() { return statusIndicator; }
 public boolean isModified() { return isModified; }

 public void setSavedInfo(String collectionName, String requestName) {
 this.savedCollectionName = collectionName;
 this.savedRequestName = requestName;
 markAsSaved();
 }

 public com.rct.manager.SessionManager.TabSession exportSession() {
 String method = (String) methodCombo.getSelectedItem();
 String url = urlField.getText();
 String body = bodyArea.getText();

 StringBuilder headersStr = new StringBuilder();
 DefaultTableModel headerModel = (DefaultTableModel) headersTable.getModel();
 for (int i = 0; i < headerModel.getRowCount(); i++) {
 String key = (String) headerModel.getValueAt(i, 0);
 String value = (String) headerModel.getValueAt(i, 1);
 if (key != null && !key.trim().isEmpty() && value != null) {
 headersStr.append(key.trim()).append(": ").append(value.trim()).append("\n");
 }
 }

 StringBuilder paramsStr = new StringBuilder();
 DefaultTableModel paramModel = (DefaultTableModel) paramsTable.getModel();
 for (int i = 0; i < paramModel.getRowCount(); i++) {
 String key = (String) paramModel.getValueAt(i, 0);
 String value = (String) paramModel.getValueAt(i, 1);
 if (key != null && !key.trim().isEmpty() && value != null) {
 paramsStr.append(key.trim()).append("=").append(value.trim()).append("\n");
 }
 }

 return new com.rct.manager.SessionManager.TabSession(
 name, method, url, headersStr.toString(), body, paramsStr.toString(),
 savedCollectionName, savedRequestName, isModified
 );
 }

 public void importSession(com.rct.manager.SessionManager.TabSession session) {
 this.name = session.getName();
 this.savedCollectionName = session.getSavedCollectionName();
 this.savedRequestName = session.getSavedRequestName();
 this.isModified = session.isModified();

 methodCombo.setSelectedItem(session.getMethod());
 urlField.setText(session.getUrl());
 bodyArea.setText(session.getBody());

 // Import headers
 DefaultTableModel headerModel = (DefaultTableModel) headersTable.getModel();
 headerModel.setRowCount(0);
 if (session.getHeaders() != null && !session.getHeaders().trim().isEmpty()) {
 String[] headerLines = session.getHeaders().split("\n");
 for (String line : headerLines) {
 String[] parts = line.split(":", 2);
 if (parts.length == 2) {
 headerModel.addRow(new Object[]{parts[0].trim(), parts[1].trim(), ""});
  }
 }
 }

 // Import params
 DefaultTableModel paramModel = (DefaultTableModel) paramsTable.getModel();
 paramModel.setRowCount(0);
 if (session.getParams() != null && !session.getParams().trim().isEmpty()) {
 String[] paramLines = session.getParams().split("\n");
 for (String line : paramLines) {
 if (line.trim().isEmpty()) continue;
 String[] parts = line.split("=", 2);
 if (parts.length == 2) {
 paramModel.addRow(new Object[]{parts[0].trim(), parts[1].trim(), ""});
 }
 }
 }

 statusIndicator.setModified(isModified);
 if (tabUpdateCallback != null) {
 tabUpdateCallback.updateTabTitle(this);
 }
 }

 private void updateVariableHighlighting() {
 if (environmentManager != null) {
 // Check URL for unresolved variables
 String url = urlField.getText();
 if (environmentManager.hasUnresolvedVariables(url)) {
 urlField.setBackground(new Color(255, 245, 245)); // Light red
 urlField.setToolTipText("Contains unresolved variables: " + environmentManager.findVariablesInText(url));
 } else {
 urlField.setBackground(Color.WHITE);
 urlField.setToolTipText(null);
 }

 // Check body for unresolved variables
 String body = bodyArea.getText();
 if (environmentManager.hasUnresolvedVariables(body)) {
 bodyArea.setBackground(new Color(255, 245, 245)); // Light red
 } else {
 bodyArea.setBackground(com.rct.util.UITheme.BACKGROUND_LIGHT);
 }
 }
 }
}

