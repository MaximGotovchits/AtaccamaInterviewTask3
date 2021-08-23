package com.project.ataccama3.controller;

import com.project.ataccama3.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("statistics")
public class StatisticsController {
    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/tables")
    public @ResponseBody
    Map<String, Map<String, String>> getTables(@RequestParam String name) throws Exception {
        return statisticsService.getTablesStats(name);
    }

    @GetMapping("/columns")
    public @ResponseBody Map<String, List<String>> getColumns(
            @RequestParam String name,
            @RequestParam String tableName) throws Exception {
        return statisticsService.getColumnsStats(name, tableName);
    }
}
