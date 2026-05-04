package de.unistuttgart.iste.ese.api.repositories;

import de.unistuttgart.iste.ese.api.domains.ExchangeDay;
import de.unistuttgart.iste.ese.api.domains.Workshop;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface WorkshopRepository extends CrudRepository<Workshop, Integer> {
    List<Workshop> findByTitle(String title);
    List<Workshop> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
    List<Workshop> findByLocation(String location);
}
