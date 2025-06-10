package com.blooddonation.blood_donation_support_system.dto;

import com.blooddonation.blood_donation_support_system.enums.BloodType;
import com.blooddonation.blood_donation_support_system.enums.ComponentType;
import com.blooddonation.blood_donation_support_system.enums.Status;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BloodUnitDto {
    private Long id;
    private Long eventId;
    private Long accountId;
    private Long profileId;
    private Double volume;
    private BloodType bloodType;
    private ComponentType componentType;
    private Status status;
}
