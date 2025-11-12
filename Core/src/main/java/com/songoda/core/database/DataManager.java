package com.songoda.core.database;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.configuration.Config;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.jetbrains.annotations.NotNull;
import org.jooq.Condition;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DataManager {
    protected final SongodaPlugin plugin;
    protected final Config databaseConfig;
    private final List<DataMigration> migrations;
    protected DatabaseConnector databaseConnector;
    protected DatabaseType type;
    private final Map<String, AtomicInteger> autoIncrementCache = new HashMap<>();

    protected final ExecutorService asyncPool = new ThreadPoolExecutor(1, 5, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadFactoryBuilder().setNameFormat(getClass().getSimpleName() + "-Database-Async-%d").build());

    @Deprecated
    private static final Map<String, LinkedList<Runnable>> queues = new HashMap<>();

    DataManager() {
        this.databaseConfig = null;
        this.plugin = null;
        this.migrations = Collections.emptyList();
        this.databaseConnector = new H2Connector();
    }

    DataManager(DatabaseType type) {
        this.databaseConfig = null;
        this.plugin = null;
        this.migrations = Collections.emptyList();
        this.databaseConnector = new SQLiteConnector();
    }

    public DataManager(SongodaPlugin plugin, List<DataMigration> migrations) {
        this(plugin, migrations, null);
    }

    public DataManager(SongodaPlugin plugin, List<DataMigration> migrations, DatabaseType forcedType) {
        this.plugin = plugin;
        this.migrations = migrations;
        this.databaseConfig = plugin.getDatabaseConfig();

        try {
            load(forcedType);
        } catch (Exception ex) {
            // FIXME: This try-catch exists for backwards-compatibility reasons (I basically don't want to invest the necessary time to do it properly)
            throw new RuntimeException(ex);
        }
    }

    private void load(DatabaseType forcedType) throws SQLException {
        String databaseType = this.databaseConfig.getString("Connection Settings.Type").toUpperCase();
        if (forcedType != null) {
            databaseType = forcedType.name();
        }
        switch (databaseType) {
            case "MYSQL": {
                this.databaseConnector = new MySQLConnector(this.plugin, this.databaseConfig);
                break;
            }
            case "MARIADB": {
                this.databaseConnector = new MariaDBConnector(this.plugin, this.databaseConfig);
                break;
            }
            case "SQLITE": {
                this.databaseConnector = new SQLiteConnector(this.plugin);
                break;
            }
            default: {
                this.databaseConnector = new H2Connector(this.plugin);
                break;
            }
        }
        this.type = this.databaseConnector.getType();
        this.plugin.getLogger().info("Data handler connected using " + this.databaseConnector.getType().name() + ".");

        runMigrations();
    }

    /**
     * @return the database connector
     */
    public DatabaseConnector getDatabaseConnector() {
        return this.databaseConnector;
    }

    /**
     * @return the database executor service
     */
    public ExecutorService getAsyncPool() {
        return this.asyncPool;
    }

    /**
     * @return the prefix to be used by all table names
     */
    public String getTablePrefix() {
        if (this.plugin == null) {
            return "";
        }

        // Check if a custom table prefix is configured in database.yml
        if (this.databaseConfig != null) {
            String customPrefix = this.databaseConfig.getString("Connection Settings.Table Prefix", "");
            if (!customPrefix.isEmpty()) {
                // Ensure underscore at the end
                return customPrefix.endsWith("_") ? customPrefix : customPrefix + "_";
            }
        }

        // Default: use plugin name as prefix for backwards compatibility
        return this.plugin.getDescription().getName().toLowerCase() + '_';
    }

    /**
     * Runs any needed data migrations
     */
    public void runMigrations() throws SQLException {
        try (Connection connection = this.databaseConnector.getConnection()) {
            int currentMigration = -1;
            boolean migrationsExist;

//            DatabaseMetaData meta = connection.getMetaData();
//            ResultSet res = meta.getTables(null, null, this.getMigrationsTableName(), new String[] {"TABLE"});
//            migrationsExist = res.next();
            try {
                connection.createStatement().execute("SELECT 1 FROM " + this.getMigrationsTableName());
                migrationsExist = true;
            } catch (Exception ex) {
                migrationsExist = false;
            }

            if (!migrationsExist) {
                // No migration table exists, create one
                String createTable = "CREATE TABLE " + this.getMigrationsTableName() + " (migration_version INT NOT NULL)";
                try (PreparedStatement statement = connection.prepareStatement(createTable)) {
                    statement.execute();
                }

                // Insert primary row into migration table
                String insertRow = "INSERT INTO " + this.getMigrationsTableName() + " VALUES (?)";
                try (PreparedStatement statement = connection.prepareStatement(insertRow)) {
                    statement.setInt(1, -1);
                    statement.execute();
                }
            } else {
                // Grab the current migration version
                // Due to the automatic SQLite to H2 conversion that might have happened, two entries (one of them -1) might exist
                String selectVersion = "SELECT migration_version FROM " + this.getMigrationsTableName() + " ORDER BY migration_version DESC LIMIT 1";
                try (PreparedStatement statement = connection.prepareStatement(selectVersion)) {
                    ResultSet result = statement.executeQuery();
                    result.next();
                    currentMigration = result.getInt("migration_version");
                }
            }

            // Grab required migrations
            int finalCurrentMigration = currentMigration;
            List<DataMigration> requiredMigrations = this.migrations.stream()
                    .filter(x -> x.getRevision() > finalCurrentMigration)
                    .sorted(Comparator.comparingInt(DataMigration::getRevision))
                    .collect(Collectors.toList());

            // Nothing to migrate, abort
            if (requiredMigrations.isEmpty()) {
                return;
            }

            // Migrate the data
            for (DataMigration dataMigration : requiredMigrations) {
                dataMigration.migrate(connection, getTablePrefix());
            }

            // Set the new current migration to be the highest migrated to
            currentMigration = requiredMigrations.stream()
                    .map(DataMigration::getRevision)
                    .max(Integer::compareTo)
                    .orElse(-1);

            String updateVersion = "UPDATE " + this.getMigrationsTableName() + " SET migration_version = ?";
            try (PreparedStatement statement = connection.prepareStatement(updateVersion)) {
                statement.setInt(1, currentMigration);
                statement.execute();
            }
        }
    }

    /**
     * @return the name of the migrations table
     */
    private String getMigrationsTableName() {
        return getTablePrefix() + "migrations";
    }

    /**
     * @return The next auto increment value for the given table
     */
    public synchronized int getNextId(String table) {
        String prefixedTable = getTablePrefix() + table;
        if (!this.autoIncrementCache.containsKey(prefixedTable)) {
            this.databaseConnector.connectDSL(context -> {
//                context.select(DSL.max(DSL.field("id"))).from(prefixedTable).fetchOptional().ifPresentOrElse(record -> {
//                    if (record.get(0, Integer.class) == null) {
//                        this.autoIncrementCache.put(prefixedTable, new AtomicInteger(1));
//                        return;
//                    }
//                    this.autoIncrementCache.put(prefixedTable, new AtomicInteger(record.get(0, Integer.class)));
//                }, () -> this.autoIncrementCache.put(prefixedTable, new AtomicInteger(1)));
//
                //recreate upper method using java 8 syntax
                try {
                    Optional<Integer> max = context.select(DSL.max(DSL.field("id"))).from(prefixedTable).fetchOptional().map(record -> record.get(0, Integer.class));
                    this.autoIncrementCache.put(prefixedTable, new AtomicInteger(max.orElse(0)));
                } catch (Exception e) {
                    //Table is empty
                    this.autoIncrementCache.put(prefixedTable, new AtomicInteger(0));
                }
            });
        }
        return this.autoIncrementCache.get(prefixedTable).incrementAndGet();
    }

    // TODO: Fix/create javadocs for all methods

    /**
     * Saves the data to the database
     */
    public void save(Data data) {
        this.asyncPool.execute(() -> {
            saveSync(data);
        });
    }

    /**
     * Saves the data to the database
     */
    public void save(Data data, String idField, Object idValue) {
        this.asyncPool.execute(() -> {
            saveSync(data, idField, idValue);
        });
    }

    /**
     * Saves the data to the database
     */
    public void saveSync(Data data, String idField, Object idValue) {
        this.databaseConnector.connectDSL(context -> {
            context.insertInto(DSL.table(getTablePrefix() + data.getTableName()))
                    .set(data.serialize())
                    .onConflict(DSL.field(idField)).doUpdate()
                    .set(data.serialize())
                    .where(DSL.field(idField).eq(idValue))
                    .execute();
        });
    }

    /**
     * Saves the data to the database synchronously
     */
    public void saveSync(Data data) {
        this.databaseConnector.connectDSL(context -> {
            context.insertInto(DSL.table(getTablePrefix() + data.getTableName()))
                    .set(data.serialize())
                    .onConflict(data.getId() != -1 ? DSL.field("id") : DSL.field("uuid")).doUpdate()
                    .set(data.serialize())
                    .where(data.getId() != -1 ? DSL.field("id").eq(data.getId()) : DSL.field("uuid").eq(data.getUniqueId().toString()))
                    .execute();
        });
    }

    /**
     * Saves the data in batch to the database
     */
    public void saveBatch(Collection<Data> dataBatch) {
        this.asyncPool.execute(() -> {
            saveBatchSync(dataBatch);
        });
    }

    /**
     * Saves the data in batch to the database
     */
    public void saveBatchSync(Collection<Data> dataBatch) {
        this.databaseConnector.connectDSL(context -> {
            List<Query> queries = new ArrayList<>();
            for (Data data : dataBatch) {
                queries.add(context.insertInto(DSL.table(getTablePrefix() + data.getTableName()))
                        .set(data.serialize())
                        .onConflict(data.getId() != -1 ? DSL.field("id") : DSL.field("uuid")).doUpdate()
                        .set(data.serialize())
                        .where(data.getId() != -1 ? DSL.field("id").eq(data.getId()) : DSL.field("uuid").eq(data.getUniqueId().toString())));
            }

            context.batch(queries).execute();
        });
    }

    /**
     * Deletes the data from the database
     */
    public void delete(Data data) {
        this.asyncPool.execute(() -> {
            deleteSync(data);
        });
    }

    /**
     * Deletes the data from the database
     */
    public void deleteSync(Data data) {
        this.databaseConnector.connectDSL(context -> {
            context.delete(DSL.table(getTablePrefix() + data.getTableName()))
                    .where(data.getId() != -1 ? DSL.field("id").eq(data.getId()) : DSL.field("uuid").eq(data.getUniqueId().toString()))
                    .execute();
        });
    }

    public void delete(Data data, String idField, Object idValue) {
        this.asyncPool.execute(() -> {
            deleteSync(data, idField, idValue);
        });
    }

    public void deleteSync(Data data, String idField, Object idValue) {
        this.databaseConnector.connectDSL(context -> {
            context.delete(DSL.table(getTablePrefix() + data.getTableName()))
                    .where(DSL.field(idField).eq(idValue))
                    .execute();
        });
    }

    /**
     * Deletes the data from the database
     */
    public void delete(Data data, String uuidColumn) {
        this.asyncPool.execute(() -> {
            this.databaseConnector.connectDSL(context -> {
                context.delete(DSL.table(getTablePrefix() + data.getTableName()))
                        .where(data.getId() != -1 ? DSL.field("id").eq(data.getId()) : DSL.field(uuidColumn).eq(data.getUniqueId().toString()))
                        .execute();
            });
        });
    }

    /**
     * Loads the data from the database
     *
     * @param id The id of the data
     *
     * @return The loaded data
     */
    @SuppressWarnings("unchecked")
    public <T extends Data> T load(int id, Class<?> clazz, String table) {
        try {
            AtomicReference<Data> data = new AtomicReference<>();
            AtomicBoolean found = new AtomicBoolean(false);
            this.databaseConnector.connectDSL(context -> {
                try {
                    Data newData = (Data) clazz.getConstructor().newInstance();
                    data.set(newData.deserialize(Objects.requireNonNull(context.select()
                                    .from(DSL.table(getTablePrefix() + table))
                                    .where(DSL.field("id").eq(id))
                                    .fetchOne())
                            .intoMap()));
                    found.set(true);
                } catch (NullPointerException ignored) {
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            if (found.get()) {
                return (T) data.get();
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Loads the data from the database
     *
     * @param uuid  The uuid of the data
     * @param clazz The class of the data
     * @param table The table of the data without prefix
     *
     * @return The loaded data
     */
    @SuppressWarnings("unchecked")
    public <T extends Data> T load(UUID uuid, Class<?> clazz, String table) {
        try {
            AtomicReference<Data> data = new AtomicReference<>();
            AtomicBoolean found = new AtomicBoolean(false);
            this.databaseConnector.connectDSL(context -> {
                try {
                    Data newData = (Data) clazz.getConstructor().newInstance();
                    data.set(newData.deserialize(Objects.requireNonNull(context.select()
                                    .from(DSL.table(getTablePrefix() + table))
                                    .where(DSL.field("uuid").eq(uuid.toString()))
                                    .fetchOne())
                            .intoMap()));
                    found.set(true);
                } catch (NullPointerException ignored) {
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            if (found.get()) {
                return (T) data.get();
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Loads the data from the database
     *
     * @param uuid       The uuid of the data
     * @param clazz      The class of the data
     * @param table      The table of the data without prefix
     * @param uuidColumn The column of the uuid
     *
     * @return The loaded data
     */
    @SuppressWarnings("unchecked")
    public <T extends Data> T load(UUID uuid, Class<?> clazz, String table, String uuidColumn) {
        try {
            AtomicReference<Data> data = new AtomicReference<>();
            AtomicBoolean found = new AtomicBoolean(false);
            this.databaseConnector.connectDSL(context -> {
                try {
                    Data newData = (Data) clazz.getConstructor().newInstance();
                    data.set(newData.deserialize(Objects.requireNonNull(context.select()
                                    .from(DSL.table(getTablePrefix() + table))
                                    .where(DSL.field(uuidColumn).eq(uuid.toString()))
                                    .fetchOne())
                            .intoMap()));
                    found.set(true);
                } catch (NullPointerException ignored) {
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            if (found.get()) {
                return (T) data.get();
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Loads the data in batch from the database
     *
     * @return The loaded data
     */
    @SuppressWarnings("unchecked")
    public <T extends Data> List<T> loadBatch(Class<?> clazz, String table) {
        try {
            List<Data> dataList = Collections.synchronizedList(new ArrayList<>());
            this.databaseConnector.connectDSL(context -> {
                try {
                    for (@NotNull Record record : Objects.requireNonNull(context.select()
                            .from(DSL.table(getTablePrefix() + table))
                            .fetchArray())) {
                        Data data = (Data) clazz.getDeclaredConstructor().newInstance();
                        dataList.add(data.deserialize(record.intoMap()));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            return (List<T>) dataList;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Loads the data in batch from the database
     *
     * @return The loaded data
     */
    @SuppressWarnings("unchecked")
    public <T extends Data> List<T> loadBatch(Class<?> clazz, String table, Condition... conditions) {
        try {
            List<Data> dataList = Collections.synchronizedList(new ArrayList<>());
            this.databaseConnector.connectDSL(context -> {
                try {
                    for (@NotNull Record record : Objects.requireNonNull(context.select()
                            .from(DSL.table(getTablePrefix() + table))
                            .where(conditions)
                            .fetchArray())) {
                        Data data = (Data) clazz.getDeclaredConstructor().newInstance();
                        dataList.add(data.deserialize(record.intoMap()));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            return (List<T>) dataList;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Close the database and shutdown the async pool
     */
    public void shutdown() {
        this.asyncPool.shutdown();
        try {
            if (!this.asyncPool.awaitTermination(30, TimeUnit.SECONDS)) {
                this.plugin.getLogger().warning("Failed to shutdown the async DataManager pool in time. Forcing shutdown");
            }
        } catch (InterruptedException ex) {
            this.plugin.getLogger().warning("Error while shutting down the async DataManager pool: " + ex.getMessage());
        }
        this.asyncPool.shutdownNow();

        this.databaseConnector.closeConnection();
    }

    /**
     * Force shutdown the async pool and close the database
     *
     * @return Tasks that were still in the pool's queue
     */
    public List<Runnable> shutdownNow() {
        List<Runnable> tasksLeftInQueue = this.asyncPool.shutdownNow();
        this.databaseConnector.closeConnection();
        return tasksLeftInQueue;
    }

    public void shutdownTaskQueue() {
        this.asyncPool.shutdown();
    }

    public List<Runnable> forceShutdownTaskQueue() {
        return this.asyncPool.shutdownNow();
    }

    public boolean isTaskQueueTerminated() {
        return this.asyncPool.isTerminated();
    }

    public long getTaskQueueSize() {
        if (this.asyncPool instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) this.asyncPool).getTaskCount();
        }

        return -1;
    }

    /**
     * @see ExecutorService#awaitTermination(long, TimeUnit)
     */
    public boolean waitForShutdown(long timeout, TimeUnit unit) throws InterruptedException {
        return this.asyncPool.awaitTermination(timeout, unit);
    }

    public String getSyntax(String string, DatabaseType type) {
        if (this.type == type) {
            return string;
        }
        return "";
    }
}
