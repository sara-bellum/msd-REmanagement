package ro.agentie.services;

import jakarta.validation.Validator;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import ro.agentie.domain.shared.Reguli;

@Service
public class ValidatingDomainService implements IValidatingDomainService {
    private final Validator validator;

    public ValidatingDomainService(Validator validator) {
        this.validator = validator;
    }

    public <T> Set<String> validate(T entity) {
        if (entity == null) return Set.of("Entitatea este obligatorie!");
        return validator.validate(entity).stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.toSet());
    }

    public <T> void validateWithException(T entity) {
        Set<String> errors = validate(entity);
        Reguli.cere(errors.isEmpty(), String.join("; ", errors));
    }
}
