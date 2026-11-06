package com.miapp.energy.infrastructure.adapter.out.postgresql;

import com.miapp.energy.application.port.out.EnergyOfferRepository;
import com.miapp.energy.application.service.OfferQuery;
import com.miapp.energy.application.service.PageResult;
import com.miapp.energy.domain.exception.EnergyOfferDomainException;
import com.miapp.energy.domain.model.EnergyOffer;
import com.miapp.energy.domain.model.EnergyOfferStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/**
 * PostgreSQL adapter for the EnergyOffer aggregate.
 */
@Repository
public class JdbcEnergyOfferRepository implements EnergyOfferRepository {

    private static final String COLUMNS = """
            id, seller_id, energy_amount_kwh, price_per_kwh, status,
            expires_at, created_at, updated_at, version
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcEnergyOfferRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public EnergyOffer save(EnergyOffer offer) {
        try {
            jdbc.update("""
                    INSERT INTO energy_market.energy_offers
                    (id, seller_id, energy_amount_kwh, price_per_kwh, status,
                     expires_at, created_at, updated_at, version)
                    VALUES (:id, :seller, :energy, :price, :status,
                            :expires, :created, :updated, :version)
                    """, params(offer));
            return offer;
        } catch (DataIntegrityViolationException exception) {
            throw new EnergyOfferDomainException("The offer could not be persisted");
        }
    }

    @Override
    public Optional<EnergyOffer> findById(UUID offerId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM energy_market.energy_offers WHERE id = :id",
                Map.of("id", offerId), this::map).stream().findFirst();
    }

    @Override
    public PageResult<EnergyOffer> findAvailable(OfferQuery query) {
        expireActiveOffers();
        return find(query, null, true);
    }

    @Override
    public PageResult<EnergyOffer> findBySeller(UUID sellerId, OfferQuery query) {
        expireActiveOffers(sellerId);
        return find(query, sellerId, false);
    }

    @Override
    public EnergyOffer update(EnergyOffer offer) {
        int changed = jdbc.update("""
                UPDATE energy_market.energy_offers
                   SET energy_amount_kwh = :energy, price_per_kwh = :price,
                       status = :status, expires_at = :expires,
                       updated_at = :updated, version = version + 1
                 WHERE id = :id AND seller_id = :seller
                   AND version = :version
                   AND status = 'ACTIVE'
                   AND expires_at > CURRENT_TIMESTAMP
                """, params(offer));
        if (changed != 1) {
            throw new EnergyOfferDomainException("Offer changed or is no longer editable");
        }
        return findById(offer.id()).orElseThrow(
                () -> new EnergyOfferDomainException("Offer was not found"));
    }

    @Override
    public int markExpired(UUID offerId) {
        return jdbc.update("""
                UPDATE energy_market.energy_offers
                   SET status = 'EXPIRED', updated_at = CURRENT_TIMESTAMP, version = version + 1
                 WHERE id = :id AND status = 'ACTIVE' AND expires_at <= CURRENT_TIMESTAMP
                """, Map.of("id", offerId));
    }

    private PageResult<EnergyOffer> find(OfferQuery query, UUID sellerId, boolean available) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("limit", query.size()).addValue("offset", query.page() * query.size());
        String filters = filters(query, sellerId, available, parameters);
        String order = order(query.sort());
        List<EnergyOffer> content = jdbc.query("SELECT " + COLUMNS
                + " FROM energy_market.energy_offers WHERE " + filters + order
                + " LIMIT :limit OFFSET :offset", parameters, this::map);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM energy_market.energy_offers WHERE "
                + filters, parameters, Long.class);
        long totalElements = total == null ? 0 : total;
        int totalPages = (int) Math.ceil((double) totalElements / query.size());
        return new PageResult<>(content, query.page(), query.size(), totalElements, totalPages);
    }

    private String filters(OfferQuery query, UUID sellerId, boolean available,
                           MapSqlParameterSource parameters) {
        List<String> conditions = new ArrayList<>();
        if (sellerId != null) {
            conditions.add("seller_id = :sellerId");
            parameters.addValue("sellerId", sellerId);
        }
        if (available) {
            conditions.add("status = 'ACTIVE'");
            conditions.add("expires_at > CURRENT_TIMESTAMP");
        } else if (query.status() != null) {
            conditions.add("status = :status");
            parameters.addValue("status", query.status().name());
        }
        if (query.id() != null) {
            conditions.add("id = :offerId");
            parameters.addValue("offerId", query.id());
        }
        if (query.minEnergy() != null) {
            conditions.add("energy_amount_kwh >= :minEnergy");
            parameters.addValue("minEnergy", query.minEnergy());
        }
        if (query.maxEnergy() != null) {
            conditions.add("energy_amount_kwh <= :maxEnergy");
            parameters.addValue("maxEnergy", query.maxEnergy());
        }
        if (query.minPrice() != null) {
            conditions.add("price_per_kwh >= :minPrice");
            parameters.addValue("minPrice", query.minPrice());
        }
        if (query.maxPrice() != null) {
            conditions.add("price_per_kwh <= :maxPrice");
            parameters.addValue("maxPrice", query.maxPrice());
        }
        if (query.expiresBefore() != null) {
            conditions.add("expires_at <= :expiresBefore");
            parameters.addValue("expiresBefore", query.expiresBefore());
        }
        return String.join(" AND ", conditions.isEmpty() ? List.of("TRUE") : conditions);
    }

    private String order(String sort) {
        return switch (sort) {
            case "expiresAt" -> " ORDER BY expires_at ASC, id ASC";
            case "pricePerKwh" -> " ORDER BY price_per_kwh ASC, id ASC";
            case "energyAmountKwh" -> " ORDER BY energy_amount_kwh ASC, id ASC";
            default -> " ORDER BY created_at DESC, id DESC";
        };
    }

    private void expireActiveOffers() {
        jdbc.update("""
                UPDATE energy_market.energy_offers
                   SET status = 'EXPIRED', updated_at = CURRENT_TIMESTAMP, version = version + 1
                 WHERE status = 'ACTIVE' AND expires_at <= CURRENT_TIMESTAMP
                """, new MapSqlParameterSource());
    }

    private void expireActiveOffers(UUID sellerId) {
        jdbc.update("""
                UPDATE energy_market.energy_offers
                   SET status = 'EXPIRED', updated_at = CURRENT_TIMESTAMP, version = version + 1
                 WHERE seller_id = :seller AND status = 'ACTIVE'
                   AND expires_at <= CURRENT_TIMESTAMP
                """, Map.of("seller", sellerId));
    }

    private MapSqlParameterSource params(EnergyOffer offer) {
        return new MapSqlParameterSource()
                .addValue("id", offer.id()).addValue("seller", offer.sellerId())
                .addValue("energy", offer.energyAmountKwh()).addValue("price", offer.pricePerKwh())
                .addValue("status", offer.status().name())
                .addValue("expires", Timestamp.from(offer.expiresAt()))
                .addValue("created", Timestamp.from(offer.createdAt()))
                .addValue("updated", Timestamp.from(offer.updatedAt()))
                .addValue("version", offer.version());
    }

    private EnergyOffer map(ResultSet rs, int row) throws SQLException {
        return EnergyOffer.restore(
                rs.getObject("id", UUID.class), rs.getObject("seller_id", UUID.class),
                rs.getBigDecimal("energy_amount_kwh"), rs.getBigDecimal("price_per_kwh"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant(),
                rs.getTimestamp("expires_at").toInstant(),
                EnergyOfferStatus.valueOf(rs.getString("status")), rs.getLong("version"));
    }
}
