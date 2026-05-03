package br.com.identityapi.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class OcrService {

    private final String tessDataPath;

    public OcrService(@Value("${tesseract.datapath}") String tessDataPath) {
        this.tessDataPath = tessDataPath;
    }

    public String extrairTexto(File imagem) {
        return extrairTexto(imagem, 6);
    }

    public String extrairTexto(File imagem, int pageSegMode) {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage("por");
        tesseract.setPageSegMode(pageSegMode);
        tesseract.setOcrEngineMode(1);
        try {
            return tesseract.doOCR(imagem);
        } catch (TesseractException e) {
            throw new RuntimeException("Erro ao processar OCR: " + e.getMessage(), e);
        }
    }
}
