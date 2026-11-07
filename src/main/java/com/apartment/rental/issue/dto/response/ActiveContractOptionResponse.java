package com.apartment.rental.issue.dto.response;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActiveContractOptionResponse {
    private Long contractId;
    private Long roomId;
    private String roomCode;
    private Number floorNumber;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
}