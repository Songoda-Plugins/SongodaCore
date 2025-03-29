package com.songoda.core.data;

import org.jooq.DSLContext;

public interface LoadsData {
    default void loadData() {
        DatabaseManager.getInstance().getDatabaseConnector().connect(false, ctx -> {
            setupTables(ctx);
            loadDataImpl(ctx);
        });
    }

    void loadDataImpl(DSLContext ctx);

    void setupTables(DSLContext ctx);
}
