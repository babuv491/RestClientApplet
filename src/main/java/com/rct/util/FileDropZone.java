package com.rct.util;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;

public class FileDropZone extends JPanel {
 private String message = "Drop files here to import";
 private boolean dragOver = false;

 public FileDropZone(FileDropListener listener) {
 setPreferredSize(new Dimension(200, 100));
 setBorder(BorderFactory.createDashedBorder(Color.GRAY, 2, 5, 5, false));

 new DropTarget(this, new DropTargetAdapter() {
 @Override
 public void dragEnter(DropTargetDragEvent dtde) {
 dragOver = true;
 repaint();
 }

 @Override
 public void dragExit(DropTargetEvent dte) {
 dragOver = false;
 repaint();
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
 dragOver = false;
 repaint();
 }
 });
 }

 @Override
 protected void paintComponent(Graphics g) {
 super.paintComponent(g);
 g.setColor(dragOver ? Color.BLUE : Color.GRAY);
 g.drawString(message, 10, getHeight()/2);
 }

 public interface FileDropListener {
 void onFilesDropped(List<File> files);
 }
}
