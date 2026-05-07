package br.com.identityapi.controller;

import br.com.identityapi.domain.Verification;
import br.com.identityapi.domain.VerificationStatus;
import br.com.identityapi.dto.DocumentoMenorResponse;
import br.com.identityapi.dto.DocumentoRgResponse;
import br.com.identityapi.dto.VerificationRequest;
import br.com.identityapi.repository.VerificationRepository;
import br.com.identityapi.service.DocumentoProcessamentoService;
import br.com.identityapi.exception.DocumentoIncompletoException;
import br.com.identityapi.exception.IdadeInvalidaException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
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
    public ResponseEntity<DocumentoRgResponse> enviarDocumentoResponsavel(
            @PathVariable UUID id,
            @RequestParam("frente") MultipartFile frente,
            @RequestParam("verso") MultipartFile verso) throws IOException {

        Verification verification = verificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Verificação não encontrada com id: " + id));

        System.out.println("Responsável — frente: " + frente.getOriginalFilename());
        System.out.println("Responsável — verso: " + verso.getOriginalFilename());

        DocumentoRgResponse resultado = documentoProcessamentoService.processar(frente, verso);

        validarDocumentoCompleto(resultado.nome(), resultado.cpf(), resultado.dataNascimento(),
                resultado.naturalidade(), resultado.nomePai(), resultado.nomeMae(),
                resultado.orgaoExpedidor(), resultado.registroGeral(), resultado.dataExpedicao());

        verification.setStatus(VerificationStatus.AUTORIZADO_RESPONSAVEL);
        verificationRepository.save(verification);

        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/{id}/documento-menor")
    public ResponseEntity<DocumentoMenorResponse> enviarDocumentoMenor(
            @PathVariable UUID id,
            @RequestParam("frente") MultipartFile frente,
            @RequestParam("verso") MultipartFile verso) throws IOException {

        Verification verification = verificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Verificação não encontrada com id: " + id));

        System.out.println("Menor — frente: " + frente.getOriginalFilename());
        System.out.println("Menor — verso: " + verso.getOriginalFilename());

        DocumentoMenorResponse resultado = documentoProcessamentoService.processarMenor(frente, verso);

        validarDocumentoCompleto(resultado.nome(), resultado.cpf(), resultado.dataNascimento(),
                resultado.naturalidade(), resultado.nomePai(), resultado.nomeMae(),
                resultado.orgaoExpedidor(), resultado.registroGeral(), resultado.dataExpedicao());

        Integer idade = resultado.idade();
        if (idade == null) {
            throw new IdadeInvalidaException(
                    "Não foi possível determinar a idade do menor",
                    "A data de nascimento não foi extraída do documento. Verifique a qualidade das imagens.");
        }
        if (idade < 0) {
            throw new IdadeInvalidaException(
                    "Idade inválida detectada no documento",
                    "A data de nascimento extraída resultou em idade negativa (" + idade + " anos). Verifique se o documento está correto.");
        }
        if (idade >= 18) {
            throw new IdadeInvalidaException(
                    "Documento não pertence a um menor de idade",
                    "A idade extraída do documento é de " + idade + " anos. Este endpoint aceita apenas documentos de menores de 18 anos.");
        }

        verification.setStatus(VerificationStatus.EM_ANALISE);
        verificationRepository.save(verification);

        return ResponseEntity.ok(resultado);
    }

    /**
     * Valida que todos os campos obrigatórios foram extraídos pelo OCR.
     * Lança DocumentoIncompletoException listando os campos ausentes.
     */
    private void validarDocumentoCompleto(String nome, String cpf, String dataNascimento,
            String naturalidade, String nomePai, String nomeMae,
            String orgaoExpedidor, String registroGeral, String dataExpedicao) {

        List<String> faltantes = new ArrayList<>();

        if (nome == null)           faltantes.add("nome");
        if (cpf == null)            faltantes.add("cpf");
        if (dataNascimento == null) faltantes.add("dataNascimento");
        if (naturalidade == null)   faltantes.add("naturalidade");
        if (nomePai == null)        faltantes.add("nomePai");
        if (nomeMae == null)        faltantes.add("nomeMae");
        if (orgaoExpedidor == null) faltantes.add("orgaoExpedidor");
        if (registroGeral == null)  faltantes.add("registroGeral");
        if (dataExpedicao == null)  faltantes.add("dataExpedicao");

        if (!faltantes.isEmpty()) {
            throw new DocumentoIncompletoException(faltantes);
        }
    }
}
