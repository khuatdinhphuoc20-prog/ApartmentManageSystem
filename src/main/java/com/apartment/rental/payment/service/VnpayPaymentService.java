package com.apartment.rental.payment.service;

import com.apartment.rental.account.entity.Role;
import com.apartment.rental.account.entity.User;
import com.apartment.rental.account.repository.UserRepository;
import com.apartment.rental.invoice.entity.Invoice;
import com.apartment.rental.invoice.entity.InvoiceStatus;
import com.apartment.rental.invoice.entity.PaymentMethod;
import com.apartment.rental.invoice.repository.InvoiceRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VnpayPaymentService {

    private static final Logger log = LoggerFactory.getLogger(VnpayPaymentService.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final String payUrl;
    private final String tmnCode;
    private final String secretKey;
    private final String returnUrl;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;

    public VnpayPaymentService(
        @Value("${payment.vnpay.pay-url}") String payUrl,
        @Value("${payment.vnpay.tmn-code}") String tmnCode,
        @Value("${payment.vnpay.secret-key}") String secretKey,
        @Value("${payment.vnpay.return-url}") String returnUrl,
        UserRepository userRepository,
        InvoiceRepository invoiceRepository
    ) {
        this.payUrl = payUrl;
        this.tmnCode = tmnCode;
        this.secretKey = secretKey;
        this.returnUrl = returnUrl;
        this.userRepository = userRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public String createPaymentUrl(Long invoiceId, String email, HttpServletRequest request) {
        Instant now = Instant.now();
        Invoice invoice = requirePayableInvoice(invoiceId, email, now);
        long amount = invoice.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValueExact();

        Map<String, String> parameters = new HashMap<>();
        parameters.put("vnp_Version", "2.1.0");
        parameters.put("vnp_Command", "pay");
        parameters.put("vnp_TmnCode", tmnCode);
        parameters.put("vnp_Amount", Long.toString(amount));
        parameters.put("vnp_CurrCode", "VND");
        parameters.put("vnp_TxnRef", newTxnReference(invoice.getId()));
        parameters.put("vnp_OrderInfo", "Thanh toan hoa don " + invoice.getId());
        parameters.put("vnp_OrderType", "other");
        parameters.put("vnp_Locale", "vn");
        parameters.put("vnp_ReturnUrl", returnUrl);
        String ipAddress = request.getRemoteAddr();
        parameters.put("vnp_IpAddr", ipAddress == null ? "127.0.0.1" : ipAddress);
        parameters.put("vnp_CreateDate", VNPAY_DATE.format(now.atZone(BUSINESS_ZONE)));
        parameters.put("vnp_ExpireDate", VNPAY_DATE.format(invoice.getDueAt().atZone(BUSINESS_ZONE)));

        String query = canonicalQuery(parameters);
        return payUrl + "?" + query + "&vnp_SecureHash=" + sign(query);
    }

    @Transactional
    public ReturnResult processReturn(HttpServletRequest request) {
        Map<String, String> parameters = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (key.startsWith("vnp_")) {
                parameters.put(key, values[0]);
            }
        });

        if (!hasValidSignature(parameters)
            || !tmnCode.equals(parameters.get("vnp_TmnCode"))) {
            return new ReturnResult(
                "Chưa xác nhận được giao dịch nên hóa đơn chưa được cập nhật. Nếu bạn đã thanh toán, vui lòng liên hệ quản lý để được hỗ trợ.",
                false
            );
        }
        if (!isGatewaySuccess(parameters)) {
            return new ReturnResult("Thanh toán chưa hoàn tất. Bạn có thể thử lại nếu hóa đơn còn hạn.", false);
        }

        Long invoiceId = invoiceIdFrom(parameters.get("vnp_TxnRef"));
        if (invoiceId == null) {
            return new ReturnResult("Không tìm thấy hóa đơn cho giao dịch này. Vui lòng liên hệ quản lý nếu cần hỗ trợ.", false);
        }

        BigDecimal amount = new BigDecimal(parameters.get("vnp_Amount")).movePointLeft(2);
        Instant paidAt = LocalDateTime.parse(parameters.get("vnp_PayDate"), VNPAY_DATE)
            .atZone(BUSINESS_ZONE)
            .toInstant();
        String transactionNo = parameters.get("vnp_TransactionNo");
        if (transactionNo == null || transactionNo.isBlank()) {
            return new ReturnResult(
                "VNPay chưa gửi đủ thông tin nên hóa đơn chưa được cập nhật. Nếu bạn đã thanh toán, vui lòng liên hệ quản lý.",
                false
            );
        }

        return confirmPayment(invoiceId, amount, transactionNo, paidAt, Instant.now(), parameters.get("vnp_TxnRef"));
    }

    private Invoice requirePayableInvoice(Long invoiceId, String email, Instant now) {
        User tenant = userRepository.findByEmail(email)
            .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản của bạn. Vui lòng đăng nhập lại."));
        if (tenant.getRole() != Role.USER) {
            throw new AccessDeniedException("Chỉ tài khoản người thuê mới có thể thanh toán hóa đơn.");
        }

        Invoice invoice = invoiceRepository.findByIdAndContract_Tenant_Id(invoiceId, tenant.getId())
            .orElseThrow(() -> new AccessDeniedException("Không tìm thấy hóa đơn trong tài khoản của bạn."));
        if (invoice.getStatus() != InvoiceStatus.UNPAID || !now.isBefore(invoice.getDueAt())) {
            throw new IllegalStateException("Hóa đơn này đã được thanh toán hoặc quá hạn nên không thể thanh toán online.");
        }
        return invoice;
    }

    private ReturnResult confirmPayment(
        Long invoiceId,
        BigDecimal amount,
        String transactionNo,
        Instant paidAt,
        Instant receivedAt,
        String txnRef
    ) {

        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId).orElse(null);
        if (invoice == null) {
            return new ReturnResult("Không tìm thấy hóa đơn cho giao dịch này. Vui lòng liên hệ quản lý nếu cần hỗ trợ.", false);
        }
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(paidAt);
        invoice.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        invoice.setPaymentReference(transactionNo);
        invoice.setConfirmedByUser(null);
        return new ReturnResult("Thanh toán thành công! Hóa đơn của bạn đã được cập nhật.", true);
    }
    private boolean isGatewaySuccess(Map<String, String> parameters) {
        return "00".equals(parameters.get("vnp_ResponseCode"))
            && "00".equals(parameters.get("vnp_TransactionStatus"));
    }

    private String newTxnReference(Long invoiceId) {
        return "I" + invoiceId + "T" + UUID.randomUUID().toString().replace("-", "");
    }

    private Long invoiceIdFrom(String txnRef) {
        if (txnRef == null || !txnRef.startsWith("I")) {
            return null;
        }
        int separator = txnRef.indexOf('T');
        return separator > 1 ? Long.parseLong(txnRef.substring(1, separator)) : null;
    }

    private boolean hasValidSignature(Map<String, String> parameters) {
        String providedHash = parameters.get("vnp_SecureHash");
        if (providedHash == null || providedHash.isBlank() || secretKey == null || secretKey.isBlank()) {
            return false;
        }

        String expectedHash = sign(canonicalQuery(parameters));
        return MessageDigest.isEqual(
            expectedHash.getBytes(StandardCharsets.US_ASCII),
            providedHash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII)
        );
    }

    private String sign(String query) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(query.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Không thể tạo chữ ký VNPAY.", exception);
        }
    }

    private String canonicalQuery(Map<String, String> parameters) {
        TreeMap<String, String> sorted = new TreeMap<>();
        parameters.forEach((key, value) -> {
            if (key.startsWith("vnp_") && !key.equals("vnp_SecureHash")
                && !key.equals("vnp_SecureHashType") && value != null) {
                sorted.put(key, value);
            }
        });
        return sorted.entrySet().stream()
            .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
            .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record ReturnResult(String message, boolean success) {}
}
