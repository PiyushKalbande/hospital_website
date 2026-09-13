package in.chinche.hospital.service;

import in.chinche.hospital.domain.Enquiry;
import in.chinche.hospital.dto.request.EnquiryRequest;
import in.chinche.hospital.repository.EnquiryRepository;
import in.chinche.hospital.repository.FacilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnquiryService {

    private final EnquiryRepository enquiries;
    private final FacilityRepository facilities;
    private final NotificationService notifications;

    @Transactional
    public Enquiry submit(EnquiryRequest req) {
        Enquiry enquiry = Enquiry.builder()
                .name(req.name().trim())
                .phone(req.phone())
                .email(req.email())
                .enquiryType(req.enquiryType() == null || req.enquiryType().isBlank() ? "GENERAL" : req.enquiryType())
                .subject(req.subject())
                .message(req.message().trim())
                .facility(req.facilityId() == null ? null : facilities.findById(req.facilityId()).orElse(null))
                .status("NEW")
                .sourcePage(req.sourcePage())
                .build();
        Enquiry saved = enquiries.save(enquiry);
        log.info("Enquiry received #{} type={} from={}", saved.getId(), saved.getEnquiryType(), saved.getName());
        notifications.sendEnquiryAcknowledgement(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Enquiry> list(String status, int page, int size) {
        PageRequest pr = PageRequest.of(page, Math.min(size, 50));
        return (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status))
                ? enquiries.findAllByOrderByCreatedAtDesc(pr)
                : enquiries.findByStatusOrderByCreatedAtDesc(status.toUpperCase(), pr);
    }

    @Transactional
    public Enquiry updateStatus(Long id, String status, String note, String assignee) {
        Enquiry e = enquiries.findById(id).orElseThrow();
        e.setStatus(status);
        if (note != null) e.setResolutionNote(note);
        if (assignee != null) e.setAssignedTo(assignee);
        return enquiries.save(e);
    }

    @Transactional(readOnly = true)
    public long openCount() {
        return enquiries.countByStatus("NEW");
    }
}
