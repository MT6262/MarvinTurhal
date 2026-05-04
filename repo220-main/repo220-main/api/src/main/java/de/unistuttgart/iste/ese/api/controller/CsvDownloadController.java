package de.unistuttgart.iste.ese.api.controller;

import de.unistuttgart.iste.ese.api.model.ToDo;
import de.unistuttgart.iste.ese.api.service.ToDoService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;


/**
 * A controller class for handling HTTP requests related to CSV downloads.
 */
@RestController
@RequestMapping("/api/v1/csv-downloads")
public class CsvDownloadController {

    private final ToDoService toDoService;

    public CsvDownloadController(ToDoService toDoService) {
        this.toDoService = toDoService;
    }

    /**
     * Exports all ToDos as a CSV file.
     *
     * @return A CSV file containing all ToDos.
     */
    @GetMapping(value = "/todos", produces = "application/csv")
    public ResponseEntity<String> exportTodosAsCsv() {
        try {
            List<ToDo> todos = toDoService.getAllToDos();
            StringWriter writer = new StringWriter();
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("id", "title", "description", "finished", "assignees",
                    "createdDate", "dueDate", "finishedDate", "category")
                .build();

            try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                dateFormat.setTimeZone(TimeZone.getDefault());

                for (ToDo todo : todos) {
                    String assignees = todo.getAssigneeList().stream()
                        .map(a -> a.getPrename() + " " + a.getName())
                        .sorted()
                        .collect(Collectors.joining("+"));

                    csvPrinter.printRecord(
                        todo.getId(),
                        todo.getTitle(),
                        todo.getDescription(),
                        String.valueOf(todo.isFinished()).toUpperCase(),
                        assignees,
                        todo.getCreatedDate() != null ? dateFormat.format(todo.getCreatedDate()) : "",
                        todo.getDueDate() != null ? dateFormat.format(todo.getDueDate()) : "",
                        todo.getFinishedDate() != null ? dateFormat.format(todo.getFinishedDate()) : "",
                        todo.getCategory()
                    );
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/csv"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=todos.csv");

            return ResponseEntity.ok()
                .headers(headers)
                .body(writer.toString());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
