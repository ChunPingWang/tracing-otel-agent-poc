package com.ecommerce.order.infrastructure.adapter.out.kafka;

import com.ecommerce.order.application.port.out.InventoryReleasePort;
import com.ecommerce.order.application.port.out.InventoryReservePort;
import com.ecommerce.order.application.port.out.PaymentPort;
import com.ecommerce.order.application.port.out.ProductQueryPort;
import com.ecommerce.order.domain.event.OrderConfirmedEvent;
import com.ecommerce.order.domain.event.OrderConfirmedEvent.OrderItemData;
import com.ecommerce.order.infrastructure.dto.OrderConfirmedMessage;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(topics = "order-confirmed", partitions = 1)
@TestPropertySource(properties = {
    "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
public class KafkaOrderEventPublisherIntegrationTest {

    @MockBean
    private ProductQueryPort productQueryPort;

    @MockBean
    private InventoryReservePort inventoryReservePort;

    @MockBean
    private InventoryReleasePort inventoryReleasePort;

    @MockBean
    private PaymentPort paymentPort;

    @Autowired
    private KafkaOrderEventPublisher publisher;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    void should_publish_order_confirmed_event_to_kafka() {
        // Given
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                "ORD-001", "C001",
                Arrays.asList(
                        new OrderItemData("P001", 2, new BigDecimal("999.00")),
                        new OrderItemData("P002", 1, new BigDecimal("29.00"))
                ),
                new BigDecimal("2027.00")
        );

        // When
        publisher.publish(event);

        // Then — consume the message and verify
        Consumer<String, OrderConfirmedMessage> consumer = createTestConsumer();
        embeddedKafkaBroker.consumeFromEmbeddedTopics(consumer, "order-confirmed");
        ConsumerRecords<String, OrderConfirmedMessage> records =
                KafkaTestUtils.getRecords(consumer, 10000);

        assertFalse(records.isEmpty(), "Should have received at least one record");

        ConsumerRecord<String, OrderConfirmedMessage> record = records.iterator().next();
        assertEquals("ORD-001", record.key());

        OrderConfirmedMessage message = record.value();
        assertEquals("ORD-001", message.getOrderId());
        assertEquals("C001", message.getCustomerId());
        assertEquals("", message.getCustomerEmail());
        assertEquals("CONFIRMED", message.getStatus());
        assertNotNull(message.getTimestamp());
        assertEquals(2, message.getItems().size());
        assertEquals(new BigDecimal("2027.00"), message.getTotalAmount());

        consumer.close();
    }

    private Consumer<String, OrderConfirmedMessage> createTestConsumer() {
        Map<String, Object> consumerProps =
                KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        JsonDeserializer<OrderConfirmedMessage> deserializer =
                new JsonDeserializer<>(OrderConfirmedMessage.class);
        deserializer.addTrustedPackages("*");

        DefaultKafkaConsumerFactory<String, OrderConfirmedMessage> factory =
                new DefaultKafkaConsumerFactory<>(
                        consumerProps, new StringDeserializer(), deserializer);
        return factory.createConsumer();
    }
}
