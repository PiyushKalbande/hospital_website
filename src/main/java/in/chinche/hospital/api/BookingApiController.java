package in.chinche.hospital.api;

import in.chinche.hospital.domain.Appointment;
import in.chinche.hospital.dto.request.BookingRequest;
import in.chinche.hospital.dto.response.ApiResponse;
import in.chinche.hospital.dto.response.BookingConfirmationDto;
import in.chinche.hospital.service.BookingReceiptPdfService;
import in.chinche.hospital.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Booking", description = "Create, track and cancel appointments")
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingApiController {

    private final BookingService bookingService;
    private final BookingReceiptPdfService receiptPdfService;

    @Operation(summary = "Confirm an appointment (final step of the widget)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BookingConfirmationDto> create(@Valid @RequestBody BookingRequest request) {
        return ApiResponse.ok("Appointment request received.", bookingService.book(request));
    }

    @Operation(summary = "Look up a booking by reference number")
    @GetMapping("/{reference}")
    public ApiResponse<BookingConfirmationDto> track(@PathVariable String reference) {
        Appointment a = bookingService.findByReference(reference);
        return ApiResponse.ok(BookingConfirmationDto.from(a));
    }

        @Operation(summary = "Download an appointment receipt PDF by booking reference")
        @GetMapping(value = "/{reference}/receipt.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
        public ResponseEntity<byte[]> receipt(@PathVariable String reference) {
        Appointment appointment = bookingService.findByReference(reference);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""
                + appointment.getReferenceNo() + "-receipt.pdf\"")
            .body(receiptPdfService.generate(appointment));
        }

    @Operation(summary = "List bookings for a mobile number (self-service tracking)")
    @GetMapping("/by-phone/{phone}")
    public ApiResponse<List<BookingConfirmationDto>> byPhone(@PathVariable String phone) {
        return ApiResponse.ok(bookingService.bookingsForPhone(phone).stream()
                .map(BookingConfirmationDto::from).toList());
    }

    @Operation(summary = "Cancel a booking")
    @PostMapping("/{reference}/cancel")
    public ApiResponse<BookingConfirmationDto> cancel(@PathVariable String reference,
                                                      @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return ApiResponse.ok("Booking cancelled.", bookingService.cancel(reference, reason));
    }
}
