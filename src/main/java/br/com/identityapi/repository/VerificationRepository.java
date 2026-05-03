package br.com.identityapi.repository;

import br.com.identityapi.domain.Verification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VerificationRepository extends JpaRepository<Verification, UUID> {
}
