package br.com.identityapi.service;

import br.com.identityapi.dto.DocumentoMenorResponse;
import br.com.identityapi.dto.DocumentoRgResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Service
public class DocumentoProcessamentoService {

    private final ImagePreProcessingService imagePreProcessingService;
    private final OcrService ocrService;
    private final RgParserService rgParserService;

    public DocumentoProcessamentoService(
            ImagePreProcessingService imagePreProcessingService,
            OcrService ocrService,
            RgParserService rgParserService) {
        this.imagePreProcessingService = imagePreProcessingService;
        this.ocrService = ocrService;
        this.rgParserService = rgParserService;
    }

    public DocumentoRgResponse processar(MultipartFile frente, MultipartFile verso) throws IOException {
        String textoFrente = extrairTexto(frente, "frente", 3);
        String textoVerso = extrairTexto(verso, "verso", 4);
        String textoCompleto = textoFrente + "\n\n" + textoVerso;
        System.out.println("=== TEXTO EXTRAÍDO (FRENTE) ===\n" + textoFrente);
        System.out.println("=== TEXTO EXTRAÍDO (VERSO) ===\n" + textoVerso);
        return rgParserService.parsear(textoCompleto);
    }

    public DocumentoMenorResponse processarMenor(MultipartFile frente, MultipartFile verso) throws IOException {
        String textoFrente = extrairTexto(frente, "frente", 3);
        String textoVerso = extrairTexto(verso, "verso", 4);
        String textoCompleto = textoFrente + "\n\n" + textoVerso;
        System.out.println("=== TEXTO EXTRAÍDO MENOR (FRENTE) ===\n" + textoFrente);
        System.out.println("=== TEXTO EXTRAÍDO MENOR (VERSO) ===\n" + textoVerso);
        return rgParserService.parsearMenor(textoCompleto);
    }

    private String extrairTexto(MultipartFile file, String label, int pageSegMode) throws IOException {
        File temp = Files.createTempFile("rg_" + label + "_", "_" + file.getOriginalFilename()).toFile();
        file.transferTo(temp);
        try {
            File imagemProcessada = imagePreProcessingService.preProcessar(temp);
            return ocrService.extrairTexto(imagemProcessada, pageSegMode);
        } finally {
            temp.delete();
        }
    }
}
