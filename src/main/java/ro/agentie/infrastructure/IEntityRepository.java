package ro.agentie.infrastructure;

import java.util.*;
import ro.agentie.domain.shared.*;

public interface IEntityRepository<T extends Entitate> {
    Integer getNextID();

    T add(T entity);

    default T save(T entity) {
        return add(entity);
    }

    Optional<T> findById(Integer id);

    default T getById(Integer id) {
        return findById(id)
                .orElseThrow(() -> new RegulaBusinessException("Entitate inexistenta: " + id));
    }

    List<T> toCollection();

    boolean remove(T entity);

    int size();
}
