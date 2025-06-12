package com.songoda.core.data;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertOnDuplicateSetMoreStep;
import org.jooq.InsertOnDuplicateSetStep;
import org.jooq.InsertValuesStepN;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SQLInsert extends SQLBase {
    private String table;
    private final Map<String, Object> fields = new LinkedHashMap<>();
    private boolean voidedKey = false;

    private InsertValuesStepN currentStep;

    public SQLInsert(DSLContext ctx) {
        super(ctx);
    }

    public static SQLInsert create(DSLContext ctx) {
        return new SQLInsert(ctx);
    }

    public SQLInsert insertInto(String table) {
        this.table = table;
        return this;
    }

    public SQLInsert withField(String field, Object value, boolean voidKey) {
        if (voidKey) {
            this.voidedKey = true;
        }
        return voidKey ? this : withField(field, value);
    }

    public SQLInsert withField(String field, Object value) {
        if (field.equalsIgnoreCase("desc")) {
            field = "`desc`";
        }
        this.fields.put(field.toLowerCase(), value);
        return this;
    }

    // For some reason, this must be used before we submit. So I'm going to apply the values here.
    public SQLExecutable onDuplicateKeyUpdate(String... columns) {
        this.currentStep = this.ctx.insertInto(DSL.table(this.table), this.fields.keySet().stream().map(DSL::field).toArray(Field[]::new));
        this.currentStep = this.currentStep.values(this.fields.values().stream().map(this::cleanValue).toArray());

        if (this.voidedKey) {
            return new SQLExecutable(this.ctx, this.currentStep);
        }

        SQLOnDupeUpdate sqlOnDupeUpdate = new SQLOnDupeUpdate(this.ctx, this.currentStep, columns);
        for (String column : (columns.length > 0 ? new HashSet<>(Arrays.asList(columns)) : this.fields.keySet())) {
            sqlOnDupeUpdate.set(column.replace("`", ""), this.fields.get(column.toLowerCase()));
        }
        return sqlOnDupeUpdate;
    }

    public static class SQLOnDupeUpdate extends SQLExecutable {
        private InsertOnDuplicateSetStep currentStep;
        private final List<String> columnsToUpdate = new ArrayList<>();

        public SQLOnDupeUpdate(DSLContext ctx, InsertValuesStepN currentStep, String... columns) {
            super(ctx, null);
            this.currentStep = currentStep.onDuplicateKeyUpdate();
            this.columnsToUpdate.addAll(Arrays.asList(columns));
        }

        public SQLOnDupeUpdate set(String field, Object value) {
            if (!this.columnsToUpdate.isEmpty() && !this.columnsToUpdate.contains(field)) {
                return this;
            }

            value = cleanValue(value);
            Field fieldName = getField(field, value == null ? null : value.getClass());

            if (this.currentStep != null) {
                this.query = value == null ? this.currentStep.setNull(fieldName)
                        : this.currentStep.set(fieldName, value);
                this.currentStep = null;
            } else {
                this.query = value == null ? ((InsertOnDuplicateSetMoreStep) this.query).setNull(fieldName)
                        : ((InsertOnDuplicateSetMoreStep) this.query).set(fieldName, value);
            }

            return this;
        }
    }
}
