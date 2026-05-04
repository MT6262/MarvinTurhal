package de.unistuttgart.iste.ese.api.controllers;

import de.unistuttgart.iste.ese.api.domains.Employee;
import de.unistuttgart.iste.ese.api.domains.Feedback;
import de.unistuttgart.iste.ese.api.repositories.EmployeeRepository;
import de.unistuttgart.iste.ese.api.repositories.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Controller zur Verwaltung und Analyse von Trainer-Bewertungen
@RestController
@RequestMapping("/api/v1/feedback/trainer")
public class TrainerStatsController {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    // GET /api/v1/feedback/trainer/{id}/stats - Statistiken für einen Trainer abrufen
    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> getTrainerStats(@PathVariable("id") int id) {
        // Validiere Trainer-Existenz und Admin-Status
        Employee trainer = employeeRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Trainer nicht gefunden"));

        if (!trainer.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Der Mitarbeiter ist kein Trainer");
        }

        // Hole alle Feedbacks für Workshops dieses Trainers
        List<Feedback> trainerFeedbacks = feedbackRepository.findByWorkshopTrainer(trainer);

        // Berechne durchschnittliche Trainer-Bewertung
        double averageRating = trainerFeedbacks.stream()
            .filter(f -> f.getTrainerRating() != null)
            .mapToDouble(Feedback::getTrainerRating)
            .average()
            .orElse(0.0);  // Standardwert

        // Gesamtanzahl der Bewertungen
        long totalRatings = trainerFeedbacks.stream()
            .filter(f -> f.getTrainerRating() != null)
            .count();

        // Erstelle Response-Map mit Statistiken
        Map<String, Object> stats = new HashMap<>();
        stats.put("averageRating", averageRating);
        stats.put("totalRatings", totalRatings);

        return ResponseEntity.ok(stats);
    }
}
