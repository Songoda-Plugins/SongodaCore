package com.songoda.core.data;

import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.UpdateConditionStep;
import org.jooq.impl.DSL;

public class SQLWhere extends SQLExecutable {
    public SQLWhere(DSLContext ctx, Query query) {
        super(ctx, query);
    }

    public SQLWhere and(String id, Object value) {
        this.query = ((UpdateConditionStep) this.query).and(DSL.field(id).eq(value));
        return this;
    }

    public SQLWhere or(String id, Object value) {
        this.query = ((UpdateConditionStep) this.query).or(DSL.field(id).eq(value));
        return this;
    }
}
