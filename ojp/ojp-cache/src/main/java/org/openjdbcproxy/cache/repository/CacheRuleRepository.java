package org.openjdbcproxy.cache.repository;

import org.openjdbcproxy.cache.entity.CacheRule;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CacheRuleRepository extends ListCrudRepository<CacheRule, String> {
}
