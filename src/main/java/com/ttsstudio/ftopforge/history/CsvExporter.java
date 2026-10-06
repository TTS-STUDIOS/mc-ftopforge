package com.ttsstudio.ftopforge.history;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.Semaphore;

public final class CsvExporter {
    private static final String HEADER =
        "recalc_id,finished_at_iso,faction_id,faction_name,rank,total_worth,chunks_value,spawners_value,items_value,blocks_value,balance";
    private static final Semaphore lock = new Semaphore(1);

    private final DataSource ds;
    private final File exportsDir;

    public CsvExporter(DataSource ds, File exportsDir) {
        this.ds = ds; this.exportsDir = exportsDir;
    }

    public static String outputFilename(long nowMs) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return "history-" + sdf.format(new Date(nowMs)) + ".csv";
    }

    public File exportAll() throws Exception {
        if (!lock.tryAcquire()) throw new IllegalStateException("export already running");
        try {
            if (!exportsDir.exists() && !exportsDir.mkdirs()) {
                throw new java.io.IOException("Cannot create exports dir: " + exportsDir.getAbsolutePath());
            }
            File out = new File(exportsDir, outputFilename(System.currentTimeMillis()));
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                     new FileOutputStream(out), StandardCharsets.UTF_8));
                 Connection c = ds.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT recalc_id, finished_at, faction_id, faction_name, `rank`, total_worth, " +
                     "chunks_value, spawners_value, items_value, blocks_value, balance " +
                     "FROM ftf_history ORDER BY finished_at ASC, `rank` ASC");
                 ResultSet rs = ps.executeQuery()) {
                w.write(HEADER); w.newLine();
                while (rs.next()) {
                    w.write(rs.getLong(1) + ",");
                    w.write(Instant.ofEpochMilli(rs.getLong(2)).toString() + ",");
                    w.write(csvEscape(rs.getString(3)) + ",");
                    w.write(csvEscape(rs.getString(4)) + ",");
                    w.write(rs.getInt(5) + ",");
                    w.write(rs.getLong(6) + ",");
                    w.write(rs.getLong(7) + ",");
                    w.write(rs.getLong(8) + ",");
                    w.write(rs.getLong(9) + ",");
                    w.write(rs.getLong(10) + ",");
                    w.write(String.valueOf(rs.getLong(11)));
                    w.newLine();
                }
            }
            return out;
        } finally {
            lock.release();
        }
    }

    static String csvEscape(String s) {
        if (s == null) return "";
        if (s.indexOf(',') < 0 && s.indexOf('"') < 0 && s.indexOf('\n') < 0) return s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
