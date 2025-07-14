package com.blooddonation.blood_donation_support_system.service;


import com.blooddonation.blood_donation_support_system.dto.CheckinTokenDto;
import com.blooddonation.blood_donation_support_system.dto.ProfileDto;
import com.blooddonation.blood_donation_support_system.dto.ProfileWithFormResponseDto;
import com.blooddonation.blood_donation_support_system.entity.DonationEvent;
import com.blooddonation.blood_donation_support_system.entity.Profile;

public interface CheckinTokenService {
    CheckinTokenDto generateTokenForProfile(Profile profile, DonationEvent event);
    ProfileWithFormResponseDto getProfileFromToken(String token, String email, Long eventId);
    ProfileWithFormResponseDto checkinInfoWithPersonalId(String personalId, Long eventId);
    String getTokenForUser(Long eventId, String email);
}
