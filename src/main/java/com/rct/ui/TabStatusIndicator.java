package com.rct.ui;

import javax.swing.*;
import java.awt.*;

public class TabStatusIndicator extends JComponent {
 public enum Status {
 READY(Color.GRAY, "●"),
 SENDING(new Color(255, 193, 7), "●"),
 SUCCESS(new Color(40, 167, 69), "●"),
 ERROR(new Color(220, 53, 69), "●"),
 MODIFIED(new Color(23, 162, 184), "●");
 
 private final Color color;
 private final String symbol;

 Status(Color color, String symbol) {
 this.color = color;
 this.symbol = symbol;
 }

 public Color getColor() { return color; }
 public String getSymbol() { return symbol; }
 }

 private Status status = Status.READY;
 private boolean isModified = false;

 public TabStatusIndicator() {
 setPreferredSize(new Dimension(8, 8));
 setOpaque(false);
 }

 public void setStatus(Status status) {
 this.status = status;
 repaint();
 }

 public void setModified(boolean modified) {
 this.isModified = modified;
 if (modified && status == Status.READY) {
 setStatus(Status.MODIFIED);
 }
 repaint();
 }

 public Status getStatus() {
 return status;
 }

 public boolean isModified() {
 return isModified;
 }

 @Override
 protected void paintComponent(Graphics g) {
 super.paintComponent(g);

 Graphics2D g2 = (Graphics2D) g.create();
 g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

 int size = Math.min(getWidth(), getHeight()) - 2;
 int x = (getWidth() - size) / 2;
 int y = (getHeight() - size) / 2;

 g2.setColor(status.getColor());
 g2.fillOval(x, y, size, size);

 // Add a subtle border
 g2.setColor(status.getColor().darker());
 g2.drawOval(x, y, size, size);

 g2.dispose();
 }

 @Override
 public String getToolTipText() {
 switch (status) {
 case READY: return "Ready";
 case SENDING: return "Sending request...";
 case SUCCESS: return "Request successful";
 case ERROR: return "Request failed";
 case MODIFIED: return "Request modified";
 default: return "";
 }
 }
}