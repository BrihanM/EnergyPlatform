package com.miapp.energy;

import com.miapp.energy.application.port.in.*;
import com.miapp.energy.application.service.PageResult;
import com.miapp.energy.domain.model.EnergyOffer;
import com.miapp.energy.infrastructure.adapter.in.web.EnergyOfferController;
import com.miapp.energy.infrastructure.adapter.in.web.EnergyMarketExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class EnergyOfferControllerAuthorizationTest {

    private final CreateEnergyOfferUseCase create = mock(CreateEnergyOfferUseCase.class);
    private final GetAvailableOffersUseCase available = mock(GetAvailableOffersUseCase.class);
    private final UpdateEnergyOfferUseCase update = mock(UpdateEnergyOfferUseCase.class);
    private final EnergyOfferController controller = new EnergyOfferController(create,
            available, mock(GetEnergyOfferByIdUseCase.class),
            mock(GetMyOffersUseCase.class), update,
            mock(CancelEnergyOfferUseCase.class));
    private final MockMvc mvc = standaloneSetup(controller)
            .setControllerAdvice(new EnergyMarketExceptionHandler()).build();

    @Test
    void normalUserCanCreateWithoutSellerInBody() throws Exception {
        UUID userId = UUID.randomUUID();
        EnergyOffer offer = EnergyOffer.create(userId, new BigDecimal("1.000"),
                new BigDecimal("1.00"), Instant.now().plusSeconds(3600), Instant.now());
        when(create.create(eq(userId), any(), any(), any())).thenReturn(offer);

        mvc.perform(post("/api/v1/offers")
                        .header("X-Authenticated-User-Id", userId)
                        .header("X-Authenticated-Roles", "NORMAL_USER")
                        .contentType("application/json")
                        .content("""
                                {"energyAmountKwh":1.000,"pricePerKwh":1.00,
                                 "expiresAt":"2099-01-01T00:00:00Z"}
                                """))
                .andExpect(status().isCreated());

        verify(create).create(eq(userId), eq(new BigDecimal("1.000")),
                eq(new BigDecimal("1.00")), any());
    }

    @Test
    void administratorWithoutNormalUserRoleCannotCreate() throws Exception {
        mvc.perform(post("/api/v1/offers")
                        .header("X-Authenticated-User-Id", UUID.randomUUID())
                        .header("X-Authenticated-Roles", "ADMIN")
                        .contentType("application/json")
                        .content("""
                                {"energyAmountKwh":1.000,"pricePerKwh":1.00,
                                 "expiresAt":"2099-01-01T00:00:00Z"}
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(create);
    }

    @Test
    void unauthenticatedCallerCanListPublicOffers() throws Exception {
        when(available.getAvailable(any())).thenReturn(new PageResult<>(java.util.List.of(),
                0, 20, 0, 0));

        mvc.perform(get("/api/v1/offers"))
                .andExpect(status().isOk());

        verify(available).getAvailable(any());
    }

    @Test
    void unauthenticatedCallerCannotCreate() throws Exception {
        mvc.perform(post("/api/v1/offers")
                        .contentType("application/json")
                        .content("""
                                {"energyAmountKwh":1.000,"pricePerKwh":1.00,
                                 "expiresAt":"2099-01-01T00:00:00Z"}
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(create);
    }

    @Test
    void authenticatedUserCannotUpdateOfferWithoutTrustedNormalUserIdentity() throws Exception {
        mvc.perform(put("/api/v1/offers/" + UUID.randomUUID())
                        .header("X-Authenticated-User-Id", UUID.randomUUID())
                        .header("X-Authenticated-Roles", "ADMIN")
                        .contentType("application/json")
                        .content("""
                                {"energyAmountKwh":1.000,"pricePerKwh":1.00,
                                 "expiresAt":"2099-01-01T00:00:00Z"}
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(update);
    }
}
