package de.unistuttgart.iste.ese.api.controllers;

import de.unistuttgart.iste.ese.api.domains.Employee;
import de.unistuttgart.iste.ese.api.domains.ExchangeDay;
import de.unistuttgart.iste.ese.api.domains.Feedback;
import de.unistuttgart.iste.ese.api.domains.Workshop;
import de.unistuttgart.iste.ese.api.dtos.WorkshopDTO;
import de.unistuttgart.iste.ese.api.repositories.*;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// Controller zur Verwaltung von Workshops
@RestController
@RequestMapping("/api/v1")
public class WorkshopController {
    @Autowired
    private WorkshopRepository workshopRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ExchangeDayRepository exchangeDayRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private FeedbackMessageRepository feedbackMessageRepository;

    // GET /api/v1/trainers - Alle verfügbaren Trainer abrufen
    @GetMapping("/trainers")
    public List<Employee> getAvailableTrainers() {
        System.out.println("WorkshopController: GET /trainers aufgerufen");
        try {
            List<Employee> trainers = employeeRepository.findByAdminTrue();
            System.out.println("Gefundene Trainer: " + trainers.size());
            trainers.forEach(trainer ->
                System.out.println("Trainer: " + trainer.getFirstName() + " " + trainer.getLastName())
            );
            return trainers;
        } catch (Exception e) {
            System.err.println("Fehler beim Abrufen der Trainer: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // GET /api/v1/workshops - Alle Workshops mit Details abrufen
    @GetMapping("/workshops")
    public List<WorkshopDTO> getAllWorkshops() {
        List<Workshop> workshops = (List<Workshop>) workshopRepository.findAll();
        return workshops.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Hilfsmethode zur Konvertierung eines Workshop Objekt in ein DTO
     */
    private WorkshopDTO convertToDTO(Workshop workshop) {
        WorkshopDTO dto = new WorkshopDTO();
        dto.setId(workshop.getId());
        dto.setTitle(workshop.getTitle());
        dto.setDescription(workshop.getDescription());
        dto.setStartTime(workshop.getStartTime());
        dto.setEndTime(workshop.getEndTime());
        dto.setMaxParticipants(workshop.getMaxParticipants());
        dto.setLocation(workshop.getLocation());
        dto.setTrainer(workshop.getTrainer());
        dto.setCurrentParticipants(workshop.getFeedbacks().size());
        dto.setExchangeDay(workshop.getExchangeDay());
        return dto;
    }

    // GET /api/v1/workshops/{id} - Einzelnen Workshop anhand seiner ID abrufen
    @GetMapping("/workshops/{id}")
    public WorkshopDTO getWorkshop(@PathVariable("id") int id) {
        Workshop workshop = workshopRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return convertToDTO(workshop);
    }

    // POST /api/v1/workshops - Neuen Workshop erstellen
    @PostMapping("/workshops")
    @ResponseStatus(HttpStatus.CREATED)
    public Workshop createWorkshop(@Valid @RequestBody Workshop workshop) {
        if (workshop.getTrainer() != null) {
            Employee trainer = employeeRepository.findById(workshop.getTrainer().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Trainer not found"));

            if (!trainer.isAdmin()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Selected trainer must be an admin");
            }
            workshop.setTrainer(trainer);
        }

        if (workshop.getExchangeDay() != null) {
            ExchangeDay exchangeDay = exchangeDayRepository.findById(workshop.getExchangeDay().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Exchange Day not found"));

            if (workshop.getStartTime().isBefore(exchangeDay.getStartTime()) ||
                workshop.getEndTime().isAfter(exchangeDay.getEndTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Workshop must be within Exchange Day time period");
            }
            workshop.setExchangeDay(exchangeDay);
        }

        return workshopRepository.save(workshop);
    }

    // PUT /api/v1/workshops/{id} - Workshop aktualisieren
    @PutMapping("/workshops/{id}")
    public Workshop updateWorkshop(@PathVariable("id") int id, @Valid @RequestBody Workshop updatedWorkshop) {
        Workshop existingWorkshop = workshopRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (updatedWorkshop.getTrainer() != null) {
            Employee trainer = employeeRepository.findById(updatedWorkshop.getTrainer().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Trainer not found"));

            if (!trainer.isAdmin()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Selected trainer must be an admin");
            }
            existingWorkshop.setTrainer(trainer);
        }

        if (updatedWorkshop.getExchangeDay() != null) {
            ExchangeDay exchangeDay = exchangeDayRepository.findById(updatedWorkshop.getExchangeDay().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Exchange Day not found"));

            if (updatedWorkshop.getStartTime().isBefore(exchangeDay.getStartTime()) ||
                updatedWorkshop.getEndTime().isAfter(exchangeDay.getEndTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Workshop must be within Exchange Day time period");
            }
            existingWorkshop.setExchangeDay(exchangeDay);
        } else {
            existingWorkshop.setExchangeDay(null);
        }

        existingWorkshop.setTitle(updatedWorkshop.getTitle());
        existingWorkshop.setDescription(updatedWorkshop.getDescription());
        existingWorkshop.setStartTime(updatedWorkshop.getStartTime());
        existingWorkshop.setEndTime(updatedWorkshop.getEndTime());
        existingWorkshop.setMaxParticipants(updatedWorkshop.getMaxParticipants());
        existingWorkshop.setLocation(updatedWorkshop.getLocation());

        return workshopRepository.save(existingWorkshop);
    }

    // DELETE /api/v1/workshops/{id} - Workshop und zugehörige Daten löschen
    @DeleteMapping("/workshops/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteWorkshop(@PathVariable("id") int id) {
        try {
            // Finde Workshop oder wirf Exception
            Workshop workshop = workshopRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Workshop mit ID %s nicht gefunden!", id)));

            // Hole alle zugehörigen Feedbacks
            List<Feedback> feedbacks = workshop.getFeedbacks();

            // Bereinige zuerst alle Feedback-Nachrichten
            for (Feedback feedback : feedbacks) {
                feedback.getMessages().clear(); // Entferne alle Nachrichten
            }

            // Bereinige dann alle Feedbacks
            workshop.getFeedbacks().clear();

            // Entferne Trainer-Beziehung
            workshop.setTrainer(null);

            // Speichere Änderungen vor dem Löschen
            workshopRepository.save(workshop);

            // Lösche den Workshop endgültig
            workshopRepository.delete(workshop);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Workshop konnte nicht gelöscht werden: " + e.getMessage());
        }
    }
}
