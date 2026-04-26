package br.com.identityapi.controller;

import br.com.identityapi.domain.Verification;
import br.com.identityapi.domain.VerificationStatus;
import br.com.identityapi.dto.DocumentoRgResponse;
import br.com.identityapi.dto.VerificationRequest;
import br.com.identityapi.repository.VerificationRepository;
import br.com.identityapi.service.DocumentoProcessamentoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/verificacoes")
public class VerificationController {

    private final VerificationRepository verificationRepository;
    private final DocumentoProcessamentoService documentoProcessamentoService;

    public VerificationController(
            VerificationRepository verificationRepository,
            DocumentoProcessamentoService documentoProcessamentoService) {
        this.verificationRepository = verificationRepository;
        this.documentoProcessamentoService = documentoProcessamentoService;
    }

    @PostMapping
    public ResponseEntity<Verification> criar(@Valid @RequestBody VerificationRequest request) {
        Verification verification = new Verification();
        verification.setIdUsuario(request.idUsuario());
        verification.setEmailResponsavel(request.emailResponsavel());

        Verification saved = verificationRepository.save(verification);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(saved);
    }

    @PostMapping("/{id}/documento-responsavel")
    public ResponseEntity<DocumentoRgResponse> enviarDocumento(
            @PathVariable UUID id,
            @RequestParam("frente") MultipartFile frente,
            @RequestParam("verso") MultipartFile verso) throws IOException {

        Verification verification = verificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Verificação não encontrada com id: " + id));

        System.out.println("Frente recebida: " + frente.getOriginalFilename());
        System.out.println("Verso recebido: " + verso.getOriginalFilename());

        DocumentoRgResponse resultado = documentoProcessamentoService.processar(frente, verso);

        verification.setStatus(VerificationStatus.EM_ANALISE);
        verificationRepository.save(verification);

        return ResponseEntity.ok(resultado);
    }
}
