package com.JavaTraining.BaiTap_RS.assignment.service;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
final class DemoAssignmentPlanner {

    private final DemoAssignmentPlanningAlgorithm algorithm = new DemoAssignmentPlanningAlgorithm();

    /* default */ List<DemoAssignmentSeeder.PlannedAssignment> plan(
            List<DemoAssignmentSeeder.WorkItem> workItems) {
        return algorithm.plan(workItems);
    }
}
