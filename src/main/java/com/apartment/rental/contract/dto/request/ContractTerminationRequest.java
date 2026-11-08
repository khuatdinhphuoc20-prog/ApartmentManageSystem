package com.apartment.rental.contract.dto.request;

import com.apartment.rental.contract.entity.ContractEndReason;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractTerminationRequest{

    @NotNull(message = "Chọn lý do kết thúc hợp đồng")
    ContractEndReason reason;

    @DecimalMin(value = "0", message = "Tiền điện không được âm")
    @Digits(integer = 19, fraction = 0, message = "Tiền điện phải là số đồng nguyên")
    BigDecimal electricityAmount;

    @DecimalMin(value = "0", message = "Tiền nước không được âm")
    @Digits(integer = 19, fraction = 0, message = "Tiền nước phải là số đồng nguyên")
    BigDecimal waterAmount;

    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
    String closingNote;


}
