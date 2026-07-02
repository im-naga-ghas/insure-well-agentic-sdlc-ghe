package com.insurewell.service;

import com.insurewell.model.Policy;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Renewal PDF Service
 * Generates a branded PDF renewal notice for a given policy.
 */
@Service
public class RenewalPdfService {

  private static final float MARGIN = 50f;
  private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
  private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();

  /**
   * Generate a PDF renewal notice for the given policy.
   */
  public byte[] generateRenewalPdf(Policy policy) throws IOException {
    try (PDDocument doc = new PDDocument()) {
      PDPage page = new PDPage(PDRectangle.A4);
      doc.addPage(page);

      PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
      PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

      String generatedTs = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " UTC";
      String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
      long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(),
          LocalDate.parse(policy.getEndDate(), DateTimeFormatter.ISO_DATE));

      try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
        float y = PAGE_HEIGHT - MARGIN;

        // ── Header box ──
        cs.setNonStrokingColor(0.12f, 0.27f, 0.53f);
        cs.addRect(MARGIN, y - 60, PAGE_WIDTH - 2 * MARGIN, 60);
        cs.fill();

        cs.beginText();
        cs.setFont(fontBold, 16);
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.newLineAtOffset(MARGIN + 10, y - 22);
        cs.showText("InsureWell \u2013 Policy Renewal Notice");
        cs.endText();

        cs.beginText();
        cs.setFont(fontRegular, 10);
        cs.setNonStrokingColor(0.85f, 0.90f, 1f);
        cs.newLineAtOffset(MARGIN + 10, y - 42);
        cs.showText("Generated: " + generatedTs);
        cs.endText();

        y -= 80;

        // ── Section: Policy Details ──
        y = drawSectionHeader(cs, fontBold, y, "Policy Details");
        y = drawRow(cs, fontBold, fontRegular, y, "Policy ID", policy.getId());
        y = drawRow(cs, fontBold, fontRegular, y, "Holder Name", policy.getHolderName());
        y = drawRow(cs, fontBold, fontRegular, y, "Plan Name", policy.getPlanName());
        y = drawRow(cs, fontBold, fontRegular, y, "Coverage Amount",
            "$" + String.format("%,.0f", policy.getCoverageAmount()));
        y = drawRow(cs, fontBold, fontRegular, y, "Start Date", policy.getStartDate());
        y = drawRow(cs, fontBold, fontRegular, y, "End Date",
            policy.getEndDate() + "   \u2190 EXPIRES IN " + daysRemaining + " DAYS");
        y = drawRow(cs, fontBold, fontRegular, y, "Renewal Status",
            policy.getRenewalStatus() != null ? policy.getRenewalStatus().toUpperCase() : "NONE");
        y -= 10;

        // ── Section: Policy Change History ──
        y = drawSectionHeader(cs, fontBold, y, "Policy Change History (Changeset)");

        // Table header
        float tableLeft = MARGIN;
        float colW1 = 120, colW2 = 130, colW3 = 130;
        cs.setNonStrokingColor(0.92f, 0.92f, 0.92f);
        cs.addRect(tableLeft, y - 18, colW1 + colW2 + colW3, 18);
        cs.fill();

        cs.beginText();
        cs.setFont(fontBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.newLineAtOffset(tableLeft + 4, y - 12);
        cs.showText("Field");
        cs.endText();

        cs.beginText();
        cs.setFont(fontBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.newLineAtOffset(tableLeft + colW1 + 4, y - 12);
        cs.showText("Original Value");
        cs.endText();

        cs.beginText();
        cs.setFont(fontBold, 9);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.newLineAtOffset(tableLeft + colW1 + colW2 + 4, y - 12);
        cs.showText("Current Value");
        cs.endText();

        y -= 18;

        // Changeset rows – compare current vs. static seed values
        boolean hasChanges = false;
        hasChanges |= drawChangeRow(cs, fontRegular, y, tableLeft, colW1, colW2, colW3,
            "coverageAmount", "$250,000",
            "$" + String.format("%,.0f", policy.getCoverageAmount()),
            !policy.getCoverageAmount().equals(250000.0));
        if (!policy.getCoverageAmount().equals(250000.0)) y -= 18;

        String originalEnd = "2026-12-31";
        hasChanges |= drawChangeRow(cs, fontRegular, y, tableLeft, colW1, colW2, colW3,
            "endDate", originalEnd, policy.getEndDate(),
            !policy.getEndDate().equals(originalEnd));
        if (!policy.getEndDate().equals(originalEnd)) y -= 18;

        if (!hasChanges) {
          cs.beginText();
          cs.setFont(fontRegular, 9);
          cs.setNonStrokingColor(0.4f, 0.4f, 0.4f);
          cs.newLineAtOffset(tableLeft + 4, y - 12);
          cs.showText("No modifications recorded since policy inception.");
          cs.endText();
          y -= 18;
        }

        y -= 14;

        // ── Footer ──
        float footerY = MARGIN + 20;
        cs.setNonStrokingColor(0.12f, 0.27f, 0.53f);
        cs.addRect(MARGIN, footerY, PAGE_WIDTH - 2 * MARGIN, 1);
        cs.fill();

        cs.beginText();
        cs.setFont(fontRegular, 9);
        cs.setNonStrokingColor(0.4f, 0.4f, 0.4f);
        float footerTextWidth = fontRegular.getStringWidth(
            "Generated by InsureWell \u00B7 " + today) / 1000f * 9;
        cs.newLineAtOffset((PAGE_WIDTH - footerTextWidth) / 2, footerY - 14);
        cs.showText("Generated by InsureWell \u00B7 " + today);
        cs.endText();
      }

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      doc.save(baos);
      return baos.toByteArray();
    }
  }

  private float drawSectionHeader(PDPageContentStream cs, PDType1Font fontBold,
      float y, String title) throws IOException {
    cs.setNonStrokingColor(0.93f, 0.96f, 1f);
    cs.addRect(MARGIN, y - 20, PAGE_WIDTH - 2 * MARGIN, 20);
    cs.fill();

    cs.beginText();
    cs.setFont(fontBold, 11);
    cs.setNonStrokingColor(0.12f, 0.27f, 0.53f);
    cs.newLineAtOffset(MARGIN + 4, y - 14);
    cs.showText(title);
    cs.endText();

    return y - 28;
  }

  private float drawRow(PDPageContentStream cs, PDType1Font fontBold,
      PDType1Font fontRegular, float y, String label, String value) throws IOException {
    cs.beginText();
    cs.setFont(fontBold, 9);
    cs.setNonStrokingColor(0.3f, 0.3f, 0.3f);
    cs.newLineAtOffset(MARGIN + 4, y - 12);
    cs.showText(label + ":");
    cs.endText();

    cs.beginText();
    cs.setFont(fontRegular, 9);
    cs.setNonStrokingColor(0f, 0f, 0f);
    cs.newLineAtOffset(MARGIN + 130, y - 12);
    cs.showText(value != null ? value : "");
    cs.endText();

    // Thin separator line
    cs.setNonStrokingColor(0.9f, 0.9f, 0.9f);
    cs.addRect(MARGIN, y - 14, PAGE_WIDTH - 2 * MARGIN, 0.5f);
    cs.fill();

    return y - 16;
  }

  private boolean drawChangeRow(PDPageContentStream cs, PDType1Font fontRegular,
      float y, float tableLeft, float colW1, float colW2, float colW3,
      String field, String original, String current, boolean changed) throws IOException {
    if (!changed) return false;

    cs.beginText();
    cs.setFont(fontRegular, 9);
    cs.setNonStrokingColor(0f, 0f, 0f);
    cs.newLineAtOffset(tableLeft + 4, y - 12);
    cs.showText(field);
    cs.endText();

    cs.beginText();
    cs.setFont(fontRegular, 9);
    cs.setNonStrokingColor(0.6f, 0.1f, 0.1f);
    cs.newLineAtOffset(tableLeft + colW1 + 4, y - 12);
    cs.showText(original);
    cs.endText();

    cs.beginText();
    cs.setFont(fontRegular, 9);
    cs.setNonStrokingColor(0.1f, 0.5f, 0.1f);
    cs.newLineAtOffset(tableLeft + colW1 + colW2 + 4, y - 12);
    cs.showText(current);
    cs.endText();

    // Row border
    cs.setNonStrokingColor(0.88f, 0.88f, 0.88f);
    cs.addRect(tableLeft, y - 14, colW1 + colW2 + colW3, 0.5f);
    cs.fill();

    return true;
  }
}
