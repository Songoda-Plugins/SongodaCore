package com.songoda.core.database;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DataMigrationManager {

    private MigrationRepository migrationRepository;

    private List<DataMigration> migrations = new ArrayList<>();

    public DataMigrationManager(MigrationRepository migrationRepository) {
        this.migrationRepository = migrationRepository;
    }

    public void registerMigration(@NotNull DataMigration migration) {
        migrations.add(migration);
    }

    public void registerMigrations(@NotNull List<DataMigration> migrations) {
        this.migrations.addAll(migrations);
    }

    public void runMigrations(Connection connection, String tablePrefix) {
        // Logic to run pending migrations
        MigrationVersion currentVersion = migrationRepository.findById(1L);
        if (currentVersion == null) {
            currentVersion = new MigrationVersion();
        }

        // If the currentVersion#getVersion() is less than the DataMigration#getRevision() for any registered migration, run that migration
        for (DataMigration migration : migrations) {
            if (currentVersion.getVersion() < migration.getRevision()) {
                try {
                    migration.migrate(connection, tablePrefix);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                currentVersion.setVersion(migration.getRevision());
                migrationRepository.save(currentVersion);
            }
        }
    }

}
