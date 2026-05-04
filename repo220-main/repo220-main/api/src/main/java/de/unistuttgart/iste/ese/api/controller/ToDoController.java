package de.unistuttgart.iste.ese.api.controller;

import de.unistuttgart.iste.ese.api.model.ToDo;
import de.unistuttgart.iste.ese.api.service.ToDoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;

/**
 * A controller class for handling HTTP requests related to ToDos.
 */
@RestController
@RequestMapping("/api/v1/todos")
public class ToDoController {

    private final ToDoService toDoService;

    public ToDoController(ToDoService toDoService) {
        this.toDoService = toDoService;
    }

    /**
     * Retrieves all ToDos from the database.
     *
     * @return A list of all ToDos.
     */
    @GetMapping
    public ResponseEntity<List<ToDo>> getAllToDos() {
        List<ToDo> toDos = toDoService.getAllToDos();
        return ResponseEntity.ok(toDos != null ? toDos : new ArrayList<>());
    }

    /**
     * Retrieves a ToDo by its ID.
     *
     * @param id The ID of the ToDo to retrieve.
     * @return The ToDo with the specified ID, or a 404 Not Found response if no such ToDo exists.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ToDo> getToDoById(@PathVariable Long id) {
        try {
            ToDo toDo = toDoService.getToDoById(id);
            return toDo != null ? ResponseEntity.ok(toDo) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Creates a new ToDo in the database.
     *
     * @param toDo The ToDo to create.
     * @return The created ToDo, or a 400 Bad Request response if the ToDo is invalid.
     */
    @PostMapping
    public ResponseEntity<ToDo> createToDo(@Valid @RequestBody ToDo toDo) {
        try {
            ToDo createdToDo = toDoService.createToDo(toDo);
            return new ResponseEntity<>(createdToDo, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Updates a ToDo in the database.
     *
     * @param id The ID of the ToDo to update.
     * @param toDo The updated ToDo.
     * @return The updated ToDo, or a 404 Not Found response if no such ToDo exists.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ToDo> updateToDo(@PathVariable Long id, @Valid @RequestBody ToDo toDo) {
        try {
            ToDo updatedToDo = toDoService.updateToDo(id, toDo);
            return ResponseEntity.ok(updatedToDo);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Deletes a ToDo from the database.
     *
     * @param id The ID of the ToDo to delete.
     * @return A 200 OK response if the ToDo was deleted successfully, or a 404 Not Found response if no such ToDo exists.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(@PathVariable Long id) {
        try {
            toDoService.deleteTodoById(id);
            return ResponseEntity.ok().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
