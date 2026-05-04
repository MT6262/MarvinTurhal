package de.unistuttgart.iste.ese.api.domains;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Klasse für die Exchange Day-Entität
 */
@Entity
@Table(name = "exchange_days")
public class ExchangeDay {

    /**
     * Eindeutige ID für den Exchange Day, die automatisch generiert wird.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @NotNull
    @Column(name = "start_time")
    private LocalDateTime startTime;

    @NotNull
    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    // Standard Konstruktor für JPA
    public ExchangeDay() {}

    // Konstruktor mit Parametern
    public ExchangeDay(LocalDateTime startTime,LocalDateTime endTime, String title, String description) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.title = title;
        this.description = description;
    }

    // Getter und Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getStartTime() {  // Geändert von getEventDate
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {  // Geändert von setEventDate
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {  // Neu
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {  // Neu
        this.endTime = endTime;
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
}

