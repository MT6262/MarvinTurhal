package de.unistuttgart.iste.ese.api.dtos;

import jakarta.validation.constraints.NotNull;

public class RegistrationRequestDTO {
    @NotNull
    private int employeeId;

    @NotNull
    private int workshopId;

    private int exchangeDayId;

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public int getWorkshopId() {
        return workshopId;
    }

    public void setWorkshopId(int workshopId) {
        this.workshopId = workshopId;
    }

    public int getExchangeDayId() {
        return exchangeDayId;
    }

    public void setExchangeDayId(int exchangeDayId) {
        this.exchangeDayId = exchangeDayId;
    }
}
