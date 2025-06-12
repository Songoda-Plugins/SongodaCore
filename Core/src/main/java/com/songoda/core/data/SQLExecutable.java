package com.songoda.core.data;

import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Result;
import org.jooq.ResultQuery;

import java.util.List;
import java.util.stream.Stream;

public class SQLExecutable extends SQLBase {
    protected Query query;

    public SQLExecutable(DSLContext ctx, Query query) {
        super(ctx);
        this.query = query;
    }

    public int execute() {
        return this.query.execute();
    }

    public Stream<SQLResult.StoredRecord> get() {
        if (this.query instanceof ResultQuery) {
            ResultQuery<?> resultQuery = (ResultQuery<?>) this.query;
            Result<?> result = resultQuery.getResult();
            return result
                    .stream()
                    .map(SQLResult::new)
                    .map(SQLResult::getResults)
                    .flatMap(List::stream);
        } else {
            throw new IllegalStateException("Query is not an instance of ResultQuery");
        }
    }

    public interface SQLResultI {
        void forEach(SQLResult.StoredRecord result);
    }
}
