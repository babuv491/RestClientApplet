package com.rct.service;

import com.rct.manager.EnvironmentManager;
import com.rct.model.RestResponse;
import com.rct.util.JsonFormatter;
import com.rct.util.LogManager;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.HashMap;
import java.util.Map;

public class RestClientService {
 private EnvironmentManager environmentManager;

 public RestClientService() {
 this.environmentManager = null;
 }

 public RestClientService(EnvironmentManager environmentManager) {
 this.environmentManager = environmentManager;
 }

 public RestResponse sendRequest(String method, String url, Map<String, String> headers,
 Map<String, String> params, String body) {
 // Resolve environment variables
 url = resolveVariables(url);
 body = resolveVariables(body);

 // Resolve variables in headers
 Map<String, String> resolvedHeaders = new HashMap<>();
 if (headers != null) {
 for (Map.Entry<String, String> entry : headers.entrySet()) {
 resolvedHeaders.put(resolveVariables(entry.getKey()), resolveVariables(entry.getValue()));
 }
 }

 // Resolve variables in params
 Map<String, String> resolvedParams = new HashMap<>();
 if (params != null) {
 for (Map.Entry<String, String> entry : params.entrySet()) {
 resolvedParams.put(resolveVariables(entry.getKey()), resolveVariables(entry.getValue()));
 }
 }

 LogManager logger = LogManager.getInstance();
 logger.log("======== REQUEST DETAILS ========");
 logger.log("Method: " + method);
 logger.log("URL: " + url);

 try {
 RequestSpecification request = RestAssured.given();

 if (resolvedHeaders != null && !resolvedHeaders.isEmpty()) {
 logger.log("Headers (" + resolvedHeaders.size() + "):");
 resolvedHeaders.forEach((k, v) -> logger.log(" " + k + ": " + v));
 request.headers(resolvedHeaders);
 } else {
 logger.log("Headers: None");
 }

 if (resolvedParams != null && !resolvedParams.isEmpty()) {
 logger.log("Query Parameters (" + resolvedParams.size() + "):");
 resolvedParams.forEach((k, v) -> logger.log(" " + k + "=" + v));
  request.queryParams(resolvedParams);
 } else {
 logger.log("Query Parameters: None");
 }

 if (body != null && !body.trim().isEmpty() &&
 (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT") ||
 method.equalsIgnoreCase("PATCH"))) {
 logger.log("Request Body (" + body.length() + " characters):");
 logger.log(body);
 request.body(body);
  } else {
 logger.log("Request Body: None");
 }

 logger.log("Sending " + method + " request...");
 Response response = executeRequest(request, method, url);

 logger.log("======== RESPONSE DETAILS ========");
 logger.log("Status Code: " + response.getStatusCode());
 logger.log("Status Line: " + response.getStatusLine());

 RestResponse restResponse = new RestResponse();
 restResponse.setStatusCode(response.getStatusCode());
 restResponse.setStatusText(response.getStatusLine());

 String responseBody = response.getBody().asString();
 restResponse.setBody(formatResponseBody(responseBody));

 logger.log("Response Headers:");
 Map<String, String> responseHeaders = new java.util.HashMap<>();
 response.getHeaders().forEach(header -> {
 responseHeaders.put(header.getName(), header.getValue());
 logger.log(" " + header.getName() + ": " + header.getValue());
 });
 restResponse.setHeaders(responseHeaders);

 if (responseBody != null && !responseBody.trim().isEmpty()) {
 logger.log("Response Body (" + responseBody.length() + " characters):");
 logger.log(responseBody.length() > 1000 ?
 responseBody.substring(0, 1000) + "... [truncated]" : responseBody);
 } else {
 logger.log("Response Body: Empty");
 }

 logger.log("======== REQUEST COMPLETED ========");
 return restResponse;

 } catch (Exception e) {
 logger.log("======== REQUEST FAILED ========");
 logger.log("Error: " + e.getMessage());
 logger.log("Exception Type: " + e.getClass().getSimpleName());
 if (e.getCause() != null) {
  logger.log("Cause: " + e.getCause().getMessage());
 }
 RestResponse errorResponse = new RestResponse();
 errorResponse.setStatusCode(0);
 errorResponse.setStatusText("Error");
 errorResponse.setBody("Error: " + e.getMessage());
 return errorResponse;
 }
 }

 private String resolveVariables(String text) {
 if (environmentManager != null && text != null) {
 return environmentManager.resolveVariables(text);
 }
 return text;
 }

 private Response executeRequest(RequestSpecification request, String method, String url) {
 switch (method.toUpperCase()) {
 case "GET": return request.get(url);
 case "POST": return request.post(url);
 case "PUT": return request.put(url);
 case "DELETE": return request.delete(url);
 case "PATCH": return request.patch(url);
 case "HEAD": return request.head(url);
  case "OPTIONS": return request.options(url);
 default: throw new IllegalArgumentException("Unsupported HTTP method: " + method);
 }
 }

 private String formatResponseBody(String body) {
 if (body == null || body.trim().isEmpty()) return "";
 String formatted = JsonFormatter.formatJson(body);
 return formatted.equals(body) ? body : formatted;
 }
}