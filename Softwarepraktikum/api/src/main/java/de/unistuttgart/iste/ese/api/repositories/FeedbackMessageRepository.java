package de.unistuttgart.iste.ese.api.repositories;

import de.unistuttgart.iste.ese.api.domains.FeedbackMessage;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackMessageRepository extends CrudRepository<FeedbackMessage, Integer> {
    List<FeedbackMessage> findByFeedbackId(int feedbackId);

    void deleteByFeedbackId(int feedbackId);

    void flush();
}
