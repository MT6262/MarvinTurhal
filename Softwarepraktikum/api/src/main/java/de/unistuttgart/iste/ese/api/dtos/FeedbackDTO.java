package de.unistuttgart.iste.ese.api.dtos;

import de.unistuttgart.iste.ese.api.domains.Feedback;
import de.unistuttgart.iste.ese.api.domains.FeedbackMessage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class FeedbackDTO {

    private int id;


    private int workshopID;


    private Integer employeeID;


    private int rating;


    private String comment;


    private boolean anonymous;


    private String anonymousToken;

    private LocalDateTime timestamp;

    private Integer trainerRating;
    private String trainerComment;

    private List<FeedbackMessageDTO> messages = new ArrayList<>();

    public FeedbackDTO(Feedback feedback) {
        this.id = feedback.getId();
        this.workshopID = feedback.getWorkshop().getId();
        // Nur setzen wenn nicht anonym und Employee existiert
        if (!feedback.isAnonymous() && feedback.getEmployee() != null) {
            this.employeeID = feedback.getEmployee().getId();
        }
        this.rating = feedback.getRating();
        this.comment = feedback.getComment();
        this.anonymous = feedback.isAnonymous();
        this.anonymousToken = feedback.getAnonymousToken();
        this.timestamp = feedback.getTimestamp();

        this.messages = new ArrayList<>();
        for (FeedbackMessage message : feedback.getMessages()) {
            this.messages.add(new FeedbackMessageDTO(message));
        }
        this.trainerRating = feedback.getTrainerRating();
        this.trainerComment = feedback.getTrainerComment();
    }

    // Getter und Setter

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getWorkshopID() {
        return workshopID;
    }

    public void setWorkshopID(int workshopID) {
        this.workshopID = workshopID;
    }

    public Integer getEmployeeID() {
        if (this.anonymous) {
            return null;  // Bei anonymem Feedback immer null zurückgeben
        }
        return employeeID;
    }

    public void setEmployeeID(int employeeID) {
        this.employeeID = employeeID;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public String getAnonymousToken() {
        return anonymousToken;
    }

    public void setAnonymousToken(String anonymousToken) {
        this.anonymousToken = anonymousToken;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<FeedbackMessageDTO> getMessages() {
        return messages;
    }

    public void setMessages(List<FeedbackMessageDTO> messages) {
        this.messages = messages;
    }

    public Integer getTrainerRating() {
        return trainerRating;
    }

    public void setTrainerRating(Integer trainerRating) {
        this.trainerRating = trainerRating;
    }

    public String getTrainerComment() {
        return trainerComment;
    }

    public void setTrainerComment(String trainerComment) {
        this.trainerComment = trainerComment;
    }
}


