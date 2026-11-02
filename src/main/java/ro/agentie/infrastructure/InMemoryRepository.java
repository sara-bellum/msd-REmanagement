package ro.agentie.infrastructure;

import java.util.*;
import ro.agentie.domain.shared.Entitate;

/** Object references in memory, as in the course example. No persistence/rollback. */
public abstract class InMemoryRepository<T extends Entitate> implements IEntityRepository<T> {
    private int nextId;
    private final Map<Integer, T> entities = new LinkedHashMap<>();

    @Override
    public Integer getNextID() {
        return ++nextId;
    }

    @Override
    public T add(T entity) {
        Objects.requireNonNull(entity);
        entities.put(entity.getId(), entity);
        nextId = Math.max(nextId, entity.getId());
        return entity;
    }

    @Override
    public Optional<T> findById(Integer id) {
        return Optional.ofNullable(entities.get(id));
    }

    @Override
    public List<T> toCollection() {
        return List.copyOf(entities.values());
    }

    @Override
    public boolean remove(T entity) {
        return entities.remove(entity.getId(), entity);
    }

    @Override
    public int size() {
        return entities.size();
    }
}
