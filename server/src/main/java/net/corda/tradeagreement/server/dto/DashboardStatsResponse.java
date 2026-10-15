package net.corda.tradeagreement.server.dto;

public class DashboardStatsResponse {

    private long totalAgreements;
    private long pendingAgreements;
    private long approvedAgreements;
    private long totalTransactions;
    private String activeParty;
    private String activePartyX500;
    private String activeRole;

    public DashboardStatsResponse() {
    }

    public DashboardStatsResponse(
            long totalAgreements,
            long pendingAgreements,
            long approvedAgreements,
            long totalTransactions,
            String activeParty,
            String activePartyX500,
            String activeRole) {
        this.totalAgreements = totalAgreements;
        this.pendingAgreements = pendingAgreements;
        this.approvedAgreements = approvedAgreements;
        this.totalTransactions = totalTransactions;
        this.activeParty = activeParty;
        this.activePartyX500 = activePartyX500;
        this.activeRole = activeRole;
    }

    public long getTotalAgreements() {
        return totalAgreements;
    }

    public long getPendingAgreements() {
        return pendingAgreements;
    }

    public long getApprovedAgreements() {
        return approvedAgreements;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public String getActiveParty() {
        return activeParty;
    }

    public String getActivePartyX500() {
        return activePartyX500;
    }

    public String getActiveRole() {
        return activeRole;
    }
}
