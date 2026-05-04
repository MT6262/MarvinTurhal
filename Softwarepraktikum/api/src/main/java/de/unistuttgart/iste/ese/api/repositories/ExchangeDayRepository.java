package de.unistuttgart.iste.ese.api.repositories;

import de.unistuttgart.iste.ese.api.domains.ExchangeDay;
import org.springframework.data.repository.CrudRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ExchangeDayRepository extends CrudRepository<ExchangeDay, Integer> {
    List<ExchangeDay> findByTitle(String title);


}
