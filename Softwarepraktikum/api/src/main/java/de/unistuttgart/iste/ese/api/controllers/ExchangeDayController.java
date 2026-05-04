package de.unistuttgart.iste.ese.api.controllers;

import de.unistuttgart.iste.ese.api.domains.ExchangeDay;
import de.unistuttgart.iste.ese.api.domains.Registration;
import de.unistuttgart.iste.ese.api.repositories.ExchangeDayRepository;
import de.unistuttgart.iste.ese.api.repositories.RegistrationRepository;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

// Controller zur Verwaltung von ExchangeDays
@RestController
@RequestMapping("/api/v1")
public class ExchangeDayController {
    @Autowired
    private ExchangeDayRepository exchangeDayRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    // GET /api/v1/exchangedays - Alle ExchangeDays abrufen
    @GetMapping("/exchangedays")
    public List<ExchangeDay> getAllExchangeDays() {
        return (List<ExchangeDay>) exchangeDayRepository.findAll();
    }

    // GET /api/v1/exchangedays/{id} - Einzelnen ExchangeDay anhand seiner ID abrufen
    @GetMapping("/exchangedays/{id}")
    public ExchangeDay getExchangeDay(@PathVariable("id") int id) {
        return exchangeDayRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                String.format("Exchange Day with ID %s not found!", id)));
    }

    // POST /api/v1/exchangedays - Neuen ExchangeDay erstellen
    @PostMapping("/exchangedays")
    @ResponseStatus(HttpStatus.CREATED)
    public ExchangeDay createExchangeDay(@Valid @RequestBody ExchangeDay exchangeDay) {
        try {
            System.out.println("Received exchange day: " + exchangeDay);

            // Validierung der erforderlichen Felder wie Titel, Start- und Endzeit
            if (exchangeDay.getTitle() == null || exchangeDay.getTitle().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
            }
            if (exchangeDay.getStartTime() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time is required");
            }
            if (exchangeDay.getEndTime() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time is required");
            }
            // Überprüft, ob die Endzeit nach der Startzeit liegt
            if (exchangeDay.getEndTime().isBefore(exchangeDay.getStartTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "End time must be after start time");
            }
            return exchangeDayRepository.save(exchangeDay);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to create Exchange Day: " + e.getMessage());
        }
    }

    // PUT /api/v1/exchangedays/{id} - ExchangeDay aktualisieren
    @PutMapping("/exchangedays/{id}")
    public ExchangeDay updateExchangeDay(@PathVariable("id") int id, @Valid @RequestBody ExchangeDay updatedExchangeDay) {
        ExchangeDay existingExchangeDay = exchangeDayRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                String.format("Exchange Day with ID %s not found!", id)));

        // Aktualisiert die ExchangeDay-Daten
        existingExchangeDay.setStartTime(updatedExchangeDay.getStartTime());
        existingExchangeDay.setEndTime(updatedExchangeDay.getEndTime());
        existingExchangeDay.setTitle(updatedExchangeDay.getTitle());
        existingExchangeDay.setDescription(updatedExchangeDay.getDescription());

        return exchangeDayRepository.save(existingExchangeDay);
    }

    // DELETE /api/v1/exchangedays/{id} - ExchangeDay löschen
    @DeleteMapping("/exchangedays/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExchangeDay(@PathVariable("id") int id) {
        if (!exchangeDayRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                String.format("Exchange Day with ID %s not found!", id));
        }

        // Finde alle Registrierungen mit diesem ExchangeDay
        List<Registration> registrations = registrationRepository.findByExchangeDay_Id(id);

        // Entferne die ExchangeDay-Referenz bei allen Registrierungen
        for (Registration registration : registrations) {
            registration.setExchangeDay(null);
            registrationRepository.save(registration);
        }

        exchangeDayRepository.deleteById(id);
    }
}
