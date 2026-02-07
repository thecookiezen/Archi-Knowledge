package com.thecookiezen.ladybugdb.spring.repository.support;

import org.springframework.data.repository.core.EntityInformation;

/**
 * Entity information for LadybugDB node entities.
 *
 * @param <T>  the entity type
 * @param <ID> the ID type
 */
public class LadybugDBEntityInformation<T, ID> implements EntityInformation<T, ID> {

    private final NodeMetadata<T> metadata;

    public LadybugDBEntityInformation(Class<T> domainClass) {
        this.metadata = new NodeMetadata<>(domainClass);
    }

    @Override
    public boolean isNew(T entity) {
        ID id = getId(entity);
        return id == null;
    }

    @Override
    public ID getId(T entity) {
        return metadata.getId(entity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<ID> getIdType() {
        if (metadata.getIdType() != null) {
            return (Class<ID>) metadata.getIdType();
        }
        return (Class<ID>) Object.class;
    }

    @Override
    public Class<T> getJavaType() {
        return metadata.getEntityType();
    }
}
