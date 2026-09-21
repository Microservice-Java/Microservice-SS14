package com.storex.saga.service;

import com.storex.saga.bus.ChoreographyEventPublisher;
import com.storex.saga.event.PaymentSuccessEvent;
import com.storex.saga.event.ShippingFailedEvent;
import com.storex.saga.event.ShippingSuccessEvent;
import com.storex.saga.model.ShipmentRecord;
import com.storex.saga.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ShippingSagaService {

    private static final Logger log = LoggerFactory.getLogger(ShippingSagaService.class);

    private final ShipmentRepository shipmentRepository;
    private final ChoreographyEventPublisher eventPublisher;

    private boolean simulateTimeout = false;

    public ShippingSagaService(ShipmentRepository shipmentRepository,
                               ChoreographyEventPublisher eventPublisher) {
        this.shipmentRepository = shipmentRepository;
        this.eventPublisher = eventPublisher;
    }

    public void setSimulateTimeout(boolean simulateTimeout) {
        this.simulateTimeout = simulateTimeout;
    }

    @EventListener
    @Transactional
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        log.info("ShippingService -> Consumed PaymentSuccessEvent for Order ID {}", event.getOrderId());

        if (simulateTimeout) {
            log.warn("ShippingService -> Simulating network timeout / hung service for Order ID {}. No event emitted!",
                    event.getOrderId());
            return; // Do not publish anything to simulate timeout
        }

        String address = event.getShippingAddress();
        if (address == null || address.toUpperCase().contains("UNSUPPORTED") || address.toUpperCase().contains("INVALID")) {
            log.error("ShippingService -> Unsupported shipping address '{}' for Order ID {}. Emitting ShippingFailedEvent...",
                    address, event.getOrderId());

            ShipmentRecord record = new ShipmentRecord(event.getOrderId(), null, address, "FAILED");
            shipmentRepository.save(record);

            eventPublisher.publish(new ShippingFailedEvent(
                    event.getOrderId(), "Unsupported shipping address: " + address
            ));
            return;
        }

        String trackingNumber = "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ShipmentRecord record = new ShipmentRecord(event.getOrderId(), trackingNumber, address, "SUCCESS");
        shipmentRepository.save(record);

        log.info("ShippingService -> Shipment created for Order ID {}. Tracking: {}. Emitting ShippingSuccessEvent...",
                event.getOrderId(), trackingNumber);

        eventPublisher.publish(new ShippingSuccessEvent(event.getOrderId(), trackingNumber));
    }
}
