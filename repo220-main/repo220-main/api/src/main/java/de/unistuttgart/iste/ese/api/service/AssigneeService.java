package de.unistuttgart.iste.ese.api.service;

import de.unistuttgart.iste.ese.api.model.Assignee;
import de.unistuttgart.iste.ese.api.model.ToDo;
import de.unistuttgart.iste.ese.api.repository.AssigneeRepository;
import de.unistuttgart.iste.ese.api.repository.ToDoRepository;
import org.springframework.stereotype.Service;
import javax.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;


/**
 * Service class for managing assignees.
 */
@Service
public class AssigneeService {

    private final AssigneeRepository assigneeRepository;
    private final ToDoRepository toDoRepository;

    public AssigneeService(AssigneeRepository assigneeRepository, ToDoRepository toDoRepository) {
        this.assigneeRepository = assigneeRepository;
        this.toDoRepository = toDoRepository;
    }

    public Assignee createAssignee(@Valid Assignee assignee) {
        validateAssignee(assignee);
        return assigneeRepository.save(assignee);
    }

    public Assignee getAssigneeById(Long id) {
        return assigneeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Assignee not found with id: " + id));
    }

    public List<Assignee> getAllAssignees() {
        return assigneeRepository.findAll();
    }

    /**
     * Updates an assignee by id.
     * @param id The id of the assignee to update.
     * @param assignee The assignee object with the updated values.
     * @return The updated assignee.
     */
    public Assignee updateAssignee(Long id, @Valid Assignee assignee) {
        if (!assigneeRepository.existsById(id)) {
            throw new NoSuchElementException("Assignee not found with id: " + id);
        }
        validateAssignee(assignee);
        assignee.setId(id);
        return assigneeRepository.save(assignee);
    }

    /**
     * Deletes an assignee by id.
     * @param id The id of the assignee to delete.
     * @return true if the assignee was deleted, false if the assignee was not found.
     */
    public boolean deleteAssignee(Long id) {
        if (assigneeRepository.existsById(id)) {
            // First update all todos that reference this assignee
            List<ToDo> todos = toDoRepository.findAll();
            for (ToDo todo : todos) {
                todo.getAssigneeList().removeIf(assignee -> assignee.getId().equals(id));
                toDoRepository.save(todo);
            }
            // Then delete the assignee
            assigneeRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Validates an assignee object.
     * @param assignee The assignee object to validate.
     * @throws IllegalArgumentException if the assignee object is invalid.
     */
    private void validateAssignee(Assignee assignee) {
        if (assignee.getPrename() == null || assignee.getPrename().trim().isEmpty()) {
            throw new IllegalArgumentException("Prename must be a non-empty string");
        }

        if (assignee.getName() == null || assignee.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name must be a non-empty string");
        }

        if (assignee.getEmail() == null || !isValidEmail(assignee.getEmail())) {
            throw new IllegalArgumentException("Email must be a valid email address ending with uni-stuttgart.de");
        }
    }

    /**
     * Checks if an email address is a valid university email address.
     * @param email The email address to check.
     * @return true if the email address is valid, false otherwise.
     */
    private boolean isValidEmail(String email) {
        return email != null &&
            email.matches("^[\\w-\\.]+@([\\w-]+\\.)+uni-stuttgart\\.de$");
    }
}
