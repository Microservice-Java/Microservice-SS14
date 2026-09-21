package com.storex.saga.service;

import com.storex.saga.model.OrchestratorShipment;
import com.storex.saga.repository.OrchestratorShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ShippingService {

    private static final Logger log = LoggerFactory.getLogger(ShippingService.class);

    private final OrchestratorShipmentRepository shipmentRepository;

    public ShippingService(OrchestratorShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

    @Transactional
    public OrchestratorShipment createShipment(Long orderId, String address) {
        log.info("ShippingService -> Creating shipment for Order ID {} to address '{}'", orderId, address);

        if (address == null || address.toUpperCase().contains("INVALID") || address.toUpperCase().contains("UNSUPPORTED")) {
            throw new IllegalArgumentException("Unsupported shipping address: " + address);
        }

        String trackingNumber = "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        OrchestratorShipment shipment = new OrchestratorShipment(orderId, trackingNumber, address, "SUCCESS");
        shipmentRepository.save(shipment);
        log.info("ShippingService -> Shipment SUCCESS for Order ID {}. Tracking: {}", orderId, trackingNumber);
        return shipment;
    }
}
