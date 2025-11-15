package com.fednow.iso20022.entity;

import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * BIC Directory entity - stores bank identification codes and routing information
 */
@Table("bic_directory")
public class BicDirectory {

    @Id
    private UUID id;

    @Column("bic")
    private String bic;  // 8 or 11 character BIC

    @Column("institution_name")
    private String institutionName;

    @Column("branch_name")
    private String branchName;

    @Column("address_line1")
    private String addressLine1;

    @Column("address_line2")
    private String addressLine2;

    @Column("city")
    private String city;

    @Column("state_province")
    private String stateProvince;

    @Column("postal_code")
    private String postalCode;

    @Column("country_code")
    private String countryCode;  // ISO 3166-1 alpha-2

    @Column("swift_enabled")
    private Boolean swiftEnabled;

    @Column("fednow_participant")
    private Boolean fednowParticipant;

    @Column("routing_number")
    private String routingNumber;  // ABA routing number (US only)

    @Column("active")
    private Boolean active;

    @Column("activation_date")
    private LocalDate activationDate;

    @Column("deactivation_date")
    private LocalDate deactivationDate;

    @Column("metadata")
    private Json metadata;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    // Constructors
    public BicDirectory() {
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getBic() {
        return bic;
    }

    public void setBic(String bic) {
        this.bic = bic;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStateProvince() {
        return stateProvince;
    }

    public void setStateProvince(String stateProvince) {
        this.stateProvince = stateProvince;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public Boolean getSwiftEnabled() {
        return swiftEnabled;
    }

    public void setSwiftEnabled(Boolean swiftEnabled) {
        this.swiftEnabled = swiftEnabled;
    }

    public Boolean getFednowParticipant() {
        return fednowParticipant;
    }

    public void setFednowParticipant(Boolean fednowParticipant) {
        this.fednowParticipant = fednowParticipant;
    }

    public String getRoutingNumber() {
        return routingNumber;
    }

    public void setRoutingNumber(String routingNumber) {
        this.routingNumber = routingNumber;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDate getActivationDate() {
        return activationDate;
    }

    public void setActivationDate(LocalDate activationDate) {
        this.activationDate = activationDate;
    }

    public LocalDate getDeactivationDate() {
        return deactivationDate;
    }

    public void setDeactivationDate(LocalDate deactivationDate) {
        this.deactivationDate = deactivationDate;
    }

    public Json getMetadata() {
        return metadata;
    }

    public void setMetadata(Json metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
