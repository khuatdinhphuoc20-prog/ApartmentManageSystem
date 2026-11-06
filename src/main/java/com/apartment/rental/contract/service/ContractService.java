package com.apartment.rental.contract.service;

import com.apartment.rental.account.dto.response.UserInfoForM04Response;
import com.apartment.rental.contract.dto.request.CreateContractFromApplicationRequest;
import com.apartment.rental.contract.dto.request.DirectContractRequest;
import com.apartment.rental.contract.dto.response.ContractDetailResponse;
import com.apartment.rental.contract.dto.response.ContractResponse;
import com.apartment.rental.contract.entity.ContractEndReason;
import com.apartment.rental.contract.entity.ContractStatus;
import com.apartment.rental.room.dto.response.RoomOptionForM04Response;

import java.time.LocalDate;
import java.util.List;

public interface ContractService {

    List<ContractResponse> getTenantContracts(Long tenantId);

    ContractDetailResponse getTenantContractDetail(
            Long contractId,
            Long tenantId
    );
    List<ContractResponse> getAllContracts();
    ContractDetailResponse getContractDetail(Long contractId);


        void cancelScheduledContract(
                Long contractId,
                Long adminId

        );

        void activateScheduledContracts(LocalDate currentDate);

        void expireActiveContracts(LocalDate currentDate);
    // UC34: Admin chấm dứt hợp đồng đang ACTIVE
    void terminateActiveContract(
            Long contractId,
            Long adminId,
            ContractEndReason reason,
            String closingNote
    );
    ContractDetailResponse createDirectContract(
            DirectContractRequest request,
            Long adminId
    );
    List<UserInfoForM04Response> getAvailableTenants();

    List<RoomOptionForM04Response> getAvailableRooms();
    List<ContractResponse> getAllContracts(
            String keyword,
            ContractStatus status
    );
    ContractDetailResponse createContractFromApplication(
            CreateContractFromApplicationRequest request,
            Long adminId
    );
}