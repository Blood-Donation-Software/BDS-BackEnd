package com.blooddonation.blood_donation_support_system.util;

import com.blooddonation.blood_donation_support_system.dto.AccountDto;
import com.blooddonation.blood_donation_support_system.dto.UserDto;
import com.blooddonation.blood_donation_support_system.entity.Account;
import com.blooddonation.blood_donation_support_system.entity.Profile;
import com.blooddonation.blood_donation_support_system.entity.User;
import com.blooddonation.blood_donation_support_system.mapper.AccountMapper;
import com.blooddonation.blood_donation_support_system.mapper.UserMapper;
import com.blooddonation.blood_donation_support_system.repository.AccountRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import com.blooddonation.blood_donation_support_system.repository.UserRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

import static com.blooddonation.blood_donation_support_system.enums.Role.MEMBER;

@Component
public class JwtUtil {

    private final AccountRepository accountRepository;
    private final SecretKey secretKey;
    private final long tokenAge = 1000 * 60 * 60;

    public JwtUtil(AccountRepository accountRepository,
                   @Value("${jwt.secret}") String secret) {
        this.accountRepository = accountRepository;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + tokenAge))
                .signWith(secretKey)
                .compact();
    }

    // This method is called when the user successfully logs in using OAuth2 and save the user
    public String saveOAuth2User(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        Account account = accountRepository.findByEmail(email);
        if (account == null) {
            Account newAccount = new Account();
            newAccount.setEmail(email);
            newAccount.setRole(MEMBER);
            newAccount.setProfile(new Profile());
            accountRepository.save(newAccount);
        }
        return generateToken(email);
    }



    public boolean isTokenExpired(String token) {
        final Date expiration = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();

        return expiration.before(new Date(System.currentTimeMillis()));
    }

    public String extractBody(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public AccountDto extractUser(String token) {
        String email = extractBody(token);
        Account account = accountRepository.findByEmail(email);
        if (account == null) {
            throw new RuntimeException("User not found with email: " + email);
        }
        return AccountMapper.toDto(account);
    }


    public boolean validateToken(String token) {
        return (!isTokenExpired(token));
    }
}