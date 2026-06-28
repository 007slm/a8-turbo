package org.openjdbcproxy.cache.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.dto.DeleteConnectionRequest;
import org.openjdbcproxy.cache.dto.UpdateCdcCredentialsRequest;
import org.openjdbcproxy.cache.entity.CacheRule;
import org.openjdbcproxy.cache.entity.ConnectionConfig;
import org.openjdbcproxy.cache.entity.SlowQuery;
import org.openjdbcproxy.cache.repository.CacheRuleRepository;
import org.openjdbcproxy.cache.repository.ConnectionConfigRepository;
import org.openjdbcproxy.cache.repository.SlowQueryRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
public class ConnectionConfigController {

    private final ConnectionConfigRepository connectionConfigRepository;
    private final SlowQueryRepository slowQueryRepository;
    private final CacheRuleRepository cacheRuleRepository;

    @GetMapping
    public List<ConnectionConfig> listConnections() {
        return StreamSupport.stream(connectionConfigRepository.findAll().spliterator(), false)
                .sorted(Comparator.comparing(ConnectionConfig::getLastActiveTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @PutMapping
    public ConnectionConfig updateConnection(@RequestBody UpdateCdcCredentialsRequest request) {
        String id = request.getId();
        log.info("Updating CDC credentials for connection: {}", id);
        return connectionConfigRepository.findById(id).map(existing -> {
            existing.setCdcUsername(request.getCdcUsername());
            existing.setCdcPassword(request.getCdcPassword());
            return connectionConfigRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Connection not found: " + id));
    }

    @DeleteMapping
    public void deleteConnection(@RequestBody DeleteConnectionRequest request) {
        String id = request.getId();
        log.info("Deleting connection config and cascading to related entities for id: {}", id);

        // 1. Delete ConnectionConfig
        connectionConfigRepository.deleteById(id);

        // 2. Delete related SlowQueries
        // Note: This might be expensive if there are many slow queries.
        // Ideally, we should use a secondary index or scan, but for Redis + CRUD repo,
        // we iterate.
        // Optimization: Use Redis scan if possible, but here we stick to repository for
        // simplicity as per requirements.
        List<SlowQuery> allQueries = StreamSupport.stream(slowQueryRepository.findAll().spliterator(), false)
                .filter(q -> id.equals(q.getConnHash()))
                .collect(Collectors.toList());
        slowQueryRepository.deleteAll(allQueries);
        log.info("Deleted {} slow queries for connection {}", allQueries.size(), id);

        // 3. Delete related CacheRules
        List<CacheRule> allRules = StreamSupport.stream(cacheRuleRepository.findAll().spliterator(), false)
                .filter(r -> id.equals(r.getConnHash()))
                .collect(Collectors.toList());
        cacheRuleRepository.deleteAll(allRules);
        log.info("Deleted {} cache rules for connection {}", allRules.size(), id);
    }
}
