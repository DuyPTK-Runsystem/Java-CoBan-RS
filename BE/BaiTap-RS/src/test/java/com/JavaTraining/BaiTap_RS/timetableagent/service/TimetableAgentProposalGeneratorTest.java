package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentInvalidOutputException;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentModelGateway;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentProposalPrompt;
import com.JavaTraining.BaiTap_RS.timetableagent.config.TimetableAgentProperties;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentModelProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentModelProposalStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentValidationResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class TimetableAgentProposalGeneratorTest {

    private static final String RAW_OUTPUT_MARKER = "PRIVATE_RAW_MODEL_OUTPUT_7f3c";

    @Test
    void repairFeedbackExplainsDatesMustBeInsideEntriesWhenRootHasExtraDates() {
        assertRepairFeedback("root fields validFrom and validTo are unknown", List.of(
                "validFrom and validTo inside each entry object", "never at the root"));
    }

    @Test
    void repairFeedbackForbidsProposalWrapperAndRequiresEntriesAtRoot() {
        assertRepairFeedback("root field proposal is unknown and entries is missing", List.of(
                "these root keys only: schemaVersion, status, snapshotId, entries, unresolvedConstraints, explanation",
                "Do not add a proposal wrapper"));
    }

    @Test
    void repairFeedbackDoesNotEchoRawModelOutputOrValidationDetails() {
        Fixture fixture = new Fixture();
        List<TimetableAgentProposalPrompt> prompts = new ArrayList<>();
        String diagnosticWithOutput = "invalid contract near " + RAW_OUTPUT_MARKER;
        Mockito.when(fixture.gateway.propose(ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    prompts.add(invocation.getArgument(0));
                    if (prompts.size() == 1) {
                        throw new TimetableAgentInvalidOutputException(diagnosticWithOutput);
                    }
                    return fixture.proposal;
                });

        TimetableAgentProposalGenerator.GeneratedProposal generated = fixture.generator
                .generate(fixture.gateway, fixture.snapshot, "Keep the same request.");
        assertSafeSuccessfulRepair(fixture, generated, prompts, diagnosticWithOutput);
    }

    @Test
    void repeatedSchemaFailureStopsAfterOneActionableRepairPrompt() {
        Fixture fixture = new Fixture();
        List<TimetableAgentProposalPrompt> prompts = new ArrayList<>();
        Mockito.when(fixture.gateway.propose(ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    prompts.add(invocation.getArgument(0));
                    throw new TimetableAgentInvalidOutputException("same schema failure");
                });

        verifyRepeatedFailure(fixture, prompts);
    }

    private void assertRepairFeedback(String failureDetail, List<String> expectedFeedback) {
        Fixture fixture = new Fixture();
        List<TimetableAgentProposalPrompt> prompts = new ArrayList<>();
        Mockito.when(fixture.gateway.propose(ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    prompts.add(invocation.getArgument(0));
                    if (prompts.size() == 1) {
                        throw new TimetableAgentInvalidOutputException(failureDetail);
                    }
                    return fixture.proposal;
                });

        TimetableAgentProposalGenerator.GeneratedProposal generated = fixture.generator
                .generate(fixture.gateway, fixture.snapshot, "Original request.");
        assertSuccessfulRepair(fixture, generated, prompts, expectedFeedback, failureDetail);
    }

    private void assertSuccessfulRepair(Fixture fixture,
            TimetableAgentProposalGenerator.GeneratedProposal generated, List<TimetableAgentProposalPrompt> prompts,
            List<String> expectedFeedback, String failureDetail) {
        Assertions.assertAll("retry should return a valid proposal and include actionable, safe feedback",
                () -> Assertions.assertSame(fixture.proposal, generated.proposal(),
                        "the valid repaired proposal should be returned"),
                () -> Assertions.assertEquals(2, prompts.size(), "exactly one repair call should follow rejection"),
                () -> expectedFeedback.forEach(feedback -> Assertions.assertTrue(
                        prompts.get(1).userRequest().contains(feedback),
                        "repair feedback should include: " + feedback)),
                () -> Assertions.assertFalse(prompts.get(1).userRequest().contains(failureDetail),
                        "model supplied or exception details must not be echoed into repair feedback"),
                () -> Assertions.assertTrue(prompts.get(1).userRequest().contains("same snapshot"),
                        "repair should target the same captured snapshot"));
    }

    private void assertSafeSuccessfulRepair(Fixture fixture,
            TimetableAgentProposalGenerator.GeneratedProposal generated, List<TimetableAgentProposalPrompt> prompts,
            String diagnosticWithOutput) {
        Assertions.assertAll("repair must succeed without echoing raw model output",
                () -> Assertions.assertSame(fixture.proposal, generated.proposal(),
                        "the valid repaired proposal should be returned"),
                () -> Assertions.assertEquals(2, prompts.size(), "exactly one repair call should follow rejection"),
                () -> Assertions.assertTrue(prompts.get(1).userRequest().contains("APPLICATION_VALIDATION_FEEDBACK"),
                        "repair prompt should include application validation feedback"),
                () -> Assertions.assertFalse(prompts.get(1).userRequest().contains(RAW_OUTPUT_MARKER),
                        "raw model output must not be echoed into repair feedback"),
                () -> Assertions.assertFalse(prompts.get(1).userRequest().contains(diagnosticWithOutput),
                        "validation details must not be echoed into repair feedback"));
    }

    private void assertRepeatedFailurePrompt(List<TimetableAgentProposalPrompt> prompts) {
        Assertions.assertAll("one actionable repair prompt should be sent before repeated failure stops",
                () -> Assertions.assertEquals(2, prompts.size(),
                        "repeated failure should stop after one repair attempt"),
                () -> Assertions.assertTrue(prompts.get(1).userRequest().contains("Do not add a proposal wrapper"),
                        "repair feedback should forbid the wrapper object"),
                () -> Assertions.assertTrue(prompts.get(1).userRequest().contains(
                        "validFrom and validTo inside each entry object"),
                        "repair feedback should locate validity dates inside entries"),
                () -> Assertions.assertFalse(prompts.get(1).userRequest().contains("same schema failure"),
                        "validation detail must not be echoed into repair feedback"));
    }

    private void verifyRepeatedFailure(Fixture fixture, List<TimetableAgentProposalPrompt> prompts) {
        Assertions.assertThrows(AppException.class,
                () -> fixture.generator.generate(fixture.gateway, fixture.snapshot, "Original request."),
                "repeated invalid output should fail after one repair attempt");
        assertRepeatedFailurePrompt(prompts);
    }

    private static final class Fixture {

        private final TimetableAgentProperties properties = new TimetableAgentProperties();
        private final TimetableProposalValidator validator = Mockito.mock(TimetableProposalValidator.class);
        private final TimetableAgentModelGateway gateway = Mockito.mock(TimetableAgentModelGateway.class);
        private final TimetableAgentSnapshot snapshot = new TimetableAgentSnapshot(
                "snapshot-test", "fingerprint", "{}", "{}", "{}", 7L, 9L, 5L, 3L,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 12, 31), List.of(1L), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
        private final TimetableAgentModelProposalDTO proposal = new TimetableAgentModelProposalDTO(
                "1", TimetableAgentModelProposalStatus.NEEDS_INPUT, "snapshot-test", List.of(), List.of(),
                "Need input");
        private final TimetableAgentValidationResult validationResult = new TimetableAgentValidationResult(
                TimetableAgentProposalStatus.NEEDS_INPUT, List.of(), List.of(), null, "Need input");
        private final TimetableAgentProposalGenerator generator;

        private Fixture() {
            properties.setProviderTimeout(Duration.ofSeconds(5));
            properties.setMaxModelCalls(3);
            Mockito.when(validator.validate(snapshot, proposal)).thenReturn(validationResult);
            generator = new TimetableAgentProposalGenerator(properties, validator,
                    new TimetableAgentPayloadCodec(new com.fasterxml.jackson.databind.ObjectMapper()));
        }
    }
}
