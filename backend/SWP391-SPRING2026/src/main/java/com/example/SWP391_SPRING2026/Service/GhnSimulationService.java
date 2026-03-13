package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.Entity.Shipment;
import com.example.SWP391_SPRING2026.Enum.ShipmentStatus;
import com.example.SWP391_SPRING2026.Repository.ShipmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhnSimulationService {

    private final ShipmentRepository shipmentRepository;
    private final OrderConfirmService orderConfirmService;

    private final Random random = new Random();

    @Scheduled(fixedRate = 30000)
    public void simulateShipmentProgress() {

        log.info("GHN Simulation running...");

        List<Shipment> shipments = shipmentRepository
                .findByStatusNot(ShipmentStatus.CANCELLED);

        for (Shipment shipment : shipments) {

            if (shipment.getGhnOrderCode() == null) {
                continue;
            }

            ShipmentStatus status = shipment.getStatus();

            log.info("Processing shipment {} current status = {}",
                    shipment.getGhnOrderCode(),
                    status);

            switch (status) {

                case READY_TO_PICK -> {

                    log.info("GHN update {} -> PICKING",
                            shipment.getGhnOrderCode());

                    orderConfirmService.updateFromWebhook(
                            shipment.getGhnOrderCode(),
                            "picking"
                    );
                }

                case PICKING -> {

                    log.info("GHN update {} -> DELIVERING",
                            shipment.getGhnOrderCode());

                    orderConfirmService.updateFromWebhook(
                            shipment.getGhnOrderCode(),
                            "delivering"
                    );
                }

                case DELIVERING -> {

                    /*
                     RANDOM DELIVERY RESULT
                     */

                    int chance = random.nextInt(100);

                    if (chance < 90) {

                        log.info("GHN update {} -> DELIVERED",
                                shipment.getGhnOrderCode());

                        orderConfirmService.updateFromWebhook(
                                shipment.getGhnOrderCode(),
                                "delivered"
                        );

                    } else {

                        log.info("GHN update {} -> DELIVERY_FAIL",
                                shipment.getGhnOrderCode());

                        orderConfirmService.updateFromWebhook(
                                shipment.getGhnOrderCode(),
                                "delivery_fail"
                        );
                    }
                }

                case DELIVERED ->
                        log.info("Shipment {} already delivered",
                                shipment.getGhnOrderCode());

                case FAILED ->
                        log.info("Shipment {} delivery failed",
                                shipment.getGhnOrderCode());
            }
        }
    }
}