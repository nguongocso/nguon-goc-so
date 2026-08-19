package vn.nguongocso.auth.service.impl;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.auth.entity.AccountLock;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.AccountLockStatus;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.AccountLockRepository;
import vn.nguongocso.auth.repository.UserRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountLockExpiryScheduler {

    private final AccountLockRepository accountLockRepository;
    private final UserRepository userRepository;

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void processExpiredLocks() {
        OffsetDateTime now = OffsetDateTime.now();
        List<AccountLock> expiredLocks = accountLockRepository
            .findByStatusAndPermanentFalseAndLockUntilBefore(AccountLockStatus.LOCKED, now);

        if (expiredLocks.isEmpty()) {
            return;
        }

        log.info("Detected {} expired temporary locks to auto-unlock", expiredLocks.size());

        for (AccountLock expiredLock : expiredLocks) {
            if (expiredLock.getUser() == null) {
                continue;
            }

            User user = expiredLock.getUser();
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);

            expiredLock.setStatus(AccountLockStatus.UNLOCKED);
            expiredLock.setUnlockedAt(now);
            accountLockRepository.save(expiredLock);

            log.info(
                "Auto-unlocked temporary lock for userId={}, lockId={}, lockUntil={}",
                user.getUserId(),
                expiredLock.getId(),
                expiredLock.getLockUntil()
            );
        }
    }
}
