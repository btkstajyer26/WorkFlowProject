package btk.staj.WorkFlowProject.subtask.repository;

import btk.staj.WorkFlowProject.subtask.model.SubtaskStatus;
import java.util.Objects;

/** Aggregate count used by approval and terminal-result calculations. */
public record SubtaskStatusCount(SubtaskStatus status, long count) {

    public SubtaskStatusCount {
        Objects.requireNonNull(status, "status");
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative");
        }
    }
}
