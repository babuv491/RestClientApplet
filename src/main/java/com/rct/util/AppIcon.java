package com.rct.util;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class AppIcon {

 public static ImageIcon createAppIcon(int size) {
 BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
 Graphics2D g2d = image.createGraphics();
 g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

 // Background circle
 g2d.setColor(new Color(74, 144, 226));
 g2d.fillOval(2, 2, size-4, size-4);

 // Inner circle
 g2d.setColor(Color.WHITE);
 g2d.fillOval(size/4, size/4, size/2, size/2);

 // REST text
 g2d.setColor(new Color(74, 144, 226));
 g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size/6));
 FontMetrics fm = g2d.getFontMetrics();
 String text = "REST";
 int textWidth = fm.stringWidth(text);
 int textHeight = fm.getHeight();
 g2d.drawString(text, (size - textWidth) / 2, (size + textHeight/2) / 2);

 g2d.dispose();
 return new ImageIcon(image);
 }

 public static void setAppIcon(JFrame frame) {
 frame.setIconImage(createAppIcon(32).getImage());
 }
}