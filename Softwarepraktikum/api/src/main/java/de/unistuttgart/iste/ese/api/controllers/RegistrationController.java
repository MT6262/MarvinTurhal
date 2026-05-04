package de.unistuttgart.iste.ese.api.controllers;

import de.unistuttgart.iste.ese.api.domains.Employee;
import de.unistuttgart.iste.ese.api.domains.ExchangeDay;
import de.unistuttgart.iste.ese.api.domains.Registration;
import de.unistuttgart.iste.ese.api.domains.Workshop;
import de.unistuttgart.iste.ese.api.dtos.RegistrationRequestDTO;
import de.unistuttgart.iste.ese.api.repositories.EmployeeRepository;
import de.unistuttgart.iste.ese.api.repositories.ExchangeDayRepository;
import de.unistuttgart.iste.ese.api.repositories.RegistrationRepository;
import de.unistuttgart.iste.ese.api.repositories.WorkshopRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

// Controller zur Verwaltung von Registrierungen
@RestController
@RequestMapping("/api/v1")
public class RegistrationController {
    @Autowired
    private RegistrationRepository registrationRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private WorkshopRepository workshopRepository;
    @Autowired
    private ExchangeDayRepository exchangeDayRepository;

    // GET /api/v1/registrations - Alle Registrierungen abrufen
    @GetMapping("/registrations")
    public List<Registration> getAllRegistrations() {
        return (List<Registration>) registrationRepository.findAll();
    }

    // GET /api/v1/registrations/{id} - Einzelne Registrierung anhand ihrer ID abrufen
    @GetMapping("/registrations/{id}")
    public Registration getRegistration(@PathVariable("id") int id) {
        return registrationRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                String.format("Registration with ID %s not found!", id)));
    }

    // POST /api/v1/registrations - Neue Workshop-Registrierung erstellen
    @PostMapping("/registrations")
    @ResponseStatus(HttpStatus.CREATED)
    public Registration createRegistration(@RequestBody RegistrationRequestDTO request) {
        // Validiere Mitarbeiter-Existenz
        Employee employee = employeeRepository.findById(request.getEmployeeId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Employee not found with ID: " + request.getEmployeeId()));

        // Validiere Workshop-Existenz
        Workshop workshop = workshopRepository.findById(request.getWorkshopId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Workshop not found with ID: " + request.getWorkshopId()));

        // Prüfe Workshop-Kapazität
        int currentRegistrations = registrationRepository.findByWorkshop_Id(workshop.getId()).size();
        if (currentRegistrations >= workshop.getMaxParticipants()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workshop is already full");
        }

        // Erstelle neue Registration mit Standardwerten
        Registration registration = new Registration();
        registration.setEmployee(employee);
        registration.setWorkshop(workshop);
        registration.setRegistrationTime(LocalDateTime.now());
        registration.setStatus(Registration.RegistrationStatus.REGISTERED);

        // Füge Exchange Day hinzu, falls vorhanden
        if (request.getExchangeDayId() != 0) {
            ExchangeDay exchangeDay = exchangeDayRepository.findById(request.getExchangeDayId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "ExchangeDay not found with ID: " + request.getExchangeDayId()));
            registration.setExchangeDay(exchangeDay);
        }

        return registrationRepository.save(registration);
    }

    // PUT /api/v1/registrations/{id} - Bestehende Registrierung aktualisieren
    @PutMapping("/registrations/{id}")
    public Registration updateRegistration(
        @PathVariable("id") int id,
        @RequestBody RegistrationRequestDTO request) {

        Registration existingRegistration = registrationRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Registration not found with ID: " + id));

        Employee employee = employeeRepository.findById(request.getEmployeeId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Employee not found with ID: " + request.getEmployeeId()));

        Workshop workshop = workshopRepository.findById(request.getWorkshopId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Workshop not found with ID: " + request.getWorkshopId()));

        if (existingRegistration.getWorkshop().getId() != workshop.getId()) {
            int currentRegistrations = registrationRepository.findByWorkshop_Id(workshop.getId()).size();
            if (currentRegistrations >= workshop.getMaxParticipants()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workshop is already full");
            }
        }

        existingRegistration.setEmployee(employee);
        existingRegistration.setWorkshop(workshop);

        if (request.getExchangeDayId() != 0) {
            ExchangeDay exchangeDay = exchangeDayRepository.findById(request.getExchangeDayId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "ExchangeDay not found with ID: " + request.getExchangeDayId()));
            existingRegistration.setExchangeDay(exchangeDay);
        } else {
            existingRegistration.setExchangeDay(null);
        }

        return registrationRepository.save(existingRegistration);
    }

    // DELETE /api/v1/registrations/{id} - Registrierung löschen
    @DeleteMapping("/registrations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRegistration(@PathVariable("id") int id) {
        if (!registrationRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                String.format("Registration with ID %s not found!", id));
        }
        registrationRepository.deleteById(id);
    }
}
