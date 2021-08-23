package com.project.ataccama3.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.Iterator;
import java.util.Map;

@Data
public class DBConnection {
    private final String dbPrefix = "jdbc:mysql://";
    private String host;
    private String port;
    private String schema;
    private String user;
    private String password;

    public DBConnection(Iterator<Map.Entry<String, JsonNode>> responseFields) {
        while (responseFields != null && responseFields.hasNext()) {
            Map.Entry<String, JsonNode> responseEntry = responseFields.next();
            if ("hostname".equals(responseEntry.getKey())) {
                host = responseEntry.getValue().asText();
            }
            if ("port".equals(responseEntry.getKey())) {
                port = responseEntry.getValue().asText();
            }
            if ("username".equals(responseEntry.getKey())) {
                user = responseEntry.getValue().asText();
            }
            if ("password".equals(responseEntry.getKey())) {
                password = responseEntry.getValue().asText();
            }
            if ("databaseName".equals(responseEntry.getKey())) {
                schema = responseEntry.getValue().asText();
            }
        }
    }
}
