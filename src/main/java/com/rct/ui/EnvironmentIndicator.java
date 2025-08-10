package com.rct.ui;

import com.rct.manager.EnvironmentManager;
import com.rct.util.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class EnvironmentIndicator extends JPanel implements EnvironmentManager.EnvironmentChangeListener {
 private EnvironmentManager environmentManager;
 private JLabel environmentLabel;
 private Runnable onClickCallback;

 public EnvironmentIndicator(EnvironmentManager environmentManager, Runnable onClickCallback) {
 this.environmentManager = environmentManager;
 this.onClickCallback = onClickCallback;

 setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
 setBackground(UITheme.BACKGROUND_WHITE);
 setBorder(BorderFactory.createCompoundBorder(
 BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
 BorderFactory.createEmptyBorder(2, 8, 2, 8)
 ));
 setCursor(new Cursor(Cursor.HAND_CURSOR));

 JLabel iconLabel = new JLabel("🌍");
 iconLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

 environmentLabel = new JLabel();
 environmentLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
 environmentLabel.setForeground(UITheme.PRIMARY_BLUE);

 add(iconLabel);
 add(environmentLabel);

 updateEnvironmentDisplay();

 // Add click listener
 addMouseListener(new MouseAdapter() {
 @Override
 public void mouseClicked(MouseEvent e) {
 if (onClickCallback != null) {
 onClickCallback.run();
 }
 }

 @Override
 public void mouseEntered(MouseEvent e) {
 setBackground(new Color(240, 245, 255));
 repaint();
 }

 @Override
 public void mouseExited(MouseEvent e) {
 setBackground(UITheme.BACKGROUND_WHITE);
 repaint();
 }
 });

 // Register as listener
 environmentManager.addListener(this);

 setToolTipText("Click to manage environment variables");
 }

 private void updateEnvironmentDisplay() {
 EnvironmentManager.Environment activeEnv = environmentManager.getActiveEnvironment();
 if (activeEnv != null) {
 environmentLabel.setText(activeEnv.getName());
 setToolTipText("Active Environment: " + activeEnv.getName() + " (Click to manage)");
 } else {
 environmentLabel.setText("No Environment");
 setToolTipText("No active environment (Click to manage)");
 }
 }

 @Override
 public void onEnvironmentChanged(String environmentName) {
 SwingUtilities.invokeLater(this::updateEnvironmentDisplay);
 }

 @Override
 public void onVariablesUpdated() {
 SwingUtilities.invokeLater(this::updateEnvironmentDisplay);
 }
}
