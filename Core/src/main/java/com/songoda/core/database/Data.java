package com.songoda.core.database;

import java.util.Map;
import java.util.UUID;

public interface Data {
    /**
     * Gets the auto increment id of this data
     *
     * @return The auto increment id or -1 if not applicable
     */
    default int getId() {
        return -1;
    }

    /**
     * Gets the unique id of this data
     *
     * @return The unique id or null if not applicable
     */
    default UUID getUniqueId() {
        return null;
    }

    /**
     * Serializes the data into a map
     * to save to the database
     *
     * @return The serialized data
     */
    Map<String, Object> serialize();

    /**
     * Method used to deserialize the data
     *
     * @param map The map to deserialize
     */
    Data deserialize(Map<String, Object> map);

    /**
     * No plugin prefix is required for the table
     *
     * @return The table name where the data should be stored
     */
    String getTableName();
}
