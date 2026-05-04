package de.unistuttgart.iste.ese.api.controllers;

import de.unistuttgart.iste.ese.api.domains.Employee;
import de.unistuttgart.iste.ese.api.domains.Registration;
import de.unistuttgart.iste.ese.api.dtos.EmployeeLoginRequestDTO;
import de.unistuttgart.iste.ese.api.dtos.EmployeeLoginResponseDTO;
import de.unistuttgart.iste.ese.api.repositories.EmployeeRepository;
import de.unistuttgart.iste.ese.api.repositories.RegistrationRepository;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

// Controller zur Verwaltung von Mitarbeitern
@RestController
@RequestMapping("/api/v1")
public class EmployeeController {
    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private RegistrationRepository registrationRepository;


    // GET /api/v1/employees - Alle Mitarbeiter abrufen
    @GetMapping("/employees")
    public List<Employee> getAllEmployees() {
        return (List<Employee>) employeeRepository.findAll();
    }

    // GET /api/v1/employees/{id} - Einzelnen Mitarbeiter abrufen(anhand seiner ID)
    @GetMapping("/employees/{id}")
    public Employee getEmployee(@PathVariable("id") int id) {
        return employeeRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                String.format("Employee with ID %s not found!", id)));
    }

    // POST /api/v1/employees - Neuen Mitarbeiter erstellen (Registrierung)
    @PostMapping("/employees")
    @ResponseStatus(HttpStatus.CREATED)
    public Employee createEmployee(@Valid @RequestBody Employee employee) {
        // Prüfen ob Email bereits existiert
        if (employeeRepository.findByEmail(employee.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "An employee with this email already exists!");
        }
        return employeeRepository.save(employee);
    }

    // PUT /api/v1/employees/{id} - Mitarbeiter aktualisieren
    @PutMapping("/employees/{id}")
    public Employee updateEmployee(@PathVariable("id") int id, @Valid @RequestBody Employee updatedEmployee) {
        Employee existingEmployee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                String.format("Employee with ID %s not found!", id)));

        // Wenn sich die Email ändert, prüfen ob die neue Email bereits existiert
        if (!existingEmployee.getEmail().equals(updatedEmployee.getEmail()) &&
            employeeRepository.findByEmail(updatedEmployee.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "An employee with this email already exists!");
        }

        existingEmployee.setFirstName(updatedEmployee.getFirstName());
        existingEmployee.setLastName(updatedEmployee.getLastName());
        existingEmployee.setEmail(updatedEmployee.getEmail());
        existingEmployee.setAdmin(updatedEmployee.isAdmin());

        return employeeRepository.save(existingEmployee);
    }

    // DELETE /api/v1/employees/{id} - Mitarbeiter löschen
    @DeleteMapping("/employees/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployee(@PathVariable("id") int id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                String.format("Employee with ID %s not found!", id));
        }

        // Lösche alle Registrierungen des Mitarbeiters
        List<Registration> registrations = registrationRepository.findByEmployee_Id(id);
        registrationRepository.deleteAll(registrations);

        // Lösche den Mitarbeiter
        employeeRepository.deleteById(id);
    }

    // POST /api/v1/login - Login mit DTO
    @PostMapping("/login")
    public ResponseEntity<EmployeeLoginResponseDTO> login(@Valid @RequestBody EmployeeLoginRequestDTO loginRequest) {
        Employee employee = employeeRepository.findByEmail(loginRequest.getEmail())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Ungültige E-Mail Adresse!"));

        EmployeeLoginResponseDTO response = new EmployeeLoginResponseDTO(employee);
        return ResponseEntity.ok(response);
    }

    // GET /api/v1/employees/check-email/{email} - Prüfen ob Email verfügbar ist
    @GetMapping("/employees/check-email/{email}")
    public ResponseEntity<Map<String, Boolean>> checkEmailAvailable(@PathVariable String email) {
        boolean isAvailable = !employeeRepository.findByEmail(email).isPresent();
        return ResponseEntity.ok(Map.of("available", isAvailable));
    }

}
