package com.eduquest.backend.infrastructure.persistence.reward.service;

import com.eduquest.backend.domain.reward.service.RewardCommandService;
import com.eduquest.backend.domain.reward.service.WalletCommandService;
import com.eduquest.backend.infrastructure.persistence.reward.entity.RewardHistoryEntity;
import com.eduquest.backend.infrastructure.persistence.reward.repository.RewardHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JpaRewardCommandService implements RewardCommandService {

    private final RewardHistoryJpaRepository rewardHistoryRepository;
    private final WalletCommandService walletCommandService;

    @Override
    @Transactional
    public void grantRewardIfNotExists(Long userId, Long stageId, Long amount, UUID stageUuid) {

        if (rewardHistoryRepository.existsByUserIdAndStageId(userId, stageId)) {
            log.info("이미 보상을 받은 스테이지라 중복 지급을 방지했습니다. userId={}, stageId={}, stageUuid={}", userId, stageId, stageUuid);
            return;
        }

        if (amount == null || amount <= 0) {
            log.info("스테이지 보상 금액이 없어 지급하지 않습니다. userId={}, stageId={}, stageUuid={}, amount={}", userId, stageId, stageUuid, amount);
            return;
        }

        log.info("스테이지 클리어 보상을 지급합니다. userId={}, stageId={}, stageUuid={}, amount={}", userId, stageId, stageUuid, amount);
        walletCommandService.changeBalance(userId, amount, "REWARD:" + stageUuid);

        RewardHistoryEntity rh = RewardHistoryEntity.of(userId, stageId, amount);
        rewardHistoryRepository.save(rh);
    }
}
