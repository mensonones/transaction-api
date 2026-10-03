package com.emerson.transaction_api.operation;

import com.emerson.transaction_api.shared.NotFoundException;
import lombok.Getter;

@Getter
public enum OperationType {
    NORMAL_PURCHASE(1),
    PURCHASE_WITH_INSTALLMENTS(2),
    WITHDRAWAL(3),
    CREDIT_VOUCHER(4);

    private final int id;

    OperationType(int id) {
        this.id = id;
    }

    public boolean isDebit() {
        return this != CREDIT_VOUCHER;
    }

    public static OperationType fromId(int id) {
        for (OperationType type : values()) {
            if (type.id == id) return type;
        }
        throw new NotFoundException("Operation type not found: " + id);
    }
}
