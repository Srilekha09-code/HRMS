package com.hrms.project.listener;

import com.hrms.project.events.OvertimeSettlementEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Slf4j
@Component
public class OvertimeSettlementListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSettlement(OvertimeSettlementEvent event) {

        // ✅ This runs ONLY if transaction is successful

        log.info("Sending SMS for worker {}", event.getWorkerId());

        try {
            // Simulating SMS call
            System.out.println(
                    "SMS SENT: Worker " + event.getWorkerId() +
                            " | Month: " + event.getMonth() +
                            " | Amount: ₹" + event.getTotalAmount()
            );

            // TODO → integrate real SMS API

        } catch (Exception e) {

            log.error("SMS sending failed for worker {}", event.getWorkerId());
        }
    }
}
