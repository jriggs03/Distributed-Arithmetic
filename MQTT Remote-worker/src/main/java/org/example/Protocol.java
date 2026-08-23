package org.example;

public class Protocol {

    public static final String AVAILABLE_TOPIC =
            "csc364/calculator/workers/available";

    public static final String RESULTS_TOPIC =
            "csc364/calculator/results";

    public static final String JOB_TOPIC_PREFIX =
            "csc364/calculator/jobs/";

    private Protocol() {
    }

    public static Operation decodeJob(String messageText) {
        String[] parts =
                messageText.split("\\|", -1);

        if (parts.length != 4) {
            throw new IllegalArgumentException("Expected jobId|left|operator|right");
        }

        if (parts[2].length() != 1) {
            throw new IllegalArgumentException("Operator must contain exactly one character");
        }

        String jobId = parts[0];
        int left = Integer.parseInt(parts[1]);
        char operator = parts[2].charAt(0);
        int right = Integer.parseInt(parts[3]);

        return new Operation(jobId, left, operator, right);
    }

    public static String encodeResult(String workerId, Operation operation) {
        return workerId + "|" + operation.getId() + "|" + operation.getResultText();
    }
}