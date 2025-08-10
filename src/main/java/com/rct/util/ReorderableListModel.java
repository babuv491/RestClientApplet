package com.rct.util;

import javax.swing.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.util.ArrayList;
import java.util.List;

public class ReorderableListModel<T> extends DefaultListModel<T> {

 public static <T> void enableReordering(JList<T> list) {
 list.setDragEnabled(true);
 list.setDropMode(DropMode.INSERT);
 list.setTransferHandler(new ListReorderTransferHandler<>());
 }

 private static class ListReorderTransferHandler<T> extends TransferHandler {
 private static final DataFlavor ITEM_FLAVOR = new DataFlavor(Integer.class, "List Item Index");

 @Override
 public int getSourceActions(JComponent c) {
 return MOVE;
 }

 @Override
 protected Transferable createTransferable(JComponent c) {
 @SuppressWarnings("unchecked")
 JList<T> list = (JList<T>) c;
 int index = list.getSelectedIndex();
 return index >= 0 ? new IndexTransferable(index) : null;
 }

 @Override
 public boolean canImport(TransferSupport support) {
 return support.isDrop() && support.isDataFlavorSupported(ITEM_FLAVOR);
 }
 
 @Override
 @SuppressWarnings("unchecked")
 public boolean importData(TransferSupport support) {
 if (!canImport(support)) return false;

 try {
 JList<T> list = (JList<T>) support.getComponent();
 DefaultListModel<T> model = (DefaultListModel<T>) list.getModel();

 int sourceIndex = (Integer) support.getTransferable().getTransferData(ITEM_FLAVOR);
 JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
 int targetIndex = dl.getIndex();

 if (sourceIndex == targetIndex || targetIndex < 0) return false;

 T item = model.getElementAt(sourceIndex);
 model.removeElementAt(sourceIndex);

 if (targetIndex > sourceIndex) targetIndex--;
 model.insertElementAt(item, targetIndex);

 list.setSelectedIndex(targetIndex);
 return true;

 } catch (Exception e) {
 return false;
 }
 }

 @Override
 protected void exportDone(JComponent source, Transferable data, int action) {
 // Cleanup if needed
 }
 }

 private static class IndexTransferable implements Transferable {
 private final int index;

 public IndexTransferable(int index) {
 this.index = index;
 }

 @Override
 public DataFlavor[] getTransferDataFlavors() {
 return new DataFlavor[]{ListReorderTransferHandler.ITEM_FLAVOR};
 }

 @Override
 public boolean isDataFlavorSupported(DataFlavor flavor) {
 return ListReorderTransferHandler.ITEM_FLAVOR.equals(flavor);
 }

 @Override
 public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
 if (isDataFlavorSupported(flavor)) {
 return index;
 }
 throw new UnsupportedFlavorException(flavor);
 }
 }
}