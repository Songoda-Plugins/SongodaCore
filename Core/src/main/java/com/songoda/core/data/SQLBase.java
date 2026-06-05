package com.songoda.core.data;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;

import java.util.Collection;

public class SQLBase {
    protected final DSLContext ctx;

    public SQLBase(DSLContext ctx) {
        this.ctx = ctx;
    }

    protected Object cleanValue(Object value) {
        if (value instanceof ComputedValue) {
            ComputedValue c = (ComputedValue) value;
            value = c.compute();
        }
        if (value instanceof Collection) {
            Collection<?> c = (Collection<?>) value;
            if (c.isEmpty()) {
                value = null;
            }
        }
        if (value instanceof String) {
            String s = (String) value;
            if (s.trim().isEmpty()) {
                value = null;
            }
        }
        if (value instanceof Boolean) {
            value = (Boolean) value ? 1 : 0;
        }
        return value;
    }

    protected Field getField(String field, Class<?> type) {
        String fieldFinal = "`" + field + "`";
        if (type == boolean.class) {
            return DSL.field(fieldFinal, int.class);
        }

        return type == null ? DSL.field(fieldFinal) : DSL.field(fieldFinal, type);
    }
}
