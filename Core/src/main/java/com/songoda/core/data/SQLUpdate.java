package com.songoda.core.data;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.UpdateSetMoreStep;
import org.jooq.UpdateSetStep;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SQLUpdate extends SQLBase {
    private final List<String> columnsToUpdate = new ArrayList<>();

    private UpdateSetStep currentStep;

    private SQLUpdate(DSLContext ctx, String... columns) {
        super(ctx);
        this.columnsToUpdate.addAll(Arrays.asList(columns));
    }

    public static SQLUpdate create(DSLContext ctx, String... columns) {
        return new SQLUpdate(ctx, columns);
    }

    public SQLUpdate update(String table) {
        this.currentStep = this.ctx.update(DSL.table(table));
        return this;
    }

    public SQLUpdate set(String field, Object value) {
        if (!this.columnsToUpdate.isEmpty() && !this.columnsToUpdate.contains(field)) {
            return this;
        }

        value = cleanValue(value);
        Field fieldName = getField(field, value == null ? null : value.getClass());

        this.currentStep = value == null ? this.currentStep.setNull(fieldName) : this.currentStep.set(fieldName, value);
        return this;
    }

    public SQLWhere where(String id, Object value) {
        return new SQLWhere(this.ctx, ((UpdateSetMoreStep) this.currentStep).where(DSL.field(id).eq(value)));
    }
}
