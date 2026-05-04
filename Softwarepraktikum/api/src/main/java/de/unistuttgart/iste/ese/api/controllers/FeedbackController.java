package de.unistuttgart.iste.ese.api.controllers;

import de.unistuttgart.iste.ese.api.domains.Employee;
import de.unistuttgart.iste.ese.api.domains.Feedback;
import de.unistuttgart.iste.ese.api.domains.FeedbackMessage;
import de.unistuttgart.iste.ese.api.dtos.ContactRequestDTO;
import de.unistuttgart.iste.ese.api.dtos.FeedbackDTO;
import de.unistuttgart.iste.ese.api.dtos.FeedbackMessageDTO;
import de.unistuttgart.iste.ese.api.repositories.EmployeeRepository;
import de.unistuttgart.iste.ese.api.repositories.FeedbackMessageRepository;
import de.unistuttgart.iste.ese.api.repositories.FeedbackRepository;
import de.unistuttgart.iste.ese.api.services.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

// Controller zur Verwaltung von Feedbacks und Feedback-Nachrichten
@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackController {
    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private FeedbackMessageRepository feedbackMessageRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private FeedbackService feedbackService;

    // GET /api/v1/feedback - Alle Feedbacks abrufen
    @GetMapping
    public ResponseEntity<List<FeedbackDTO>> getAllFeedback() {
        List<Feedback> feedbackList = (List<Feedback>) feedbackRepository.findAll();
        List<FeedbackDTO> cleanedList = new ArrayList<>();
        for (Feedback feedback : feedbackList) {
            cleanedList.add(new FeedbackDTO(feedback));
        }
        return ResponseEntity.ok(cleanedList);
    }

    // POST /api/v1/feedback - Neues Feedback erstellen
    @PostMapping
    public ResponseEntity<Feedback> createFeedback(@Valid @RequestBody Feedback feedback) {
        feedback.setTimestamp(LocalDateTime.now());
        try {
            Feedback savedFeedback = feedbackService.createFeedback(feedback);
            return ResponseEntity.ok(savedFeedback);
        } catch (ResponseStatusException e) {
            throw e;
        }
    }

    // GET /api/v1/feedback/workshop/{workshopId} - Feedback für einen Workshop abrufen
    @GetMapping("/workshop/{workshopId}")
    public ResponseEntity<List<Feedback>> getFeedbackForWorkshop(@PathVariable int workshopId) {
        return ResponseEntity.ok(feedbackService.getFeedbackForWorkshop(workshopId));
    }

    // GET /api/v1/feedback/anonymous/{token} - Anonymes Feedback per Token abrufen
    @GetMapping("/anonymous/{token}")
    public ResponseEntity<Feedback> getAnonymousFeedback(@PathVariable String token) {
        Feedback feedback = feedbackRepository.findByAnonymousToken(token);
        if (feedback == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(feedback);
    }

    // DELETE /api/v1/feedback/{id} - Feedback löschen
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeedback(@PathVariable int id) {
        feedbackRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // POST /api/v1/feedback/contact - Kontaktnachricht an Benutzer senden
    @PostMapping("/contact")
    public ResponseEntity<FeedbackMessage> contactUser(@Valid @RequestBody ContactRequestDTO request) {
        Feedback feedback = feedbackRepository.findById(request.getFeedbackId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Feedback nicht gefunden"));

        Employee admin = employeeRepository.findById(request.getEmployeeId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Admin nicht gefunden"));

        FeedbackMessage message = new FeedbackMessage(
            feedback,
            request.getMessage(),
            true,     // isAdminMessage = true für Admin-Nachrichten
            admin,    // Admin als Absender
            false     // Admin-Nachrichten sind nie anonym
        );

        FeedbackMessage savedMessage = feedbackMessageRepository.save(message);
        return ResponseEntity.ok(savedMessage);
    }

    // GET /api/v1/feedback/employees/{employeeId} - Feedback eines Mitarbeiters abrufen
    @GetMapping("/employees/{employeeId}")
    public ResponseEntity<List<FeedbackDTO>> getFeedbackForEmployee(@PathVariable int employeeId) {
        List<Feedback> feedbacks = feedbackService.getFeedbackByEmployeeId(employeeId);
        List<FeedbackDTO> feedbackDTOs = feedbacks.stream()
            .map(FeedbackDTO::new)
            .toList();
        return ResponseEntity.ok(feedbackDTOs);
    }

    // GET /api/v1/feedback/{feedbackId}/messages - Nachrichten eines Feedbacks abrufen
    @GetMapping("/{feedbackId}/messages")
    public ResponseEntity<List<FeedbackMessageDTO>> getFeedbackMessages(@PathVariable int feedbackId) {
        List<FeedbackMessage> messages = feedbackMessageRepository.findByFeedbackId(feedbackId);
        return ResponseEntity.ok(
            messages.stream()
                .map(FeedbackMessageDTO::new)
                .collect(Collectors.toList())
        );
    }

    // POST /api/v1/feedback/{feedbackId}/messages - Neue Nachricht zu Feedback hinzufügen
    @PostMapping("/{feedbackId}/messages")
    public ResponseEntity<FeedbackMessage> addMessage(
        @PathVariable int feedbackId,
        @RequestBody String message,
        @RequestParam int employeeId,
        @RequestParam(defaultValue = "false") boolean anonymous,
        @RequestParam(defaultValue = "false") boolean isAdminMessage
    ) {
        // Aktuellen Zeitstempel in Berliner Zeitzone setzen
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Berlin"));

        // Überprüfe ob das Feedback existiert
        Feedback feedback = feedbackRepository.findById(feedbackId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Finde den Mitarbeiter und prüfe Existenz
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Prüfe Admin-Rechte für Admin-Nachrichten
        if (isAdminMessage && !employee.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nur Admins können Admin-Nachrichten senden");
        }

        // Erstelle neue Feedback-Nachricht mit Basis-Daten
        FeedbackMessage feedbackMessage = new FeedbackMessage();
        feedbackMessage.setFeedback(feedback);
        feedbackMessage.setMessage(message);
        feedbackMessage.setAdminMessage(isAdminMessage && employee.isAdmin());
        feedbackMessage.setTimestamp(now.toLocalDateTime());
        feedbackMessage.setAnonymous(anonymous);

        // Setze Autor basierend auf Anonymitäts-Flag
        if (anonymous) {
            feedbackMessage.setAnonymousAuthorId(employeeId);
            feedbackMessage.setEmployee(null);
        } else {
            feedbackMessage.setEmployee(employee);
            feedbackMessage.setAnonymousAuthorId(null);
        }

        return ResponseEntity.ok(feedbackMessageRepository.save(feedbackMessage));
    }

    // DELETE /api/v1/feedback/messages/{messageId} - Feedback-Nachricht löschen
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable int messageId) {
        Optional<FeedbackMessage> message = feedbackMessageRepository.findById(messageId);

        if (message.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nachricht nicht gefunden");
        }

        try {
            feedbackMessageRepository.deleteById(messageId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Fehler beim Löschen der Nachricht: " + e.getMessage()
            );
        }
    }
}
