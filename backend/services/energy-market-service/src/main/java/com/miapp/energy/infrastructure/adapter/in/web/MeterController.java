package com.miapp.energy.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.miapp.energy.application.port.in.ManageMeterDevicesUseCase;
import com.miapp.energy.application.port.in.SubmitMeterReadingUseCase;
import com.miapp.energy.application.service.MeterException;
import com.miapp.energy.domain.model.EnergyReading;
import com.miapp.energy.domain.model.MeterDevice;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

/** Administrative lifecycle operations and independently authenticated ingestion. */
@RestController
@RequestMapping("/api/v1/market/devices")
public class MeterController {
    private final ManageMeterDevicesUseCase devices;
    private final SubmitMeterReadingUseCase readings;

    public MeterController(ManageMeterDevicesUseCase devices, SubmitMeterReadingUseCase readings) {
        this.devices = devices;
        this.readings = readings;
    }

    @PostMapping
    ResponseEntity<ManageMeterDevicesUseCase.Provisioned> provision(@Valid @RequestBody ProvisionRequest body,
                                                       HttpServletRequest request) {
        UUID actor = requireAdmin(request);
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore())
                .body(devices.provision(actor, body.ownerUserId(), body.deviceIdentifier()));
    }

    @PostMapping("/{id}/credential-rotation")
    ResponseEntity<ManageMeterDevicesUseCase.Provisioned> rotate(@PathVariable UUID id, HttpServletRequest request) {
        UUID actor = requireAdmin(request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(devices.rotate(actor, id));
    }

    @PatchMapping("/{id}/status")
    MeterDevice status(@PathVariable UUID id, @Valid @RequestBody StatusRequest body, HttpServletRequest request) {
        UUID actor = requireAdmin(request);
        return devices.setStatus(actor, id, body.status());
    }

    @PostMapping("/{id}/readings")
    ResponseEntity<EnergyReading> ingest(@PathVariable UUID id,
                                         @RequestHeader(value = "X-Device-Credential", required = false) String credential,
                                         @Valid @RequestBody ReadingRequest body) {
        EnergyReading reading = readings.ingest(id, credential, body.id(), body.generatedEnergyKwh(),
                body.surplusEnergyKwh(), body.measuredAt());
        return ResponseEntity.status(201).body(reading);
    }

    private UUID requireAdmin(HttpServletRequest request) {
        String identity = request.getHeader(OfferIdentity.USER_ID);
        UUID actor;
        try {
            actor = UUID.fromString(identity);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new MeterException(MeterException.Kind.UNAUTHORIZED);
        }
        String roles = request.getHeader(OfferIdentity.ROLES);
        if (roles == null || Arrays.stream(roles.split(",")).map(String::trim).noneMatch("ADMIN"::equals)) {
            throw new MeterException(MeterException.Kind.FORBIDDEN);
        }
        return actor;
    }

    public record ProvisionRequest(@NotNull UUID ownerUserId,
                                   @NotNull @Pattern(regexp = "[A-Za-z0-9._:-]{1,128}") String deviceIdentifier) {}
    public record StatusRequest(@NotNull MeterDevice.Status status) {}
    public record ReadingRequest(@NotNull UUID id,
                                 @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 6) BigDecimal generatedEnergyKwh,
                                 @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 6) BigDecimal surplusEnergyKwh,
                                 @NotNull Instant measuredAt) {
        @JsonAnySetter
        public void rejectUnknownField(String name, Object value) {
            throw new IllegalArgumentException("Unexpected reading field");
        }
    }
}
