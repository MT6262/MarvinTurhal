package de.unistuttgart.iste.ese.api.dtos;

import de.unistuttgart.iste.ese.api.domains.FeedbackMessage;

public class FeedbackMessageDTO {
    private int id;
    private String message;
    private String timestamp;
    private boolean isAdminMessage;
    private boolean isAnonymous;
    private Integer anonymousAuthorId;
    private EmployeeLoginResponseDTO employee;

    public FeedbackMessageDTO(FeedbackMessage message) {
        this.id = message.getId();
        this.message = message.getMessage();
        this.timestamp = message.getTimestamp().toString();
        this.isAdminMessage = message.isAdminMessage();
        this.isAnonymous = message.isAnonymous();

        if (message.isAnonymous()) {
            this.anonymousAuthorId = message.getAnonymousAuthorId();
            this.employee = null;  // Kein Employee bei anonymen Nachrichten
        } else {
            this.employee = message.getEmployee() != null ?
                new EmployeeLoginResponseDTO(message.getEmployee()) : null;
        }
    }

    // Getter und Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isAdminMessage() {
        return isAdminMessage;
    }

    public void setAdminMessage(boolean adminMessage) {
        isAdminMessage = adminMessage;
    }

    public EmployeeLoginResponseDTO getEmployee() {
        return employee;
    }

    public void setEmployee(EmployeeLoginResponseDTO employee) {
        this.employee = employee;
    }

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public void setAnonymous(boolean anonymous) {
        isAnonymous = anonymous;
    }

    public Integer getAnonymousAuthorId() {
        return anonymousAuthorId;
    }

    public void setAnonymousAuthorId(Integer anonymousAuthorId) {
        this.anonymousAuthorId = anonymousAuthorId;
    }
}
