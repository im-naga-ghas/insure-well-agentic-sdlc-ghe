package com.insurewell.service;

import com.insurewell.model.Policy;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class RenewalReminderPdfService {

    public byte[] generateRenewalReminderPdf(Policy policy) throws Exception {
        if (policy == null) {
            throw new IllegalArgumentException("Policy cannot be null");
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Letter page size is 612 x 792 points.
                
                // 1. Header
                // InsureWell Logo
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 24);
                contentStream.newLineAtOffset(50, 720);
                contentStream.showText("InsureWell");
                contentStream.endText();

                // "Policy Renewal Reminder" Title
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.newLineAtOffset(50, 680);
                contentStream.showText("Policy Renewal Reminder");
                contentStream.endText();

                // Decorative Line below header
                contentStream.setLineWidth(1f);
                contentStream.moveTo(50, 665);
                contentStream.lineTo(562, 665);
                contentStream.stroke();

                // 2. Policy Details Table
                // Table start y: 630
                // Row height: 25
                // Columns: Label (width 150), Value (width 362)
                int startX = 50;
                int startY = 630;
                int rowHeight = 25;
                
                String[][] tableData = {
                    {"Policy ID", policy.getId() != null ? policy.getId() : "N/A"},
                    {"Holder Name", policy.getHolderName() != null ? policy.getHolderName() : "N/A"},
                    {"Plan Name", policy.getPlanName() != null ? policy.getPlanName() : "N/A"},
                    {"Coverage Amount", formatCoverageAmount(policy.getCoverageAmount())},
                    {"Start Date", policy.getStartDate() != null ? policy.getStartDate() : "N/A"},
                    {"End Date", policy.getEndDate() != null ? policy.getEndDate() : "N/A"},
                    {"Days Until Expiry", calculateDaysUntilExpiry(policy.getEndDate())}
                };

                for (int i = 0; i < tableData.length; i++) {
                    int currentY = startY - (i * rowHeight);
                    
                    // Draw row border
                    contentStream.setLineWidth(0.5f);
                    contentStream.moveTo(startX, currentY);
                    contentStream.lineTo(562, currentY);
                    contentStream.stroke();

                    // Label
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 11);
                    contentStream.newLineAtOffset(startX + 10, currentY - 18);
                    contentStream.showText(tableData[i][0]);
                    contentStream.endText();

                    // Value
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 11);
                    contentStream.newLineAtOffset(startX + 180, currentY - 18);
                    contentStream.showText(tableData[i][1]);
                    contentStream.endText();
                }
                
                // Draw bottom border of the table
                int tableBottomY = startY - (tableData.length * rowHeight);
                contentStream.moveTo(startX, tableBottomY);
                contentStream.lineTo(562, tableBottomY);
                contentStream.stroke();

                // 3. Reminder Message
                int messageY = tableBottomY - 40;
                String endDateStr = policy.getEndDate() != null ? policy.getEndDate() : "N/A";
                String reminderMsg = "Your policy expires on " + endDateStr + ". Please contact InsureWell to renew before this date.";
                
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.newLineAtOffset(50, messageY);
                contentStream.showText(reminderMsg);
                contentStream.endText();

                // 4. Footer
                // Generated timestamp in UTC
                String timestamp = Instant.now().toString();
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 9);
                contentStream.newLineAtOffset(50, 50);
                contentStream.showText("Generated at (UTC): " + timestamp);
                contentStream.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private String formatCoverageAmount(Double amount) {
        if (amount == null) {
            return "$0.00";
        }
        DecimalFormat formatter = new DecimalFormat("$#,##0.00");
        return formatter.format(amount);
    }

    private String calculateDaysUntilExpiry(String endDateStr) {
        if (endDateStr == null || endDateStr.trim().isEmpty()) {
            return "N/A";
        }
        try {
            LocalDate endDate = LocalDate.parse(endDateStr);
            LocalDate today = LocalDate.now();
            long days = ChronoUnit.DAYS.between(today, endDate);
            if (days < 0) {
                return days + " (Expired)";
            } else {
                return String.valueOf(days);
            }
        } catch (Exception e) {
            return "N/A";
        }
    }
}
