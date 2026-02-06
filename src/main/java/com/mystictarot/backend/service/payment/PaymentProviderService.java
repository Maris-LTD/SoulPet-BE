package com.mystictarot.backend.service.payment;

import com.mystictarot.backend.entity.Transaction;

public interface PaymentProviderService {

    CreateOrderResult createOrder(Transaction transaction, CreateOrderCommand command);
}
