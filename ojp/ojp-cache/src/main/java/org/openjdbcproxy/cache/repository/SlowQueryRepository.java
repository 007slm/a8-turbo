package org.openjdbcproxy.cache.repository;

import org.openjdbcproxy.cache.entity.SlowQuery;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

public interface SlowQueryRepository extends ListCrudRepository<SlowQuery, String> {
}
