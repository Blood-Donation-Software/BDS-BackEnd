package com.blooddonation.blood_donation_support_system.controller;

import com.blooddonation.blood_donation_support_system.dto.AccountDto;
import com.blooddonation.blood_donation_support_system.dto.ProfileDto;
import com.blooddonation.blood_donation_support_system.dto.UserDonationHistoryDto;
import com.blooddonation.blood_donation_support_system.service.ProfileService;
import com.blooddonation.blood_donation_support_system.service.TokenBlacklistService;
import com.blooddonation.blood_donation_support_system.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/profile")
public class ProfileController {
    @Autowired
    private ProfileService profileService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    // Update User Profile
    @PutMapping("/update")
    public ResponseEntity<Object> update(@CookieValue("jwt-token") String jwtToken,
                                         @Valid @RequestBody ProfileDto profileDto) {
        try {
            AccountDto accountDto = jwtUtil.extractUser(jwtToken);
            ProfileDto updatedAccount = profileService.updateUser(accountDto, profileDto);
            if (updatedAccount == null) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.ok(updatedAccount);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating user information");
        }
    }

    @PostMapping("/create")
    public ResponseEntity<Object> create(@Valid @RequestBody ProfileDto profileDto) {
        try {
            String result = profileService.createProfile(profileDto);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return  ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while creating new profile");
        }
    }

    // Get User Profile Info
    @GetMapping()
    public ResponseEntity<Object> profile(@CookieValue(value = "jwt-token") String jwtToken) {
        try {
            AccountDto accountDto = jwtUtil.extractUser(jwtToken);
            ProfileDto profileDto = profileService.getProfileByAccountId(accountDto.getId());
            return ResponseEntity.ok(profileDto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while retrieving profile information");
        }
    }

    // Show member history
    @GetMapping("/history")
    public ResponseEntity<Page<UserDonationHistoryDto>> history(
            @CookieValue(value = "jwt-token") String jwtToken,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "true") boolean ascending) {
        try {
            AccountDto accountDto = jwtUtil.extractUser(jwtToken);
            Page<UserDonationHistoryDto> history = profileService.getDonationHistory(accountDto.getId(), page, size, sortBy, ascending);
            return ResponseEntity.ok(history);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/list-profile/{profileId}")
    public ResponseEntity<Object> getProfileById(@PathVariable Long profileId) {
        try {
            ProfileDto profileDto = profileService.getProfileByProfileId(profileId);
            return ResponseEntity.ok(profileDto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while retrieving user profile by email");
        }
    }
  
    @GetMapping("/search-by-personal-id")
    public ResponseEntity<Object> getProfileByPersonalId(@RequestParam String personalId) {
        try {
            List<ProfileDto> profileDtos = profileService.getProfileByPersonalId(personalId);
            return ResponseEntity.ok(profileDtos);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while retrieving user profile by personal ID");
            }
    }
  
  
    @PostMapping("/list-profile/{profileId}/update")
    public ResponseEntity<Object> updateProfile(@PathVariable Long profileId,
                                                @Valid @RequestBody ProfileDto profileDto) {
        try {
        ProfileDto profileDtoUpdated = profileService.updateProfile(profileId, profileDto);
        return ResponseEntity.ok(profileDtoUpdated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body("An error occurred while updating user information");
            }
    }


    // Show a list of all profiles
    @GetMapping("/list-profile")
    public ResponseEntity<Page<ProfileDto>> getProfileList(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size,
                                                           @RequestParam(defaultValue = "id") String sortBy,
                                                           @RequestParam(defaultValue = "true") boolean ascending) {
        Page<ProfileDto> profileDtoList = profileService.getAllProfiles(page, size, sortBy, ascending);
        return ResponseEntity.ok(profileDtoList);
    }

    // Show history of specific account
    @GetMapping("/list-profile/accountId/{accountId}/history")
    public ResponseEntity<Page<UserDonationHistoryDto>> getHistoryById(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "true") boolean ascending) {
        try {
            Page<UserDonationHistoryDto> history = profileService.getDonationHistory(accountId, page, size, sortBy, ascending);
            return ResponseEntity.ok(history);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Search profiles for blood request
    @GetMapping("/search")
    public ResponseEntity<List<ProfileDto>> searchProfiles(@RequestParam String q) {
        try {
            List<ProfileDto> profiles = profileService.searchProfiles(q);
            return ResponseEntity.ok(profiles);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Get profile by ID
//    @GetMapping("/{id}")
//    public ResponseEntity<ProfileDto> getProfileById(@PathVariable Long id) {
//        try {
//            ProfileDto profile = profileService.getProfileById(id);
//            return ResponseEntity.ok(profile);
//        } catch (RuntimeException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//    }
    // Show history of specific profile
    @GetMapping("/list-profile/profileId/{profileId}/history")
    public ResponseEntity<Page<UserDonationHistoryDto>> getHistoryByProfileId(
            @PathVariable Long profileId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "true") boolean ascending) {
        try {
            Page<UserDonationHistoryDto> history = profileService.getDonationHistoryByProfileId(profileId, page, size, sortBy, ascending);
            return ResponseEntity.ok(history);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
