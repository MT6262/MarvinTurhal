package de.unistuttgart.iste.ese.api.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public class EmployeeLoginRequestDTO {
    @NotNull
    @Email
    private String email;

    // Getter und Setter
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
