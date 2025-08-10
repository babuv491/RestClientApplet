package com.rct.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.*;

public class BulkRequestParser {

 public static class BulkRequest {
 private String name;
 private String method;
 private String url;
 private Map<String, String> headers = new HashMap<>();
 private Map<String, String> queryParams = new HashMap<>();
 private String body;
 private String authType;
 private String authUsername;
 private String authPassword;
 private String authToken;
 private String authKey;
 private String authValue;
 private Map<String, String> cookies = new HashMap<>();

 // Getters and setters
 public String getName() { return name; }
 public void setName(String name) { this.name = name; }
 public String getMethod() { return method; }
 public void setMethod(String method) { this.method = method; }
  public String getUrl() { return url; }
 public void setUrl(String url) { this.url = url; }
 public Map<String, String> getHeaders() { return headers; }
 public void setHeaders(Map<String, String> headers) { this.headers = headers; }
 public Map<String, String> getQueryParams() { return queryParams; }
 public void setQueryParams(Map<String, String> queryParams) { this.queryParams = queryParams; }
 public String getBody() { return body; }
 public void setBody(String body) { this.body = body; }
 public String getAuthType() { return authType; }
 public void setAuthType(String authType) { this.authType = authType; }
 public String getAuthUsername() { return authUsername; }
 public void setAuthUsername(String authUsername) { this.authUsername = authUsername; }
 public String getAuthPassword() { return authPassword; }
 public void setAuthPassword(String authPassword) { this.authPassword = authPassword; }
 public String getAuthToken() { return authToken; }
 public void setAuthToken(String authToken) { this.authToken = authToken; }
 public String getAuthKey() { return authKey; }
 public void setAuthKey(String authKey) { this.authKey = authKey; }
 public String getAuthValue() { return authValue; }
 public void setAuthValue(String authValue) { this.authValue = authValue; }
 public Map<String, String> getCookies() { return cookies; }
 public void setCookies(Map<String, String> cookies) { this.cookies = cookies; }
 }

 public static List<BulkRequest> parseFromFile(File file) throws Exception {
 String fileName = file.getName().toLowerCase();
 if (fileName.endsWith(".json")) {
 return parseFromJson(file);
 } else if (fileName.endsWith(".csv")) {
 return parseFromCsv(file);
 } else {
 throw new IllegalArgumentException("Unsupported file format. Only JSON and CSV are supported.");
 }
 }

 private static List<BulkRequest> parseFromJson(File file) throws Exception {
 ObjectMapper mapper = new ObjectMapper();
 JsonNode root = mapper.readTree(file);
 List<BulkRequest> requests = new ArrayList<>();

 if (root.isArray()) {
 for (JsonNode requestNode : root) {
 requests.add(parseJsonRequest(requestNode));
 }
 } else if (root.has("requests")) {
 for (JsonNode requestNode : root.get("requests")) {
 requests.add(parseJsonRequest(requestNode));
 }
 } else {
 requests.add(parseJsonRequest(root));
 }

 return requests;
 }

 private static BulkRequest parseJsonRequest(JsonNode node) {
 BulkRequest request = new BulkRequest();

 request.setName(getStringValue(node, "name", "Request"));
 request.setMethod(getStringValue(node, "method", "GET"));
 request.setUrl(getStringValue(node, "url", ""));
 request.setBody(getStringValue(node, "body", ""));

 // Parse headers
 if (node.has("headers")) {
 JsonNode headers = node.get("headers");
 if (headers.isObject()) {
 headers.fields().forEachRemaining(entry ->
 request.getHeaders().put(entry.getKey(), entry.getValue().asText()));
 }
 }

 // Parse query parameters
 if (node.has("queryParams") || node.has("params")) {
  JsonNode params = node.has("queryParams") ? node.get("queryParams") : node.get("params");
 if (params.isObject()) {
 params.fields().forEachRemaining(entry ->
 request.getQueryParams().put(entry.getKey(), entry.getValue().asText()));
 }
 }

 // Parse authentication
 if (node.has("auth")) {
 JsonNode auth = node.get("auth");
 request.setAuthType(getStringValue(auth, "type", "none"));
 request.setAuthUsername(getStringValue(auth, "username", ""));
 request.setAuthPassword(getStringValue(auth, "password", ""));
 request.setAuthToken(getStringValue(auth, "token", ""));
 request.setAuthKey(getStringValue(auth, "key", ""));
 request.setAuthValue(getStringValue(auth, "value", ""));
 }

 // Parse cookies
 if (node.has("cookies")) {
 JsonNode cookies = node.get("cookies");
 if (cookies.isObject()) {
 cookies.fields().forEachRemaining(entry ->
 request.getCookies().put(entry.getKey(), entry.getValue().asText()));
 }
 }

 return request;
 }

 private static List<BulkRequest> parseFromCsv(File file) throws Exception {
 List<BulkRequest> requests = new ArrayList<>();

 try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
 String headerLine = reader.readLine();
 if (headerLine == null) return requests;

 String[] headers = headerLine.split(",");
 Map<String, Integer> columnMap = new HashMap<>();

 for (int i = 0; i < headers.length; i++) {
 columnMap.put(headers[i].trim().toLowerCase(), i);
 }

 String line;
 while ((line = reader.readLine()) != null) {
 String[] values = parseCsvLine(line);
 if (values.length > 0) {
 requests.add(parseCsvRequest(values, columnMap));
 }
 }
 }

 return requests;
 }

 private static BulkRequest parseCsvRequest(String[] values, Map<String, Integer> columnMap) {
 BulkRequest request = new BulkRequest();

 request.setName(getCsvValue(values, columnMap, "name", "Request"));
 request.setMethod(getCsvValue(values, columnMap, "method", "GET"));
 request.setUrl(getCsvValue(values, columnMap, "url", ""));
 request.setBody(getCsvValue(values, columnMap, "body", ""));

 // Parse headers from CSV (JSON format in cell)
 String headersJson = getCsvValue(values, columnMap, "headers", "{}");
 parseJsonMap(headersJson, request.getHeaders());

 // Parse query parameters
 String paramsJson = getCsvValue(values, columnMap, "queryparams", "{}");
 if (paramsJson.isEmpty()) {
 paramsJson = getCsvValue(values, columnMap, "params", "{}");
 }
 parseJsonMap(paramsJson, request.getQueryParams());

 // Parse authentication
 request.setAuthType(getCsvValue(values, columnMap, "authtype", "none"));
 request.setAuthUsername(getCsvValue(values, columnMap, "authusername", ""));
 request.setAuthPassword(getCsvValue(values, columnMap, "authpassword", ""));
 request.setAuthToken(getCsvValue(values, columnMap, "authtoken", ""));
 request.setAuthKey(getCsvValue(values, columnMap, "authkey", ""));
 request.setAuthValue(getCsvValue(values, columnMap, "authvalue", ""));

 // Parse cookies
 String cookiesJson = getCsvValue(values, columnMap, "cookies", "{}");
 parseJsonMap(cookiesJson, request.getCookies());

 return request;
 }

 private static String[] parseCsvLine(String line) {
 List<String> result = new ArrayList<>();
 boolean inQuotes = false;
 StringBuilder current = new StringBuilder();

 for (int i = 0; i < line.length(); i++) {
 char c = line.charAt(i);
 if (c == '"') {
 inQuotes = !inQuotes;
 } else if (c == ',' && !inQuotes) {
  result.add(current.toString().trim());
 current = new StringBuilder();
 } else {
 current.append(c);
 }
 }
 result.add(current.toString().trim());

 return result.toArray(new String[0]);
 }

 private static String getStringValue(JsonNode node, String key, String defaultValue) {
 return node.has(key) ? node.get(key).asText() : defaultValue;
 }

 private static String getCsvValue(String[] values, Map<String, Integer> columnMap, String column, String defaultValue) {
 Integer index = columnMap.get(column);
 if (index != null && index < values.length) {
 String value = values[index].trim();
 if (value.startsWith("\"") && value.endsWith("\"")) {
 value = value.substring(1, value.length() - 1);
 }
 return value.isEmpty() ? defaultValue : value;
 }
 return defaultValue;
 }

 private static void parseJsonMap(String jsonStr, Map<String, String> map) {
 if (jsonStr == null || jsonStr.trim().isEmpty() || jsonStr.equals("{}")) return;

 try {
 ObjectMapper mapper = new ObjectMapper();
 JsonNode node = mapper.readTree(jsonStr);
 if (node.isObject()) {
 node.fields().forEachRemaining(entry ->
 map.put(entry.getKey(), entry.getValue().asText()));
 }
 } catch (Exception e) {
 // If JSON parsing fails, try simple key=value format
 String[] pairs = jsonStr.split(";");
 for (String pair : pairs) {
 String[] kv = pair.split("=", 2);
 if (kv.length == 2) {
 map.put(kv[0].trim(), kv[1].trim());
 }
 }
 }
 }
}