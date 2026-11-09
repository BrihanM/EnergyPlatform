package com.miapp.energy;

import com.miapp.energy.domain.exception.EnergyOfferDomainException;
import com.miapp.energy.domain.model.EnergyOffer;
import com.miapp.energy.domain.model.EnergyOfferStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnergyOfferDomainTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void createsActiveOfferWithServerGeneratedIdentity() {
        EnergyOffer offer = EnergyOffer.create(UUID.randomUUID(), new BigDecimal("15.000"),
                new BigDecimal("500.00"), NOW.plusSeconds(3600), NOW);

        assertThat(offer.id()).isNotNull();
        assertThat(offer.status()).isEqualTo(EnergyOfferStatus.ACTIVE);
        assertThat(offer.version()).isZero();
    }

    @Test
    void rejectsNonPositiveAndOverPrecisionValues() {
        assertThatThrownBy(() -> EnergyOffer.create(UUID.randomUUID(), BigDecimal.ZERO,
                new BigDecimal("1.00"), NOW.plusSeconds(1), NOW))
                .isInstanceOf(EnergyOfferDomainException.class);
        assertThatThrownBy(() -> EnergyOffer.create(UUID.randomUUID(), new BigDecimal("1.000"),
                new BigDecimal("1.001"), NOW.plusSeconds(1), NOW))
                .isInstanceOf(EnergyOfferDomainException.class);
    }

    @Test
    void rejectsExpirationAtOrBeforeNow() {
        assertThatThrownBy(() -> EnergyOffer.create(UUID.randomUUID(), new BigDecimal("1.000"),
                new BigDecimal("1.00"), NOW, NOW))
                .isInstanceOf(EnergyOfferDomainException.class);
    }

    @Test
    void expiresAndCannotBeModifiedOrCancelled() {
        EnergyOffer offer = EnergyOffer.create(UUID.randomUUID(), new BigDecimal("1.000"),
                new BigDecimal("1.00"), NOW.plusSeconds(1), NOW);

        offer.expireIfNeeded(NOW.plusSeconds(1));

        assertThat(offer.status()).isEqualTo(EnergyOfferStatus.EXPIRED);
        assertThatThrownBy(() -> offer.update(new BigDecimal("2.000"), new BigDecimal("2.00"),
                NOW.plusSeconds(5), NOW.plusSeconds(1)))
                .isInstanceOf(EnergyOfferDomainException.class);
        assertThatThrownBy(() -> offer.cancel(NOW.plusSeconds(1)))
                .isInstanceOf(EnergyOfferDomainException.class);
    }

    @Test
    void cancelledOfferIsTerminal() {
        EnergyOffer offer = EnergyOffer.create(UUID.randomUUID(), new BigDecimal("1.000"),
                new BigDecimal("1.00"), NOW.plusSeconds(3600), NOW);

        offer.cancel(NOW.plusSeconds(1));

        assertThat(offer.status()).isEqualTo(EnergyOfferStatus.CANCELLED);
        assertThatThrownBy(() -> offer.update(new BigDecimal("2.000"), new BigDecimal("2.00"),
                NOW.plusSeconds(3600), NOW.plusSeconds(2)))
                .isInstanceOf(EnergyOfferDomainException.class);
    }
}
