package com.rct.util;

import com.rct.manager.CollectionManager;
import java.util.List;

public class DocumentationGenerator {

    public enum Format {
        MARKDOWN("Markdown", "md"),
        HTML("HTML", "html"),
        JSON("JSON", "json"),
        POSTMAN("Postman Collection", "json");

        private final String displayName;
        private final String extension;

        Format(String displayName, String extension) {
            this.displayName = displayName;
            this.extension = extension;
        }

        public String getDisplayName() { return displayName; }
        public String getExtension() { return extension; }
    }

    public static String generateDocumentation(CollectionManager.Collection collection, Format format) {
        switch (format) {
            case MARKDOWN: return generateMarkdown(collection);
            case HTML: return generateHTML(collection);
            case JSON: return generateJSON(collection);
            case POSTMAN: return generatePostman(collection);
            default: return "# Unsupported format";
        }
    }

    private static String generateMarkdown(CollectionManager.Collection collection) {
        StringBuilder md = new StringBuilder();

        md.append("# ").append(collection.getName()).append(" API Documentation\n\n");
        md.append("Generated on: ").append(java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");

        md.append("## Overview\n\n");
        md.append("This document describes the API endpoints for ").append(collection.getName()).append(".\n\n");
        md.append("**Total Endpoints:** ").append(collection.getRequests().size()).append("\n\n");

        md.append("## Table of Contents\n\n");
        for (int i = 0; i < collection.getRequests().size(); i++) {
            CollectionManager.SavedRequest request = collection.getRequests().get(i);
            md.append("- [").append(request.getName()).append("](#")
                    .append(request.getName().toLowerCase().replaceAll("[^a-z0-9]", "-"))
                    .append(")\n");
        }
        md.append("\n");

        for (CollectionManager.SavedRequest request : collection.getRequests()) {
            md.append("## ").append(request.getName()).append("\n\n");
            md.append("**Method:** `").append(request.getMethod()).append("`\n\n");
            md.append("**URL:** `").append(request.getUrl()).append("`\n\n");

            if (request.getHeaders() != null && !request.getHeaders().trim().isEmpty()) {
                md.append("### Headers\n\n");
                md.append("```\n").append(request.getHeaders()).append("\n```\n\n");
            }

            if (request.getParams() != null && !request.getParams().trim().isEmpty()) {
                md.append("### Query Parameters\n\n");
                String[] paramLines = request.getParams().split("\n");
                md.append("| Parameter | Value |\n");
                md.append("|-----------|-------|\n");
                for (String line : paramLines) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        md.append("| ").append(parts[0].trim()).append(" | ")
                                .append(parts[1].trim()).append(" |\n");
                    }
                }
                md.append("\n");
            }

            if (request.getBody() != null && !request.getBody().trim().isEmpty()) {
                md.append("### Request Body\n\n");
                md.append("```json\n").append(request.getBody()).append("\n```\n\n");
            }

            md.append("### Example Request\n\n");
            md.append("```bash\n");
            md.append("curl -X ").append(request.getMethod()).append(" \\\n");
            md.append(" '").append(request.getUrl()).append("'");

            if (request.getHeaders() != null && !request.getHeaders().trim().isEmpty()) {
                String[] headerLines = request.getHeaders().split("\n");
                for (String line : headerLines) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        md.append(" \\\n -H '").append(parts[0].trim()).append(": ")
                                .append(parts[1].trim()).append("'");
                    }
                }
            }

            if (request.getBody() != null && !request.getBody().trim().isEmpty()) {
                md.append(" \\\n -d '").append(request.getBody().replace("'", "\\'")).append("'");
            }

            md.append("\n```\n\n");
            md.append("---\n\n");
        }

        return md.toString();
    }

    private static String generateHTML(CollectionManager.Collection collection) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        html.append(" <meta charset=\"UTF-8\">\n");
        html.append(" <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append(" <title>").append(collection.getName()).append(" API Documentation</title>\n");
        html.append(" <style>\n");
        html.append(" body { font-family: Arial, sans-serif; max-width: 1200px; margin: 0 auto; padding: 20px; }\n");
        html.append(" .endpoint { border: 1px solid #ddd; margin: 20px 0; padding: 20px; border-radius: 5px; }\n");
        html.append(" .method { display: inline-block; padding: 4px 8px; border-radius: 3px; color: white; font-weight: bold; }\n");
        html.append(" .GET { background-color: #61affe; }\n");
        html.append(" .POST { background-color: #49cc90; }\n");
        html.append(" .PUT { background-color: #fca130; }\n");
        html.append(" .DELETE { background-color: #f93e3e; }\n");
        html.append(" .PATCH { background-color: #50e3c2; }\n");
        html.append(" pre { background-color: #f5f5f5; padding: 10px; border-radius: 3px; overflow-x: auto; }\n");
        html.append(" table { border-collapse: collapse; width: 100%; }\n");
        html.append(" th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append(" th { background-color: #f2f2f2; }\n");
        html.append(" </style>\n</head>\n<body>\n");

        html.append(" <h1>").append(collection.getName()).append(" API Documentation</h1>\n");
        html.append(" <p><strong>Generated on:</strong> ").append(java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>\n");
        html.append(" <p><strong>Total Endpoints:</strong> ").append(collection.getRequests().size()).append("</p>\n\n");

        html.append(" <h2>Table of Contents</h2>\n <ul>\n");
        for (CollectionManager.SavedRequest request : collection.getRequests()) {
            html.append(" <li><a href=\"#").append(request.getName().toLowerCase().replaceAll("[^a-z0-9]", "-"))
                    .append("\">").append(request.getName()).append("</a></li>\n");
        }
        html.append(" </ul>\n\n");

        for (CollectionManager.SavedRequest request : collection.getRequests()) {
            html.append(" <div class=\"endpoint\" id=\"").append(request.getName().toLowerCase().replaceAll("[^a-z0-9]", "-")).append("\">\n");
            html.append(" <h2>").append(request.getName()).append("</h2>\n");
            html.append(" <p><span class=\"method ").append(request.getMethod()).append("\">").append(request.getMethod()).append("</span> ");
            html.append("<code>").append(request.getUrl()).append("</code></p>\n");

            if (request.getHeaders() != null && !request.getHeaders().trim().isEmpty()) {
                html.append(" <h3>Headers</h3>\n");
                html.append(" <pre>").append(escapeHtml(request.getHeaders())).append("</pre>\n");
            }

            if (request.getParams() != null && !request.getParams().trim().isEmpty()) {
                html.append(" <h3>Query Parameters</h3>\n");
                html.append(" <table>\n <tr><th>Parameter</th><th>Value</th></tr>\n");
                String[] paramLines = request.getParams().split("\n");
                for (String line : paramLines) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        html.append(" <tr><td>").append(escapeHtml(parts[0].trim()))
                                .append("</td><td>").append(escapeHtml(parts[1].trim())).append("</td></tr>\n");
                    }
                }
                html.append(" </table>\n");
            }

            if (request.getBody() != null && !request.getBody().trim().isEmpty()) {
                html.append(" <h3>Request Body</h3>\n");
                html.append(" <pre>").append(escapeHtml(request.getBody())).append("</pre>\n");
            }

            html.append(" <h3>Example Request</h3>\n");
            html.append(" <pre>curl -X ").append(request.getMethod()).append(" \\\n");
            html.append(" '").append(escapeHtml(request.getUrl())).append("'");

            if (request.getHeaders() != null && !request.getHeaders().trim().isEmpty()) {
                String[] headerLines = request.getHeaders().split("\n");
                for (String line : headerLines) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        html.append(" \\\n -H '").append(escapeHtml(parts[0].trim())).append(": ")
                                .append(escapeHtml(parts[1].trim())).append("'");
                    }
                }
            }

            if (request.getBody() != null && !request.getBody().trim().isEmpty()) {
                html.append(" \\\n -d '").append(escapeHtml(request.getBody().replace("'", "\\'")))
                        .append("'");
            }

            html.append("</pre>\n");
            html.append(" </div>\n\n");
        }

        html.append("</body>\n</html>");
        return html.toString();
    }

    private static String generateJSON(CollectionManager.Collection collection) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append(" \"name\": \"").append(escapeJson(collection.getName())).append("\",\n");
        json.append(" \"description\": \"API documentation for ").append(escapeJson(collection.getName())).append("\",\n");
        json.append(" \"version\": \"1.0.0\",\n");
        json.append(" \"generatedAt\": \"").append(java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))).append("\",\n");
        json.append(" \"endpoints\": [\n");

        for (int i = 0; i < collection.getRequests().size(); i++) {
            CollectionManager.SavedRequest request = collection.getRequests().get(i);
            json.append(" {\n");
            json.append(" \"name\": \"").append(escapeJson(request.getName())).append("\",\n");
            json.append(" \"method\": \"").append(request.getMethod()).append("\",\n");
            json.append(" \"url\": \"").append(escapeJson(request.getUrl())).append("\",\n");

            if (request.getHeaders() != null && !request.getHeaders().trim().isEmpty()) {
                json.append(" \"headers\": {\n");
                String[] headerLines = request.getHeaders().split("\n");
                for (int j = 0; j < headerLines.length; j++) {
                    String[] parts = headerLines[j].split(":", 2);
                    if (parts.length == 2) {
                        json.append(" \"").append(escapeJson(parts[0].trim())).append("\": \"")
                                .append(escapeJson(parts[1].trim())).append("\"");
                        if (j < headerLines.length - 1) json.append(",");
                        json.append("\n");
                    }
                }
                json.append(" },\n");
            }

            if (request.getBody() != null && !request.getBody().trim().isEmpty()) {
                json.append(" \"body\": \"").append(escapeJson(request.getBody())).append("\",\n");
            }

            json.append(" \"example\": \"curl -X ").append(request.getMethod())
                    .append(" '").append(escapeJson(request.getUrl())).append("'\"\n");
            json.append(" }");
            if (i < collection.getRequests().size() - 1) json.append(",");
            json.append("\n");
        }

        json.append(" ]\n");
        json.append("}");
        return json.toString();
    }

    private static String generatePostman(CollectionManager.Collection collection) {
        StringBuilder postman = new StringBuilder();
        postman.append("{\n");
        postman.append(" \"info\": {\n");
        postman.append(" \"name\": \"").append(escapeJson(collection.getName())).append("\",\n");
        postman.append(" \"description\": \"Generated from REST Client Pro\",\n");
        postman.append(" \"schema\": \"https://schema.getpostman.com/json/collection/v2.1.0/collection.json\"\n");
        postman.append(" },\n");
        postman.append(" \"item\": [\n");

        for (int i = 0; i < collection.getRequests().size(); i++) {
            CollectionManager.SavedRequest request = collection.getRequests().get(i);
            postman.append(" {\n");
            postman.append(" \"name\": \"").append(escapeJson(request.getName())).append("\",\n");
            postman.append(" \"request\": {\n");
            postman.append(" \"method\": \"").append(request.getMethod()).append("\",\n");
            postman.append(" \"header\": [\n");

            if (request.getHeaders() != null && !request.getHeaders().trim().isEmpty()) {
                String[] headerLines = request.getHeaders().split("\n");
                for (int j = 0; j < headerLines.length; j++) {
                    String[] parts = headerLines[j].split(":", 2);
                    if (parts.length == 2) {
                        postman.append(" {\n");
                        postman.append(" \"key\": \"").append(escapeJson(parts[0].trim())).append("\",\n");
                        postman.append(" \"value\": \"").append(escapeJson(parts[1].trim())).append("\"\n");
                        postman.append(" }");
                        if (j < headerLines.length - 1) postman.append(",");
                        postman.append("\n");
                    }
                }
            }

            postman.append(" ],\n");
            postman.append(" \"url\": {\n");
            postman.append(" \"raw\": \"").append(escapeJson(request.getUrl())).append("\",\n");
            postman.append(" \"protocol\": \"https\",\n");
            postman.append(" \"host\": [\"api\", \"example\", \"com\"],\n");
            postman.append(" \"path\": [\"endpoint\"]\n");
            postman.append(" }");

            if (request.getBody() != null && !request.getBody().trim().isEmpty()) {
                postman.append(",\n \"body\": {\n");
                postman.append(" \"mode\": \"raw\",\n");
                postman.append(" \"raw\": \"").append(escapeJson(request.getBody())).append("\"\n");
                postman.append(" }");
            }

            postman.append("\n }\n");
            postman.append(" }");
            if (i < collection.getRequests().size() - 1) postman.append(",");
            postman.append("\n");
        }

        postman.append(" ]\n");
        postman.append("}");
        return postman.toString();
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}