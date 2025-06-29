package com.blooddonation.blood_donation_support_system.service;

import com.blooddonation.blood_donation_support_system.dto.AccountDto;
import com.blooddonation.blood_donation_support_system.dto.ProfileDto;
import com.blooddonation.blood_donation_support_system.dto.UserDonationHistoryDto;
import org.springframework.data.domain.Page;

public interface ProfileService {
    ProfileDto updateUser(AccountDto accountDto, ProfileDto profileDto);

    ProfileDto getProfileById(Long accountId);

    ProfileDto getProfileByPersonalId(String personalId);

    Page<UserDonationHistoryDto> getDonationHistory(long accountId, int pageNumber, int pageSize, String sortBy, boolean ascending);

    Page<ProfileDto> getAllProfiles(int pageNumber, int pageSize, String sortBy, boolean ascending);

    void notifyEligibleDonors();
}
