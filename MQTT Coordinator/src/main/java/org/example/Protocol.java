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

    public static String encodeJob(Operation operation) {

        return operation.getId()
                + "|"
                + operation.getLeft()
                + "|"
                + operation.getOperator()
                + "|"
                + operation.getRight();
    }

    public static ResultMessage decodeResult(String messageText) {

        String[] parts = messageText.split("\\|", -1);

        if (parts.length != 3) {throw new IllegalArgumentException("Expected workerId|jobId|result");
        }

        String workerId = parts[0];
        String jobId = parts[1];
        String result = parts[2];

        return new ResultMessage(workerId, jobId, result);
    }

    public static class ResultMessage {

        private final String workerId;
        private final String jobId;
        private final String result;

        public ResultMessage(String workerId, String jobId, String result) {
            this.workerId = workerId;
            this.jobId = jobId;
            this.result = result;
        }
        public String getWorkerId() {
            return workerId;
        }
        public String getJobId() {
            return jobId;
        }
        public String getResult() {
            return result;
        }
    }
}