package de.unistuttgart.iste.ese.api.repositories;

import de.unistuttgart.iste.ese.api.domains.Employee;
import de.unistuttgart.iste.ese.api.domains.Feedback;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FeedbackRepository extends CrudRepository<Feedback, Integer> {
    List<Feedback> findByWorkshopId(int workshopId);
    List<Feedback> findByEmployeeId(Integer employeeId);  // explizit List<Feedback> zurückgeben
    Feedback findByAnonymousToken(String anonymousToken);
    List<Feedback> findByAnonymousTokenStartingWith(String tokenPrefix);  // neue Methode

    List<Feedback> findByWorkshopTrainer(Employee trainer);

    void deleteByWorkshopId(int workshopId);
}

