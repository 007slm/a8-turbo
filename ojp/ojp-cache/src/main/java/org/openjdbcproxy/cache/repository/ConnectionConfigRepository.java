package org.openjdbcproxy.cache.repository;

import org.openjdbcproxy.cache.entity.ConnectionConfig;
import org.springframework.data.repository.CrudRepository;

public interface ConnectionConfigRepository extends CrudRepository<ConnectionConfig, String> {
}
