package com.example.lostandfound.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

public final class ImportSpatiotemporalSeedMain {

    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3306/lost_found?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=UTF-8";
    private static final String USER = System.getenv().getOrDefault("DB_USERNAME", "root");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "uzi159357+");
    private static final Path SQL_FILE = Path.of("sql/seed_spatiotemporal_lost_50.sql");

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.setProperty("user", USER);
        props.setProperty("password", PASSWORD);

        try (Connection connection = DriverManager.getConnection(JDBC_URL, props)) {
            int existing = countDemoRows(connection);
            if (existing > 0) {
                System.out.println("Demo seed already present (" + existing + " rows). Skipping import.");
                printDistribution(connection);
                return;
            }

            String sql = Files.readString(SQL_FILE);
            String insertSql = extractInsertStatement(sql);
            try (Statement statement = connection.createStatement()) {
                int inserted = statement.executeUpdate(insertSql);
                System.out.println("Imported rows: " + inserted);
            }
            printDistribution(connection);
        }
    }

    private static int countDemoRows(Connection connection) throws Exception {
        String query = "SELECT COUNT(*) FROM lost_item WHERE contact LIKE '13800001%' OR contact LIKE '13800002%' "
                + "OR contact LIKE '13800003%' OR contact LIKE '13800004%'";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private static String extractInsertStatement(String sql) {
        int start = sql.indexOf("INSERT INTO lost_item");
        if (start < 0) {
            throw new IllegalStateException("INSERT statement not found in seed file");
        }
        return sql.substring(start).trim();
    }

    private static void printDistribution(Connection connection) throws Exception {
        String query = "SELECT location, COUNT(*) AS cnt FROM lost_item WHERE status = 1 "
                + "GROUP BY location ORDER BY cnt DESC LIMIT 8";
        System.out.println("Top locations (approved):");
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                System.out.printf("  %s : %d%n", resultSet.getString("location"), resultSet.getInt("cnt"));
            }
        }
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM lost_item WHERE status = 1")) {
            resultSet.next();
            System.out.println("Total approved lost items: " + resultSet.getInt(1));
        }
    }
}
