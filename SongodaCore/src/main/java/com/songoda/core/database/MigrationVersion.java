package com.songoda.core.database;

import lombok.Data;
import net.vortexdevelopment.vinject.annotation.database.Column;
import net.vortexdevelopment.vinject.annotation.database.Entity;

@Entity(table = "migration_version")
@Data
public class MigrationVersion {

    @Column(primaryKey = true, nullable = false)
    private Integer id = 1;
    private Integer version = 0;
}