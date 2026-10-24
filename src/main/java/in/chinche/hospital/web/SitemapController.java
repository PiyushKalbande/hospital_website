package in.chinche.hospital.web;

import in.chinche.hospital.domain.*;
import in.chinche.hospital.service.CatalogService;
import in.chinche.hospital.service.ContentService;
import in.chinche.hospital.service.DoctorService;
import in.chinche.hospital.service.FacilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class SitemapController {

    private final CatalogService catalogue;
    private final DoctorService doctorService;
    private final FacilityService facilityService;
    private final ContentService contentService;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        List<String> paths = new ArrayList<>(List.of(
                "/", "/about", "/contact", "/doctors", "/procedures", "/conditions",
                "/specialities", "/health-packages", "/lab-tests", "/locations", "/blogs",
                "/announcements", "/patient-stories", "/insurance", "/careers", "/emergency",
                "/blood-bank", "/search", "/book"));

        catalogue.allSpecialities().forEach(item -> paths.add("/specialities/" + item.getSlug()));
        doctorService.all().forEach(item -> paths.add("/doctors/" + item.getSlug()));
        catalogue.allProcedures().forEach(item -> paths.add("/procedures/" + item.getSlug()));
        catalogue.allConditions().forEach(item -> paths.add("/conditions/" + item.getSlug()));
        catalogue.allPackages().forEach(item -> paths.add("/health-packages/" + item.getSlug()));
        facilityService.all().forEach(item -> paths.add("/locations/" + item.getSlug()));
        contentService.blogs(0, 50, null).getContent()
                .forEach(item -> paths.add("/blogs/" + item.getSlug()));
        contentService.announcements(0, 50).getContent()
                .forEach(item -> paths.add("/announcements/" + item.getSlug()));

        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
        paths.stream().distinct().forEach(path -> xml.append("<url><loc>")
                .append(escapeXml(baseUrl + path))
                .append("</loc></url>"));
        return xml.append("</urlset>").toString();
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}