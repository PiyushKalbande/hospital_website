package in.chinche.hospital.service;

import in.chinche.hospital.domain.*;
import in.chinche.hospital.dto.response.SearchSuggestionDto;
import in.chinche.hospital.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Unified omni-search behind the header search box and the widget's step-1 field.
 * Fans out to every searchable catalogue and merges into one ranked suggestion list.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private static final int PER_SOURCE = 4;

    private final DoctorRepository doctors;
    private final SpecialityRepository specialities;
    private final ProcedureRepository procedures;
    private final HealthConditionRepository conditions;
    private final HealthPackageRepository packages;

    public List<SearchSuggestionDto> suggest(String term, int limit) {
        if (term == null || term.trim().length() < 2) return List.of();
        String q = term.trim().toLowerCase(Locale.ROOT);
        List<SearchSuggestionDto> out = new ArrayList<>();

        for (Speciality s : specialities.search(q)) {
            if (out.size() >= PER_SOURCE) break;
            out.add(new SearchSuggestionDto("SPECIALITY", String.valueOf(s.getId()), s.getName(),
                    s.getNameMr(), "/specialities/" + s.getSlug()));
        }

        int doctorCount = 0;
        for (Doctor d : doctors.findByActiveTrueOrderBySortOrderAscNameAsc()) {
            if (doctorCount >= PER_SOURCE) break;
            String hay = (d.getName() + " " + nullSafe(d.getDesignation()) + " " + nullSafe(d.getQualifications())
                    + " " + (d.getSpeciality() == null ? "" : d.getSpeciality().getName())).toLowerCase(Locale.ROOT);
            if (hay.contains(q)) {
                doctorCount++;
                out.add(new SearchSuggestionDto("DOCTOR", String.valueOf(d.getId()), d.getName(),
                        (d.getSpeciality() == null ? "" : d.getSpeciality().getName())
                                + (d.getFacility() == null ? "" : " · " + d.getFacility().getCity()),
                        "/doctors/" + d.getSlug()));
            }
        }

        int procCount = 0;
        for (Procedure p : procedures.findByActiveTrueOrderBySortOrderAsc()) {
            if (procCount >= PER_SOURCE) break;
            if (matches(p.getName(), p.getNameMr(), p.getSummary(), q)) {
                procCount++;
                out.add(new SearchSuggestionDto("PROCEDURE", String.valueOf(p.getId()), p.getName(),
                        p.getSpeciality() == null ? "Procedure" : p.getSpeciality().getName(),
                        "/procedures/" + p.getSlug()));
            }
        }

        int condCount = 0;
        for (HealthCondition c : conditions.findByActiveTrueOrderBySortOrderAsc()) {
            if (condCount >= PER_SOURCE) break;
            if (matches(c.getName(), c.getNameMr(), null, q)) {
                condCount++;
                out.add(new SearchSuggestionDto("CONDITION", String.valueOf(c.getId()), c.getName(),
                        c.getNameMr(), "/conditions/" + c.getSlug()));
            }
        }

        int pkgCount = 0;
        for (HealthPackage p : packages.findByActiveTrueOrderBySortOrderAsc()) {
            if (pkgCount >= PER_SOURCE) break;
            if (matches(p.getName(), p.getNameMr(), p.getTagline(), q)) {
                pkgCount++;
                out.add(new SearchSuggestionDto("PACKAGE", String.valueOf(p.getId()), p.getName(),
                        "Health check-up", "/health-packages/" + p.getSlug()));
            }
        }

        out.sort(Comparator.comparing(SearchSuggestionDto::type));
        return out.size() > limit ? out.subList(0, limit) : out;
    }

    private boolean matches(String... fields) {
        String q = fields[fields.length - 1];
        for (int i = 0; i < fields.length - 1; i++) {
            if (fields[i] != null && fields[i].toLowerCase(Locale.ROOT).contains(q)) return true;
        }
        return false;
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
