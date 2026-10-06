package com.songoda.core.compatibility.folia;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.junit.Test;

public class PrintExecutionStatesTest {
    @Test
    public void printStates() {
        for (ScheduledTask.ExecutionState state : ScheduledTask.ExecutionState.values()) {
            System.out.println("STATE: " + state.name());
        }
    }
}
