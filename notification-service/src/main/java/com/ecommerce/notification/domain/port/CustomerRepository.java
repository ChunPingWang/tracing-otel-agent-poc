package com.ecommerce.notification.domain.port;

import com.ecommerce.notification.domain.model.Customer;
import java.util.Optional;

/**
 * Domain port for retrieving Customer entities from the data store.
 */
public interface CustomerRepository {

    /** Finds a customer by their business customer ID. */
    Optional<Customer> findByCustomerId(String customerId);
}
