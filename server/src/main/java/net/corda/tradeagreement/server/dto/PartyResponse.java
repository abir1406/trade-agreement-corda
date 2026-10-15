package net.corda.tradeagreement.server.dto;

public class PartyResponse {

    private String name;
    private String organisation;
    private String locality;
    private String country;

    public PartyResponse() {
    }

    public PartyResponse(String name, String organisation, String locality, String country) {
        this.name = name;
        this.organisation = organisation;
        this.locality = locality;
        this.country = country;
    }

    public String getName() {
        return name;
    }

    public String getOrganisation() {
        return organisation;
    }

    public String getLocality() {
        return locality;
    }

    public String getCountry() {
        return country;
    }
}
