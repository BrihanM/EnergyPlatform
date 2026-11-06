package com.miapp.energy.infrastructure.adapter.out.postgresql;

import com.miapp.energy.application.port.out.EnergyTransactionRepository;
import com.miapp.energy.domain.exception.EnergyTransactionDomainException;
import com.miapp.energy.domain.model.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
public class JdbcEnergyTransactionRepository implements EnergyTransactionRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcEnergyTransactionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<EnergyTransaction> findByBuyerAndKey(UUID buyerId, String key) {
        return jdbc.query(select() + " WHERE buyer_id = :buyer AND idempotency_key = :key",
                Map.of("buyer", buyerId, "key", key), this::map).stream().findFirst();
    }

    @Override
    public boolean claimIdempotency(UUID buyerId, String key, UUID offerId) {
        int changed = jdbc.update("""
                INSERT INTO energy_market.transaction_idempotency_keys
                (buyer_id, idempotency_key, offer_id)
                VALUES (:buyer, :key, :offer)
                ON CONFLICT (buyer_id, idempotency_key) DO NOTHING
                """, new MapSqlParameterSource().addValue("offer", offerId)
                .addValue("buyer", buyerId).addValue("key", key));
        return changed == 1;
    }

    @Override
    public EnergyTransaction save(EnergyTransaction transaction) {
        try {
            jdbc.update("""
                    INSERT INTO energy_market.energy_transactions
                    (id, offer_id, seller_id, buyer_id, energy_amount_kwh, price_per_kwh,
                     total_amount, currency, status, idempotency_key, created_at, completed_at)
                    VALUES (:id, :offer, :seller, :buyer, :energy, :price, :total,
                            :currency, :status, :key, :created, :completed)
                    """, params(transaction));
            jdbc.update("""
                    UPDATE energy_market.transaction_idempotency_keys
                       SET transaction_id = :transaction
                     WHERE buyer_id = :buyer AND idempotency_key = :key
                    """, Map.of("transaction", transaction.id(), "buyer", transaction.buyerId(),
                    "key", transaction.idempotencyKey()));
            return transaction;
        } catch (DataIntegrityViolationException exception) {
            throw new EnergyTransactionDomainException("Transaction could not be persisted");
        }
    }

    @Override
    public EnergyTransaction update(EnergyTransaction transaction) {
        jdbc.update("""
                UPDATE energy_market.energy_transactions
                   SET status = :status, completed_at = :completed
                 WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", transaction.id())
                .addValue("status", transaction.status().name())
                .addValue("completed", Timestamp.from(transaction.completedAt())));
        return transaction;
    }

    @Override
    public void saveVerification(UUID transactionId, VerificationResult result, String reason,
                                 UUID simulationReference, Instant verifiedAt) {
        jdbc.update("""
                INSERT INTO energy_market.operator_verifications
                (id, transaction_id, result, reason, simulation_reference, verified_at)
                VALUES (:id, :transaction, :result, :reason, :reference, :verified)
                """, new MapSqlParameterSource().addValue("id", UUID.randomUUID())
                .addValue("transaction", transactionId).addValue("result", result.name())
                .addValue("reason", reason).addValue("reference", simulationReference)
                .addValue("verified", Timestamp.from(verifiedAt)));
    }

    @Override
    public Optional<EnergyTransaction> findById(UUID id) {
        return jdbc.query(select() + " WHERE id = :id", Map.of("id", id), this::map).stream().findFirst();
    }

    @Override
    public Optional<VerificationSnapshot> findVerification(UUID transactionId) {
        return jdbc.query("""
                SELECT result, reason, simulation_reference, verified_at
                  FROM energy_market.operator_verifications
                 WHERE transaction_id = :transaction
                """, Map.of("transaction", transactionId), (rs, row) ->
                new VerificationSnapshot(VerificationResult.valueOf(rs.getString("result")),
                        rs.getString("reason"), rs.getObject("simulation_reference", UUID.class),
                        rs.getTimestamp("verified_at").toInstant())).stream().findFirst();
    }

    @Override
    public List<EnergyTransaction> findForUser(UUID userId, int limit, int offset,
                                                EnergyTransactionStatusFilter status,
                                                Instant from, Instant to) {
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("user", userId)
                .addValue("limit", limit).addValue("offset", offset);
        List<String> conditions = new ArrayList<>(List.of("(buyer_id = :user OR seller_id = :user)"));
        if (status != null) { conditions.add("status = :status"); p.addValue("status", status.name()); }
        if (from != null) { conditions.add("created_at >= :from"); p.addValue("from", Timestamp.from(from)); }
        if (to != null) { conditions.add("created_at <= :to"); p.addValue("to", Timestamp.from(to)); }
        return jdbc.query(select() + " WHERE " + String.join(" AND ", conditions)
                + " ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset", p, this::map);
    }

    private String select() {
        return "SELECT id, offer_id, seller_id, buyer_id, energy_amount_kwh, price_per_kwh, "
                + "total_amount, currency, status, idempotency_key, created_at, completed_at "
                + "FROM energy_market.energy_transactions";
    }

    private MapSqlParameterSource params(EnergyTransaction t) {
        return new MapSqlParameterSource().addValue("id", t.id()).addValue("offer", t.offerId())
                .addValue("seller", t.sellerId()).addValue("buyer", t.buyerId())
                .addValue("energy", t.energyAmountKwh()).addValue("price", t.pricePerKwh())
                .addValue("total", t.totalAmount()).addValue("currency", t.currency())
                .addValue("status", t.status().name()).addValue("key", t.idempotencyKey())
                .addValue("created", Timestamp.from(t.createdAt()))
                .addValue("completed", t.completedAt() == null ? null : Timestamp.from(t.completedAt()));
    }

    private EnergyTransaction map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return EnergyTransaction.restore(rs.getObject("id", UUID.class), rs.getObject("offer_id", UUID.class),
                rs.getObject("seller_id", UUID.class), rs.getObject("buyer_id", UUID.class),
                rs.getBigDecimal("energy_amount_kwh"), rs.getBigDecimal("price_per_kwh"),
                rs.getBigDecimal("total_amount"), rs.getString("currency"),
                EnergyTransactionStatus.valueOf(rs.getString("status")), rs.getString("idempotency_key"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toInstant());
    }
}
