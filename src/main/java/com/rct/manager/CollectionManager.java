package com.rct.manager;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CollectionManager {
 private List<Collection> collections;
 private JTree collectionsTree;
 private DefaultTreeModel treeModel;
 private ObjectMapper objectMapper;

 public static class Collection {
 private String name;
 private List<SavedRequest> requests;

 public Collection() { this.requests = new ArrayList<>(); }
 public Collection(String name) { this.name = name; this.requests = new ArrayList<>(); }

 public String getName() { return name; }
 public void setName(String name) { this.name = name; }

 public List<SavedRequest> getRequests() { return requests; }
 public void setRequests(List<SavedRequest> requests) { this.requests = requests; }

 public void addRequest(SavedRequest request) { this.requests.add(request); }
 }

 public static class SavedRequest {
 private String name;
 private String method;
 private String url;
 private String headers;
 private String body;
 private String params;

 public SavedRequest() {}
 public SavedRequest(String name, String method, String url, String headers, String body) {
 this.name = name; this.method = method; this.url = url;
  this.headers = headers; this.body = body; this.params = "";
 }
 public SavedRequest(String name, String method, String url, String headers, String body, String params) {
 this.name = name; this.method = method; this.url = url;
 this.headers = headers; this.body = body; this.params = params;
 }

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
 }

 public CollectionManager() {
 collections = new ArrayList<>();
 objectMapper = new ObjectMapper();
 initializeTree();
 loadCollections();
 }

 private void initializeTree() {
 DefaultMutableTreeNode root = new DefaultMutableTreeNode("Collections");
 treeModel = new DefaultTreeModel(root);
 collectionsTree = new JTree(treeModel);
 collectionsTree.setRootVisible(false);
 refreshTree();
 }

 public JTree getCollectionsTree() { return collectionsTree; }
 public List<Collection> getCollections() { return collections; }

 public void addCollection(Collection collection) {
 collections.add(collection);
 refreshTree();
 saveCollections();
 }

 public void removeCollection(String name) {
 collections.removeIf(c -> c.getName().equals(name));
 refreshTree();
 saveCollections();
 }

 public void removeRequest(String collectionName, String requestName, String method) {
 for (Collection collection : collections) {
 if (collection.getName().equals(collectionName)) {
 collection.getRequests().removeIf(r -> r.getName().equals(requestName) && r.getMethod().equals(method));
 refreshTree();
 saveCollections();
 break;
 }
 }
 }

 public void refreshTree() {
 SwingUtilities.invokeLater(() -> {
 DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
 root.removeAllChildren();

 for (Collection collection : collections) {
 CollectionTreeNode collectionNode = new CollectionTreeNode(
 "<html><font color='" + com.rct.util.UITheme.ICON_COLLECTION + "'>📁</font> " + collection.getName() + "</html>",
 collection.getName(), null, null);
 for (SavedRequest request : collection.getRequests()) {
 String methodIcon = com.rct.util.UITheme.getMethodIcon(request.getMethod());
 RequestTreeNode requestNode = new RequestTreeNode(
 "<html>" + methodIcon + " " + request.getMethod() + " " + request.getName() + "</html>",
 collection.getName(), request.getMethod(), request.getName());
 collectionNode.add(requestNode);
 }
 root.add(collectionNode);
 }

 treeModel.reload();
 expandAllNodes();
 collectionsTree.repaint();
 });
 }

 public static class CollectionTreeNode extends DefaultMutableTreeNode {
 private String collectionName;
 private String method;
 private String requestName;

 public CollectionTreeNode(String displayText, String collectionName, String method, String requestName) {
 super(displayText);
 this.collectionName = collectionName;
 this.method = method;
 this.requestName = requestName;
 }

 public String getCollectionName() { return collectionName; }
 public String getMethod() { return method; }
 public String getRequestName() { return requestName; }
 }

 public static class RequestTreeNode extends DefaultMutableTreeNode {
 private String collectionName;
 private String method;
 private String requestName;

 public RequestTreeNode(String displayText, String collectionName, String method, String requestName) {
 super(displayText);
 this.collectionName = collectionName;
 this.method = method;
 this.requestName = requestName;
 }

 public String getCollectionName() { return collectionName; }
 public String getMethod() { return method; }
 public String getRequestName() { return requestName; }
 }



 private void expandAllNodes() {
 for (int i = 0; i < collectionsTree.getRowCount(); i++) {
 collectionsTree.expandRow(i);
 }
 }

 private File getCollectionsFile() {
 return new File(com.rct.util.FileManager.getCollectionsFile());
 }

 private void loadCollections() {
 try {
 File file = getCollectionsFile();
 com.rct.util.LogManager.getInstance().log("Loading collections from: " + file.getAbsolutePath());
 if (file.exists()) {
 Collection[] loadedCollections = objectMapper.readValue(file, Collection[].class);
 collections.clear();
 for (Collection collection : loadedCollections) {
 collections.add(collection);
 com.rct.util.LogManager.getInstance().log("Loaded collection: " + collection.getName() + " with " + collection.getRequests().size() + " requests");
 for (SavedRequest request : collection.getRequests()) {
 com.rct.util.LogManager.getInstance().log(" - Request: " + request.getName() + " (" + request.getMethod() + ") URL: " + request.getUrl());
 }
 }
 refreshTree();
 com.rct.util.LogManager.getInstance().log("Collections loaded successfully. Total: " + collections.size());
 } else {
 com.rct.util.LogManager.getInstance().log("Collections file does not exist yet.");
 }
 } catch (Exception e) {
 com.rct.util.LogManager.getInstance().log("Error loading collections: " + e.getMessage());
 e.printStackTrace();
 }
 }

 private void saveCollections() {
 try {
 File file = getCollectionsFile();
 objectMapper.writeValue(file, collections);
 } catch (Exception e) {
 com.rct.util.LogManager.getInstance().log("Error saving collections: " + e.getMessage());
 }
 }

 public void exportToFile(File file) throws Exception {
 // Export in wrapper format for compatibility
 java.util.Map<String, Object> wrapper = new java.util.HashMap<>();
 wrapper.put("collections", collections);
 objectMapper.writeValue(file, wrapper);
 com.rct.util.LogManager.getInstance().log("Successfully exported " + collections.size() + " collections to " + file.getName());
 }

 public File getAppDirectory() {
 return new File(com.rct.util.FileManager.getAppDataDirectory());
 }

 public void importFromFile(File file) throws Exception {
 com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(file);
 Collection[] importedCollections;

 if (rootNode.isArray()) {
 // Direct array format
 importedCollections = objectMapper.treeToValue(rootNode, Collection[].class);
 } else if (rootNode.has("collections")) {
 // Wrapper object format
 importedCollections = objectMapper.treeToValue(rootNode.get("collections"), Collection[].class);
 } else {
 throw new IllegalArgumentException("Invalid JSON format. Expected array of collections or object with 'collections' property.");
 }

 int importedCount = 0;
 for (Collection importedCollection : importedCollections) {
 String originalName = importedCollection.getName();
 String uniqueName = getUniqueCollectionName(originalName);
  importedCollection.setName(uniqueName);

 collections.add(importedCollection);
 importedCount++;
 }

 refreshTree();
 saveCollections();

 com.rct.util.LogManager.getInstance().log("Successfully imported " + importedCount + " collections from " + file.getName());
 }

 private String getUniqueCollectionName(String baseName) {
 String uniqueName = baseName;
  int counter = 1;

 while (collectionExists(uniqueName)) {
 uniqueName = baseName + " (" + counter + ")";
 counter++;
 }

 return uniqueName;
 }

 private boolean collectionExists(String name) {
 return collections.stream().anyMatch(c -> c.getName().equals(name));
 }
}
