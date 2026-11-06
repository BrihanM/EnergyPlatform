package com.miapp.energy;

import com.miapp.energy.domain.exception.EnergyTransactionDomainException;
import com.miapp.energy.domain.model.EnergyTransaction;
import com.miapp.energy.domain.model.EnergyTransactionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class EnergyTransactionDomainTest {
    @Test
    void calculatesImmutableMoneySnapshot() {
        EnergyTransaction transaction = EnergyTransaction.pending(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), new BigDecimal("15.000"), new BigDecimal("500.00"),
                "checkout-1", Instant.now());

        assertThat(transaction.totalAmount()).isEqualByComparingTo("7500.00");
        transaction.complete(true, Instant.now());
        assertThat(transaction.status()).isEqualTo(EnergyTransactionStatus.CONFIRMED);
    }

    @Test
    void rejectsSelfPurchaseAndInvalidPrecision() {
        UUID user = UUID.randomUUID();
        assertThatThrownBy(() -> EnergyTransaction.pending(UUID.randomUUID(), user, user,
                new BigDecimal("1.000"), new BigDecimal("1.00"), "key", Instant.now()))
                .isInstanceOf(EnergyTransactionDomainException.class);
        assertThatThrownBy(() -> EnergyTransaction.pending(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), new BigDecimal("1.0000"), new BigDecimal("1.00"),
                "key", Instant.now())).isInstanceOf(EnergyTransactionDomainException.class);
    }
}
