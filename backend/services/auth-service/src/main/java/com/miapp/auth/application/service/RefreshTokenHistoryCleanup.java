package com.miapp.auth.application.service;

import com.miapp.auth.application.port.out.SessionPersistencePort;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Removes retained refresh hashes after the associated session has expired. */
@Service
@EnableScheduling
public class RefreshTokenHistoryCleanup {
    private final SessionPersistencePort sessions;

    public RefreshTokenHistoryCleanup(SessionPersistencePort sessions) {
        this.sessions = sessions;
    }

    @Scheduled(fixedDelayString = "${security.refresh-token.history-cleanup-interval:PT1H}",
            initialDelayString = "${security.refresh-token.history-cleanup-interval:PT1H}")
    @Transactional
    public void cleanup() {
        sessions.deleteExpiredRotatedHashes(500);
    }
}
