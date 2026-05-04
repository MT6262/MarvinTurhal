package de.unistuttgart.iste.ese.api.services;

import de.unistuttgart.iste.ese.api.domains.Feedback;
import de.unistuttgart.iste.ese.api.repositories.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

// Service Klasse mit der Logik für Feedbacks (Senden von Feedbacks)
@Service
public class FeedbackService {
    @Autowired
    private FeedbackRepository feedbackRepository;

    public Feedback createFeedback(Feedback feedback) {
        List<Feedback> existingFeedbacks = feedbackRepository.findByWorkshopId(feedback.getWorkshop().getId());

        // Prüfe auf existierendes Feedback vom gleichen Mitarbeiter
        boolean hasExistingFeedback = existingFeedbacks.stream().anyMatch(existingFeedback -> {
            if (feedback.isAnonymous()) {
                // Wenn das neue Feedback anonym ist, prüfe die Employee ID im Token
                // des existierenden Feedbacks (falls anonym)
                if (existingFeedback.isAnonymous() && existingFeedback.getAnonymousToken() != null) {
                    String existingEmployeeId = existingFeedback.getAnonymousToken().split("-")[0];
                    String newEmployeeId = String.valueOf(feedback.getEmployee().getId());
                    return existingEmployeeId.equals(newEmployeeId);
                } else if (!existingFeedback.isAnonymous() && existingFeedback.getEmployee() != null) {
                    // Vergleiche mit der Employee ID des nicht-anonymen Feedbacks
                    return existingFeedback.getEmployee().getId() == feedback.getEmployee().getId();
                }
            } else {
                // Wenn das neue Feedback nicht anonym ist
                if (existingFeedback.isAnonymous() && existingFeedback.getAnonymousToken() != null) {
                    // Prüfe die Employee ID im Token des existierenden anonymen Feedbacks
                    String existingEmployeeId = existingFeedback.getAnonymousToken().split("-")[0];
                    return existingEmployeeId.equals(String.valueOf(feedback.getEmployee().getId()));
                } else if (!existingFeedback.isAnonymous() && existingFeedback.getEmployee() != null) {
                    // Vergleiche direkt die Employee IDs
                    return existingFeedback.getEmployee().getId() == feedback.getEmployee().getId();
                }
            }
            return false;
        });
        // Falls bereits Feedback existiert, wird ein Fehler geworfen
        if (hasExistingFeedback) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Sie haben bereits ein Feedback für diesen Workshop abgegeben."
            );
        }

        // Wenn anonymes Feedback, Employee-Referenz entfernen und Token setzen
        if (feedback.isAnonymous()) {
            String anonymousToken = feedback.getEmployee().getId() + "-" + UUID.randomUUID().toString();
            feedback.setAnonymousToken(anonymousToken);
            feedback.setEmployee(null);
        }

        return feedbackRepository.save(feedback);
    }

    public List<Feedback> getFeedbackForWorkshop(int workshopId) {
        return feedbackRepository.findByWorkshopId(workshopId);
    }

    public List<Feedback> getFeedbackByEmployeeId(int employeeId) {
        return feedbackRepository.findByEmployeeId(employeeId);
    }

    /**
     * Prüft, ob ein Employee bereits Feedback für einen Workshop abgegeben hat.
     * @param workshopId ID des Workshops
     * @param employeeId ID des Employees
     * @return true wenn bereits Feedback existiert, sonst false
     */
    public boolean hasEmployeeSubmittedFeedback(int workshopId, int employeeId) {
        return feedbackRepository
            .findByWorkshopId(workshopId)
            .stream()
            .anyMatch(feedback ->
                !feedback.isAnonymous() &&
                    feedback.getEmployee() != null &&
                    feedback.getEmployee().getId() == employeeId
            );
    }
}
