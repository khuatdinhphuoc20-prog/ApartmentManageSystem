package com.apartment.rental.contract.port;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Adapter mặc định hỗ trợ chạy độc lập / mock khi M04 chưa hoàn tất implementation thật.
 * Khi M04 cung cấp Bean ContractQueryPort chính thức, Bean này sẽ tự động nhường quyền.
 */
@Component
@ConditionalOnMissingBean(type = "com.apartment.rental.contract.port.RealContractQueryPort")
public class DefaultContractQueryAdapter implements ContractQueryPort {

    @Override
    public boolean hasActiveOrScheduledContract(Long roomId) {
        return false;
    }

    @Override
    public int getCurrentOccupantCount(Long roomId) {
        return 0;
    }
}
