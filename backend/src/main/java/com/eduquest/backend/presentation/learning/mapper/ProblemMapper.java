package com.eduquest.backend.presentation.learning.mapper;

import com.eduquest.backend.application.learning.dto.ProblemCommand;
import com.eduquest.backend.application.learning.dto.ProblemDto;
import com.eduquest.backend.application.learning.dto.ProblemListDto;
import com.eduquest.backend.presentation.learning.dto.request.ProblemCreateRequest;
import com.eduquest.backend.presentation.learning.dto.request.ProblemUpdateRequest;
import com.eduquest.backend.presentation.learning.dto.response.ProblemListResponse;
import com.eduquest.backend.presentation.learning.dto.response.ProblemResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.stream.Collectors;

public final class ProblemMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String EMPTY_BLOCK = "{\"answer\":[],\"blocks\":[]}";

    private ProblemMapper() {}

    public static ProblemResponse toResponse(ProblemDto dto) {
        return ProblemResponse.of(
                dto.uuid(), dto.stageUuid(), dto.stageTitle(), dto.stageNumber(), dto.type(), dto.number(), htmlUnescape(dto.summary()), htmlUnescape(dto.example()), htmlUnescape(dto.expectedOutput()), unescapeBlockCode(dto.block()), HintMapper.toResponseList(dto.hints())
        );
    }

    public static List<ProblemResponse> toResponseList(List<ProblemDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream().map(ProblemMapper::toResponse).collect(Collectors.toList());
    }

    public static ProblemListResponse toListResponse(ProblemListDto dto) {
        return ProblemListResponse.of(dto.page(), dto.size(), dto.sort(), dto.isAsc(), toResponseList(dto.results()));
    }

    public static ProblemCommand toCommand(ProblemCreateRequest req) {
        String block = req.block() == null ? EMPTY_BLOCK : unescapeBlockCode(req.block().toString());
        return ProblemCommand.of(req.stageUuid(), req.type(), req.number(), htmlUnescape(req.summary()), htmlUnescape(req.example()), htmlUnescape(req.expectedOutput()), block, HintMapper.toDtoList(req.hints()));
    }

    public static ProblemCommand toCommand(ProblemUpdateRequest req) {
        String block = req.block() == null ? EMPTY_BLOCK : unescapeBlockCode(req.block().toString());
        return ProblemCommand.of(req.stageUuid(), req.type(), req.number(), htmlUnescape(req.summary()), htmlUnescape(req.example()), htmlUnescape(req.expectedOutput()), block, HintMapper.toDtoList(req.hints()));
    }

    private static String htmlUnescape(String value) {
        return value == null ? null : HtmlUtils.htmlUnescape(value);
    }

    private static String unescapeBlockCode(String block) {
        if (block == null || block.isBlank()) {
            return block;
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(block);
            unescapeCodeFields(root);
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (Exception ignored) {
            return block;
        }
    }

    private static void unescapeCodeFields(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            JsonNode codeNode = objectNode.get("code");
            if (codeNode != null && codeNode.isTextual()) {
                objectNode.put("code", htmlUnescape(codeNode.asText()));
            }
            objectNode.elements().forEachRemaining(ProblemMapper::unescapeCodeFields);
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            arrayNode.elements().forEachRemaining(ProblemMapper::unescapeCodeFields);
        }
    }
}
