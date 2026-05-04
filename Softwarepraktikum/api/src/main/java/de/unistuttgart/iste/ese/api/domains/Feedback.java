package de.unistuttgart.iste.ese.api.domains;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Klasse für die Feedback-Entität
 */
@Entity
@Table(name = "feedback")
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // Beziehung zur Workshop-Entität, wird beim Löschen des Workshops ebenfalls gelöscht
    @ManyToOne
    @JoinColumn(name = "workshop_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnoreProperties("feedbacks")
    private Workshop workshop;

    // Beziehung zur Employee-Entität, optional für anonymes Feedback
    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = true)
    private Employee employee;

    // Bewertung des Workshops (1 bis 5)
    @NotNull
    @Min(1)
    @Max(5)
    private int rating;

    // Optionaler Kommentar
    @Column(columnDefinition = "TEXT")
    private String comment;

    // Gibt an, ob das Feedback anonym ist
    @NotNull
    private boolean isAnonymous;

    // Für anonymes Feedback: verschlüsselter Token der den Mitarbeiter identifiziert
    @Column(name = "anonymous_token", unique = true)
    private String anonymousToken;

    // Zeitpunkt des Feedbacks
    @NotNull
    private LocalDateTime timestamp;

    // Liste von Feedback-Nachrichten, wird zusammen mit Feedback verwaltet
    // `cascade = CascadeType.ALL`: Änderungen an Feedback wirken sich automatisch auf FeedbackMessages aus (z. B. Speichern, Löschen)
    // `orphanRemoval = true`: Nachrichten werden entfernt, wenn sie nicht mehr mit einem Feedback verknüpft sind
    // `fetch = FetchType.EAGER`: Nachrichten werden immer mitgeladen, wenn Feedback geladen wird
    @OneToMany(mappedBy = "feedback", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonIgnoreProperties("feedback")
    private List<FeedbackMessage> messages = new ArrayList<>();

    // Bewertung des Trainers (optional)
    @Column(name = "trainer_rating")
    @Min(1)
    @Max(5)
    private Integer trainerRating;

    // Kommentar zum Trainer (optional)
    @Column(name = "trainer_comment", columnDefinition = "TEXT")
    private String trainerComment;

    // Standard Konstruktor
    public Feedback() {}

    // Konstruktor
    public Feedback(Workshop workshop, Employee employee, int rating,
                    String comment, boolean isAnonymous) {
        this.workshop = workshop;
        this.rating = rating;
        this.comment = comment;
        this.isAnonymous = isAnonymous;
        this.timestamp = LocalDateTime.now();

        if (isAnonymous) {
            // Generiere einen einzigartigen Token für anonymes Feedback
            this.anonymousToken = employee.getId() + "-" + UUID.randomUUID().toString();
            this.employee = null;  // Entferne Employee-Referenz
        } else {
            this.employee = employee;
            this.anonymousToken = null;
        }
    }

    // Getter und Setter
    public String getAnonymousToken() {
        return anonymousToken;
    }

    public void setAnonymousToken(String anonymousToken) {
        this.anonymousToken = anonymousToken;
    }

    // Getter und Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Workshop getWorkshop() {
        return workshop;
    }

    public void setWorkshop(Workshop workshop) {
        this.workshop = workshop;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public void setAnonymous(boolean anonymous) {
        isAnonymous = anonymous;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<FeedbackMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<FeedbackMessage> messages) {
        this.messages = messages;
    }

    public Integer getTrainerRating() {
        return trainerRating;
    }

    public void setTrainerRating(Integer trainerRating) {
        this.trainerRating = trainerRating;
    }

    public String getTrainerComment() {
        return trainerComment;
    }

    public void setTrainerComment(String trainerComment) {
        this.trainerComment = trainerComment;
    }
}
