package de.unistuttgart.iste.ese.api.domains;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Klasse für die Employee-Enität (Nutzer und Admin)
 */
@Entity
@Table(name = "employees")
public class Employee {

    // Primärschlüssel der "employees"-Tabelle, wird automatisch generiert.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    // Vorname des Mitarbeiters, erforderlich und mit einer maximalen Länge von 50 Zeichen.
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "first_name")
    private String firstName;

    // Nachname des Mitarbeiters, erforderlich und mit einer maximalen Länge von 50 Zeichen.
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "last_name")
    private String lastName;

    // Gibt an, ob der Mitarbeiter Administratorrechte hat, erforderlich.
    @NotNull
    @Column(name="is_admin")
    private boolean admin;

    // E-Mail-Adresse des Mitarbeiters, erforderlich, eindeutig und muss einem gültigen E-Mail-Format entsprechen.
    @Column(name="email", unique=true)
    @Email(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
        message = "Ungültige E-Mail-Adresse")
    @NotNull(message = "E-Mail darf nicht leer sein")
    private String email;

    // Standard Konstruktor für JPA
    public Employee() {}

    // Konstruktor mit Parametern
    public Employee(String firstName, String lastName, String email, boolean admin) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.admin = admin;
    }

    // Getter und Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isAdmin() {
        return admin;
    }
    public void setAdmin(boolean admin) {
        this.admin = admin;
    }
}
