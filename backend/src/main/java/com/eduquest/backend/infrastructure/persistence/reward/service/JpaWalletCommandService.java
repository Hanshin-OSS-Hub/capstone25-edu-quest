package com.eduquest.backend.infrastructure.persistence.reward.service;

import com.eduquest.backend.domain.reward.service.WalletCommandService;
import com.eduquest.backend.infrastructure.persistence.reward.entity.WalletEntity;
import com.eduquest.backend.infrastructure.persistence.reward.entity.WalletHistoryEntity;
import com.eduquest.backend.infrastructure.persistence.reward.repository.WalletHistoryJpaRepository;
import com.eduquest.backend.infrastructure.persistence.reward.repository.WalletJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JpaWalletCommandService implements WalletCommandService {

    private final WalletJpaRepository walletRepository;
    private final WalletHistoryJpaRepository walletHistoryRepository;

    @Override
    @Transactional
    public void changeBalance(Long userId, Long amount, String reason) {

        if (amount == null) {
            throw new IllegalArgumentException("지갑 변경 금액은 null일 수 없습니다.");
        }

        WalletEntity wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> walletRepository.save(WalletEntity.of(userId, 0L)));

        if (amount >= 0) {
            wallet.increaseBalance(amount);
        } else {
            long debitAmount = -amount;
            long currentBalance = wallet.getBalance() == null ? 0L : wallet.getBalance();
            if (currentBalance < debitAmount) {
                throw new IllegalStateException("포인트 잔액이 부족합니다.");
            }
            wallet.decreaseBalance(-amount);
        }

        walletRepository.save(wallet);

        WalletHistoryEntity history = WalletHistoryEntity.of(wallet.getId(), amount, reason);
        walletHistoryRepository.save(history);

    }

}
