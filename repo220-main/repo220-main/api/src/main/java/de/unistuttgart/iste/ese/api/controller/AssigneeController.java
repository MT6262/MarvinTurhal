package de.unistuttgart.iste.ese.api.controller;

import de.unistuttgart.iste.ese.api.model.Assignee;
import de.unistuttgart.iste.ese.api.service.AssigneeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A controller class for handling HTTP requests related to assignees.
 */
@RestController
@RequestMapping("/api/v1/assignees")
public class AssigneeController {

    private final AssigneeService assigneeService;
    public AssigneeController(AssigneeService assigneeService) {
        this.assigneeService = assigneeService;
    }


    /**
     * Retrieves all assignees from the database.
     *
     * @return A list of all assignees.
     */
    @GetMapping
    public ResponseEntity<List<Assignee>> getAllAssignees() {
        List<Assignee> assignees = assigneeService.getAllAssignees();
        return ResponseEntity.ok(assignees);
    }

    /**
     * Retrieves an assignee by their ID.
     *
     * @param id The ID of the assignee to retrieve.
     * @return The assignee with the specified ID, or a 404 Not Found response if no such assignee exists.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Assignee> getAssigneeById(@PathVariable Long id) {
        Assignee assignee = assigneeService.getAssigneeById(id);
        return assignee != null ? ResponseEntity.ok(assignee) : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /**
     * Creates a new assignee in the database.
     *
     * @param assignee The assignee to create.
     * @return The created assignee, or a 400 Bad Request response if the assignee is invalid.
     */
    @PostMapping
    public ResponseEntity<Assignee> createAssignee(@Valid @RequestBody Assignee assignee) {
        try {
            Assignee createdAssignee = assigneeService.createAssignee(assignee);
            return new ResponseEntity<>(createdAssignee, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Updates an assignee in the database.
     *
     * @param id The ID of the assignee to update.
     * @param assignee The updated assignee data.
     * @return The updated assignee, or a 404 Not Found response if no such assignee exists, or a 400 Bad Request response if the assignee is invalid.
     */
    @PutMapping("/{id}")
public ResponseEntity<Assignee> updateAssignee(@PathVariable Long id, @Valid @RequestBody Assignee assignee) {
    try {
        Assignee updatedAssignee = assigneeService.updateAssignee(id, assignee);
        return ResponseEntity.ok(updatedAssignee);
    } catch (NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }
}

    /**
     * Deletes an assignee from the database.
     * @param id The ID of the assignee to delete.
     * @return The deleted assignee, or a 404 Not Found response if no such assignee exists.
     */
        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteAssignee(@PathVariable Long id) {
            boolean deleted = assigneeService.deleteAssignee(id);
            return deleted ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
