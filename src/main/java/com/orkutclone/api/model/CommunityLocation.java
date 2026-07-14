package com.orkutclone.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Embeddable location for a community. Column names are prefixed with
 * {@code location_} so they do not collide with other columns on the table.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityLocation {

    @Column(name = "location_city")
    private String city;

    @Column(name = "location_state")
    private String state;

    @Column(name = "location_zip_code")
    private String zipCode;

    @Column(name = "location_country")
    private String country;
}
