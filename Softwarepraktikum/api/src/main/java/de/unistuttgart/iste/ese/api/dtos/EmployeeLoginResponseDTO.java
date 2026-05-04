// EmployeeLoginResponseDTO.java
package de.unistuttgart.iste.ese.api.dtos;

import de.unistuttgart.iste.ese.api.domains.Employee;

public class EmployeeLoginResponseDTO {
    private int id;
    private String email;
    private boolean isAdmin;
    private String firstName;
    private String lastName;

    // Default Konstruktor
    public EmployeeLoginResponseDTO() {}

    // Konstruktor mit Employee
    public EmployeeLoginResponseDTO(Employee employee) {
        this.id = employee.getId();
        this.email = employee.getEmail();
        this.isAdmin = employee.isAdmin();
        this.firstName = employee.getFirstName();
        this.lastName = employee.getLastName();
    }

    // Getter und Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
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
}
