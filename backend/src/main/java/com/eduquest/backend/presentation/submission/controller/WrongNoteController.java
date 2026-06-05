package com.eduquest.backend.presentation.submission.controller;

import com.eduquest.backend.application.submission.dto.WrongNoteDto;
import com.eduquest.backend.application.submission.dto.WrongNoteListDto;
import com.eduquest.backend.application.submission.service.WrongNoteService;
import com.eduquest.backend.presentation.submission.dto.request.WrongNoteListRequest;
import com.eduquest.backend.presentation.submission.dto.response.WrongNoteListResponse;
import com.eduquest.backend.presentation.submission.dto.response.WrongNoteResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/wrong-notes")
public class WrongNoteController {

    private final WrongNoteService wrongNoteService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{uuid}")
    public ResponseEntity<WrongNoteResponse> getWrongNote(
            @PathVariable UUID uuid,
            Authentication authentication
    ) {
        WrongNoteDto dto = wrongNoteService.findWrongNoteByUuid(uuid, authentication.getName());
        return ResponseEntity.ok(WrongNoteResponse.of(
                dto.uuid(), dto.id(), dto.problemId(), dto.problemUuid(), dto.problemSummary(), dto.userUuid(), dto.wrongAnswer(), dto.feedback(),
                dto.isReviewed(), dto.lastSubmittedAt(), dto.createdAt(), dto.updatedAt()
        ));
    }

    @PreAuthorize("@authz.isSelfByUuid(authentication, #uuid) or hasRole('ADMIN')")
    @GetMapping("/users/{uuid}")
    public ResponseEntity<WrongNoteListResponse.WrongNoteList> listByUser(
            @PathVariable UUID uuid,
            @Valid @ModelAttribute WrongNoteListRequest request
    ) {
        log.info("오답노트 사용자 목록 API 요청: userUuid={}, page={}, size={}, sort={}, isAsc={}",
                uuid, request.page(), request.size(), request.sort(), request.isAsc());

        WrongNoteListDto listDto;
        try {
            listDto = wrongNoteService.findWrongNotesByUserUuid(
                    uuid,
                    request.page(),
                    request.size(),
                    request.sort(),
                    Boolean.TRUE.equals(request.isAsc())
            );
        } catch (RuntimeException ex) {
            log.error("오답노트 사용자 목록 조회 실패: userUuid={}, page={}, size={}, sort={}, isAsc={}",
                    uuid, request.page(), request.size(), request.sort(), request.isAsc(), ex);
            throw ex;
        }

        List<WrongNoteResponse> wrongNoteResponseList = listDto.results().stream()
                .map(dto -> WrongNoteResponse.of(
                        dto.uuid(), dto.id(), dto.problemId(), dto.problemUuid(), dto.problemSummary(), dto.userUuid(), dto.wrongAnswer(), dto.feedback(),
                        dto.isReviewed(), dto.lastSubmittedAt(), dto.createdAt(), dto.updatedAt()
                ))
                .toList();

        return ResponseEntity.ok(WrongNoteListResponse.WrongNoteList.of(
                listDto.page(), listDto.size(), listDto.sort(), listDto.isAsc(),
                listDto.total(), wrongNoteResponseList
        ));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<WrongNoteListResponse.WrongNoteList> listAll(
            @Valid @ModelAttribute WrongNoteListRequest request
    ) {
        WrongNoteListDto listDto = wrongNoteService.findWrongNotes(
                request.page(), request.size(), request.sort(), Boolean.TRUE.equals(request.isAsc())
        );

        List<WrongNoteResponse> wrongNoteResponseList = listDto.results().stream()
                .map(dto -> WrongNoteResponse.of(
                        dto.uuid(), dto.id(), dto.problemId(), dto.problemUuid(), dto.problemSummary(), dto.userUuid(), dto.wrongAnswer(), dto.feedback(),
                        dto.isReviewed(), dto.lastSubmittedAt(), dto.createdAt(), dto.updatedAt()
                ))
                .toList();

        return ResponseEntity.ok(WrongNoteListResponse.WrongNoteList.of(
                listDto.page(), listDto.size(), listDto.sort(), listDto.isAsc(),
                listDto.total(), wrongNoteResponseList
        ));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteByUuid(@PathVariable UUID uuid, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        wrongNoteService.deleteWrongNoteByUuid(uuid, authentication.getName(), isAdmin);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{uuid}/ai-feedback")
    public ResponseEntity<WrongNoteResponse> putWrongNoteAiExplain(@PathVariable @NotNull UUID uuid, Authentication authentication) {
        WrongNoteDto dto = wrongNoteService.requestAiFeedback(uuid, authentication.getName());
        return ResponseEntity.ok(WrongNoteResponse.of(
                dto.uuid(), dto.id(), dto.problemId(), dto.problemUuid(), dto.problemSummary(), dto.userUuid(), dto.wrongAnswer(), dto.feedback(),
                dto.isReviewed(), dto.lastSubmittedAt(), dto.createdAt(), dto.updatedAt()
        ));
    }

}
