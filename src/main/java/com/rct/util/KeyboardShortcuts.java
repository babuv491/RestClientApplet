package com.rct.util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

public class KeyboardShortcuts{
 private static final Map<String, ShortcutInfo> shortcuts = new HashMap<>();

 public static class ShortcutInfo{
 public final String key;
 public final String description;
 public final Runnable action;

 public ShortcutInfo(String key, String description, Runnable action) {
 this.key= key;
 this.description= description;
 this.action= action;
 }
 }

 public static void setupGlobalShortcuts(JComponent component, GlobalShortcutHandler handler) {
 InputMap inputMap= component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
 ActionMap actionMap= component.getActionMap();

 // File operations
 addShortcut(inputMap, actionMap, "ctrl N", "newRequest", "New Request", handler::newRequest);
 addShortcut(inputMap, actionMap, "ctrl O", "openRequest", "Open Request", handler::openRequest);
 addShortcut(inputMap, actionMap, "ctrl S", "saveRequest", "Save Request", handler::saveRequest);
 addShortcut(inputMap, actionMap, "ctrl shift S", "saveAs", "Save As", handler::saveAs);
 addShortcut(inputMap, actionMap, "ctrl I", "importCurl", "Import cURL", handler::importCurl);
 addShortcut(inputMap, actionMap, "ctrl E", "exportCollection", "Export Collection", handler::exportCollection);

 // Tab operations
 addShortcut(inputMap, actionMap, "ctrl T", "newTab", "New Tab", handler::newTab);
 addShortcut(inputMap, actionMap, "ctrl W", "closeTab", "Close Tab", handler::closeTab);
 addShortcut(inputMap, actionMap, "ctrl shift W", "closeAllTabs", "Close All Tabs", handler::closeAllTabs);
 addShortcut(inputMap, actionMap, "ctrl D", "duplicateTab", "Duplicate Tab", handler::duplicateTab);
 addShortcut(inputMap, actionMap, "ctrl TAB", "nextTab", "Next Tab", handler::nextTab);
 addShortcut(inputMap, actionMap, "ctrl shift TAB", "prevTab", "Previous Tab", handler::prevTab);
 addShortcut(inputMap, actionMap, "ctrl 1", "tab1", "Go to Tab 1", () -> handler.goToTab(0));
 addShortcut(inputMap, actionMap, "ctrl 2", "tab2", "Go to Tab 2", () -> handler.goToTab(1));
 addShortcut(inputMap, actionMap, "ctrl 3", "tab3", "Go to Tab 3", () -> handler.goToTab(2));
 addShortcut(inputMap, actionMap, "ctrl 4", "tab4", "Go to Tab 4", () -> handler.goToTab(3));
 addShortcut(inputMap, actionMap, "ctrl 5", "tab5", "Go to Tab 5", () -> handler.goToTab(4));

 // Request operations
 addShortcut(inputMap, actionMap, "ENTER", "sendRequest", "Send Request", handler::sendRequest);
 addShortcut(inputMap, actionMap, "ctrl ENTER", "sendRequestNewTab", "Send in New Tab", handler::sendRequestNewTab);
 addShortcut(inputMap, actionMap, "F5", "refreshRequest", "Refresh/Resend", handler::refreshRequest);
 addShortcut(inputMap, actionMap, "ESCAPE", "cancelRequest", "Cancel Request", handler::cancelRequest);

 // Navigation
 addShortcut(inputMap, actionMap, "ctrl 1", "focusUrl", "Focus URL", handler::focusUrl);
 addShortcut(inputMap, actionMap, "ctrl 2", "focusParams", "Focus Params", handler::focusParams);
 addShortcut(inputMap, actionMap, "ctrl 3", "focusHeaders", "Focus Headers", handler::focusHeaders);
 addShortcut(inputMap, actionMap, "ctrl 4", "focusBody", "Focus Body", handler::focusBody);
 addShortcut(inputMap, actionMap, "ctrl 5", "focusAuth", "Focus Auth", handler::focusAuth);

 // Tools
 addShortcut(inputMap, actionMap, "ctrl B", "bulkExecution", "Bulk Execution", handler::bulkExecution);
 addShortcut(inputMap, actionMap, "ctrl P", "performanceTest", "Performance Test", handler::performanceTest);
 addShortcut(inputMap, actionMap, "ctrl G", "codeGeneration", "Code Generation", handler::codeGeneration);
 addShortcut(inputMap, actionMap, "ctrl shift T", "testGeneration", "Test Generation", handler::testGeneration);
 addShortcut(inputMap, actionMap, "ctrl shift D", "documentation", "API Documentation", handler::documentation);
 addShortcut(inputMap, actionMap, "F12", "console", "Console Logs", handler::showConsole);

 // JSON operations
 addShortcut(inputMap, actionMap, "ctrl shift F", "formatJson", "Format JSON", handler::formatJson);
 addShortcut(inputMap, actionMap, "ctrl shift M", "minifyJson", "Minify JSON", handler::minifyJson);
 addShortcut(inputMap, actionMap, "ctrl shift V", "validateJson", "Validate JSON", handler::validateJson);

 // Copy operations
 addShortcut(inputMap, actionMap, "ctrl shift C", "copyAsCurl", "Copy as cURL", handler::copyAsCurl);
 addShortcut(inputMap, actionMap, "ctrl shift R", "copyResponse", "Copy Response", handler::copyResponse);

 // View operations
 addShortcut(inputMap, actionMap, "F11", "toggleFullscreen", "Toggle Fullscreen", handler::toggleFullscreen);
 addShortcut(inputMap, actionMap, "ctrl PLUS", "zoomIn", "Zoom In", handler::zoomIn);
 addShortcut(inputMap, actionMap, "ctrl MINUS", "zoomOut", "Zoom Out", handler::zoomOut);
 addShortcut(inputMap, actionMap, "ctrl 0", "resetZoom", "Reset Zoom", handler::resetZoom);

 // Help
 addShortcut(inputMap, actionMap, "F1", "showHelp", "Show Help", handler::showHelp);
 addShortcut(inputMap, actionMap, "ctrl shift K", "showShortcuts", "Show Shortcuts", handler::showShortcuts);
 }

 private static void addShortcut(InputMap inputMap, ActionMap actionMap, String keyStroke,
 String actionKey, String description, Runnable action) {
 KeyStroke key = KeyStroke.getKeyStroke(keyStroke);
 inputMap.put(key, actionKey);
 actionMap.put(actionKey, new AbstractAction() {
 @Override
 public void actionPerformed(ActionEvent e) {
 action.run();
 }
 });
 shortcuts.put(actionKey, new ShortcutInfo(keyStroke, description, action));
 }

 public static Map<String, ShortcutInfo> getAllShortcuts() {
 return new HashMap<>(shortcuts);
 }

 public interface GlobalShortcutHandler{
 // File operations
 void newRequest();
 void openRequest();
 void saveRequest();
 void saveAs();
 void importCurl();
 void exportCollection();

 // Tab operations
 void newTab();
 void closeTab();
 void closeAllTabs();
 void duplicateTab();
 void nextTab();
 void prevTab();
 void goToTab(int index);

 // Request operations
 void sendRequest();
 void sendRequestNewTab();
 void refreshRequest();
 void cancelRequest();

 // Navigation
 void focusUrl();
 void focusParams();
 void focusHeaders();
 void focusBody();
 void focusAuth();

 // Tools
 void bulkExecution();
 void performanceTest();
 void codeGeneration();
 void testGeneration();
 void documentation();
 void showConsole();

 // JSON operations
 void formatJson();
 void minifyJson();
 void validateJson();

 // Copy operations
 void copyAsCurl();
 void copyResponse();

 // View operations
 void toggleFullscreen();
 void zoomIn();
 void zoomOut();
 void resetZoom();

 // Help
 void showHelp();
 void showShortcuts();
 }
}
