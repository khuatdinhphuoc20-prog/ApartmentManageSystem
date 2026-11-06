# Payment module (VNPAY Sandbox)

This is an MVC/Thymeleaf payment flow. The tenant starts from `/customer/invoices`; VNPAY request creation, invoice eligibility checks and verified payment updates are together in `VnpayPaymentService`. It reuses the existing `invoices` columns and creates no Payment/PaymentAttempt table. New PAID rows use only `CASH` or `BANK_TRANSFER`; verified VNPAY transactions are stored as `BANK_TRANSFER`. `SANDBOX` remains in the Java enum only so existing rows can still be read.

## Configuration

Spring reads the following environment variables. `.env.example` documents the names, but Spring Boot does not load a `.env` file by itself.

- `VNPAY_PAY_URL`: sandbox payment URL; defaults to `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html`.
- `VNPAY_TMN_CODE`: sandbox merchant/terminal code.
- `VNPAY_SECRET_KEY`: sandbox signing secret. Keep it outside source control.
- `VNPAY_RETURN_URL`: browser return URL; local default is `http://localhost:8080/payment/vnpay/return`.
- VNPAY version (`2.1.0`), command (`pay`) and order type (`other`) are fixed in the payment service.

Set the environment variables in the IDE run configuration or the shell used to start Spring Boot. Do not commit actual credentials.

## Gateway URL

- Return URL: `/payment/vnpay/return`. It validates the signed VNPAY result and updates Invoice when the verified success matches the billing rules.
- This learning version does not implement IPN. VNPAY's production guidance recommends IPN for reliable server-to-server updates. With Return-only, if the tenant closes the browser or cannot return to the site, VNPAY may have received the money while the application has not yet changed Invoice status; Admin must reconcile that case.
- The local Return URL works in the tenant's browser. Use a public HTTPS Return URL for sandbox testing on another device or when VNPAY requires a public domain.

## Flow and safeguards

1. An authenticated tenant posts to `/payment/invoices/{invoiceId}/vnpay`.
2. Billing verifies that the signed-in `USER` owns the invoice, its status is `UNPAID`, and the current time is strictly before `dueAt`.
3. Payment signs the full invoice amount in VND and sets VNPAY expiry to the invoice `dueAt`.
4. Return validates HMAC-SHA512, merchant code, invoice reference and full amount. The Invoice row is locked before status changes to prevent duplicate/concurrent PAID transitions.
5. Failed payment leaves Invoice unchanged. Late success or conflicting success is written to application logs for Admin reconciliation; it does not override an overdue/already-paid invoice.
6. Admin payment recording is available at `/admin/payment/reconciliation`; choose CASH or BANK_TRANSFER only after receiving the full invoice amount. Verify any late VNPAY transaction in VNPAY before recording it as BANK_TRANSFER.

Customers can also request cash collection from an unpaid or overdue invoice. The request is sent as an in-app system notification to Admin and appears on the outstanding-invoice page; it does not set `payment_method` or mark the Invoice PAID. Admin records `CASH` only after receiving the full amount.

The sandbox flow was compile-checked only; to try it, configure rotated sandbox credentials and a Return URL reachable by the browser.
