package com.songoda.core.data;

import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.impl.DSL;

public interface SavesData {
    default void save(String... columns) {
        DatabaseManager.getInstance().getDatabaseConnector().connect(ctx ->
                saveImpl(ctx, columns));
    }

    default void save(Runnable callback, String... columns) {
        DatabaseManager.getInstance().getDatabaseConnector().connect(ctx -> {
            saveImpl(ctx, columns);
            callback.run();
        });
    }

    default void delete() {
        DatabaseManager.getInstance().getDatabaseConnector().connect(this::deleteImpl);
    }

    void saveImpl(DSLContext ctx, String... columns);

    void deleteImpl(DSLContext ctx);

    default int lastInsertedId(String table, DSLContext ctx) {
        try {
            Result<Record1<Object>> results = ctx.select(DSL.field("id")).from(DSL.table(table)).orderBy(DSL.field("id").desc()).fetch();
            Record1<Object> result = results.get(0);
            return result != null ? result.get(DSL.field("id"), Integer.class) : -1;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return -1;
    }
}
