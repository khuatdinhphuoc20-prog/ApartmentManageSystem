package com.apartment.rental.contract.dto.request;

import com.apartment.rental.contract.entity.ContractStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContractFilterRequest {

    private String keyword;
    private ContractStatus status;

    private int page = 0;
    private int size = 10;
}
