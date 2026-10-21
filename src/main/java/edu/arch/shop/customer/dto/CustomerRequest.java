package edu.arch.shop.customer.dto;

import java.math.BigDecimal;

public record CustomerRequest(
        String name,
        String email,
        String phoneNumber
) {
}