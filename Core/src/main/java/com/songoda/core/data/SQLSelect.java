package com.songoda.core.data;

import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.Select;
import org.jooq.SelectSelectStep;
import org.jooq.SelectWhereStep;
import org.jooq.impl.DSL;

public class SQLSelect extends SQLBase {
    private Select<?> currentStep;

    public SQLSelect(DSLContext ctx) {
        super(ctx);
        this.currentStep = ctx.select();
    }

    public static SQLSelect create(DSLContext ctx) {
        return new SQLSelect(ctx);
    }

    public SQLSelect select(String... fields) {
        if (fields.length > 0) {
            this.currentStep = this.ctx.select(DSL.field(fields[0]));
            for (int i = 1; i < fields.length; i++) {
                this.currentStep = ((SelectSelectStep<?>) this.currentStep).select(DSL.field(fields[i]));
            }
        } else {
            this.currentStep = this.ctx.select();
        }
        return this;
    }

    public SQLSelect from(String table) {
        this.currentStep = ((SelectSelectStep<?>) this.currentStep).from(DSL.table(table));
        return this;
    }

    public void from(String table, SQLResult.SQLResultI result) {
        Result<?> resultData = ((SelectSelectStep<?>) this.currentStep).from(DSL.table(table)).fetch();
        SQLResult rs = new SQLResult(resultData);
        for (SQLResult.StoredRecord record : rs.getResults()) {
            result.forEach(record);
        }
    }

    public SQLWhere where(String id, Object value) {
        this.currentStep = ((SelectWhereStep<?>) this.currentStep).where(DSL.field(id).eq(value));
        return new SQLWhere(this.ctx, this.currentStep);
    }

    public SQLWhere whereBetween(String id, Object value1, Object value2) {
        this.currentStep = ((SelectWhereStep<?>) this.currentStep).where(DSL.field(id).between(value1, value2));
        return new SQLWhere(this.ctx, this.currentStep);
    }
}
