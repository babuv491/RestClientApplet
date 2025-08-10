package com.rct.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SessionManager {
 private ObjectMapper objectMapper;

 public static class TabSession {
 private String name;
 private String method;
 private String url;
 private String headers;
 private String body;
 private String params;
 private String savedCollectionName;
  private String savedRequestName;
 private boolean isModified;

 public TabSession() {}

 public TabSession(String name, String method, String url, String headers, String body, String params,
 String savedCollectionName, String savedRequestName, boolean isModified) {
 this.name = name;
 this.method = method;
 this.url = url;
 this.headers = headers;
 this.body = body;
 this.params = params;
 this.savedCollectionName = savedCollectionName;
 this.savedRequestName = savedRequestName;
 this.isModified = isModified;
 }

 // Getters and setters
 public String getName() { return name; }
 public void setName(String name) { this.name = name; }
 public String getMethod() { return method; }
 public void setMethod(String method) { this.method = method; }
 public String getUrl() { return url; }
 public void setUrl(String url) { this.url = url; }
 public String getHeaders() { return headers; }
 public void setHeaders(String headers) { this.headers = headers; }
 public String getBody() { return body; }
 public void setBody(String body) { this.body = body; }
 public String getParams() { return params; }
 public void setParams(String params) { this.params = params; }
 public String getSavedCollectionName() { return savedCollectionName; }
 public void setSavedCollectionName(String savedCollectionName) { this.savedCollectionName = savedCollectionName; }
 public String getSavedRequestName() { return savedRequestName; }
 public void setSavedRequestName(String savedRequestName) { this.savedRequestName = savedRequestName; }
 public boolean isModified() { return isModified; }
 public void setModified(boolean modified) { isModified = modified; }
 }

 public SessionManager() {
 this.objectMapper = new ObjectMapper();
 }

 public void saveSessions(List<TabSession> sessions) {
 try {
 File file = getSessionFile();
 objectMapper.writeValue(file, sessions);
 } catch (Exception e) {
 System.err.println("Error saving sessions: " + e.getMessage());
 }
 }

 public List<TabSession> loadSessions() {
 try {
 File file = getSessionFile();
 if (file.exists()) {
 TabSession[] sessions = objectMapper.readValue(file, TabSession[].class);
 List<TabSession> sessionList = new ArrayList<>();
 for (TabSession session : sessions) {
 sessionList.add(session);
 }
 return sessionList;
 }
 } catch (Exception e) {
 System.err.println("Error loading sessions: " + e.getMessage());
 }
 return new ArrayList<>();
 }

 private File getSessionFile() {
 return new File(com.rct.util.FileManager.getSessionsFile());
 }
}
