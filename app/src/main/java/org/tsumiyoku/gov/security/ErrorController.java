package org.tsumiyoku.gov.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/error")
@RequiredArgsConstructor
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {
    private final ErrorAttributes errorAttributes;

    @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> errorJson(HttpServletRequest request) {
        WebRequest webRequest = new ServletWebRequest(request);
        var opts = ErrorAttributeOptions.defaults().including(Include.MESSAGE);
        Map<String, Object> attrs = errorAttributes.getErrorAttributes(webRequest, opts);

        int status = (int) attrs.getOrDefault("status", 500);
        Map<String, Object> body = new HashMap<>();
        body.put("status", attrs.get("status"));
        body.put("error", attrs.get("error"));
        body.put("message", attrs.getOrDefault("message", ""));
        body.put("path", attrs.get("path"));
        body.put("timestamp", attrs.get("timestamp"));

        return ResponseEntity.status(status).body(body);
    }

    @RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> errorHtml(HttpServletRequest request) {
        WebRequest webRequest = new ServletWebRequest(request);
        var opts = ErrorAttributeOptions.defaults().including(Include.MESSAGE);
        Map<String, Object> attrs = errorAttributes.getErrorAttributes(webRequest, opts);

        int status = (int) attrs.getOrDefault("status", 500);
        String path = String.valueOf(attrs.getOrDefault("path", ""));
        String message = String.valueOf(attrs.getOrDefault("message", ""));

        String html = "<!doctype html><html><head><meta charset='utf-8'><title>Error</title></head><body>"
                + "<h1>Error " + status + "</h1>"
                + "<p>Path: " + escapeHtml(path) + "</p>"
                + (message.isEmpty() ? "" : "<pre>" + escapeHtml(message) + "</pre>")
                + "</body></html>";

        return ResponseEntity.status(status).contentType(MediaType.TEXT_HTML).body(html);
    }

    private static String escapeHtml(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
