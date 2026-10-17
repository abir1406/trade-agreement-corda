package net.corda.tradeagreement.server.controllers;

import net.corda.core.identity.Party;
import net.corda.tradeagreement.server.NodeRPCConnection;
import net.corda.tradeagreement.server.dto.AgreementResponse;
import net.corda.tradeagreement.server.dto.CreateAgreementRequest;
import net.corda.tradeagreement.server.dto.DashboardStatsResponse;
import net.corda.tradeagreement.server.dto.PartyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Validated
public class TradeAgreementController {

    private static final Logger log = LoggerFactory.getLogger(TradeAgreementController.class);

    private final NodeRPCConnection nodeRPCConnection;

    public TradeAgreementController(NodeRPCConnection nodeRPCConnection) {
        this.nodeRPCConnection = nodeRPCConnection;
    }

    /**
     * Get identity of the currently active simulated node (Seller or Buyer).
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getActiveIdentity() {
        Map<String, Object> response = new HashMap<>();
        Party active = nodeRPCConnection.getActiveParty();
        response.put("name", active.getName().toString());
        response.put("x500", active.getName().toString());
        response.put("party", active.getName().getOrganisation());
        response.put("organisation", active.getName().getOrganisation());
        response.put("role", nodeRPCConnection.getActiveRole());
        return ResponseEntity.ok(response);
    }

    /**
     * Switch active simulated node between SELLER (ABC Electronics) and BUYER (XYZ Retailers).
     */
    @PostMapping("/switch-party")
    public ResponseEntity<Map<String, Object>> switchParty(@RequestBody(required = false) Map<String, String> request) {
        String roleOrParty = "SELLER";
        if (request != null) {
            if (request.containsKey("role") && request.get("role") != null) {
                roleOrParty = request.get("role");
            } else if (request.containsKey("partyName") && request.get("partyName") != null) {
                roleOrParty = request.get("partyName");
            } else if (request.containsKey("party") && request.get("party") != null) {
                roleOrParty = request.get("party");
            }
        }
        nodeRPCConnection.setActiveRole(roleOrParty);
        return getActiveIdentity();
    }

    /**
     * Get available network counterparties.
     */
    @GetMapping("/parties")
    public ResponseEntity<List<PartyResponse>> getParties() {
        return ResponseEntity.ok(nodeRPCConnection.getAvailableParties());
    }

    /**
     * Get dashboard summary statistics (strictly ledger-backed).
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        return ResponseEntity.ok(nodeRPCConnection.getStats());
    }

    /**
     * Get all trade agreements from the ledger vault.
     */
    @GetMapping("/agreements")
    public ResponseEntity<List<AgreementResponse>> getAllAgreements() {
        return ResponseEntity.ok(nodeRPCConnection.getAllAgreements());
    }

    /**
     * Get details of a specific trade agreement by ID.
     */
    @GetMapping("/agreements/{agreementId}")
    public ResponseEntity<?> getAgreementById(@PathVariable String agreementId) {
        AgreementResponse agreement = nodeRPCConnection.getAgreementById(agreementId);
        if (agreement == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Agreement with ID '" + agreementId + "' was not found on the Corda ledger.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        return ResponseEntity.ok(agreement);
    }

    /**
     * Create a new trade agreement on the Corda distributed ledger.
     */
    @PostMapping("/agreements")
    public ResponseEntity<?> createAgreement(@Valid @RequestBody CreateAgreementRequest request) {
        try {
            AgreementResponse response = nodeRPCConnection.createAgreement(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error creating agreement: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            log.error("Failed to create trade agreement", e);
            Map<String, String> error = new HashMap<>();
            String message = extractRootMessage(e);
            error.put("error", "Ledger transaction failed: " + message);
            error.put("message", "Ledger transaction failed: " + message);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Approve an existing pending trade agreement on the Corda distributed ledger.
     */
    @PostMapping("/agreements/{agreementId}/approve")
    public ResponseEntity<?> approveAgreement(@PathVariable String agreementId) {
        try {
            AgreementResponse response = nodeRPCConnection.approveAgreement(agreementId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error approving agreement: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            log.error("Failed to approve trade agreement: {}", agreementId, e);
            Map<String, String> error = new HashMap<>();
            String message = extractRootMessage(e);
            error.put("error", "Approval failed: " + message);
            error.put("message", "Approval failed: " + message);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        StringBuilder sb = new StringBuilder();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            if (sb.length() > 0) sb.append("; ");
            sb.append(error.getField()).append(": ").append(error.getDefaultMessage());
        });
        Map<String, String> error = new HashMap<>();
        String message = sb.toString();
        error.put("error", message);
        error.put("message", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String msg = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        Map<String, String> error = new HashMap<>();
        error.put("error", "Invalid request format: " + msg);
        error.put("message", "Invalid request format: " + msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    private String extractRootMessage(Throwable throwable) {
        if (throwable == null) return "Unknown error";
        String message = throwable.getMessage();
        Throwable cause = throwable.getCause();
        while (cause != null) {
            if (cause.getMessage() != null && !cause.getMessage().trim().isEmpty()) {
                message = cause.getMessage();
            }
            cause = cause.getCause();
        }
        return message != null ? message : throwable.getClass().getSimpleName();
    }
}
