package de.unistuttgart.iste.ese.api.domains;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Klasse für die Workshops
 */
@Entity
@Table(name = "workshops")
public class Workshop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    // Der Titel des Workshops muss zwischen 1 und 100 Zeichen lang sein
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "title")
    private String title;

    // Beschreibung des Workshops (optional)
    @Column(name = "description")
    private String description;

    // Startzeit des Workshops
    @NotNull
    @Column(name = "start_time")
    private LocalDateTime startTime;

    // Endzeit des Workshops
    @NotNull
    @Column(name = "end_time")
    private LocalDateTime endTime;

    // Maximale Teilnehmerzahl des Workshops (nicht mehr als 30)
    @NotNull
    @Max(30)
    @Column(name = "max_participants")
    private int maxParticipants;

    // Ort des Workshops (optional)
    @Column(name = "location")
    private String location;

    // Fremdschlüssel, Trainer des Workshops, ein Workshop kann einen Trainer haben, werden sofort geladen
    @ManyToOne(fetch = FetchType.EAGER)  // Ändern zu EAGER loading
    @JoinColumn(name = "trainer_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Employee trainer;

    // Fremdschlüssel, ExchangeDay zu dem Workshop gehören kann
    @ManyToOne
    @JoinColumn(name = "exchange_day_id")
    private ExchangeDay exchangeDay;

    // Liste von Registrierungen, die zu diesem Workshop gehören, Registrierung wird sofort entfernt wenn der Workshop gelöscht wird
    @OneToMany(mappedBy = "workshop", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Registration> registrations;

    // Feedback zum Workshop, Workshop kann viele Feedbacks haben, wenn der Workshop gelöscht wird werden auch die zugehörigen Feedbacks gelöscht
    @OneToMany(mappedBy = "workshop", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("workshop")
    private List<Feedback> feedbacks;


    // Standard Konstruktor für JPA
    public Workshop() {}

    // Konstruktor mit Parametern
    public Workshop(String title, String description, LocalDateTime startTime,
                    LocalDateTime endTime, int maxParticipants, String location) {
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxParticipants = maxParticipants;
        this.location = location;
    }

    // Getter und Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
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

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Employee getTrainer() {
        return trainer;
    }

    public void setTrainer(Employee trainer) {
        this.trainer = trainer;
    }

    public ExchangeDay getExchangeDay() {
        return exchangeDay;
    }

    public void setExchangeDay(ExchangeDay exchangeDay) {
        this.exchangeDay = exchangeDay;
    }

    // Berechnet die Anzahl der aktuellen Teilnehmer, indem alle Registrierungen gezählt werden, deren Status "REGISTERED" ist
    @Transient
    public int getCurrentParticipants() {
        if (registrations == null) return 0;
        return (int) registrations.stream()
            .filter(r -> r.getStatus() == Registration.RegistrationStatus.REGISTERED)
            .count();
    }

    public List<Feedback> getFeedbacks() {
        return feedbacks;
    }

    public void setFeedbacks(List<Feedback> feedbacks) {
        this.feedbacks = feedbacks;
    }

    public void setRegistrations(List<Registration> registrations) {
        this.registrations = registrations;
    }
}
