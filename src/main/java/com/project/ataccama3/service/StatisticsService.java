package com.project.ataccama3.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.ataccama3.model.DBConnection;
import com.project.ataccama3.util.ConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.sql.ResultSetMetaData;
import java.util.*;

@Service
public class StatisticsService {
    @Autowired
    private final RestTemplate restTemplate;

    @Autowired
    private ConnectionManager connectionManager;

    public StatisticsService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public Map<String, Map<String, String>> getTablesStats(String name) throws Exception {
        UriComponentsBuilder queryBuilder = UriComponentsBuilder.fromHttpUrl("http://localhost:8080/tables")
                .queryParam("name", name);
        Collection<List<String>> tableResponse = restTemplate.getForEntity(queryBuilder.toUriString(), Map.class, name)
                .getBody()
                .values();

        List<String> allTables = tableResponse.stream().findFirst().get();

        Map<String, Map<String, String>> tableToStats = new HashMap<>();

        DBConnection dbConnection = getDbConnection(name);
        JdbcTemplate jdbcTemplate = getJdbcTemplateByName(dbConnection);

        for (String currentTable : allTables) {
            String recordsAmount = queryForSingleColumnResult(
                    "SELECT count(1) FROM " + currentTable + ";",
                    jdbcTemplate).values().stream().findFirst().get().get(0);
            String attributesAmount = queryForSingleColumnResult(
                    "SELECT count(1) FROM information_schema.columns WHERE table_name=?",
                    jdbcTemplate, currentTable).values().stream().findFirst().get().get(0);

            Map<String, String> stats = new HashMap<>();
            stats.put("Records amount", recordsAmount);
            stats.put("Attributes amount", attributesAmount);

            tableToStats.put(currentTable, stats);
        }

        return tableToStats;
    }

    public Map<String, List<String>> getColumnsStats(String name, String tableName) throws Exception {
        UriComponentsBuilder queryBuilder = UriComponentsBuilder.fromHttpUrl("http://localhost:8080/columns")
                .queryParam("name", name)
                .queryParam("tableName", tableName);
        Collection<List<String>> tableResponse = restTemplate.getForEntity(queryBuilder.toUriString(), Map.class, name)
                .getBody()
                .values();

        List<String> allColumns = tableResponse.stream().findFirst().get();

        DBConnection dbConnection = getDbConnection(name);
        JdbcTemplate jdbcTemplate = getJdbcTemplateByName(dbConnection);

        String minMaxAvgQuery = constructColumnStatsQuery(allColumns) + " FROM " + tableName + ";";

        Map<String, List<String>> resultStats = new HashMap<>();

        for (String currentCol : allColumns) {
            String meanQuery = constructMeanQuery(currentCol, tableName);

            resultStats.putAll(queryForSingleColumnResult(meanQuery, jdbcTemplate));
        }
        resultStats.putAll(queryForSingleColumnResult(minMaxAvgQuery, jdbcTemplate));
        return resultStats;
    }

    private String constructMeanQuery(String currentCol, String tableName) {
        return String.format("SELECT AVG(avg.%2$s) as mean_%2$s \n" +
                "FROM (\n" +
                "SELECT d.%2$s, @rownum:=@rownum+1 as `row_number`, @total_rows:=@rownum\n" +
                "  FROM %1$s d, (SELECT @rownum:=0) r\n" +
                "  WHERE d.%2$s is NOT NULL\n" +
                "  ORDER BY d.%2$s\n" +
                ") as avg\n" +
                "WHERE avg.row_number IN ( FLOOR((@total_rows+1)/2), FLOOR((@total_rows+2)/2) );\n", tableName, currentCol);
    }

    private String constructColumnStatsQuery(Collection<String> columns) {
        StringBuilder query = new StringBuilder("SELECT ");

        for (String colName : columns) {
            query.append(String.format("min(%1$s) as min_%1$s, max(%1$s) as max_%1$s, avg(%1$s) as avg_%1$s, ", colName));
        }

        return query.substring(0, query.length() - 2);
    }

    private JdbcTemplate getJdbcTemplateByName(DBConnection dbConnection) throws Exception {
        return connectionManager.getJdbcTemplate(dbConnection);
    }

    private DBConnection getDbConnection(String name) {
        UriComponentsBuilder queryBuilder = UriComponentsBuilder.fromHttpUrl("http://localhost:8081/credentials/find")
                .queryParam("name", name);
        JsonNode credentials = restTemplate.getForEntity(queryBuilder.toUriString(), JsonNode.class, name).getBody();

        Iterator<Map.Entry<String, JsonNode>> responseFields = Objects.requireNonNull(credentials).fields();

        return new DBConnection(responseFields);
    }

    private Map<String, List<String>> queryForSingleColumnResult(String query, JdbcTemplate jdbcTemplate, Object... args) {
        return jdbcTemplate.query(query, resultSet -> {
            Map<String, List<String>> res = new HashMap<>();
            ResultSetMetaData metaData = resultSet.getMetaData();
            while (resultSet.next()) {
                int colAmount = metaData.getColumnCount();
                for (int i = 1; i <= colAmount; ++i) {
                    if (res.containsKey(metaData.getColumnName(i))) {
                        res.get(metaData.getColumnName(i)).add(resultSet.getString(i));
                    } else {
                        List<String> singleValue = new ArrayList<>();
                        singleValue.add(resultSet.getString(i));
                        res.put(metaData.getColumnName(i), singleValue);
                    }
                }
            }
            return res;
        }, args);
    }
}
