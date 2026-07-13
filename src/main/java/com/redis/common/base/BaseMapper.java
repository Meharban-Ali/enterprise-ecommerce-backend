package com.redis.common.base;

public interface BaseMapper<D, E> {
    D toDto(E entity);
    E toEntity(D dto);
}
