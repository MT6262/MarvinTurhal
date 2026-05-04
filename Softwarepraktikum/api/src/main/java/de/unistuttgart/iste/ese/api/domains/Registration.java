package de.unistuttgart.iste.ese.api.domains;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * Klasse für die Registrierungen zum Workshop
 */
@Entity
@Table(name = "registrations")
public class Registration {

    // Primärschlüssel, Automatisch generierte ID für jede Registrierung
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // Fremdschlüssel, registrierter Benutzer, Registrierung gehört zu einem Mitarbeiter
    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // Fremdschlüssel, Workshop für den sich Mitarbeiter registrieren will
    // Wenn der Workshop gelöscht wird, werden auch die Registrierungen gelöscht
    @ManyToOne
    @JoinColumn(name = "workshop_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Workshop workshop;

    // Fremdschlüssel, Registrierung kann zu einem ExchangeDay gehören
    // Setzt die Referenz auf NULL, wenn der ExchangeDay gelöscht wird
    @ManyToOne
    @JoinColumn(name = "exchange_day_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private ExchangeDay exchangeDay;

    // Zeitpunkt der Registrierung
    @NotNull
    private LocalDateTime registrationTime;

    // Status der Registrierung
    @NotNull
    @Enumerated(EnumType.STRING)
    private RegistrationStatus status;

    public enum RegistrationStatus {
        REGISTERED, CONFIRMED, CANCELLED
    }

    // Standard Konstruktor für JPA
    public Registration() {}

    // Konstruktor mit Parametern
    public Registration(Employee employee, Workshop workshop, ExchangeDay exchangeDay) {
        this.employee = employee;
        this.workshop = workshop;
        this.exchangeDay = exchangeDay;
        this.registrationTime = LocalDateTime.now();// Setzt die aktuelle Zeit als Registrierungszeitpunkt
        this.status = RegistrationStatus.REGISTERED;// Standardstatus ist "Registriert"
    }

    // Getter und Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Workshop getWorkshop() {
        return workshop;
    }

    public void setWorkshop(Workshop workshop) {
        this.workshop = workshop;
    }

    public ExchangeDay getExchangeDay() {
        return exchangeDay;
    }

    public void setExchangeDay(ExchangeDay exchangeDay) {
        this.exchangeDay = exchangeDay;
    }

    public LocalDateTime getRegistrationTime() {
        return registrationTime;
    }

    public void setRegistrationTime(LocalDateTime registrationTime) {
        this.registrationTime = registrationTime;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }
}
