package de.unistuttgart.iste.ese.api.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.LoadingModelEvaluatorBuilder;
import org.jpmml.evaluator.OutputField;
import org.jpmml.evaluator.TargetField;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class for loading and using a PMML-based Todo classification model.
 */
public class TodoModel {
    private static final Log LOG = LogFactory.getLog(TodoModel.class);
    private Evaluator evaluator;

    /**
     * Constructs a TodoModel with the specified PMML model file path.
     *
     * @param pathname The path to the PMML model file.
     */
    public TodoModel(String pathname) {
        loadModel(pathname);
    }

    /**
     * Loads the PMML model from the specified file path and initializes the model
     * evaluator.
     * If the loading process encounters any exceptions, the evaluator is set to
     * null.
     */
    public void loadModel(String pathname) {
        try {
            this.evaluator = new LoadingModelEvaluatorBuilder()
                .load(getClass().getClassLoader().getResourceAsStream(pathname))
                .build();
            this.evaluator.verify();
            LOG.info("Model loaded successfully from " + pathname);
        } catch (Exception e) {
            LOG.error("Could not load AI model:", e);
            this.evaluator = null;
        }
    }

    /**
     * Parses the model's output to retrieve the predicted class/category.
     *
     * @param results The results obtained from evaluating the model on input data.
     * @return The predicted class/category based on the model's output.
     */
    private String parseModelOutput(Map<String, ?> results) {
        List<TargetField> targetFields = this.evaluator.getTargetFields();
        List<OutputField> outputFields = this.evaluator.getOutputFields();

        LOG.info("Model output fields: " + outputFields);
        LOG.info("Model target fields: " + targetFields);

        int predictedLabelIndex = Integer.parseInt(results.get(outputFields.get(2).getName()).toString());
        String[] targetLabels = targetFields.get(0).getName().replaceAll("[\\['\\]]", "").split(" ");
        LOG.info("Predicted label index: " + predictedLabelIndex);
        LOG.info("Target labels: " + String.join(", ", targetLabels));
        return targetLabels[predictedLabelIndex];
    }

    /**
     * Predicts the class/category of a given input text.
     *
     * @param inputString The input text to be classified.
     * @return The predicted class/category for the input text.
     */
    public String predictClass(String inputString) {
        if (evaluator == null) {
            LOG.warn("Cannot predict class without a loaded model");
            return "unknown";
        }
        // Prepare input data for the model
        Map<String, Object> input = new HashMap<>();
        input.put("text", inputString);

        LOG.info("Input to model: " + input);
        // Evaluate the model on the input data
        Map<String, ?> results = this.evaluator.evaluate(input);

        LOG.info("Model results: " + results);

        return parseModelOutput(results);
    }

    /**
     * Unloads the loaded PMML model, releasing resources.
     */
    public void unloadModel() {
        this.evaluator = null;
    }
}
