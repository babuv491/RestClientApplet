package com.rct.manager;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HistoryManager {
 private List<HistoryEntry> history;
 private DefaultListModel<String> listModel;
 private JList<String> historyList;
 private DateTimeFormatter formatter;
 private ObjectMapper objectMapper;

 public static class HistoryEntry {
 private String method;
 private String url;
 private int statusCode;
 private long responseTime;
 private String timestamp;
 private java.util.Map<String, String> headers;
 private String body;

 public HistoryEntry() {
 this.headers = new java.util.HashMap<>();
 this.body = "";
 }

 public HistoryEntry(String method, String url, int statusCode, long responseTime) {
 this();
 this.method = method; this.url = url;
 this.statusCode = statusCode; this.responseTime = responseTime;
 this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
 }

 public HistoryEntry(String method, String url, int statusCode, long responseTime, java.util.Map<String, String> headers, String body) {
  this(method, url, statusCode, responseTime);
 this.headers = headers != null ? new java.util.HashMap<>(headers) : new java.util.HashMap<>();
 this.body = body != null ? body : "";
 }

 public String getMethod() { return method; }
 public void setMethod(String method) { this.method = method; }
 public String getUrl() { return url; }
 public void setUrl(String url) { this.url = url; }
 public int getStatusCode() { return statusCode; }
 public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
 public long getResponseTime() { return responseTime; }
 public void setResponseTime(long responseTime) { this.responseTime = responseTime; }
 public String getTimestamp() { return timestamp; }
 public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
 public java.util.Map<String, String> getHeaders() { return headers; }
 public void setHeaders(java.util.Map<String, String> headers) { this.headers = headers; }
 public String getBody() { return body; }
 public void setBody(String body) { this.body = body; }
 }

 public static class HistoryListCellRenderer extends DefaultListCellRenderer {
 @Override
 public Component getListCellRendererComponent(JList<?> list, Object value, int index,
 boolean isSelected, boolean cellHasFocus) {
 super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

 if (value instanceof String) {
 String text = (String) value;
 if (text.contains("200") || text.contains("201") || text.contains("204")) {
 setForeground(isSelected ? Color.WHITE : new Color(0, 150, 0));
 } else if (text.contains("4") || text.contains("5")) {
 setForeground(isSelected ? Color.WHITE : new Color(200, 0, 0));
 }
 }
 
 return this;
 }
 }

 public HistoryManager() {
 com.rct.util.LogManager.getInstance().log("Creating new HistoryManager instance");
 history = new ArrayList<>();
 listModel = new DefaultListModel<>();
 historyList = new JList<>(listModel);
 historyList.setCellRenderer(new HistoryListCellRenderer());
 formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
 objectMapper = new ObjectMapper();
 loadHistory();
 }

 public void addHistoryEntry(String method, String url, int statusCode, long responseTime) {
 addHistoryEntry(method, url, statusCode, responseTime, null, null);
 }

 public void addHistoryEntry(String method, String url, int statusCode, long responseTime, java.util.Map<String, String> headers, String body) {
 com.rct.util.LogManager.getInstance().log("Adding history entry: " + method + " " + url + " - " + statusCode);

 // Check for duplicate entry (same method, url, status within last 5 seconds)
 if (!history.isEmpty()) {
 HistoryEntry lastEntry = history.get(0);
 if (lastEntry.getMethod().equals(method) &&
 lastEntry.getUrl().equals(url) &&
 lastEntry.getStatusCode() == statusCode) {

 // Check if it's within 5 seconds
 try {
 LocalDateTime lastTime = LocalDateTime.parse(lastEntry.getTimestamp(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
 LocalDateTime now = LocalDateTime.now();
 if (java.time.Duration.between(lastTime, now).getSeconds() < 5) {
 com.rct.util.LogManager.getInstance().log("Duplicate history entry detected, skipping");
 return;
 }
 } catch (Exception e) {
 // If parsing fails, continue with adding the entry
 }
 }
 }

 HistoryEntry entry = new HistoryEntry(method, url, statusCode, responseTime, headers, body);
 history.add(0, entry);

 String displayText = String.format("[%s] %s %s - %d (%dms)",
 LocalDateTime.now().format(formatter), method, url, statusCode, responseTime);

 listModel.add(0, displayText);

 if (history.size() > 100) {
 history.remove(history.size() - 1);
 listModel.remove(listModel.size() - 1);
 }
 saveHistory();
 }

 public void clearHistory() {
 history.clear();
 listModel.clear();
 saveHistory();
 }

 public void removeHistoryEntry(int index) {
 if (index >= 0 && index < history.size()) {
 history.remove(index);
 listModel.remove(index);
 saveHistory();
 }
 }

 public List<HistoryEntry> getHistory() { return history; }
 public JList<String> getHistoryList() { return historyList; }

 private File getHistoryFile() {
 return new File(com.rct.util.FileManager.getHistoryFile());
 }

 private void loadHistory() {
 try {
 File file = getHistoryFile();
 com.rct.util.LogManager.getInstance().log("Loading history from: " + file.getAbsolutePath());
 if (file.exists()) {
 HistoryEntry[] entries = objectMapper.readValue(file, HistoryEntry[].class);
 com.rct.util.LogManager.getInstance().log("Loaded " + entries.length + " history entries from file");
 history.clear();
 listModel.clear();
 for (HistoryEntry entry : entries) {
 history.add(entry);
 String displayText = String.format("[%s] %s %s - %d (%dms)",
 entry.getTimestamp(), entry.getMethod(),
 entry.getUrl(), entry.getStatusCode(), entry.getResponseTime());
 listModel.addElement(displayText);
 }
 } else {
 com.rct.util.LogManager.getInstance().log("History file does not exist yet");
 }
 } catch (Exception e) {
 com.rct.util.LogManager.getInstance().log("Error loading history: " + e.getMessage());
 }
 }

 private void saveHistory() {
 try {
 File file = getHistoryFile();
 objectMapper.writeValue(file, history);
 } catch (Exception e) {
 com.rct.util.LogManager.getInstance().log("Error saving history: " + e.getMessage());
 }
 }
}
