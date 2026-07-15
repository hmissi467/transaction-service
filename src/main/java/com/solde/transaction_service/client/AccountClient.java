package com.solde.transaction_service.client;

import com.solde.transaction_service.dto.AmountRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "account-service",
        url = "http://account-service:8081"
)
public interface AccountClient {

    @PostMapping("/api/accounts/{id}/credit")
    void credit(@PathVariable("id") Long id,
                @RequestBody AmountRequest request);

    @PostMapping("/api/accounts/{id}/debit")
    void debit(@PathVariable("id") Long id,
               @RequestBody AmountRequest request);
}
