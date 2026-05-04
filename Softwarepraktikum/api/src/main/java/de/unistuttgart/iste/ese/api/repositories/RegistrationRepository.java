package de.unistuttgart.iste.ese.api.repositories;

import de.unistuttgart.iste.ese.api.domains.Registration;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends CrudRepository<Registration, Integer> {
    List<Registration> findByEmployee_Id(int employeeId);
    List<Registration> findByWorkshop_Id(int workshopId);
    List<Registration> findByExchangeDay_Id(int exchangeDayId);
    List<Registration> findByStatus(Registration.RegistrationStatus status);
    Optional<Registration> findByEmployee_IdAndWorkshop_Id(Integer staffId, int workshopId);

    void deleteByWorkshopId(int workshopId);
}
