package com.eduquest.backend.presentation.learning.controller;

import com.eduquest.backend.application.learning.dto.ProblemDto;
import com.eduquest.backend.application.learning.dto.ProblemListDto;
import com.eduquest.backend.application.learning.service.ProblemService;
import com.eduquest.backend.presentation.learning.dto.request.ProblemCreateRequest;
import com.eduquest.backend.presentation.learning.dto.request.ProblemListRequest;
import com.eduquest.backend.presentation.learning.dto.request.ProblemUpdateRequest;
import com.eduquest.backend.presentation.learning.dto.response.HintResponse;
import com.eduquest.backend.presentation.learning.dto.response.ProblemListResponse;
import com.eduquest.backend.presentation.learning.dto.response.ProblemResponse;
import com.eduquest.backend.presentation.learning.mapper.HintMapper;
import com.eduquest.backend.presentation.learning.mapper.ProblemMapper;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1")
@Slf4j
public class ProblemController {

	private final ProblemService problemService;

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/problems")
	public ResponseEntity<String> createProblem(@Valid @RequestBody ProblemCreateRequest request) {
		log.info(
				"Create problem request: type={}, summary={}, expectedOutput={}, blockNull={}, block={}",
				request.type(),
				request.summary(),
				request.expectedOutput(),
				request.block() == null,
				request.block() == null ? null : request.block().toString()
		);

		problemService.createProblem(ProblemMapper.toCommand(request));

		return ResponseEntity.status(201).body("문제 생성 성공");
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/problems/{uuid}")
	public ResponseEntity<Void> updateProblem(@PathVariable UUID uuid, @Valid @RequestBody ProblemUpdateRequest request) {
		log.info(
				"Update problem request: uuid={}, stageUuid={}, type={}, number={}, summary={}, expectedOutput={}, blockNull={}, hintCount={}",
				uuid,
				request.stageUuid(),
				request.type(),
				request.number(),
				request.summary(),
				request.expectedOutput(),
				request.block() == null,
				request.hints() == null ? null : request.hints().size()
		);
		log.info("Update problem request block={}", request.block() == null ? null : request.block().toString());

		problemService.updateProblem(uuid, ProblemMapper.toCommand(request));

		return ResponseEntity.noContent().build();
	}

	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/problems/{uuid}")
	public ResponseEntity<Void> deleteProblem(@PathVariable UUID uuid) {

		problemService.deleteProblem(uuid);

		return ResponseEntity.noContent().build();
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/problems/{uuid}")
	public ResponseEntity<ProblemResponse> getProblem(@PathVariable UUID uuid) {

		ProblemDto problem = problemService.getProblem(uuid);

		return ResponseEntity.ok(ProblemMapper.toResponse(problem));
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping(value = "/problems", params = "stage_number")
	public ResponseEntity<List<ProblemResponse>> getProblemsByStageNumber(@RequestParam("stage_number") Integer stageNumber) {
		if (stageNumber == null || stageNumber <= 0) {
			return ResponseEntity.badRequest().build();
		}

		List<ProblemDto> results = problemService.findProblemsByStageNumber(stageNumber);

		return ResponseEntity.ok(ProblemMapper.toResponseList(results));
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/problems")
	public ResponseEntity<ProblemListResponse> listProblems(@Valid ProblemListRequest request) {

		ProblemListDto problems = problemService.listProblems(request.page(), request.size(), request.sort(), request.isAsc());

		return ResponseEntity.ok(ProblemMapper.toListResponse(problems));
	}

	@PreAuthorize("@authz.isSelfByUuid(authentication, #userUuid) or hasRole('ADMIN')")
	@GetMapping("/users/{userUuid}/review-problems")
	public ResponseEntity<ProblemListResponse> listReviewProblems(
			@PathVariable UUID userUuid,
			Authentication authentication
	) {
		ProblemListDto problems = problemService.findReviewProblemsByUserUuid(userUuid, authentication.getName());

		return ResponseEntity.ok(ProblemMapper.toListResponse(problems));
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/problems/{uuid}/hint")
	public ResponseEntity<HintResponse> findHint(
			@PathVariable UUID uuid,
			@RequestParam("level") Integer level,
			Authentication authentication
	) {

		String userId = authentication.getName();

		return ResponseEntity.ok(HintMapper.toResponse(problemService.findHint(uuid, level, authentication.getName())));
	}

}
