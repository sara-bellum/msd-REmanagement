package ro.agentie.services;

import java.util.Set;

public interface IValidatingDomainService {
    <T> Set<String> validate(T entity);

    <T> void validateWithException(T entity);
}
