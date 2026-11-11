package in.chinche.hospital.service;

import in.chinche.hospital.config.AppProperties;
import in.chinche.hospital.domain.Appointment;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import com.ibm.icu.text.Transliterator;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BookingReceiptPdfService {

    private static final float MARGIN = 52;
    private static final float CONTENT_WIDTH = PDRectangle.A4.getWidth() - MARGIN * 2;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final Color INK = new Color(28, 43, 48);
    private static final Color MUTED = new Color(92, 108, 112);
    private static final Color BRAND = new Color(21, 107, 89);
    private static final Color PALE = new Color(239, 247, 244);
    private static final Color RULE = new Color(221, 229, 226);
    private static final ThreadLocal<Transliterator> ROMANIZER = ThreadLocal.withInitial(
            () -> Transliterator.getInstance("Any-Latin; Latin-ASCII"));

    private final AppProperties properties;

    public byte[] generate(Appointment appointment) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream canvas = new PDPageContentStream(document, page)) {
                drawReceipt(canvas, appointment);
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not generate the booking receipt PDF.", ex);
        }
    }

    private void drawReceipt(PDPageContentStream canvas, Appointment appointment) throws IOException {
        float pageHeight = PDRectangle.A4.getHeight();
        canvas.setNonStrokingColor(BRAND);
        canvas.addRect(0, pageHeight - 142, PDRectangle.A4.getWidth(), 142);
        canvas.fill();

        text(canvas, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20,
                MARGIN, pageHeight - 58, Color.WHITE, "CHINCHE HOSPITAL");
        text(canvas, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9,
                MARGIN, pageHeight - 78, Color.WHITE, "KATOL, NAGPUR DISTRICT, MAHARASHTRA");
        text(canvas, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9,
                MARGIN, pageHeight - 96, Color.WHITE, contactPhone());

        text(canvas, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16,
                MARGIN, pageHeight - 183, "APPOINTMENT RECEIPT");
        text(canvas, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9,
                MARGIN, pageHeight - 201, "Booking request acknowledgement");

        float referenceY = pageHeight - 258;
        canvas.setNonStrokingColor(PALE);
        canvas.addRect(MARGIN, referenceY, CONTENT_WIDTH, 38);
        canvas.fill();
        text(canvas, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 9,
                MARGIN + 14, referenceY + 22, "BOOKING REFERENCE");
        text(canvas, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14,
                MARGIN + 177, referenceY + 13, appointment.getReferenceNo());

        float y = referenceY - 30;
        y = row(canvas, y, "Patient", appointment.getPatientNameDisplay());
        y = row(canvas, y, "Phone", appointment.getPatientPhoneDisplay());
        y = row(canvas, y, "Service", appointment.getServiceType().getLabel());
        y = row(canvas, y, "Doctor", appointment.getDoctor() == null
                ? "Care team" : appointment.getDoctor().getName());
        y = row(canvas, y, "Date", appointment.getAppointmentDate().format(DATE_FORMAT));
        y = row(canvas, y, "Time", appointment.getSlotStart().format(TIME_FORMAT)
                + " - " + appointment.getSlotEnd().format(TIME_FORMAT));
        y = row(canvas, y, "Centre", facilityText(appointment));
        y = row(canvas, y, "Visit mode", appointment.getVisitMode().getLabel());
        y = row(canvas, y, "Status", appointment.getStatus().getLabel());
        y = row(canvas, y, "Amount", appointment.getAmountPayable() == null
                ? "To be confirmed" : "INR " + appointment.getAmountPayable().toPlainString());
        row(canvas, y, "Payment", displayPaymentMode(appointment.getPaymentMode()));

        text(canvas, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8,
                MARGIN, 58, MUTED, "Keep this receipt and quote the reference when contacting the hospital.");
        text(canvas, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8,
                MARGIN, 44, MUTED, "This receipt acknowledges the booking request; it is not a medical record or tax invoice.");
    }

    private float row(PDPageContentStream canvas, float y, String label, String value) throws IOException {
        PDType1Font valueFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        var lines = wrap(pdfText(value), valueFont, 10, CONTENT_WIDTH - 142);
        float rowHeight = Math.max(39, 18 + lines.size() * 12);
        canvas.setStrokingColor(RULE);
        canvas.moveTo(MARGIN, y - 10);
        canvas.lineTo(MARGIN + CONTENT_WIDTH, y - rowHeight + 8);
        canvas.stroke();
        text(canvas, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8,
                MARGIN, y + 2, label.toUpperCase(Locale.ROOT));
        for (int index = 0; index < lines.size(); index++) {
            text(canvas, valueFont, 10, MARGIN + 142, y + 1 - index * 12, lines.get(index));
        }
        return y - rowHeight;
    }

    private static void text(PDPageContentStream canvas, PDType1Font font, float size,
                             float x, float y, String value) throws IOException {
        text(canvas, font, size, x, y, INK, value);
    }

    private static void text(PDPageContentStream canvas, PDType1Font font, float size,
                             float x, float y, Color color, String value) throws IOException {
        canvas.beginText();
        canvas.setNonStrokingColor(color);
        canvas.setFont(font, size);
        canvas.newLineAtOffset(x, y);
        canvas.showText(pdfText(value));
        canvas.endText();
    }

    private static java.util.List<String> wrap(String value, PDType1Font font,
                                                float fontSize, float maxWidth) throws IOException {
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && font.getStringWidth(candidate) * fontSize / 1000 > maxWidth) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines.isEmpty() ? java.util.List.of("-") : lines;
    }

    private String facilityText(Appointment appointment) {
        if (appointment.getFacility() != null) {
            String address = appointment.getFacility().getAddressLine();
                        String name = appointment.getFacility().getName();
                        return (name == null || name.isBlank() ? "Hospital centre" : name)
                                        + (address == null || address.isBlank() ? "" : ", " + address);
        }
                if (properties.getContact() == null) return "Katol, Nagpur district, Maharashtra";
                String firstLine = properties.getContact().getAddressLine1();
                String secondLine = properties.getContact().getAddressLine2();
                String address = firstLine == null || firstLine.isBlank() ? "Katol, Maharashtra" : firstLine;
                return secondLine == null || secondLine.isBlank() ? address : address + ", " + secondLine;
        }

        private String contactPhone() {
                if (properties.getContact() == null || properties.getContact().getPhone() == null) {
                        return "Contact the hospital for assistance";
                }
                return properties.getContact().getPhone();
    }

    private static String displayPaymentMode(String paymentMode) {
        if (paymentMode == null || paymentMode.isBlank()) return "Pay at hospital";
        return paymentMode.replace('_', ' ');
    }

    private static String pdfText(String value) {
        if (value == null || value.isBlank()) return "-";
        return Normalizer.normalize(ROMANIZER.get().transliterate(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^\\x20-\\x7E]", "?");
    }
}