package com.apartment.rental.billing.dto;

import java.util.ArrayList;
import java.util.List;

public class BillingCycleItemsForm {

    private List<BillingCycleItemForm> items = new ArrayList<>();

    public List<BillingCycleItemForm> getItems() {
        return items;
    }

    public void setItems(List<BillingCycleItemForm> items) {
        this.items = items;
    }
}
