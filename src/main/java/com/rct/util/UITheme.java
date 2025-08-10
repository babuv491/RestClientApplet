package com.rct.util;

import java.awt.*;

public class UITheme {
 // Primary Colors
 public static final Color PRIMARY_BLUE = new Color(52, 152, 219);
 public static final Color PRIMARY_GREEN = new Color(46, 204, 113);
 public static final Color PRIMARY_RED = new Color(231, 76, 60);
 public static final Color PRIMARY_ORANGE = new Color(230, 126, 34);
 public static final Color PRIMARY_PURPLE = new Color(155, 89, 182);

 // Background Colors
 public static final Color BACKGROUND_LIGHT = new Color(248, 249, 250);
 public static final Color BACKGROUND_WHITE = Color.WHITE;
 public static final Color BACKGROUND_DARK = new Color(52, 58, 64);

 // Table Colors
 public static final Color TABLE_HEADER = new Color(233, 236, 239);
 public static final Color TABLE_ROW_EVEN = Color.WHITE;
 public static final Color TABLE_ROW_ODD = new Color(248, 249, 250);
 public static final Color TABLE_SELECTION = new Color(52, 152, 219, 50);
 public static final Color TABLE_BORDER = new Color(222, 226, 230);

 // Button Colors
 public static final Color BUTTON_SUCCESS = new Color(40, 167, 69);
 public static final Color BUTTON_DANGER = new Color(220, 53, 69);
 public static final Color BUTTON_WARNING = new Color(255, 193, 7);
 public static final Color BUTTON_INFO = new Color(23, 162, 184);

 // Text Colors
 public static final Color TEXT_PRIMARY = new Color(33, 37, 41);
 public static final Color TEXT_SECONDARY = new Color(108, 117, 125);
 public static final Color TEXT_SUCCESS = new Color(40, 167, 69);
 public static final Color TEXT_DANGER = new Color(220, 53, 69);

 // Status Colors
 public static final Color STATUS_SUCCESS = new Color(212, 237, 218);
 public static final Color STATUS_ERROR = new Color(248, 215, 218);
 public static final Color STATUS_WARNING = new Color(255, 243, 205);
 public static final Color STATUS_INFO = new Color(209, 236, 241);

 // Icon Colors
 public static final String ICON_COLLECTION = "#4A90E2";
 public static final String ICON_HISTORY = "#F5A623";
 public static final String ICON_GET = "#28A745";
 public static final String ICON_POST = "#007BFF";
 public static final String ICON_PUT = "#FFC107";
 public static final String ICON_DELETE = "#DC3545";
 public static final String ICON_PATCH = "#6F42C1";
 public static final String ICON_HEAD = "#17A2B8";
 public static final String ICON_OPTIONS = "#6C757D";
 public static final String ICON_DEFAULT = "#495057";

 // Helper method to get method icon with color
 public static String getMethodIcon(String method) {
 switch (method.toUpperCase()) {
 case "GET": return "<font color='" + ICON_GET + "'>🔽</font>";
 case "POST": return "<font color='" + ICON_POST + "'>📤</font>";
 case "PUT": return "<font color='" + ICON_PUT + "'>🔄</font>";
 case "DELETE": return "<font color='" + ICON_DELETE + "'>🗑️</font>";
 case "PATCH": return "<font color='" + ICON_PATCH + "'>✏️</font>";
 case "HEAD": return "<font color='" + ICON_HEAD + "'>ℹ️</font>";
 case "OPTIONS": return "<font color='" + ICON_OPTIONS + "'>⚙️</font>";
 default: return "<font color='" + ICON_DEFAULT + "'>🔘</font>";
 }
 }
}
