import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "GET /internal/orders/{orderId} returns order validation response"

    request {
        method GET()
        url '/internal/orders/550e8400-e29b-41d4-a716-446655440000'
    }

    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(
            orderId: '550e8400-e29b-41d4-a716-446655440000',
            totalPaymentAmount: 50000L,
            status: 'PENDING'
        )
    }
}
