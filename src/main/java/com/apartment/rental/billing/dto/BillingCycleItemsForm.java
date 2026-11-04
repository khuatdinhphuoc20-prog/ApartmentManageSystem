package com.apartment.rental.billing.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BillingCycleItemsForm {

    private List<BillingCycleItemForm> items = new ArrayList<>();
}
