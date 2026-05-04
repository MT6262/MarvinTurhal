package de.unistuttgart.iste.ese.api.repositories;

import de.unistuttgart.iste.ese.api.domains.Employee;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends CrudRepository<Employee, Integer> {
    Employee findByFirstNameAndLastName(String firstName, String lastName);

    Optional<Employee> findByEmail(String email);

    List<Employee> findByAdminTrue();
}
