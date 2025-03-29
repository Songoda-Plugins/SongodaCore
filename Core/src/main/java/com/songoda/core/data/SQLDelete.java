package com.songoda.core.data;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;

public class SQLDelete extends SQLBase {
    private SQLDelete(DSLContext ctx) {
        super(ctx);
    }

    public static SQLDelete create(DSLContext ctx) {
        return new SQLDelete(ctx);
    }

    public void delete(String table, String id, Object value) {
        new SQLExecutable(this.ctx, this.ctx.delete(DSL.table(table)).where(DSL.field(id).eq(value))).execute();
    }

    public void delete(String table, String id, Object value, String id2, Object value2) {
        new SQLExecutable(this.ctx, this.ctx.delete(DSL.table(table)).where(DSL.field(id).eq(value))
                .and(DSL.field(id2).eq(value2))).execute();
    }
}
