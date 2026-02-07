package com.ecommerce.notification.domain.port;

import com.ecommerce.notification.domain.model.Customer;
import java.util.Optional;

public interface CustomerRepository {
    Optional<Customer> findByCustomerId(String customerId);
}
