package com.blooddonation.blood_donation_support_system.service;

import com.blooddonation.blood_donation_support_system.dto.GoogleMapsDistanceResponse;
import com.blooddonation.blood_donation_support_system.entity.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GoogleMapsService {
    
    @Value("${SPRING_GOOGLE_MAPS_API_KEY}")
    private String apiKey;
    
    @Value("${STREET_ADDRESS}")
    private String streetAddress;
    
    @Value("${DISTRICT}")
    private String district;
    
    @Value("${CITY}")
    private String city;
    
    @Value("${STATE}")
    private String state;
    
    @Autowired
    private RestTemplate restTemplate;
    
    public GoogleMapsDistanceResponse calculateDistance(Profile profile) {
//        try {
            validateApiKey();
            
            String origin = buildProfileAddress(profile);
            String destination = buildMedicalFacilityAddress();

            String url = UriComponentsBuilder.fromUriString("https://maps.googleapis.com/maps/api/distancematrix/json")
                    .queryParam("origins", origin)
                    .queryParam("destinations", destination)
                    .queryParam("key", apiKey)
                    .queryParam("mode", "driving")
                    .queryParam("language", "en")
                    .queryParam("units", "metric")
                    .build()
                    .encode() // This ensures proper URL encoding
                    .toUriString();
//            String url = UriComponentsBuilder.fromUriString("https://maps.googleapis.com/maps/api/distancematrix/json")
//                    .queryParam("origins", origin)         // Do NOT pre-encode 'origin'
//                    .queryParam("destinations", destination)
//                    .queryParam("key", apiKey)
//                    .encode()
//                    .toUriString();// let Spring encode it
        log.info("RAW ORIGIN: '{}'", origin);
        log.info("RAW DEST: '{}'", destination);

        log.info("Calling Google Maps API for profile ID: {} with encoded URL: {}", profile.getId(), url);
            String rawJson = restTemplate.getForObject(url, String.class);
            log.info("Google Maps raw response: {}", rawJson);
            GoogleMapsDistanceResponse response = restTemplate.getForObject(url, GoogleMapsDistanceResponse.class);
            
            // Debug logging
            if (response != null) {
                log.debug("Raw API Response - Status: {}, Origin addresses: {}, Destination addresses: {}", 
                    response.getStatus(), response.getOriginAddresses(), response.getDestinationAddresses());
            }
            
            if (response != null && "OK".equals(response.getStatus())) {
                log.info("Successfully calculated distance for profile ID: {}", profile.getId());
                return response;
            } else {
                String errorStatus = response != null ? response.getStatus() : "null response";
                log.error("Google Maps API returned error status: {} for profile ID: {}", errorStatus, profile.getId());
                
                // Handle specific error cases
                if ("REQUEST_DENIED".equals(errorStatus)) {
                    throw new RuntimeException("Google Maps API request denied. Check API key, billing, and API restrictions.");
                } else if ("OVER_QUERY_LIMIT".equals(errorStatus)) {
                    throw new RuntimeException("Google Maps API query limit exceeded.");
                } else if ("ZERO_RESULTS".equals(errorStatus)) {
                    throw new RuntimeException("No route found between the addresses.");
                } else {
                    throw new RuntimeException("Failed to calculate distance: " + errorStatus);
                }
            }
            
//        } catch (Exception e) {
//            log.error("Error calculating distance for profile ID: {}", profile.getId(), e);
//            throw new RuntimeException("Error calculating distance: " + e.getMessage(), e);
//        }
    }

    private String buildProfileAddress(Profile profile) {
        StringBuilder address = new StringBuilder();

        if (profile.getAddress() != null && !profile.getAddress().trim().isEmpty()) {
            address.append(profile.getAddress().trim());
        }

        if (profile.getWard() != null && !profile.getWard().trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(profile.getWard().replaceFirst("(?i)phường\\s*", "").trim());
        }

        if (profile.getDistrict() != null && !profile.getDistrict().trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(profile.getDistrict().replaceFirst("(?i)quận\\s*", "").trim());
        }

        if (profile.getCity() != null && !profile.getCity().trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(profile.getCity().trim());
        }

        // Always add country
        if (address.length() > 0) {
            address.append(", Vietnam");
        }

        return address.toString();
    }


    private String buildMedicalFacilityAddress() {
        StringBuilder address = new StringBuilder();
        
        if (streetAddress != null && !streetAddress.trim().isEmpty()) {
            address.append(streetAddress.trim());
        }
        
        if (district != null && !district.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(district.trim());
        }
        
        if (city != null && !city.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(city.trim());
        }
        
        if (state != null && !state.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(state.trim());
        }
        
        return address.toString();
    }
    
    public String getMedicalFacilityAddress() {
        return buildMedicalFacilityAddress();
    }
    
    public void validateApiKey() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("Google Maps API key is not configured. Please check SPRING_GOOGLE_MAPS_API_KEY environment variable.");
        }
        log.info("Google Maps API key is configured (length: {})", apiKey.length());
    }
}
