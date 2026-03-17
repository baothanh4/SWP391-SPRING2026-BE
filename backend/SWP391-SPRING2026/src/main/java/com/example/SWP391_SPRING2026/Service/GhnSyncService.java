package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.Entity.Shipment;
import com.example.SWP391_SPRING2026.Enum.ShipmentStatus;
import com.example.SWP391_SPRING2026.Repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhnSyncService {

    private final ShipmentRepository shipmentRepository;
    private final OrderConfirmService orderConfirmService;
    private final GhnService ghnService;

    @Scheduled(fixedRate = 20000) // 20s
    public void syncShipmentStatus() {

        log.info("🔄 GHN Sync running...");

        List<Shipment> shipments = shipmentRepository.findAll();

        for (Shipment shipment : shipments) {

            if (shipment.getGhnOrderCode() == null) continue;

            try {
                String ghnCode = shipment.getGhnOrderCode();

                // 🟢 CALL GHN API
                String ghnStatus = ghnService.getOrderStatus(ghnCode);

                ShipmentStatus dbStatus = shipment.getStatus();
                ShipmentStatus ghnMapped =
                        orderConfirmService.mapStatusExternal(ghnStatus);

                if (ghnMapped == null) continue;

                // 🔥 SO SÁNH
                if (dbStatus != ghnMapped) {

                    log.info("📦 Update {}: {} -> {}",
                            ghnCode, dbStatus, ghnMapped);

                    orderConfirmService.updateFromWebhook(
                            ghnCode,
                            ghnStatus
                    );
                }

            } catch (Exception e) {
                log.error("❌ Error syncing shipment {}", shipment.getId(), e);
            }
        }
    }
}
