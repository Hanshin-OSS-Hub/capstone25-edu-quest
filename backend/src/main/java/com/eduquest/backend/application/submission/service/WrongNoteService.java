package com.eduquest.backend.application.submission.service;

import com.eduquest.backend.application.submission.dto.WrongNoteDto;
import com.eduquest.backend.application.submission.dto.WrongNoteListDto;
import com.eduquest.backend.application.submission.exception.WrongNoteErrorCode;
import com.eduquest.backend.common.exception.EduQuestException;
import com.eduquest.backend.domain.identity.model.Member;
import com.eduquest.backend.domain.identity.service.MemberQueryService;
import com.eduquest.backend.domain.learning.model.Problem;
import com.eduquest.backend.domain.learning.service.ProblemQueryService;
import com.eduquest.backend.domain.submission.dto.AiFeedBackRequest;
import com.eduquest.backend.domain.submission.dto.WrongNoteQuery;
import com.eduquest.backend.domain.submission.model.WrongNote;
import com.eduquest.backend.domain.submission.service.ChatModelService;
import com.eduquest.backend.domain.submission.service.WrongNoteCommandService;
import com.eduquest.backend.domain.submission.service.WrongNoteQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WrongNoteService {

    private static final Pattern NUMERIC_HTML_ENTITY_PATTERN = Pattern.compile("&#(x?[0-9a-fA-F]+);");

    private final MemberQueryService memberQueryService;
    private final WrongNoteQueryService wrongNoteQueryService;
    private final WrongNoteCommandService wrongNoteCommandService;
    private final ProblemQueryService problemQueryService;
    private final ChatModelService chatModelService;

    @Transactional(readOnly = true)
    public WrongNoteListDto findWrongNotesByUserUuid(UUID userUuid, int page, int size, String sortBy, boolean isAsc) {
        Long userId = memberQueryService.findMemberIdByUuid(userUuid);

        String sort = sortBy == null ? "updatedAt" : sortBy;
        log.info("오답노트 사용자 목록 조회: userUuid={}, userId={}, page={}, size={}, sort={}, isAsc={}",
                userUuid, userId, page, size, sort, isAsc);

        List<WrongNoteQuery.Detail> details = wrongNoteQueryService.findWrongDetailNotesByUserId(userId, page, size, sort, isAsc);
        long total = wrongNoteQueryService.countWrongNotesByUserId(userId);

        List<WrongNoteDto> results = details.stream()
                .map(detail -> toWrongNoteDto(detail, userUuid))
                .collect(Collectors.toList());

        return WrongNoteListDto.of(page, size, sort, isAsc, total, results);
    }

    @Transactional(readOnly = true)
    public WrongNoteDto findWrongNoteByUuid(UUID wrongNoteUuid, String userId) {
        WrongNoteQuery.Detail detail = wrongNoteQueryService.findWrongDetailNoteByUuid(wrongNoteUuid);
        if (detail == null) {
            throw new EduQuestException(WrongNoteErrorCode.WRONG_NOTE_NOT_FOUND);
        }

        // memberQueryService를 사용해 userId -> userUuid 변환
        Member member = memberQueryService.findMemberById(detail.userId());

        if (!userId.equals(member.getUserId())) {
            throw new EduQuestException(WrongNoteErrorCode.FORBIDDEN_WRONG_NOTE_ACCESS);
        }

        UUID userUuid = member.getUuid();

        return toWrongNoteDto(detail, userUuid);
    }

    @Transactional(readOnly = true)
    public WrongNoteListDto findWrongNotes(int page, int size, String sortBy, boolean isAsc) {
        String sort = sortBy == null ? "updatedAt" : sortBy;
        log.info("오답노트 전체 목록 조회: page={}, size={}, sort={}, isAsc={}", page, size, sort, isAsc);

        List<WrongNoteQuery.Detail> details = wrongNoteQueryService.findWrongNotes(page, size, sort, isAsc);
        long total = wrongNoteQueryService.countWrongNotes();

        Map<Long, UUID> userUuidMap = memberQueryService.findMemberUuidByUserIds(
                details.stream()
                        .map(WrongNoteQuery.Detail::userId)
                        .toList()
        );

        List<WrongNoteDto> results = details.stream()
                .map(detail -> toWrongNoteDto(detail, userUuidMap.get(detail.userId())))
                .collect(Collectors.toList());

        return WrongNoteListDto.of(page, size, sort, isAsc, total, results);
    }

    @Transactional
    public void deleteWrongNoteByUuid(UUID wrongNoteUuid, String userId, boolean isAdmin) {
        WrongNoteQuery.Detail detail = wrongNoteQueryService.findWrongDetailNoteByUuid(wrongNoteUuid);
        if (detail == null) {
            throw new EduQuestException(WrongNoteErrorCode.WRONG_NOTE_NOT_FOUND);
        }

        if (!isAdmin) {
            Member member = memberQueryService.findMemberById(detail.userId());
            if (!userId.equals(member.getUserId())) {
                throw new EduQuestException(WrongNoteErrorCode.FORBIDDEN_WRONG_NOTE_ACCESS);
            }
        }

        wrongNoteCommandService.deleteByUuid(wrongNoteUuid);
    }

    @Transactional
    public WrongNoteDto requestAiFeedback(UUID wrongNoteUuid, String userId) {

        if (!memberQueryService.isExistByUserId(userId)) {
            throw new EduQuestException(WrongNoteErrorCode.WRONG_NOTE_NOT_FOUND);
        }

        // wrong note 존재 확인
        Long memberId = memberQueryService.findMemberIdByUserId(userId);
        WrongNoteQuery.Detail detail = wrongNoteQueryService.findWrongDetailNoteByUuid(wrongNoteUuid);
        if (detail == null) {
            throw new EduQuestException(WrongNoteErrorCode.WRONG_NOTE_NOT_FOUND);
        }

        if (!detail.userId().equals(memberId)) {
            throw new EduQuestException(WrongNoteErrorCode.FORBIDDEN_WRONG_NOTE_ACCESS);
        }

        Problem problem = problemQueryService.findProblemById(detail.problemId());

        AiFeedBackRequest request = AiFeedBackRequest.of(
                problem.getSummary(),
                problem.getExpectedOutput(),
                detail.wrongAnswer(),
                Map.of("block", problem.getBlock() == null ? "" : problem.getBlock())
        );

        log.info("AI 피드백 생성 시작: wrongNoteUuid={}, userId={}, problemId={}", wrongNoteUuid, memberId, detail.problemId());
        String aiExplanation;
        try {
            aiExplanation = normalizeAiFeedback(chatModelService.generateAiExplanation(request));
        } catch (RuntimeException exception) {
            log.warn("AI 피드백 생성 실패: wrongNoteUuid={}, userId={}, problemId={}, message={}",
                    wrongNoteUuid, memberId, detail.problemId(), exception.getMessage());
            throw new EduQuestException(WrongNoteErrorCode.AI_FEEDBACK_UNAVAILABLE);
        }

        WrongNote wrongNote = wrongNoteQueryService.findWrongNoteByUuid(wrongNoteUuid);
        wrongNote.updateAiExplanation(aiExplanation);
        wrongNoteCommandService.updateWrongNote(wrongNote);

        log.info("AI 피드백 생성 완료: wrongNoteUuid={}, userId={}, problemId={}", wrongNoteUuid, memberId, detail.problemId());

        return findWrongNoteByUuid(wrongNoteUuid, userId);
    }

    private WrongNoteDto toWrongNoteDto(WrongNoteQuery.Detail detail, UUID userUuid) {
        Problem problem = problemQueryService.findProblemById(detail.problemId());

        return WrongNoteDto.of(
                detail.uuid(),
                detail.id(),
                detail.problemId(),
                problem.getUuid(),
                problem.getSummary(),
                userUuid,
                detail.wrongAnswer(),
                normalizeAiFeedback(detail.aiExplanation()),
                detail.isReviewed(),
                detail.updatedAt(),
                detail.createdAt(),
                detail.updatedAt()
        );
    }

    private String normalizeAiFeedback(String aiExplanation) {
        if (aiExplanation == null) {
            return null;
        }

        String decoded = decodeHtmlText(aiExplanation);
        return decoded
                .replaceFirst("^\\s*aiExplanation\\s*:\\s*", "")
                .trim();
    }

    private String decodeHtmlText(String text) {
        String decoded = text;

        for (int i = 0; i < 3; i++) {
            String next = decoded
                    .replace("&amp;", "&")
                    .replace("&quot;", "\"")
                    .replace("&#34;", "\"")
                    .replace("&#39;", "'")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&#96;", "`");

            decoded = decodeNumericHtmlEntities(next);
        }

        return decoded;
    }

    private String decodeNumericHtmlEntities(String text) {
        Matcher matcher = NUMERIC_HTML_ENTITY_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String rawCode = matcher.group(1);
            try {
                int radix = rawCode.startsWith("x") || rawCode.startsWith("X") ? 16 : 10;
                String digits = radix == 16 ? rawCode.substring(1) : rawCode;
                String replacement = new String(Character.toChars(Integer.parseInt(digits, radix)));
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
            } catch (RuntimeException ignored) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group()));
            }
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

}
