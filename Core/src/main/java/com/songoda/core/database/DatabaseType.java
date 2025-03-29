package com.songoda.core.database;

import org.jooq.SQLDialect;

public enum DatabaseType {
    MARIADB,
    MYSQL,
    H2,
    SQLITE;

    public SQLDialect getDialect() {
        switch (this) {
            case MARIADB:
                return SQLDialect.MARIADB;
            case MYSQL:
                return SQLDialect.MYSQL;
            case SQLITE:
                return SQLDialect.SQLITE;
            case H2:
                return SQLDialect.H2;
            default:
                return SQLDialect.DEFAULT;
        }
    }
}
