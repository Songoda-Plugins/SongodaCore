package com.songoda.core.vinject.database;

import net.vortexdevelopment.vinject.annotation.database.EnableCaching;
import net.vortexdevelopment.vinject.database.cache.CachePolicy;
import net.vortexdevelopment.vinject.database.repository.CachedCrudRepository;

/**
 * A CRUD repository with preloaded static caching enabled.
 * Should only be used where we know the data set is small
 *
 * @param <T>  the entity type
 * @param <ID> the ID type
 */
@EnableCaching(
        preload = true,
        policy = CachePolicy.TTL,
        maxSize = Integer.MAX_VALUE
)
public interface PreloadedCrudRepository<T, ID> extends CachedCrudRepository<T, ID> {
}
