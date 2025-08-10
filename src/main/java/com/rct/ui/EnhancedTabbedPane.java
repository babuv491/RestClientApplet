package com.rct.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class EnhancedTabbedPane extends JTabbedPane {
 private List<TabCloseListener> closeListeners = new ArrayList<>();
 private JPopupMenu tabContextMenu;
 private int contextMenuTabIndex = -1;

 public interface TabCloseListener {
 void tabClosed(int index);
  void tabCloseAll();
 void tabCloseOthers(int index);
 void tabDuplicate(int index);
 }

 public interface NewTabListener {
 void newTabRequested();
 }

 public EnhancedTabbedPane() {
 super();
 setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
 setupContextMenu();
 setupMouseListener();
 customizeUI();
 }

 private void customizeUI() {
 setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
 setBackground(com.rct.util.UITheme.BACKGROUND_WHITE);
 setForeground(com.rct.util.UITheme.TEXT_PRIMARY);
 setBorder(new EmptyBorder(5, 5, 0, 5));
 }

 private void setupContextMenu() {
 tabContextMenu = new JPopupMenu();

 JMenuItem closeTab = new JMenuItem("🗙Close Tab");
 closeTab.addActionListener(e -> {
 if (contextMenuTabIndex >= 0) {
 notifyTabClosed(contextMenuTabIndex);
 }
 });

 JMenuItem closeOthers = new JMenuItem("🗙Close Others");
 closeOthers.addActionListener(e -> {
 if (contextMenuTabIndex >= 0) {
 notifyTabCloseOthers(contextMenuTabIndex);
 }
 });

 JMenuItem closeAll = new JMenuItem("🗙Close All");
 closeAll.addActionListener(e -> notifyTabCloseAll());

 JMenuItem duplicateTab = new JMenuItem("📋Duplicate Tab");
 duplicateTab.addActionListener(e -> {
 if (contextMenuTabIndex >= 0) {
 notifyTabDuplicate(contextMenuTabIndex);
 }
 });

 tabContextMenu.add(duplicateTab);
 tabContextMenu.addSeparator();
 tabContextMenu.add(closeTab);
 tabContextMenu.add(closeOthers);
 tabContextMenu.add(closeAll);
 }

 private void setupMouseListener() {
 addMouseListener(new MouseAdapter() {
 @Override
 public void mousePressed(MouseEvent e) {
 if (SwingUtilities.isRightMouseButton(e)) {
 int tabIndex = indexAtLocation(e.getX(), e.getY());
 if (tabIndex >= 0) {
 contextMenuTabIndex = tabIndex;
 tabContextMenu.show(EnhancedTabbedPane.this, e.getX(), e.getY());
 }
 } else if (SwingUtilities.isMiddleMouseButton(e)) {
 int tabIndex = indexAtLocation(e.getX(), e.getY());
 if (tabIndex >= 0) {
 notifyTabClosed(tabIndex);
 }
 }
 }
 });
 }

 public void addEnhancedTab(String title, Component component, String tooltip, boolean closeable) {
 addEnhancedTab(title, component, tooltip, closeable, null);
 }

 public void addEnhancedTab(String title, Component component, String tooltip, boolean closeable, TabStatusIndicator statusIndicator) {
 addTab(null, component);
 int index = getTabCount() - 1;

 JPanel tabHeader = createTabHeader(title, tooltip, closeable, index, statusIndicator);
 setTabComponentAt(index, tabHeader);

 if (tooltip != null && !tooltip.isEmpty()) {
 setToolTipTextAt(index, tooltip);
 }
 }

 private JPanel createTabHeader(String title, String tooltip, boolean closeable, int tabIndex) {
 return createTabHeader(title, tooltip, closeable, tabIndex, null);
 }

 private JPanel createTabHeader(String title, String tooltip, boolean closeable, int tabIndex, TabStatusIndicator statusIndicator) {
 JPanel tabPanel = new JPanel(new BorderLayout(3, 0));
 tabPanel.setOpaque(false);
 tabPanel.setBorder(new EmptyBorder(4, 6, 4, closeable ? 4 : 8));

 // Left panel for status indicator
 JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
 leftPanel.setOpaque(false);

 if (statusIndicator != null) {
 leftPanel.add(statusIndicator);
 }

 // Tab icon and title
 JLabel titleLabel = new JLabel(title);
 titleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
 titleLabel.setForeground(com.rct.util.UITheme.TEXT_PRIMARY);

 // Add method indicator if title contains method info
 if (title.contains("GET")) titleLabel.setIcon(createMethodIcon("GET", com.rct.util.UITheme.BUTTON_SUCCESS));
 else if (title.contains("POST")) titleLabel.setIcon(createMethodIcon("POST", com.rct.util.UITheme.PRIMARY_BLUE));
 else if (title.contains("PUT")) titleLabel.setIcon(createMethodIcon("PUT", com.rct.util.UITheme.PRIMARY_ORANGE));
 else if (title.contains("DELETE")) titleLabel.setIcon(createMethodIcon("DELETE", com.rct.util.UITheme.BUTTON_DANGER));
 else titleLabel.setIcon(createMethodIcon("REQ", com.rct.util.UITheme.PRIMARY_PURPLE));

 if (statusIndicator != null) {
 tabPanel.add(leftPanel, BorderLayout.WEST);
 }
 tabPanel.add(titleLabel, BorderLayout.CENTER);

 if (closeable) {
 JButton closeButton = createCloseButton(tabIndex);
 tabPanel.add(closeButton, BorderLayout.EAST);
 }

 // Add hover effect and click handling
 tabPanel.addMouseListener(new MouseAdapter() {
 @Override
 public void mouseEntered(MouseEvent e) {
 tabPanel.setOpaque(true);
 tabPanel.setBackground(new Color(240, 240, 240));
 tabPanel.repaint();
 }

 @Override
 public void mouseExited(MouseEvent e) {
 tabPanel.setOpaque(false);
 tabPanel.repaint();
 }

 @Override
 public void mouseClicked(MouseEvent e) {
 if (SwingUtilities.isLeftMouseButton(e)) {
 setSelectedIndex(tabIndex);
 }
 }
 });

 return tabPanel;
 }

 private Icon createMethodIcon(String method, Color color) {
 return new Icon() {
 @Override
 public void paintIcon(Component c, Graphics g, int x, int y) {
 Graphics2D g2 = (Graphics2D) g.create();
 g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

 g2.setColor(color);
 g2.fillRoundRect(x, y, getIconWidth(), getIconHeight(), 4, 4);

 g2.setColor(Color.WHITE);
 g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 8));
 FontMetrics fm = g2.getFontMetrics();
 String text = method.length() > 3 ? method.substring(0, 3) : method;
 int textX = x + (getIconWidth() - fm.stringWidth(text)) / 2;
 int textY = y + (getIconHeight() + fm.getAscent()) / 2 - 1;
 g2.drawString(text, textX, textY);

 g2.dispose();
 }

 @Override
 public int getIconWidth() { return 24; }

 @Override
 public int getIconHeight() { return 12; }
 };
 }

 private JButton createCloseButton(int tabIndex) {
 JButton closeButton = new JButton("×");
 closeButton.setPreferredSize(new Dimension(16, 16));
 closeButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
 closeButton.setBorderPainted(false);
 closeButton.setContentAreaFilled(false);
 closeButton.setFocusPainted(false);
 closeButton.setForeground(com.rct.util.UITheme.TEXT_SECONDARY);
 closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

 closeButton.addMouseListener(new MouseAdapter() {
 @Override
 public void mouseEntered(MouseEvent e) {
 closeButton.setContentAreaFilled(true);
 closeButton.setBackground(com.rct.util.UITheme.BUTTON_DANGER);
 closeButton.setForeground(Color.WHITE);
 }

 @Override
 public void mouseExited(MouseEvent e) {
 closeButton.setContentAreaFilled(false);
 closeButton.setForeground(com.rct.util.UITheme.TEXT_SECONDARY);
 }
 });

 closeButton.addActionListener(e -> notifyTabClosed(tabIndex));

 return closeButton;
 }

 public void updateTabTitle(int index, String newTitle) {
 if (index >= 0 && index < getTabCount()) {
 JPanel tabHeader = (JPanel) getTabComponentAt(index);
 if (tabHeader != null) {
 Component centerComponent = ((BorderLayout) tabHeader.getLayout()).getLayoutComponent(BorderLayout.CENTER);
 if (centerComponent instanceof JLabel) {
 JLabel titleLabel = (JLabel) centerComponent;
 titleLabel.setText(newTitle);

 // Update method icon
 if (newTitle.contains("GET")) titleLabel.setIcon(createMethodIcon("GET", com.rct.util.UITheme.BUTTON_SUCCESS));
 else if (newTitle.contains("POST")) titleLabel.setIcon(createMethodIcon("POST", com.rct.util.UITheme.PRIMARY_BLUE));
 else if (newTitle.contains("PUT")) titleLabel.setIcon(createMethodIcon("PUT", com.rct.util.UITheme.PRIMARY_ORANGE));
 else if (newTitle.contains("DELETE")) titleLabel.setIcon(createMethodIcon("DELETE", com.rct.util.UITheme.BUTTON_DANGER));
 else titleLabel.setIcon(createMethodIcon("REQ", com.rct.util.UITheme.PRIMARY_PURPLE));

 tabHeader.revalidate();
 tabHeader.repaint();
 }
 }
 }
 }

 public void addTabCloseListener(TabCloseListener listener) {
 closeListeners.add(listener);
 }

 public void removeTabCloseListener(TabCloseListener listener) {
 closeListeners.remove(listener);
 }

 private void notifyTabClosed(int index) {
 for (TabCloseListener listener : closeListeners) {
  listener.tabClosed(index);
 }
 }

 private void notifyTabCloseAll() {
 for (TabCloseListener listener : closeListeners) {
 listener.tabCloseAll();
 }
 }

 private void notifyTabCloseOthers(int index) {
 for (TabCloseListener listener : closeListeners) {
 listener.tabCloseOthers(index);
 }
 }

 private void notifyTabDuplicate(int index) {
 for (TabCloseListener listener : closeListeners) {
 listener.tabDuplicate(index);
 }
 }

 public void addNewTabButton() {
 JPanel newTabPanel = new JPanel(new BorderLayout());
 newTabPanel.setOpaque(false);
 newTabPanel.setBorder(new EmptyBorder(4, 8, 4, 8));

 JButton newTabButton = new JButton("+");
  newTabButton.setPreferredSize(new Dimension(20, 20));
 newTabButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
 newTabButton.setBorderPainted(false);
 newTabButton.setContentAreaFilled(false);
 newTabButton.setFocusPainted(false);
 newTabButton.setForeground(com.rct.util.UITheme.PRIMARY_BLUE);
 newTabButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
 newTabButton.setToolTipText("New Request Tab (Ctrl+N)");

 newTabButton.addMouseListener(new MouseAdapter() {
 @Override
 public void mouseEntered(MouseEvent e) {
 newTabButton.setContentAreaFilled(true);
 newTabButton.setBackground(com.rct.util.UITheme.PRIMARY_BLUE);
 newTabButton.setForeground(Color.WHITE);
 }

 @Override
 public void mouseExited(MouseEvent e) {
 newTabButton.setContentAreaFilled(false);
 newTabButton.setForeground(com.rct.util.UITheme.PRIMARY_BLUE);
 }
 });

 newTabButton.addActionListener(e -> {
 // Notify listeners to create a new tab
 for (TabCloseListener listener : closeListeners) {
 if (listener instanceof NewTabListener) {
 ((NewTabListener) listener).newTabRequested();
 }
 }
 });

 newTabPanel.add(newTabButton, BorderLayout.CENTER);

 // Add empty component for the new tab button
 addTab(null, new JPanel());
 int index = getTabCount() - 1;
 setTabComponentAt(index, newTabPanel);
 setEnabledAt(index, false); // Disable selection of the new tab button
 }
}
