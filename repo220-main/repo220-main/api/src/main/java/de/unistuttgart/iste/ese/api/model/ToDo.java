package de.unistuttgart.iste.ese.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A class representing a ToDo item.
 */
@Entity
public class ToDo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    private String description;

    private boolean finished;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "todo_assignee",
        joinColumns = @JoinColumn(name = "todo_id"),
        inverseJoinColumns = @JoinColumn(name = "assignee_id")
    )
    private List<Assignee> assigneeList = new ArrayList<>();

    @Transient
    private List<Long> assigneeIdList = new ArrayList<>();

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    private Date dueDate;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    private Date finishedDate;

    private String category;

    // Getters and setters

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public List<Assignee> getAssigneeList() {
        return assigneeList;
    }

    public List<Long> getAssigneeIdList() {
            return assigneeIdList;
    }

    public void setAssigneeIdList(List<Long> assigneeIdList) {
        this.assigneeIdList = assigneeIdList;
    }

    public void setAssigneeList(List<Assignee> assigneeList) {
        this.assigneeList = assigneeList;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Date getFinishedDate() {
        return finishedDate;
    }

    public void setFinishedDate(Date finishedDate) {
        this.finishedDate = finishedDate;
    }
}

