package com.songoda.core.database;

import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class DatabaseTest {
    static {
        System.setProperty("org.jooq.no-tips", "true");
        System.setProperty("org.jooq.no-logo", "true");
    }

    @Test
    public void testId() {
        File dbDir = new File("./db_test");
        File logsDir = new File("./logs");
        DataManager dataManager = new DataManager();
        if (!dataManager.getDatabaseConnector().isInitialized()) {
            throw new RuntimeException("Database 'Data' test failed - DatabaseConnector not initialized");
        }
        //Create tables
        dataManager.getDatabaseConnector().connectDSL(context -> {
            context.createTableIfNotExists("data_test")
                    .column("id", SQLDataType.INTEGER.identity(true))
                    .column("name", SQLDataType.VARCHAR(16))
                    .column("points", SQLDataType.INTEGER)
                    .column("other_points", SQLDataType.INTEGER)
                    .constraint(DSL.constraint().primaryKey(DSL.field("id")))
                    .execute();
        });

        int id = new Random().nextInt(1000);
        DataTestId dataTest = new DataTestId(id, "Test", 10, 20);
        dataManager.saveSync(dataTest);

        DataTestId dataTest2 = dataManager.load(id, DataTestId.class, "data_test");
        if (!dataTest.equals(dataTest2)) {


            System.err.println("Database 'Data - ID' test failed");
        } else {
            System.out.println("Database 'Data - ID' test passed");
        }

        dataManager.shutdownNow();

        if (dbDir.exists()) {
            deleteDirectory(dbDir);
        }
        if (logsDir.exists()) {
            deleteDirectory(logsDir);
        }
    }

    @Test
    public void testUUID() {
        File dbDir = new File("./db_test");
        File logsDir = new File("./logs");

        DataManager dataManager = new DataManager();
        if (!dataManager.getDatabaseConnector().isInitialized()) {
            throw new RuntimeException("Database 'Data' test failed - DatabaseConnector not initialized");
        }
        //Create tables
        dataManager.getDatabaseConnector().connectDSL(context -> {
            context.createTableIfNotExists("data_uuid_test")
                    .column("uuid", SQLDataType.VARCHAR(36))
                    .column("name", SQLDataType.VARCHAR(16))
                    .column("points", SQLDataType.INTEGER)
                    .column("other_points", SQLDataType.INTEGER)
                    .constraint(DSL.constraint().primaryKey(DSL.field("uuid")))
                    .execute();
        });


        UUID uuid = UUID.randomUUID();
        DataTestUUID dataTest = new DataTestUUID(uuid, "Test", 10, 20);
        dataManager.saveSync(dataTest);

        DataTestUUID dataTest2 = dataManager.load(uuid, DataTestUUID.class, "data_uuid_test");

        if (!dataTest.equals(dataTest2)) {
            System.err.println(dataTest);
            System.err.println(dataTest2);
            System.err.println("Database 'Data - UUID' test failed");
        } else {
            System.out.println("Database 'Data - UUID' test passed");
        }


        dataManager.shutdownNow();

        if (dbDir.exists()) {
            deleteDirectory(dbDir);
        }
        if (logsDir.exists()) {
            deleteDirectory(logsDir);
        }
    }

    @Test
    public void testConvert() {
        File dbDir = new File("./db_test");
        File logsDir = new File("./logs");

        DataManager sqlite = new DataManager(DatabaseType.SQLITE);
        DataManager h2 = new DataManager();

        if (!sqlite.getDatabaseConnector().isInitialized()) {
            throw new RuntimeException("Database 'Data - Convert' test failed - DatabaseConnector not initialized");
        }

        if (!h2.getDatabaseConnector().isInitialized()) {
            throw new RuntimeException("Database 'Data - Convert' test failed - DatabaseConnector not initialized");
        }

        //Create tables for SQLite
        try (Connection sqliteConnection = sqlite.getDatabaseConnector().getConnection(); Connection h2Connection = h2.getDatabaseConnector().getConnection()) {
            sqliteConnection.createStatement().execute("CREATE TABLE IF NOT EXISTS `data_convert_test` (`id` INTEGER PRIMARY KEY NOT NULL, `name` VARCHAR(16), `points` INTEGER, `other_points` INTEGER)");
            //create 2 other tables for some example data with different column names and types
            sqliteConnection.createStatement().execute("CREATE TABLE IF NOT EXISTS `data_convert_test2` (`id` INTEGER PRIMARY KEY NOT NULL, `name` VARCHAR(16), `points` INTEGER, `other_points` INTEGER)");
            sqliteConnection.createStatement().execute("CREATE TABLE IF NOT EXISTS `data_convert_test3` (`id` INTEGER PRIMARY KEY NOT NULL, `name` VARCHAR(16), `points` INTEGER, `other_points` INTEGER)");

            //Fill with some example data using loops
            for (int i = 0; i < 10; i++) {
                sqliteConnection.createStatement().execute("INSERT INTO `data_convert_test` (`name`, `points`, `other_points`) VALUES ('Test" + i + "', " + i + ", " + i + ")");
                sqliteConnection.createStatement().execute("INSERT INTO `data_convert_test2` (`name`, `points`, `other_points`) VALUES ('Test" + i + "', " + i + ", " + i + ")");
                sqliteConnection.createStatement().execute("INSERT INTO `data_convert_test3` (`name`, `points`, `other_points`) VALUES ('Test" + i + "', " + i + ", " + i + ")");
            }

            //Migrate tables and data to H2
            try {
                // Export schema
                DatabaseMetaData meta = sqliteConnection.getMetaData();
                ResultSet tables = meta.getTables(null, null, null, new String[] {"TABLE"});

                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    Statement stmt = sqliteConnection.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName);

                    ResultSetMetaData metaRs = rs.getMetaData();
                    int columnCount = metaRs.getColumnCount();

                    StringBuilder createTableQuery = new StringBuilder();
                    createTableQuery.append("CREATE TABLE ").append(tableName).append(" (");

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaRs.getColumnName(i);
                        String columnType = metaRs.getColumnTypeName(i);
                        int columnSize = metaRs.getColumnDisplaySize(i);

                        createTableQuery.append(columnName).append(" ").append(columnType).append("(").append(columnSize).append(")");

                        if (i < columnCount) {
                            createTableQuery.append(", ");
                        }
                    }

                    createTableQuery.append(")");

                    h2Connection.createStatement().execute(createTableQuery.toString());

                    while (rs.next()) {
                        StringBuilder insertQuery = new StringBuilder();
                        insertQuery.append("INSERT INTO ").append(tableName).append(" VALUES (");

                        for (int i = 1; i <= columnCount; i++) {
                            Object value = rs.getObject(i);

                            if (value == null) {
                                insertQuery.append("NULL");
                            } else if (value instanceof String || value instanceof Timestamp) {
                                insertQuery.append("'").append(value).append("'");
                            } else {
                                insertQuery.append(value);
                            }

                            if (i < columnCount) {
                                insertQuery.append(", ");
                            }
                        }

                        insertQuery.append(")");
                        h2Connection.createStatement().execute(insertQuery.toString());
                    }
                }

                //Query data from both databases and compare
                Statement stmt = sqliteConnection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM `data_convert_test`");

                //H2
                Statement stmt2 = h2Connection.createStatement();
                ResultSet rs2 = stmt2.executeQuery("SELECT * FROM `data_convert_test`");

                //Compare data
                while (rs.next() && rs2.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    int points = rs.getInt("points");
                    int otherPoints = rs.getInt("other_points");

                    int id2 = rs2.getInt("id");
                    String name2 = rs2.getString("name");
                    int points2 = rs2.getInt("points");
                    int otherPoints2 = rs2.getInt("other_points");

                    if (id != id2 || !name.equals(name2) || points != points2 || otherPoints != otherPoints2) {
                        System.err.println("Database 'Data - Convert' test failed - Data mismatch");
                        System.err.println("SQLite: " + id + " " + name + " " + points + " " + otherPoints);
                        System.err.println("H2: " + id2 + " " + name2 + " " + points2 + " " + otherPoints2);
                        return;
                    }
                }
                System.out.println("Database 'Data - Convert' test passed");
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        sqlite.shutdownNow();
        h2.shutdownNow();

        if (dbDir.exists()) {
            deleteDirectory(dbDir);
        }
        if (logsDir.exists()) {
            deleteDirectory(logsDir);
        }
    }

    private static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    private static class DataTestId implements Data {
        private int id;
        private String name;
        private int points;
        private int otherPoints;

        public DataTestId() {
        }

        public DataTestId(int id, String name, int points, int otherPoints) {
            this.id = id;
            this.name = name;
            this.points = points;
            this.otherPoints = otherPoints;
        }

        @Override
        public int getId() {
            return this.id;
        }

        @Override
        public Map<String, Object> serialize() {
            Map<String, Object> map = new HashMap<>();
            map.put("id", this.id);
            map.put("name", this.name);
            map.put("points", this.points);
            map.put("other_points", this.otherPoints);
            return map;
        }

        @Override
        public Data deserialize(Map<String, Object> map) {
            this.id = (int) map.get("id");
            this.name = (String) map.get("name");
            this.points = (int) map.get("points");
            this.otherPoints = (int) map.get("other_points");
            return this;
        }

        @Override
        public String getTableName() {
            return "data_test";
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof DataTestId)) return false;

            DataTestId other = (DataTestId) obj;
            return this.id == other.id &&
                    this.name.equals(other.name) &&
                    this.points == other.points &&
                    this.otherPoints == other.otherPoints;
        }
    }

    private static class DataTestUUID implements Data {
        private UUID uuid;
        private String name;
        private int points;
        private int otherPoints;

        public DataTestUUID() {
        }

        public DataTestUUID(UUID uuid, String name, int points, int otherPoints) {
            this.uuid = uuid;
            this.name = name;
            this.points = points;
            this.otherPoints = otherPoints;
        }

        @Override
        public UUID getUniqueId() {
            return this.uuid;
        }

        @Override
        public Map<String, Object> serialize() {
            Map<String, Object> map = new HashMap<>();
            map.put("uuid", this.uuid.toString());
            map.put("name", this.name);
            map.put("points", this.points);
            map.put("other_points", this.otherPoints);
            return map;
        }

        @Override
        public Data deserialize(Map<String, Object> map) {
            this.uuid = UUID.fromString((String) map.get("uuid"));
            this.name = (String) map.get("name");
            this.points = (int) map.get("points");
            this.otherPoints = (int) map.get("other_points");
            return this;
        }

        @Override
        public String getTableName() {
            return "data_uuid_test";
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof DataTestUUID)) return false;

            DataTestUUID other = (DataTestUUID) obj;
            return this.uuid.equals(other.uuid) &&
                    this.name.equals(other.name) &&
                    this.points == other.points &&
                    this.otherPoints == other.otherPoints;
        }

        @Override
        public String toString() {
            return "DataTestUUID{" +
                    "uuid=" + this.uuid +
                    ", name='" + this.name + '\'' +
                    ", points=" + this.points +
                    ", otherPoints=" + this.otherPoints +
                    '}';
        }
    }
}
