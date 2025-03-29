package com.songoda.core.data;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SQLResult {
    private final List<StoredRecord> results = new LinkedList<>();

    public SQLResult(Result<?> result) {
        for (Record record : result) {
            Map<String, StoredData> map = new java.util.HashMap<>();
            for (Field<?> field : record.fields()) {
                String fieldName = field.getName();
                Object value = record.get(fieldName);
                map.put(fieldName, new StoredData(value));
            }
            this.results.add(new StoredRecord(map));
        }
    }

    public SQLResult(Record record) {
        Map<String, StoredData> map = new java.util.HashMap<>();
        for (Field<?> field : record.fields()) {
            String fieldName = field.getName();
            Object value = record.get(fieldName);
            map.put(fieldName, new StoredData(value));
        }
        this.results.add(new StoredRecord(map));
    }

    public List<StoredRecord> getResults() {
        return this.results;
    }

    public interface SQLResultI {
        void forEach(StoredRecord result);
    }

    public static class StoredRecord {
        private final Map<String, StoredData> record;

        public StoredRecord(Map<String, StoredData> record) {
            this.record = record;
        }

        public StoredData get(String key) {
            return this.record.get(key);
        }

        public boolean has(String key) {
            return this.record.containsKey(key) && this.record.get(key).asString() != null;
        }

        public boolean isNull(String key) {
            return this.record.containsKey(key) && this.record.get(key).isNull();
        }
    }
}
