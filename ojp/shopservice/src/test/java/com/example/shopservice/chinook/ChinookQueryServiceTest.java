package com.example.shopservice.chinook;

import com.example.shopservice.chinook.dto.ChinookQueryRequest;
import com.example.shopservice.chinook.dto.ChinookQueryResponse;
import com.example.shopservice.chinook.service.ChinookQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChinookQueryServiceTest {

    private ChinookQueryService chinookQueryService;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = createDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("DROP TABLE IF EXISTS Album");
        jdbcTemplate.execute("DROP TABLE IF EXISTS Artist");

        jdbcTemplate.execute("""
                CREATE TABLE Artist (
                    ArtistId INT PRIMARY KEY,
                    Name VARCHAR(100) NOT NULL
                );
                """);
        jdbcTemplate.execute("""
                CREATE TABLE Album (
                    AlbumId INT PRIMARY KEY,
                    Title VARCHAR(200) NOT NULL,
                    ArtistId INT NOT NULL,
                    FOREIGN KEY (ArtistId) REFERENCES Artist(ArtistId)
                );
                """);

        for (int i = 1; i <= 5; i++) {
            jdbcTemplate.update(
                    "INSERT INTO Artist (ArtistId, Name) VALUES (?, ?)",
                    i,
                    "Artist-" + i
            );
        }

        jdbcTemplate.update("INSERT INTO Album (AlbumId, Title, ArtistId) VALUES (?,?,?)", 1, "Album-1", 1);
        jdbcTemplate.update("INSERT INTO Album (AlbumId, Title, ArtistId) VALUES (?,?,?)", 2, "Album-2", 2);

        chinookQueryService = new ChinookQueryService(jdbcTemplate, 5, "PUBLIC");
    }

    @Test
    void executeQueryReturnsRowsAndMetadata() {
        ChinookQueryResponse response = chinookQueryService.executeQuery(
                new ChinookQueryRequest("SELECT ArtistId, Name FROM Artist ORDER BY ArtistId", 5)
        );

        assertEquals(5, response.rowCount());
        assertFalse(response.truncated());
        assertEquals(2, response.columns().size());
        assertEquals("ARTISTID", response.columns().get(0).name().toUpperCase());
        assertTrue(response.executionTimeMs() >= 0);
    }

    @Test
    void executeQueryAppliesRowLimit() {
        ChinookQueryResponse response = chinookQueryService.executeQuery(
                new ChinookQueryRequest("SELECT * FROM Artist ORDER BY ArtistId", 2)
        );

        assertEquals(2, response.rowCount());
        assertTrue(response.truncated());
    }

    @Test
    void executeQuerySupportsCte() {
        ChinookQueryResponse response = chinookQueryService.executeQuery(
                new ChinookQueryRequest("""
                        WITH ranked AS (
                            SELECT ArtistId,
                                   Name,
                                   ROW_NUMBER() OVER (ORDER BY ArtistId) AS rn
                            FROM Artist
                        )
                        SELECT ArtistId, Name FROM ranked WHERE rn <= 3
                        """, 5)
        );

        assertEquals(3, response.rowCount());
        assertFalse(response.truncated());
    }

    @Test
    void executeQueryRejectsNonSelectStatements() {
        assertThrows(ResponseStatusException.class, () ->
                chinookQueryService.executeQuery(new ChinookQueryRequest("DELETE FROM Artist", null))
        );
    }

    @Test
    void listTablesReturnsMetadata() {
        Set<String> tableNames = chinookQueryService.listTables().stream()
                .map(table -> table.name().toUpperCase())
                .collect(Collectors.toSet());

        assertTrue(tableNames.containsAll(Set.of("ARTIST", "ALBUM")));
    }

    private DataSource createDataSource() throws ClassNotFoundException {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        @SuppressWarnings("unchecked")
        Class<java.sql.Driver> driverClass =
                (Class<java.sql.Driver>) Class.forName("org.h2.Driver");
        dataSource.setDriverClass(driverClass);
        dataSource.setUrl("jdbc:h2:mem:chinook;MODE=MySQL;DATABASE_TO_UPPER=false;CASE_INSENSITIVE_IDENTIFIERS=true;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
