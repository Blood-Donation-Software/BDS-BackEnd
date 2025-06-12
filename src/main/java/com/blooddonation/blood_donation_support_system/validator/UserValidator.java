package com.blooddonation.blood_donation_support_system.validator;

import com.blooddonation.blood_donation_support_system.dto.AccountDto;
import com.blooddonation.blood_donation_support_system.entity.Account;
import com.blooddonation.blood_donation_support_system.entity.Profile;
import com.blooddonation.blood_donation_support_system.enums.Status;
import com.blooddonation.blood_donation_support_system.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserValidator {
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public Account getUserOrThrow(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return account;
    }

    public Profile getProfileOrThrow(Profile profile) {
        if (profile == null) {
            throw new RuntimeException("Profile not found for account");
        }
        return profile;
    }

    public Account getEmailOrThrow(String email) {
        Account account = accountRepository.findByEmail(email);
        if (account == null) {
            throw new RuntimeException("Email not found");
        }
        return account;
    }

    public void validateLogin(Account account, AccountDto accountDto) {
        if (account == null || !passwordEncoder.matches(accountDto.getPassword(), account.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }
        if (account.getStatus() == Status.DISABLE) {
            throw new RuntimeException("User has been deleted");
        }
    }

    public void validateUpdatePassword(String oldPassword, String oldUserPassword, String newPassword) {
        if (!passwordEncoder.matches(oldPassword, oldUserPassword)) {
            throw new RuntimeException("Old password is incorrect");
        }
        if (newPassword.isEmpty()) {
            throw new RuntimeException("New password cannot be empty");
        }
    }
    }
