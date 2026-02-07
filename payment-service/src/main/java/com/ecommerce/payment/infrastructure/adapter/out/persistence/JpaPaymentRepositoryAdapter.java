package com.ecommerce.payment.infrastructure.adapter.out.persistence;

import com.ecommerce.payment.domain.model.Payment;
import com.ecommerce.payment.domain.model.PaymentStatus;
import com.ecommerce.payment.domain.port.PaymentRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaPaymentRepositoryAdapter implements PaymentRepository {

    private final SpringDataPaymentRepository springDataRepo;

    public JpaPaymentRepositoryAdapter(SpringDataPaymentRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public Payment save(Payment payment) {
        PaymentJpaEntity entity = toEntity(payment);
        PaymentJpaEntity saved = springDataRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Payment> findByPaymentId(String paymentId) {
        return springDataRepo.findByPaymentId(paymentId).map(this::toDomain);
    }

    private PaymentJpaEntity toEntity(Payment payment) {
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.setId(payment.getId());
        entity.setPaymentId(payment.getPaymentId());
        entity.setOrderId(payment.getOrderId());
        entity.setAmount(payment.getAmount());
        entity.setStatus(payment.getStatus().name());
        entity.setCreatedAt(payment.getCreatedAt());
        return entity;
    }

    private Payment toDomain(PaymentJpaEntity entity) {
        return Payment.reconstitute(
                entity.getId(),
                entity.getPaymentId(),
                entity.getOrderId(),
                entity.getAmount(),
                PaymentStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt()
        );
    }
}
