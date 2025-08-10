package com.rct.util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileManager {
  private static final String APP_DIR = "restclienttool-data";

 public static String getAppDataDirectory() {
 String homeDir = System.getProperty("user.home");
 return Paths.get(homeDir, APP_DIR).toString();
 }

 public static String getCollectionsFile() {
 return Paths.get(getAppDataDirectory(), "collections.json").toString();
 }

 public static String getHistoryFile() {
 return Paths.get(getAppDataDirectory(), "history.json").toString();
 }

 public static String getSessionsFile() {
 return Paths.get(getAppDataDirectory(), "sessions.json").toString();
 }

 public static String getEnvironmentsFile() {
 return Paths.get(getAppDataDirectory(), "environments.json").toString();
 }

 public static String getLogsFile() {
 return Paths.get(getAppDataDirectory(), "app.log").toString();
 }

 public static void ensureAppDataDirectory() {
 File dir = new File(getAppDataDirectory());
 if (!dir.exists()) {
 dir.mkdirs();
 }
 }

 public static String getFileSeparator() {
 return File.separator;
 }
}