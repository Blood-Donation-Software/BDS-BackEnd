package com.blooddonation.blood_donation_support_system.service.serviceImplement;

import com.blooddonation.blood_donation_support_system.dto.*;
import com.blooddonation.blood_donation_support_system.entity.*;
import com.blooddonation.blood_donation_support_system.enums.ComponentType;
import com.blooddonation.blood_donation_support_system.enums.DonationType;
import com.blooddonation.blood_donation_support_system.enums.Status;
import com.blooddonation.blood_donation_support_system.mapper.*;
import com.blooddonation.blood_donation_support_system.repository.*;
import com.blooddonation.blood_donation_support_system.service.DonationEventService;
import com.blooddonation.blood_donation_support_system.service.DonationTimeSlotService;
import com.blooddonation.blood_donation_support_system.validator.DonationEventValidator;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DonationEventServiceImpl implements DonationEventService {
    @Autowired
    private DonationEventRepository donationEventRepository;

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    @Autowired
    private BloodUnitRepository bloodUnitRepository;

    @Autowired
    private DonationTimeSlotService donationTimeSlotService;

    @Autowired
    private DonationEventValidator validator;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Transactional
    public String createDonation(DonationEventDto donationEventDto, String staffEmail) {
        // Fetch Data
        Account staff = accountRepository.findByEmail(staffEmail);

        // Create And Save Donation Event
        DonationEvent donationEvent = DonationEventMapper.createDonation(donationEventDto, staff);
        DonationEvent savedDonationEvent = donationEventRepository.save(donationEvent);

        // Create time slots for the event
        List<DonationTimeSlot> timeSlots = donationTimeSlotService.createTimeSlotsForEvent(donationEventDto.getTimeSlotDtos(), savedDonationEvent);
        savedDonationEvent.setTimeSlots(timeSlots);
        return "Donation event created successfully";
    }

    @Transactional
    public String verifyDonationEvent(Long eventId, String adminEmail, String action) {
        // Validate Input
        validator.validateEventVerification(action);

        // Fetch Data
        DonationEvent donationEvent = validator.getEventOrThrow(eventId);
        Account admin = accountRepository.findByEmail(adminEmail);

        // Update Event Status
        donationEvent.setStatus(action.equals("approve") ? Status.APPROVED : Status.REJECTED);
        donationEvent.setAccount(admin);
        donationEventRepository.save(donationEvent);

        return "Donation event " + action + "d successfully";
    }

    public DonationEventDto getDonationEventById(Long eventId) {
        // Fetch Data
        DonationEvent donationEvent = validator.getEventOrThrow(eventId);

        return DonationEventMapper.toDto(donationEvent);
    }

    public List<DonationEventDto> getAllDonationEvents() {
        // Fetch Data
        List<DonationEvent> donationEvents = donationEventRepository.findAll();

        return donationEvents.stream()
                .map(DonationEventMapper::toDto)
                .toList();
    }

    public Page<DonationEventDto> getSortedPaginatedEvents(int pageNumber, int pageSize, String sortBy, boolean ascending) {
        Sort sort = ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        return donationEventRepository.findAll(pageable).map(DonationEventMapper::toDto);
    }


    public Page<DonationEventDto> getPaginatedEventsByDateRange(
            LocalDate start, LocalDate end,
            int pageNumber, int pageSize,
            String sortBy, boolean ascending) {

        Sort sort = ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        Page<DonationEvent> page = donationEventRepository.findByDonationDateBetween(start, end, pageable);
        return page.map(DonationEventMapper::toDto);
    }


    @Transactional
    public String recordMultipleBloodDonations(Long eventId, List<SingleBloodUnitRecordDto> records, String userEmail) {
        // Fetch Data
        DonationEvent event = validator.getEventOrThrow(eventId);
        validator.validateBloodDonationRecording(event, records);

        Set<Long> registeredProfileIds = getEventDonorProfiles(eventId).stream()
                .map(ProfileDto::getId)
                .collect(Collectors.toSet());

        // Extract profile IDs from records
        Set<Long> recordProfileIds = records.stream()
                .map(SingleBloodUnitRecordDto::getProfileId)
                .collect(Collectors.toSet());

        // Check if all record profile IDs are registered
        if (!recordProfileIds.containsAll(registeredProfileIds)) {
            List<Long> missingProfileIds = registeredProfileIds.stream()
                    .filter(id -> !recordProfileIds.contains(id))
                    .toList();
            throw new IllegalArgumentException(
                    "The following registered profiles are missing in the records: " + missingProfileIds
            );
        }
        for (SingleBloodUnitRecordDto record : records) {
            Profile profile = validator.getProfileOrThrow(record.getProfileId());
            recordSingleBloodDonation(record, event, profile);
        }

        event.setStatus(Status.COMPLETED);
        donationEventRepository.save(event);
        return String.format("Successfully recorded %d blood donation(s)", records.size());
    }

    @Transactional
    public void recordSingleBloodDonation(@Valid SingleBloodUnitRecordDto record, DonationEvent event, Profile profile) {
        EventRegistration registration = eventRegistrationRepository.findByEventAndProfileId(event, profile.getId())
                .orElseThrow(() -> new RuntimeException(String.format("User profile %s is not registered for this event", profile.getId())));
        if (registration.getStatus() != Status.CHECKED_IN) {
            throw new RuntimeException(String.format("User profile %s is not checked in for this event", profile.getId()));
        }

        // Use the mapper to create the BloodUnit
        Account donor = validator.getDonorOrThrow(registration.getAccount().getId());
        BloodUnit bloodUnit = BloodUnitMapper.toEntityFromRecord(record, event, donor, profile);
        if (event.getDonationType().equals(DonationType.WHOLE_BLOOD)) {
            bloodUnit.setComponentType(ComponentType.WHOLE_BLOOD);
            profile.setNextEligibleDonationDate(event.getDonationDate().plusWeeks(12));
        } else {
            bloodUnit.setComponentType(ComponentType.PLATELETS);
            profile.setNextEligibleDonationDate(event.getDonationDate().plusWeeks(3));
        }

        profile.setLastDonationDate(event.getDonationDate());
        profileRepository.save(profile);
        bloodUnitRepository.save(bloodUnit);

        registration.setStatus(Status.COMPLETED);
        eventRegistrationRepository.save(registration);
    }

    @Transactional
    public Page<AccountDto> getEventDonors(Long eventId, Long timeSlotId, int pageNumber, int pageSize, String sortBy, boolean ascending) {
        DonationEvent event = validator.getEventOrThrow(eventId);
        DonationTimeSlot timeSlot = validator.getSlotOrThrow(timeSlotId);

        Sort sort = ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        Page<EventRegistration> registrations = eventRegistrationRepository.findByEventAndTimeSlot(event, timeSlot, pageable);

        return registrations.map(reg -> AccountMapper.toDto(reg.getAccount()));
    }

    @Transactional
    public Page<ProfileDto> getEventDonorProfilesPage(Long eventId, int pageNumber, int pageSize, String sortBy, boolean ascending) {
        Sort sort = ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        // Use a custom repository method to exclude CANCELLED
        Page<EventRegistration> registrations = eventRegistrationRepository.findByEventIdAndStatusNot(
                eventId, Status.CANCELLED, pageable
        );

        return registrations.map(registration -> {
            Long profileId = registration.getProfileId();
            Profile profile = profileRepository.findById(profileId)
                    .orElseThrow(() -> new RuntimeException("Profile not found: " + profileId));
            return ProfileMapper.toDto(profile);
        });
    }


    public List<ProfileDto> getEventDonorProfiles(Long eventId) {
        return eventRegistrationRepository.findByEventId(eventId).stream()
                .filter(registration -> !Status.CANCELLED.equals(registration.getStatus()))
                .map(EventRegistration::getProfileId)
                .map(profileId -> profileRepository.findById(profileId)
                        .orElseThrow(() -> new RuntimeException("Profile not found: " + profileId)))
                .map(ProfileMapper::toDto)
                .collect(Collectors.toList());
    }

}