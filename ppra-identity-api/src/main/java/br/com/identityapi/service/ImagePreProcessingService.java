package br.com.identityapi.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;

@Service
public class ImagePreProcessingService {

    public File preProcessar(File imagemOriginal) throws IOException {
        BufferedImage original = ImageIO.read(imagemOriginal);

        // 1. Converte para escala de cinza
        BufferedImage cinza = new BufferedImage(
                original.getWidth(), original.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = cinza.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();

        // 2. Aumenta contraste: fator 1.8, offset -30 (suave, preserva o texto)
        RescaleOp contraste = new RescaleOp(1.8f, -30f, null);
        BufferedImage contrastada = contraste.filter(cinza, null);

        // 3. Corrige rotação se a imagem vier em portrait
        BufferedImage corrigida = contrastada;
        if (contrastada.getHeight() > contrastada.getWidth()) {
            corrigida = new BufferedImage(contrastada.getHeight(), contrastada.getWidth(), BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D gr = corrigida.createGraphics();
            gr.translate(0, contrastada.getWidth());
            gr.rotate(-Math.PI / 2);
            gr.drawImage(contrastada, 0, 0, null);
            gr.dispose();
        }

        // 4. Amplia 2x para o Tesseract ler melhor
        int novaLargura = corrigida.getWidth() * 2;
        int novaAltura = corrigida.getHeight() * 2;
        BufferedImage ampliada = new BufferedImage(novaLargura, novaAltura, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = ampliada.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(corrigida, 0, 0, novaLargura, novaAltura, null);
        g2.dispose();

        File saida = new File(System.getProperty("java.io.tmpdir"),
                "rg_processado_" + System.currentTimeMillis() + ".png");
        ImageIO.write(ampliada, "png", saida);
        System.out.println("Imagem processada salva em: " + saida.getAbsolutePath());
        return saida;
    }
}
