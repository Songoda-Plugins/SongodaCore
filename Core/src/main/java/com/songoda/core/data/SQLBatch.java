package com.songoda.core.data;

import org.jooq.DSLContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class SQLBatch implements SavesData {
    private final List<SavesData> batch = new LinkedList<>();

    public SQLBatch add(SavesData... data) {
        this.batch.addAll(Arrays.asList(data));
        return this;
    }

    public SQLBatch addAll(Collection<? extends SavesData> data) {
        this.batch.addAll(data);
        return this;
    }

    public List<SavesData> getBatch() {
        return this.batch;
    }

    @Override
    public void save(String... columns) {
        DatabaseManager.getInstance().getDatabaseConnector().connect(ctx -> {
            for (SavesData data : this.batch) {
                data.saveImpl(ctx, columns);
            }
        });
    }

    @Override
    public void saveImpl(DSLContext ctx, String... columns) {
        for (SavesData data : this.batch) {
            data.saveImpl(ctx, columns);
        }
    }

    @Override
    public void deleteImpl(DSLContext ctx) {
        for (SavesData data : this.batch) {
            data.deleteImpl(ctx);
        }
    }
}
