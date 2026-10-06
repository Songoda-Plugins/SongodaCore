package com.songoda.core.vinject.database;

import net.vortexdevelopment.vinject.database.repository.CachedCrudRepository;

/**
 * SongodaCore compatibility name for a VInject cached CRUD repository.
 *
 * @param <T>  entity type
 * @param <ID> primary-key type
 */
public interface CachedRepository<T, ID> extends CachedCrudRepository<T, ID> {
}
