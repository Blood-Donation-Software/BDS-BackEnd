package com.blooddonation.blood_donation_support_system.service.serviceImplement;

import com.blooddonation.blood_donation_support_system.dto.AccountDto;
import com.blooddonation.blood_donation_support_system.dto.ProfileDto;
import com.blooddonation.blood_donation_support_system.dto.UserDonationHistoryDto;
import com.blooddonation.blood_donation_support_system.entity.Account;
import com.blooddonation.blood_donation_support_system.entity.Profile;
import com.blooddonation.blood_donation_support_system.enums.AccountStatus;
import com.blooddonation.blood_donation_support_system.enums.Role;
import com.blooddonation.blood_donation_support_system.mapper.ProfileMapper;
import com.blooddonation.blood_donation_support_system.mapper.UserDonationHistoryMapper;
import com.blooddonation.blood_donation_support_system.repository.AccountRepository;
import com.blooddonation.blood_donation_support_system.repository.EventRegistrationRepository;
import com.blooddonation.blood_donation_support_system.repository.ProfileRepository;
import com.blooddonation.blood_donation_support_system.service.EmailService;
import com.blooddonation.blood_donation_support_system.service.ProfileService;
import com.blooddonation.blood_donation_support_system.validator.UserValidator;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ProfileServiceImpl implements ProfileService {
    @Autowired
    private ProfileRepository profileRepository;
    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;
    @Autowired
    private UserDonationHistoryMapper userDonationHistoryMapper;
    @Autowired
    private EmailService emailService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserValidator validator;

    @Transactional
    public ProfileDto updateUser(AccountDto accountDto, ProfileDto profileDto) {

        Account account = validator.getUserOrThrow(accountDto.getId());
        Profile profile = validator.getProfileOrThrow(account.getProfile());


        if (profile.getPersonalId() == null || !profile.getPersonalId().equals(profileDto.getPersonalId())) {
            profileRepository.findByPersonalId(profileDto.getPersonalId())
                    .ifPresent(existingProfile -> {
                        throw new RuntimeException("Personal ID already exists");
                    });
        }

        ProfileMapper.updateEntityFromDto(profile, profileDto);

        Profile updatedProfile = profileRepository.save(profile);
        return ProfileMapper.toDto(updatedProfile);
    }

    public ProfileDto getProfileById(Long accountId) {
        Account account = validator.getUserOrThrow(accountId);
        Profile profile = validator.getProfileOrThrow(account.getProfile());
        return ProfileMapper.toDto(profile);
    }

    public ProfileDto getProfileByPersonalId(String personalId) {
        Profile profile = profileRepository.findByPersonalId(personalId)
                .orElseThrow(() -> new RuntimeException("Profile not found with personal ID: " + personalId));
        return ProfileMapper.toDto(profile);
    }

    public ProfileDto saveProfile(ProfileDto profileDto) {
        if (profileRepository.findByPersonalId(profileDto.getPersonalId()).isPresent()) {
            throw new RuntimeException("Personal ID already exists");
        }

        Profile profile = ProfileMapper.toEntity(profileDto);
        Profile savedProfile = profileRepository.save(profile);
        return ProfileMapper.toDto(savedProfile);
    }

    @Transactional
    public Page<UserDonationHistoryDto> getDonationHistory(long accountId, int pageNumber, int pageSize, String sortBy, boolean ascending) {
        Account account = validator.getUserOrThrow(accountId);
        Sort sort = ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        return eventRegistrationRepository.findByAccount(account, pageable).map(userDonationHistoryMapper::toDto);
    }

    public Page<ProfileDto> getAllProfiles(int pageNumber, int pageSize, String sortBy, boolean ascending) {
        Sort sort = ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        return profileRepository.findAll(pageable).map(ProfileMapper::toDto);
    }

    @Override
    public String createProfile(ProfileDto profileDto) {
        Optional<Profile> existingProfile = profileRepository.findByPersonalId(profileDto.getPersonalId());
        if (existingProfile.isPresent()) {
            throw new RuntimeException("Profile with this Personal ID already exists");
        }
        profileDto.setNextEligibleDonationDate(profileDto.getLastDonationDate());
        Profile profile = ProfileMapper.toEntity(profileDto);
        profileRepository.save(profile);
        return "Profile created successfully";
    }

    @Scheduled(cron = "0 0 0 * * *") // Runs daily at 00:00
    @Transactional
    public void notifyEligibleDonors() {
        // Get all profiles where nextEligibleDonationDate is today
        List<Profile> eligibleDonors = profileRepository.findByNextEligibleDonationDateLessThanEqual(LocalDate.now());

//         Add logging to verify execution
//        System.out.println("Checking eligible donors at: " + LocalDate.now());
//        System.out.println("Found " + eligibleDonors.size() + " eligible donors");


        for (Profile profile : eligibleDonors) {
            Account account = accountRepository.findById(profile.getAccountId())
                    .orElse(null);

            if (account == null || account.getStatus().equals(AccountStatus.DISABLE) || !account.getRole().equals(Role.MEMBER)) {
                continue;
            }

            try {
                String htmlMessage = "<html>"
                        + "<body>"
                        + "<h2>Blood Donation Eligibility Notice</h2>"
                        + "<p>Dear " + profile.getName() + ",</p>"
                        + "<p>You are now eligible to donate blood again!</p>"
                        + "<p>Please consider making another donation to help those in need.</p>"
                        + "<br>"
                        + "<p>Best regards,<br>Blood Donation Support System</p>"
                        + "</body>"
                        + "</html>";
                emailService.sendVerificationEmail(
                        account.getEmail(),
                        "You're Eligible to Donate Blood Again!",
                        htmlMessage
                );
//                System.out.println("Notification sent to: " + account.getEmail());
            } catch (Exception e) {
                System.err.println("Failed to send email to " + account.getEmail() + ": " + e.getMessage());
            }
        }
    }
}
