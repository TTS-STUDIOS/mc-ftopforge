package com.ttsstudio.ftopforge.history;

import com.ttsstudio.ftopforge.storage.Dialect;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvExporterTest {

    private HikariDataSource ds;
    private HistoryDao dao;

    @BeforeEach
    void setUp() throws Exception {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:sqlite::memory:");
        cfg.setMaximumPoolSize(1);
        ds = new HikariDataSource(cfg);
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute(Dialect.SQLITE.historyDdl());
        }
        dao = new HistoryDao(ds);
    }

    @AfterEach
    void tearDown() { ds.close(); }

    @Test
    void header_isCorrect(@TempDir Path tmp) throws Exception {
        dao.insertTop10(java.util.Collections.singletonList(
            new HistoryPoint(1L, 100L, "f1", "F1", 1, 100L, 1L, 2L, 3L, 4L, 5L)));
        CsvExporter exporter = new CsvExporter(ds, tmp.toFile());
        File out = exporter.exportAll();
        List<String> lines = Files.readAllLines(out.toPath());
        assertEquals("recalc_id,finished_at_iso,faction_id,faction_name,rank,total_worth,chunks_value,spawners_value,items_value,blocks_value,balance",
            lines.get(0));
    }

    @Test
    void escapesCommasAndQuotesInFactionName(@TempDir Path tmp) throws Exception {
        dao.insertTop10(java.util.Collections.singletonList(
            new HistoryPoint(1L, 100L, "f1", "Drag,ons \"Imperio\"", 1, 100L, 0L, 0L, 0L, 0L, 0L)));
        CsvExporter exporter = new CsvExporter(ds, tmp.toFile());
        File out = exporter.exportAll();
        List<String> lines = Files.readAllLines(out.toPath());
        assertTrue(lines.get(1).contains("\"Drag,ons \"\"Imperio\"\"\""));
    }

    @Test
    void filenameContainsTimestamp() {
        String name = CsvExporter.outputFilename(0L);
        assertTrue(name.startsWith("history-"));
        assertTrue(name.endsWith(".csv"));
        String middle = name.substring("history-".length(), name.length() - ".csv".length());
        assertTrue(middle.matches("\\d{14}"));
    }
}
