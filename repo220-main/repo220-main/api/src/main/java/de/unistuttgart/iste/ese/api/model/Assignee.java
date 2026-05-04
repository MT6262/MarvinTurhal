package de.unistuttgart.iste.ese.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * A class representing an assignee of a ToDo.
 */
@Entity
public class Assignee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String prename;

    @NotBlank
    private String name;

    @Email(regexp = "^[\\w-\\.]+@([\\w-]+\\.)+uni-stuttgart\\.de$")
    private String email;

    @JsonIgnore
    @ManyToMany(mappedBy = "assigneeList")
    private List<ToDo> todos;

    // Constructor
    public Assignee() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPrename() {
        return prename;
    }

    public void setPrename(String prename) {
        this.prename = prename;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @JsonIgnore
    public List<ToDo> getTodos() {
        return todos;
    }

    public void setTodos(List<ToDo> todos) {
        this.todos = todos;
    }
}

