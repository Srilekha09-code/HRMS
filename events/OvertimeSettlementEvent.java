package com.hrms.project.events;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class OvertimeSettlementEvent {

    private Long workerId;
    private String month;
    private BigDecimal totalAmount;
}