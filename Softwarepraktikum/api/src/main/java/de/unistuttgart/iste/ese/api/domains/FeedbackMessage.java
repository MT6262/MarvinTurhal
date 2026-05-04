package de.unistuttgart.iste.ese.api.domains;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Klasse für die Nachrichten zum Feedback
 */
@Entity
@Table(name = "feedback_messages")
public class FeedbackMessage {

    // Automatisch generierte ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // Fremdschlüssel, Beziehung zu Feedback: Jede Nachricht gehört zu genau einem Feedback, wird erst bei Zugriff geladen(LAZY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_id", nullable = false)
    @JsonIgnoreProperties("messages")
    private Feedback feedback;

    // Fremdschlüssel, Optionaler Bezug zu einem Mitarbeiter (Ersteller der Nachricht), wird direkt geladen(EAGER)
    @ManyToOne(fetch = FetchType.EAGER)  // Ändern zu EAGER
    @JoinColumn(name = "employee_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Employee employee;

    //Nachricht zum Feedback
    @Column(columnDefinition = "TEXT")
    private String message;

    // Zeitpunkt, an dem die Nachricht erstellt wurde
    @NotNull
    private LocalDateTime timestamp;

    // Gibt an, ob die Nachricht von einem Admin stammt
    private boolean isAdminMessage;

    // Gibt an, ob die Nachricht anonym ist
    private boolean isAnonymous;

    // ID des anonymen Autors, wenn `isAnonymous` true ist
    private Integer anonymousAuthorId;

    // Konstruktoren
    public FeedbackMessage() {}

    // Angepasster Konstruktor
    public FeedbackMessage(Feedback feedback, String message, boolean isAdminMessage, Employee employee, boolean isAnonymous) {
        this.feedback = feedback;
        this.message = message;
        this.isAdminMessage = isAdminMessage;
        this.employee = employee;
        this.isAnonymous = isAnonymous;

        // Wenn die Nachricht anonym ist, speichere die ID des Autors und setze den Employee auf null
        if (isAnonymous && employee != null) {
            this.anonymousAuthorId = employee.getId();
            this.employee = null;
        }
        // Setzt den Zeitpunkt auf die aktuelle Zeit
        this.timestamp = LocalDateTime.now();
    }

    // Getter und Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Feedback getFeedback() {
        return feedback;
    }

    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isAdminMessage() {
        return isAdminMessage;
    }

    public void setAdminMessage(boolean adminMessage) {
        isAdminMessage = adminMessage;
    }

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public void setAnonymous(boolean anonymous) {
        isAnonymous = anonymous;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Integer getAnonymousAuthorId() {
        return anonymousAuthorId;
    }

    public void setAnonymousAuthorId(Integer anonymousAuthorId) {
        this.anonymousAuthorId = anonymousAuthorId;
    }
}
