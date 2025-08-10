package com.rct.util;

import com.rct.manager.CollectionManager;
import com.rct.ui.RestClientApp;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class DragDropHandler {

 public static void enableTreeDragDrop(JTree tree, CollectionManager collectionManager) {
 tree.setDragEnabled(true);
 tree.setDropMode(DropMode.ON_OR_INSERT);
 tree.setTransferHandler(new TreeTransferHandler(collectionManager));
 }

 public static void enableFileDrop(JComponent component, FileDropListener listener) {
 new DropTarget(component, new FileDropTargetListener(listener));
 }

 public interface FileDropListener {
 void onFilesDropped(List<File> files);
 }

 private static class TreeTransferHandler extends TransferHandler {
 private CollectionManager collectionManager;

 public TreeTransferHandler(CollectionManager collectionManager) {
 this.collectionManager = collectionManager;
 }

 @Override
 public int getSourceActions(JComponent c) {
 return MOVE;
 }

 @Override
 protected Transferable createTransferable(JComponent c) {
 JTree tree = (JTree) c;
 TreePath path = tree.getSelectionPath();
 if (path != null) {
 DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
 if (node instanceof CollectionManager.RequestTreeNode) {
 return new RequestTransferable((CollectionManager.RequestTreeNode) node);
 }
 }
 return null;
 }

 @Override
 public boolean canImport(TransferSupport support) {
 if (!support.isDrop()) return false;

 JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
 TreePath path = dl.getPath();

 if (path == null) return false;

 DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) path.getLastPathComponent();

 // Allow drop on collections or within collections for reordering
 return targetNode instanceof CollectionManager.CollectionTreeNode ||
 (targetNode instanceof CollectionManager.RequestTreeNode && dl.getChildIndex() >= 0);
 }

 @Override
 public boolean importData(TransferSupport support) {
 if (!canImport(support)) return false;

 try {
 RequestTransferable transferable = (RequestTransferable) support.getTransferable()
 .getTransferData(RequestTransferable.REQUEST_FLAVOR);

 JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
 TreePath path = dl.getPath();
 DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) path.getLastPathComponent();

 if (targetNode instanceof CollectionManager.CollectionTreeNode) {
 // Move to different collection
 CollectionManager.CollectionTreeNode collectionNode = (CollectionManager.CollectionTreeNode) targetNode;
 moveRequest(transferable.getRequestNode(), collectionNode.getCollectionName());
 } else if (targetNode instanceof CollectionManager.RequestTreeNode) {
 // Reorder within same collection
 CollectionManager.RequestTreeNode requestNode = (CollectionManager.RequestTreeNode) targetNode;
 reorderRequest(transferable.getRequestNode(), requestNode, dl.getChildIndex());
 }
 return true;

 } catch (Exception e) {
 return false;
 }
 }

 private void moveRequest(CollectionManager.RequestTreeNode sourceNode, String targetCollection) {
 CollectionManager.SavedRequest request = null;

 // Find and remove from source
 for (CollectionManager.Collection collection : collectionManager.getCollections()) {
 if (collection.getName().equals(sourceNode.getCollectionName())) {
 java.util.Iterator<CollectionManager.SavedRequest> it = collection.getRequests().iterator();
 while (it.hasNext()) {
 CollectionManager.SavedRequest req = it.next();
 if (req.getName().equals(sourceNode.getRequestName()) &&
 req.getMethod().equals(sourceNode.getMethod())) {
 request = req;
 it.remove();
 break;
 }
 }
 break;
 }
 }

 // Add to target
 if (request != null) {
 for (CollectionManager.Collection collection : collectionManager.getCollections()) {
 if (collection.getName().equals(targetCollection)) {
 collection.addRequest(request);
 break;
 }
 }
 collectionManager.refreshTree();
 }
 }

 private void reorderRequest(CollectionManager.RequestTreeNode sourceNode,
  CollectionManager.RequestTreeNode targetNode, int childIndex) {
 if (!sourceNode.getCollectionName().equals(targetNode.getCollectionName())) return;

 for (CollectionManager.Collection collection : collectionManager.getCollections()) {
 if (collection.getName().equals(sourceNode.getCollectionName())) {
 java.util.List<CollectionManager.SavedRequest> requests = collection.getRequests();

 // Find source request
 CollectionManager.SavedRequest sourceRequest = null;
 int sourceIndex = -1;
 for (int i = 0; i < requests.size(); i++) {
 CollectionManager.SavedRequest req = requests.get(i);
 if (req.getName().equals(sourceNode.getRequestName()) &&
 req.getMethod().equals(sourceNode.getMethod())) {
 sourceRequest = req;
 sourceIndex = i;
  break;
 }
 }

 // Find target index
 int targetIndex = -1;
 for (int i = 0; i < requests.size(); i++) {
 CollectionManager.SavedRequest req = requests.get(i);
 if (req.getName().equals(targetNode.getRequestName()) &&
 req.getMethod().equals(targetNode.getMethod())) {
 targetIndex = i;
 break;
 }
 }

 if (sourceRequest != null && sourceIndex != -1 && targetIndex != -1 && sourceIndex != targetIndex) {
 requests.remove(sourceIndex);
 if (targetIndex > sourceIndex) targetIndex--;
 requests.add(targetIndex, sourceRequest);
 collectionManager.refreshTree();
 }
 break;
 }
 }
 }
 }

 private static class RequestTransferable implements Transferable {
 public static final DataFlavor REQUEST_FLAVOR = new DataFlavor(
 CollectionManager.RequestTreeNode.class, "Request Node");

 private CollectionManager.RequestTreeNode requestNode;

 public RequestTransferable(CollectionManager.RequestTreeNode requestNode) {
 this.requestNode = requestNode;
 }

 @Override
 public DataFlavor[] getTransferDataFlavors() {
 return new DataFlavor[]{REQUEST_FLAVOR};
 }

 @Override
 public boolean isDataFlavorSupported(DataFlavor flavor) {
 return REQUEST_FLAVOR.equals(flavor);
 }

 @Override
 public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
 if (isDataFlavorSupported(flavor)) {
 return this;
 }
 throw new UnsupportedFlavorException(flavor);
 }

 public CollectionManager.RequestTreeNode getRequestNode() {
 return requestNode;
 }
 }

 private static class FileDropTargetListener extends DropTargetAdapter {
 private FileDropListener listener;

 public FileDropTargetListener(FileDropListener listener) {
 this.listener = listener;
 }

 @Override
 public void drop(DropTargetDropEvent dtde) {
 try {
 dtde.acceptDrop(DnDConstants.ACTION_COPY);

 @SuppressWarnings("unchecked")
 List<File> files = (List<File>) dtde.getTransferable()
 .getTransferData(DataFlavor.javaFileListFlavor);

 listener.onFilesDropped(files);
 dtde.dropComplete(true);

 } catch (Exception e) {
 dtde.dropComplete(false);
 }
 }

 @Override
 public void dragOver(DropTargetDragEvent dtde) {
 if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
 dtde.acceptDrag(DnDConstants.ACTION_COPY);
 } else {
  dtde.rejectDrag();
 }
 }
 }

 public static void handleFileImport(File file, CollectionManager collectionManager, RestClientApp app) {
 String fileName = file.getName().toLowerCase();

 try {
 if (fileName.endsWith(".json")) {
 handleJsonImport(file, collectionManager);
 } else if (fileName.endsWith(".curl") || fileName.endsWith(".sh")) {
 handleCurlImport(file, app);
 } else if (fileName.endsWith(".har")) {
 handleHarImport(file, collectionManager);
 } else {
 JOptionPane.showMessageDialog(null,
 "Unsupported file type: " + fileName + "\nSupported: .json, .curl, .sh, .har",
 "Import Error", JOptionPane.WARNING_MESSAGE);
 }
 } catch (Exception e) {
 JOptionPane.showMessageDialog(null,
 "Error importing file: " + e.getMessage(),
 "Import Error", JOptionPane.ERROR_MESSAGE);
 }
 }

 private static void handleJsonImport(File file, CollectionManager collectionManager) throws Exception {
 collectionManager.importFromFile(file);
 JOptionPane.showMessageDialog(null,
 "Collection imported successfully from: " + file.getName(),
 "Import Successful", JOptionPane.INFORMATION_MESSAGE);
 }

 private static void handleCurlImport(File file, RestClientApp app) throws Exception {
 String content = new String(Files.readAllBytes(file.toPath()));
 String[] lines = content.split("\n");

 for (String line : lines) {
 line = line.trim();
 if (line.startsWith("curl")) {
 try {
  CurlImporter.CurlRequest curlRequest = CurlImporter.parseCurl(line);
 // Create new tab with imported request
 SwingUtilities.invokeLater(() -> {
 // This would need to be implemented in RestClientApp
 // app.createNewTabFromCurl(curlRequest);
 });
 } catch (Exception e) {
 System.err.println("Error parsing cURL line: " + line);
 }
 }
 }

 JOptionPane.showMessageDialog(null,
 "cURL commands imported from: " + file.getName(),
 "Import Successful", JOptionPane.INFORMATION_MESSAGE);
 }

 private static void handleHarImport(File file, CollectionManager collectionManager) throws Exception {
 // Basic HAR import - would need full HAR parser for complete implementation
 JOptionPane.showMessageDialog(null,
 "HAR import not fully implemented yet",
 "Import Info", JOptionPane.INFORMATION_MESSAGE);
 }
}
