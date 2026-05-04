
package de.unistuttgart.iste.ese.api.service;

import de.unistuttgart.iste.ese.api.model.TodoModel;
import de.unistuttgart.iste.ese.api.model.Assignee;
import de.unistuttgart.iste.ese.api.model.ToDo;
import de.unistuttgart.iste.ese.api.repository.AssigneeRepository;
import de.unistuttgart.iste.ese.api.repository.ToDoRepository;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import java.util.*;

/**
 * Service class for managing todos.
 */
@Service
public class ToDoService {

    private final ToDoRepository toDoRepository;
    private final AssigneeRepository assigneeRepository;
    private final TodoModel todoModel;

    public ToDoService(ToDoRepository toDoRepository, AssigneeRepository assigneeRepository) {
        this.toDoRepository = toDoRepository;
        this.assigneeRepository = assigneeRepository;
        this.todoModel = new TodoModel("modelpmml.sec"); // Initialize the model with the correct path
    }

    /**
     * Creates a new todo.
     * @param toDo The todo object to create.
     * @return The created todo.
     */
    public ToDo createToDo(@Valid ToDo toDo) {
        validateToDo(toDo);

        // Set initial values
        toDo.setCreatedDate(new Date());
        toDo.setFinished(false);
        toDo.setFinishedDate(null);

        // Predict category using ML model
        String category = todoModel.predictClass(toDo.getTitle());
        toDo.setCategory(category);

        // Handle assignees
        if (toDo.getAssigneeIdList() != null && !toDo.getAssigneeIdList().isEmpty()) {
            List<Assignee> assignees = assigneeRepository.findAllById(toDo.getAssigneeIdList());
            if (assignees.size() != toDo.getAssigneeIdList().size()) {
                throw new IllegalArgumentException("One or more assignee IDs are invalid");
            }
            toDo.setAssigneeList(assignees);
        } else {
            toDo.setAssigneeList(new ArrayList<>());
        }

        return toDoRepository.save(toDo);
    }

    public ToDo getToDoById(Long id) {
        if (id == null) {
            throw new NoSuchElementException("Todo ID cannot be null");
        }
        return toDoRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Todo not found"));
    }

    public List<ToDo> getAllToDos() {
        return toDoRepository.findAll();
    }

    /**
     * Updates a todo by id.
     * @param id The id of the todo to update.
     * @param toDo The todo object with the updated values.
     * @return The updated todo.
     */
    public ToDo updateToDo(Long id, @Valid ToDo toDo) {
        if (!toDoRepository.existsById(id)) {
            throw new NoSuchElementException("Todo not found");
        }

        validateToDo(toDo);
        ToDo existingToDo = toDoRepository.findById(id).get();

        toDo.setId(id);
        toDo.setCreatedDate(existingToDo.getCreatedDate());

        // Handle finished state and date
        if (toDo.isFinished() && !existingToDo.isFinished()) {
            toDo.setFinishedDate(new Date());
        } else if (!toDo.isFinished()) {
            toDo.setFinishedDate(null);
        } else {
            toDo.setFinishedDate(existingToDo.getFinishedDate());
        }

        // Predict category using ML model
        String category = todoModel.predictClass(toDo.getTitle());
        toDo.setCategory(category);

        // Handle assignees
        if (toDo.getAssigneeIdList() != null) {
            List<Assignee> assignees = assigneeRepository.findAllById(toDo.getAssigneeIdList());
            if (!toDo.getAssigneeIdList().isEmpty() && assignees.size() != toDo.getAssigneeIdList().size()) {
                throw new IllegalArgumentException("One or more assignee IDs are invalid");
            }
            toDo.setAssigneeList(assignees);
        } else {
            toDo.setAssigneeList(new ArrayList<>());
        }

        return toDoRepository.save(toDo);
    }

    public void deleteTodoById(Long id) {
        if (!toDoRepository.existsById(id)) {
            throw new NoSuchElementException("Todo not found");
        }
        toDoRepository.deleteById(id);
    }

    /**
     * Validates a todo object.
     * @param toDo The todo object to validate.
     * @throws IllegalArgumentException if the todo object is invalid.
     */
    private void validateToDo(ToDo toDo) {
        // Validate title
        if (toDo.getTitle() == null || toDo.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title must not be empty");
        }

        // Validate assignees
        if (toDo.getAssigneeIdList() != null) {
            // Check for duplicates
            Set<Long> uniqueIds = new HashSet<>(toDo.getAssigneeIdList());
            if (uniqueIds.size() != toDo.getAssigneeIdList().size()) {
                throw new IllegalArgumentException("Duplicate assignees are not allowed");
            }

            // Check for negative IDs
            if (toDo.getAssigneeIdList().stream().anyMatch(id -> id < 0)) {
                throw new IllegalArgumentException("Negative assignee IDs are not allowed");
            }
        }

        // Validate due date format
        if (toDo.getDueDate() != null) {
            try {
                if (toDo.getDueDate().getTime() < 0) {
                    throw new IllegalArgumentException("Invalid due date");
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid due date format");
            }
        }
    }
}
