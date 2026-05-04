package de.unistuttgart.iste.ese.api.repository;

import de.unistuttgart.iste.ese.api.model.ToDo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * A repository for ToDo entities.
 */
public interface ToDoRepository extends JpaRepository<ToDo, Long> {
}
