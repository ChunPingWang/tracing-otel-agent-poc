package com.ecommerce.notification.infrastructure.adapter.out.persistence;

import com.ecommerce.notification.domain.model.Customer;
import com.ecommerce.notification.domain.port.CustomerRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaCustomerRepositoryAdapter implements CustomerRepository {

    private final SpringDataCustomerRepository springDataRepo;

    public JpaCustomerRepositoryAdapter(SpringDataCustomerRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public Optional<Customer> findByCustomerId(String customerId) {
        return springDataRepo.findByCustomerId(customerId).map(this::toDomain);
    }

    private Customer toDomain(CustomerJpaEntity entity) {
        return new Customer(
                entity.getId(),
                entity.getCustomerId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPhone()
        );
    }
}
