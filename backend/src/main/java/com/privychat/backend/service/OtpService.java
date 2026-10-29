package com.privychat.backend.service;

import com.privychat.backend.entity.VerificationCode;
import com.privychat.backend.exception.BadRequestException;
import com.privychat.backend.exception.RateLimitExceededException;
import com.privychat.backend.repository.VerificationCodeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class OtpService {

    private final VerificationCodeRepository verificationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(VerificationCodeRepository verificationCodeRepository, PasswordEncoder passwordEncoder) {
        this.verificationCodeRepository = verificationCodeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String generateAndSaveOtp(String identifier, String type) {
        // Clean up previous codes for this identifier and type
        verificationCodeRepository.deleteByIdentifierAndType(identifier, type);

        // Generate 6-digit cryptographically secure numeric OTP
        int code = 100000 + secureRandom.nextInt(900000);
        String codeStr = String.valueOf(code);

        // Store hashed code with 10-minute validity
        String hash = passwordEncoder.encode(codeStr);
        Instant expiry = Instant.now().plus(10, ChronoUnit.MINUTES);

        VerificationCode entity = new VerificationCode(identifier, hash, type, expiry);
        verificationCodeRepository.save(entity);

        return codeStr;
    }

    @Transactional
    public boolean verifyOtp(String identifier, String rawCode, String type) {
        Optional<VerificationCode> codeOpt = verificationCodeRepository.findTopByIdentifierAndTypeOrderByCreatedAtDesc(identifier, type);

        if (codeOpt.isEmpty()) {
            throw new BadRequestException("No active verification code found for this identifier");
        }

        VerificationCode code = codeOpt.get();

        if (Instant.now().isAfter(code.getExpiresAt())) {
            verificationCodeRepository.delete(code);
            throw new BadRequestException("Verification code has expired. Please request a new code.");
        }

        if (code.getAttempts() >= 5) {
            verificationCodeRepository.delete(code);
            throw new RateLimitExceededException("Too many invalid attempts. Verification code invalidated.");
        }

        if (!passwordEncoder.matches(rawCode, code.getCodeHash())) {
            code.setAttempts(code.getAttempts() + 1);
            verificationCodeRepository.save(code);
            throw new BadRequestException("Invalid verification code");
        }

        // Code matches successfully, delete it to prevent replay
        verificationCodeRepository.delete(code);
        return true;
    }
}

