package com.JavaTraining.BaiTap_RS.timetableagent.ai;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentModelProposalDTO;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.converter.StructuredOutputConverter;

public class TimetableAgentProposalConverter implements StructuredOutputConverter<TimetableAgentModelProposalDTO> {

    private static final Set<String> PROPOSAL_FIELDS = Set.of(
            "schemaVersion", "status", "snapshotId", "entries", "unresolvedConstraints", "explanation");
    private static final Set<String> ENTRY_FIELDS = Set.of(
            "assignmentId", "periodId", "functionalRoomId", "validFrom", "validTo");
    private static final Set<String> CONSTRAINT_FIELDS = Set.of("code", "field", "message");
    private final ObjectMapper strictMapper;
    private final String schema;

    public TimetableAgentProposalConverter(ObjectMapper objectMapper, String schema) {
        this.strictMapper = objectMapper.copy().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.schema = schema;
    }

    @Override
    public String getJsonSchema() {
        return schema;
    }

    @Override
    public String getFormat() {
        return "";
    }

    @Override
    public TimetableAgentModelProposalDTO convert(String source) {
        if (source == null || source.isBlank()) {
            throw new TimetableAgentModelException("Model returned an empty proposal.");
        }
        try {
            JsonNode root = strictMapper.readTree(source);
            validateRoot(root);
            return strictMapper.treeToValue(root, TimetableAgentModelProposalDTO.class);
        } catch (IOException exception) {
            throw new TimetableAgentInvalidOutputException(
                    "Model output did not match the timetable proposal contract.", exception);
        }
    }

    private void validateRoot(JsonNode root) {
        TimetableAgentJsonFields.requireObjectWithExactFields(root, PROPOSAL_FIELDS, "proposal");
        validateEntries(root.get("entries"));
        validateConstraints(root.get("unresolvedConstraints"));
        if (!"1".equals(TimetableAgentJsonFields.requireText(root, "schemaVersion"))) {
            throw TimetableAgentJsonFields.invalid("schemaVersion must be 1");
        }
        String status = TimetableAgentJsonFields.requireText(root, "status");
        if (!Set.of("PROPOSED", "NEEDS_INPUT", "NO_SOLUTION_FOUND").contains(status)) {
            throw TimetableAgentJsonFields.invalid("status is not supported");
        }
        TimetableAgentJsonFields.requireNonBlankText(root, "snapshotId");
        TimetableAgentJsonFields.requireText(root, "explanation");
        if (!"PROPOSED".equals(status) && !root.get("entries").isEmpty()) {
            throw TimetableAgentJsonFields.invalid("non-proposal statuses must not contain entries");
        }
    }

    private void validateEntries(JsonNode entries) {
        if (!entries.isArray()) {
            throw TimetableAgentJsonFields.invalid("entries must be an array");
        }
        for (JsonNode entry : entries) {
            TimetableAgentJsonFields.requireObjectWithExactFields(entry, ENTRY_FIELDS, "entry");
            TimetableAgentJsonFields.requirePositiveInteger(entry, "assignmentId");
            TimetableAgentJsonFields.requirePositiveInteger(entry, "periodId");
            TimetableAgentJsonFields.requireNullablePositiveInteger(entry, "functionalRoomId");
            LocalDate from = TimetableAgentJsonFields.requireDate(entry, "validFrom");
            LocalDate to = TimetableAgentJsonFields.requireDate(entry, "validTo");
            if (to.isBefore(from)) {
                throw TimetableAgentJsonFields.invalid("validTo must not be before validFrom");
            }
        }
    }

    private void validateConstraints(JsonNode constraints) {
        if (!constraints.isArray()) {
            throw TimetableAgentJsonFields.invalid("unresolvedConstraints must be an array");
        }
        for (JsonNode constraint : constraints) {
            TimetableAgentJsonFields.requireObjectWithExactFields(constraint, CONSTRAINT_FIELDS,
                    "unresolved constraint");
            TimetableAgentJsonFields.requireNonBlankText(constraint, "code");
            TimetableAgentJsonFields.requireText(constraint, "field");
            TimetableAgentJsonFields.requireNonBlankText(constraint, "message");
        }
    }
}
