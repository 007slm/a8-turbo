package com.example.shopservice.chinook.controller;

import com.example.shopservice.chinook.dto.ChinookQueryRequest;
import com.example.shopservice.chinook.dto.ChinookQueryResponse;
import com.example.shopservice.chinook.dto.ChinookSampleQuery;
import com.example.shopservice.chinook.dto.ChinookTable;
import com.example.shopservice.chinook.service.ChinookQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/chinook")
public class ChinookQueryController {

    private final ChinookQueryService chinookQueryService;

    public ChinookQueryController(ChinookQueryService chinookQueryService) {
        this.chinookQueryService = chinookQueryService;
    }

    @PostMapping("/query")
    public ChinookQueryResponse executeQuery(@RequestBody ChinookQueryRequest request) {
        return chinookQueryService.executeQuery(request);
    }

    @GetMapping("/tables")
    public List<ChinookTable> listTables() {
        return chinookQueryService.listTables();
    }

    @GetMapping("/sample-queries")
    public List<ChinookSampleQuery> sampleQueries() {
        return chinookQueryService.getSampleQueries();
    }
}
