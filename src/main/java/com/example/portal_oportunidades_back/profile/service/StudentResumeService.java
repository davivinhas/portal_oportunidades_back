package com.example.portal_oportunidades_back.profile.service;

import com.example.portal_oportunidades_back.exception.BusinessException;
import com.example.portal_oportunidades_back.profile.dto.StudentProfileResponse;
import java.io.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;

@Service
public class StudentResumeService {
    private final StudentProfileService profiles;

    public StudentResumeService(StudentProfileService profiles) { this.profiles = profiles; }

    public byte[] generate(Long studentId) {
        StudentProfileResponse profile = profiles.getProfile(studentId);
        if (blank(profile.name()) || blank(profile.email()) ||
                blank(profile.registrationNumber()) || blank(profile.course()))
            throw new BusinessException("Incomplete profile: name, email, registration number and course are required");
        try (PDDocument document = new PDDocument();
             InputStream fontStream = getClass().getResourceAsStream("/fonts/DejaVuSans.ttf");
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (fontStream == null) throw new IOException("Resume font resource is missing");
            PDType0Font font = PDType0Font.load(document, fontStream);
            document.getDocumentInformation().setTitle("Currículo");
            try (ResumeLayout layout = new ResumeLayout(document, font)) {
                layout.text(profile.name(), 18);
                layout.text(profile.email(), 11);
                if (!blank(profile.phone())) layout.text(profile.phone(), 11);
                layout.text("Formação acadêmica", 14);
                layout.text(profile.course() + (profile.semester() == null ? "" : " — Período " + profile.semester()), 11);
                layout.section("Resumo", profile.summary());
                layout.section("Habilidades", profile.skills());
                layout.section("Interesses", profile.interests());
                if (!profile.experiences().isEmpty()) layout.text("Experiências profissionais", 14);
                for (var experience : profile.experiences()) {
                    layout.text(experience.position() + " — " + experience.organization(), 12);
                    layout.text(experience.startDate() + " — " +
                            (experience.current() ? "Atual" : experience.endDate()), 11);
                    if (!blank(experience.description())) layout.text(experience.description(), 11);
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate resume", exception);
        }
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }

    private static final class ResumeLayout implements AutoCloseable {
        private static final float MARGIN = 50;
        private final PDDocument document;
        private final PDType0Font font;
        private PDPageContentStream stream;
        private float y;

        private ResumeLayout(PDDocument document, PDType0Font font) throws IOException {
            this.document = document;
            this.font = font;
            newPage();
        }

        private void newPage() throws IOException {
            if (stream != null) stream.close();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = PDRectangle.A4.getHeight() - MARGIN;
        }

        private void section(String title, String value) throws IOException {
            if (!blank(value)) { text(title, 14); text(value, 11); }
        }

        private void text(String value, float size) throws IOException {
            StringBuilder line = new StringBuilder();
            for (String paragraph : value.split("\\R", -1)) {
                for (int codePoint : paragraph.codePoints().toArray()) {
                    String character = supported(codePoint);
                    if (font.getStringWidth(line + character) / 1000 * size >
                            PDRectangle.A4.getWidth() - 2 * MARGIN && !line.isEmpty()) {
                        write(line.toString(), size);
                        line.setLength(0);
                    }
                    line.append(character);
                }
                write(line.toString(), size);
                line.setLength(0);
            }
            y -= 6;
        }

        private String supported(int codePoint) {
            if (Character.isISOControl(codePoint)) return " ";
            String value = new String(Character.toChars(codePoint));
            try { font.encode(value); return value; }
            catch (IOException | IllegalArgumentException exception) { return "?"; }
        }

        private void write(String line, float size) throws IOException {
            if (y - size < MARGIN) newPage();
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(line);
            stream.endText();
            y -= size + 5;
        }

        @Override
        public void close() throws IOException { if (stream != null) stream.close(); }
    }
}
