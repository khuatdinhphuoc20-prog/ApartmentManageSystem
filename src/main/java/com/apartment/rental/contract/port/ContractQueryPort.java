package com.apartment.rental.contract.port;

public interface ContractQueryPort {

    boolean hasActiveOrScheduledContract(Long roomId);

    int getCurrentOccupantCount(Long roomId);
}

