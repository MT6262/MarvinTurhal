package de.unistuttgart.iste.ese.api.repository;

import de.unistuttgart.iste.ese.api.model.Assignee;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * A repository for Assignee entities.
 */
public interface AssigneeRepository extends JpaRepository<Assignee, Long> {
}
