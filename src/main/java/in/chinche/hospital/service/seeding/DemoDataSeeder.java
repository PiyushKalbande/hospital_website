package in.chinche.hospital.service.seeding;

import in.chinche.hospital.domain.*;
import in.chinche.hospital.domain.enums.*;
import in.chinche.hospital.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads a realistic demo catalogue the first time the application runs against an
 * empty database. Idempotent: it checks for existing rows and no-ops afterwards, so
 * it is safe on both H2 (dev) and MySQL (prod) — in production you would simply set
 * {@code chinche.seeding.enabled=false} and manage content through the admin console.
 *
 * <p>All copy below is written for चींचे हॉस्पिटल, कटोल and is bilingual (English / मराठी).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataSeeder {

    private final FacilityRepository facilities;
    private final SpecialityRepository specialities;
    private final DoctorRepository doctors;
    private final DoctorScheduleRepository schedules;
    private final ProcedureRepository procedures;
    private final HealthConditionRepository conditions;
    private final HealthPackageRepository packages;
    private final LabTestRepository labTests;
    private final BlogRepository blogs;
    private final AnnouncementRepository announcements;
    private final SuccessStoryRepository stories;
    private final TestimonialRepository testimonials;
    private final AwardRepository awards;
    private final HeroSlideRepository heroSlides;
    private final QuickLinkRepository quickLinks;
    private final AppUserRepository users;

    @Transactional
    public void seedIfEmpty(PasswordEncoder encoder) {
        if (specialities.count() > 0) {
            log.info("Database already populated — skipping demo seeding.");
            ensureAdminUser(encoder);
            return;
        }
        log.info("Empty database detected — seeding the Chinche Hospital demo catalogue…");

        List<Facility> fs = seedFacilities();
        List<Speciality> sps = seedSpecialities();
        seedProcedures(sps);
        seedConditions(sps);
        List<Doctor> ds = seedDoctors(fs, sps);
        seedSchedules(ds, fs);
        seedPackages();
        seedLabTests();
        seedHeroSlides();
        seedQuickLinks();
        seedBlogs();
        seedAnnouncements();
        seedStories(ds, sps);
        seedTestimonials(sps);
        seedAwards();
        ensureAdminUser(encoder);

        log.info("Demo catalogue ready: {} facilities, {} specialities, {} doctors, {} packages.",
                fs.size(), sps.size(), ds.size(), packages.count());
    }

    // ------------------------------------------------------------------ facilities

    private List<Facility> seedFacilities() {
        List<Facility> list = new ArrayList<>();
        list.add(facilities.save(Facility.builder()
                .name("Chinche Hospital — Katol").nameMr("चींचे हॉस्पिटल — कटोल")
                .slug("chinche-hospital-katol")
                .facilityType(FacilityType.MAIN_HOSPITAL)
                .city("Katol").district("Nagpur").state("Maharashtra").pincode("441302")
                .addressLine("मुळाई रोड, बस स्थानक चौक, कटोल, जि. नागपूर — 441302")
                .phone("+91 712 240 1100").emergencyPhone("+91 94221 00108")
                .email("care@chinchehospital.in")
                .latitude(21.2705).longitude(78.5839)
                .rating(4.7).reviewCount(1284).bedCount(120)
                .amenities("icu,ot,emergency,pharmacy,diagnostics,ambulance,maternity,cashless")
                .hoursText("24×7 emergency · OPD 8:00 AM – 8:00 PM")
                .headquarter(true).sortOrder(1).active(true).build()));

        list.add(facilities.save(Facility.builder()
                .name("Chinche Clinic — Narkhed").nameMr("चींचे क्लिनिक — नरखेड")
                .slug("chinche-clinic-narkhed")
                .facilityType(FacilityType.CLINIC)
                .city("Narkhed").district("Nagpur").state("Maharashtra").pincode("441304")
                .addressLine("मुख्य बाजार चौक, नरखेड, जि. नागपूर — 441304")
                .phone("+91 712 255 3300").emergencyPhone("+91 94221 00108")
                .latitude(21.4683).longitude(78.5985).rating(4.5).reviewCount(412).bedCount(12)
                .amenities("opd,pharmacy,diagnostics,ambulance")
                .hoursText("OPD 9:00 AM – 2:00 PM, 5:00 PM – 9:00 PM")
                .headquarter(false).sortOrder(2).active(true).build()));

        list.add(facilities.save(Facility.builder()
                .name("Chinche Clinic — Saoner").nameMr("चींचे क्लिनिक — सावनेर")
                .slug("chinche-clinic-saoner")
                .facilityType(FacilityType.CLINIC)
                .city("Saoner").district("Nagpur").state("Maharashtra").pincode("441101")
                .addressLine("नागपूर रोड, सावनेर, जि. नागपूर — 441101")
                .phone("+91 712 266 4400").emergencyPhone("+91 94221 00108")
                .latitude(21.3850).longitude(78.9200).rating(4.4).reviewCount(356).bedCount(10)
                .amenities("opd,pharmacy,diagnostics")
                .hoursText("OPD 9:00 AM – 1:00 PM, 4:00 PM – 8:00 PM")
                .headquarter(false).sortOrder(3).active(true).build()));

        list.add(facilities.save(Facility.builder()
                .name("Chinche Diagnostics — Katol").nameMr("चींचे डायग्नोस्टिक्स — कटोल")
                .slug("chinche-diagnostics-katol")
                .facilityType(FacilityType.DIAGNOSTIC_CENTRE)
                .city("Katol").district("Nagpur").state("Maharashtra").pincode("441302")
                .addressLine("स्टेशन रोड, कटोल, जि. नागपूर — 441302")
                .phone("+91 712 240 1122").emergencyPhone("+91 94221 00108")
                .latitude(21.2740).longitude(78.5860).rating(4.6).reviewCount(698).bedCount(0)
                .amenities("diagnostics,home_collection,pharmacy")
                .hoursText("Sample collection 6:30 AM – 8:00 PM")
                .headquarter(false).sortOrder(4).active(true).build()));

        return list;
    }

    // ------------------------------------------------------------------ specialities

    private List<Speciality> seedSpecialities() {
        List<Speciality> list = new ArrayList<>();
        list.add(sp(name("General Medicine", "सर्वसाधारण वैद्यक", "general-medicine", "stethoscope", "#0E7C66",
                "Everyday illness, fever, infections, chronic disease follow-up and complete adult health care.",
                1, true)));
        list.add(sp(name("Cardiology", "हृदयरोग", "cardiology", "heart", "#D64550",
                "Chest pain, hypertension, ECG, echocardiography, angiography and post-operative cardiac care.",
                2, true)));
        list.add(sp(name("Orthopaedics & Joint Replacement", "अस्थिरोग व सांधे बदली", "orthopaedics", "bone", "#2F6FB0",
                "Fracture care, arthritis, sports injuries, spine problems and knee or hip replacement surgery.",
                3, true)));
        list.add(sp(name("Obstetrics & Gynaecology", "स्त्रीरोग व प्रसूती", "obstetrics-gynaecology", "mother-child", "#C2569B",
                "Pregnancy care, painless delivery, laparoscopic surgery and women's health at every age.",
                4, true)));
        list.add(sp(name("Paediatrics & Neonatology", "बालरोग व नवजात", "paediatrics", "baby", "#E08A2E",
                "Newborn intensive care, vaccination, growth tracking and childhood illness management.",
                5, true)));
        list.add(sp(name("General & Laparoscopic Surgery", "सामान्य व लॅपरोस्कोपिक शस्त्रक्रिया", "general-surgery", "scalpel", "#4A7BB5",
                "Hernia, appendix, gallbladder, piles and fissure treatment — mostly by keyhole surgery.",
                6, true)));
        list.add(sp(name("Neurology", "मेंदुरोग", "neurology", "brain", "#7A5CB8",
                "Stroke, epilepsy, migraine, paralysis and nerve-related disorders with EEG and EMG support.",
                7, true)));
        list.add(sp(name("Nephrology & Dialysis", "मूत्रपिंड व डायलिसिस", "nephrology", "kidney", "#2E9AA8",
                "Kidney disease, dialysis care, stone management and transplant work-up.",
                8, false)));
        list.add(sp(name("Diabetology & Endocrinology", "मधुमेह व अंतर्ग्रंथी", "diabetology", "droplet", "#3F8F5B",
                "Diabetes control, thyroid, hormone disorders and lifestyle counselling for Vidarbha families.",
                9, true)));
        list.add(sp(name("ENT & Head–Neck", "कान-नाक-घसा", "ent", "ear", "#B5713A",
                "Hearing loss, sinusitis, tonsils, vertigo and endoscopic sinus surgery.",
                10, false)));
        list.add(sp(name("Ophthalmology", "नेत्ररोग", "ophthalmology", "eye", "#3E7CB1",
                "Cataract surgery, glaucoma, diabetic retina care, spectacles and squint correction.",
                11, false)));
        list.add(sp(name("Dermatology", "त्वचारोग", "dermatology", "skin", "#A8638F",
                "Acne, allergy, psoriasis, hair loss, laser treatment and cosmetic dermatology.",
                12, false)));
        list.add(sp(name("Pulmonology", "फुप्फुसरोग", "pulmonology", "lungs", "#4C8C7A",
                "Asthma, COPD, tuberculosis follow-up, sleep studies and pulmonary function testing.",
                13, false)));
        list.add(sp(name("Urology", "मूत्रविकार", "urology", "uro", "#5A77A8",
                "Kidney stones, prostate, urinary infection and urodynamic evaluation.",
                14, false)));
        list.add(sp(name("Psychiatry & Counselling", "मानसोपचार व समुपदेशन", "psychiatry", "mind", "#6E7BB3",
                "Anxiety, depression, de-addiction, adolescent counselling and family therapy.",
                15, false)));
        list.add(sp(name("Dentistry", "दंतरोग", "dentistry", "tooth", "#4E9C8D",
                "Cleaning, fillings, root canal, crowns, braces and paediatric dental care.",
                16, false)));
        list.add(sp(name("Physiotherapy & Rehabilitation", "फिजिओथेरपी व पुनर्वसन", "physiotherapy", "rehab", "#7B9A4E",
                "Post-surgery recovery, back pain, stroke rehab, sports injury and home physiotherapy.",
                17, false)));
        list.add(sp(name("Emergency & Trauma Care", "आपत्कालीन व अपघात कक्ष", "emergency-trauma", "ambulance", "#C0392B",
                "Round-the-clock trauma, cardiac and stroke response with a fully equipped ambulance.",
                18, true)));

        return specialities.saveAll(list);
    }

    private Speciality sp(String[] f) {
        return Speciality.builder()
                .name(f[0]).nameMr(f[1]).slug(f[2]).iconKey(f[3]).accentColor(f[4])
                .summary(f[5]).sortOrder(Integer.parseInt(f[6])).featured(Boolean.parseBoolean(f[7]))
                .active(true).build();
    }

    private String[] name(String en, String mr, String slug, String icon, String color, String summary,
                          int order, boolean featured) {
        return new String[]{en, mr, slug, icon, color, summary, String.valueOf(order), String.valueOf(featured)};
    }

    // ------------------------------------------------------------------ procedures

    private void seedProcedures(List<Speciality> sps) {
        record P(String sp, String en, String mr, String slug, String icon, String summary, int cost, int stay, int order) {}
        List<P> rows = List.of(
                new P("cardiology", "Coronary Angioplasty (PTCA)", "कोरोनरी अँजिओप्लास्टी", "coronary-angioplasty", "heart",
                        "Stent placement to open a blocked heart artery, performed in a cath lab with same-week discharge.", 95000, 3, 1),
                new P("cardiology", "2D Echocardiography & TMT", "इकोकार्डिओग्राफी व टी.एम.टी.", "echocardiography-tmt", "heart-monitor",
                        "Non-invasive heart imaging and treadmill stress testing to assess pumping function.", 2500, 0, 2),
                new P("orthopaedics", "Total Knee Replacement", "संपूर्ण सांधे बदली", "total-knee-replacement", "knee",
                        "Cemented and cement-free knee implants with next-day mobilisation and physiotherapy support.", 185000, 4, 3),
                new P("orthopaedics", "Hip Replacement", "हिप सांधे बदली", "hip-replacement", "bone",
                        "Primary and revision hip replacement for advanced arthritis and femoral neck fractures.", 210000, 5, 4),
                new P("orthopaedics", "Arthroscopic Ligament Repair", "आर्थ्रोस्कोपिक लिगामेंट दुरुस्ती", "arthroscopic-ligament-repair", "knee",
                        "Keyhole ACL reconstruction for sports injuries using hamstring or quadriceps grafts.", 78000, 2, 5),
                new P("general-surgery", "Laparoscopic Hernia Repair", "लॅपरोस्कोपिक हर्निया शस्त्रक्रिया", "laparoscopic-hernia-repair", "scalpel",
                        "Mesh-based keyhole repair for inguinal, umbilical and incisional hernia with early return to work.", 62000, 2, 6),
                new P("general-surgery", "Laparoscopic Gallbladder Removal", "लॅपरोस्कोपिक पित्ताशय शस्त्रक्रिया", "laparoscopic-cholecystectomy", "scalpel",
                        "Single-day keyhole surgery for gallstones using three small incisions.", 55000, 1, 7),
                new P("obstetrics-gynaecology", "Painless Normal Delivery Programme", "वेदनामुक्त प्रसूती कार्यक्रम", "painless-delivery", "mother-child",
                        "Epidural-supported vaginal birth with a dedicated labour room and neonatal resuscitation team.", 0, 2, 8),
                new P("obstetrics-gynaecology", "Laparoscopic Hysterectomy", "लॅपरोस्कोपिक हिस्टेरेक्टोमी", "laparoscopic-hysterectomy", "scalpel",
                        "Keyhole uterus removal for fibroids and abnormal bleeding, with a one-night stay.", 88000, 2, 9),
                new P("nephrology", "Haemodialysis (per session)", "रक्तशुद्धी (प्रति सत्र)", "haemodialysis", "kidney",
                        "Machine-assisted blood purification on imported dialysers with a dedicated nursing team.", 1800, 0, 10),
                new P("urology", "Endoscopic Kidney Stone Removal", "एन्डोस्कोपिक मुतखडा काढणे", "endoscopic-kidney-stone", "uro",
                        "RIRS and PCNL stone clearance without open surgery, usually discharged within two days.", 48000, 2, 11),
                new P("neurology", "Stroke Care & Thrombolysis", "स्ट्रोक उपचार व थ्रोम्बोलायसिस", "stroke-care", "brain",
                        "Time-critical clot-busting therapy and structured post-stroke rehabilitation.", 35000, 6, 12),
                new P("ophthalmology", "Phaco Cataract Surgery", "फॅको मोतीबिंदू शस्त्रक्रिया", "phaco-cataract", "eye",
                        "Foldable-lens cataract removal under topical anaesthesia — most patients go home the same day.", 22000, 1, 13),
                new P("ent", "Endoscopic Sinus Surgery (FESS)", "एन्डोस्कोपिक सायनस शस्त्रक्रिया", "endoscopic-sinus-surgery", "ear",
                        "Image-guided clearance of chronic sinus blockage with no external scar.", 42000, 2, 14),
                new P("emergency-trauma", "Advanced Trauma Life Support", "अत्याधुनिक अपघात उपचार", "trauma-life-support", "ambulance",
                        "Golden-hour response for road accidents with in-house blood bank tie-up and ICU backup.", 0, 0, 15)
        );

        List<Procedure> out = new ArrayList<>();
        for (P r : rows) {
            Speciality parent = find(sps, r.sp());
            out.add(Procedure.builder()
                    .name(r.en()).nameMr(r.mr()).slug(r.slug()).iconKey(r.icon())
                    .speciality(parent).summary(r.summary())
                    .indicativeCost(r.cost() == 0 ? null : r.cost())
                    .typicalStayDays(r.stay()).sortOrder(r.order()).active(true).build());
        }
        procedures.saveAll(out);
    }

    // ------------------------------------------------------------------ conditions

    private void seedConditions(List<Speciality> sps) {
        record C(String sp, String en, String mr, String slug, String icon, int order) {}
        List<C> rows = List.of(
                new C("diabetology", "Diabetes", "मधुमेह", "diabetes", "droplet", 1),
                new C("cardiology", "High Blood Pressure", "उच्च रक्तदाब", "hypertension", "bp-monitor", 2),
                new C("orthopaedics", "Knee Pain & Arthritis", "गुडघेदुखी व संधिवात", "knee-pain", "knee", 3),
                new C("urology", "Kidney Stones", "मुतखडा", "kidney-stones", "kidney", 4),
                new C("general-surgery", "Hernia", "हर्निया", "hernia", "scalpel", 5),
                new C("pulmonology", "Asthma", "दमा", "asthma", "lungs", 6),
                new C("diabetology", "Thyroid Disorder", "थायरॉईड विकार", "thyroid", "thyroid", 7),
                new C("neurology", "Migraine", "अर्धवट डोकेदुखी", "migraine", "brain", 8),
                new C("gastro", "Gastritis & Acidity", "जठराग व आमपित्त", "gastritis", "stomach", 9),
                new C("dermatology", "Skin Allergy", "त्वचा अ‍ॅलर्जी", "skin-allergy", "skin", 10),
                new C("paediatrics", "Childhood Anaemia", "बालकांतील रक्तक्षय", "childhood-anaemia", "blood", 11),
                new C("obstetrics-gynaecology", "PCOS / Irregular Periods", "पी.सी.ओ.एस. / अनियमित पाळी", "pcos", "mother-child", 12)
        );
        List<HealthCondition> out = new ArrayList<>();
        for (C r : rows) {
            out.add(HealthCondition.builder()
                    .name(r.en()).nameMr(r.mr()).slug(r.slug()).iconKey(r.icon())
                    .speciality(find(sps, r.sp())).sortOrder(r.order()).active(true).build());
        }
        conditions.saveAll(out);
    }

    // ------------------------------------------------------------------ doctors

    private List<Doctor> seedDoctors(List<Facility> fs, List<Speciality> sps) {
        record Dr(String name, String slug, String sp, String desig, String quals, String reg, int exp,
                  String langs, int fee, String bio, String expertise, double rating, int reviews,
                  int facilityIdx, boolean tele, boolean featured, int order) {}

        List<Dr> rows = List.of(
                new Dr("Dr. Anand Chinche", "dr-anand-chinche", "general-medicine",
                        "Founder & Chief Physician", "MBBS, MD (General Medicine)", "MMC-2003-44821", 22,
                        "mr,hi,en", 500,
                        "Founder of Chinche Hospital and a physician serving the Katol taluka for over two decades. Special interest in rural internal medicine, fever clinics and long-term management of lifestyle disease.",
                        "Fever & infections,Hypertension,Diabetes follow-up,Preventive care", 4.9, 612, 0, true, true, 1),
                new Dr("Dr. Sunita Deshmukh", "dr-sunita-deshmukh", "obstetrics-gynaecology",
                        "Senior Consultant", "MBBS, DGO, DNB (Obst. & Gyn.)", "MMC-2008-51137", 17,
                        "mr,hi,en", 700,
                        "Has conducted more than 4,000 deliveries including high-risk pregnancies. Leads the painless-delivery programme and the adolescent health initiative at the Katol campus.",
                        "High-risk pregnancy,Painless delivery,Laparoscopic gynae surgery,PCOS", 4.8, 486, 0, true, true, 2),
                new Dr("Dr. Rajesh Kulkarni", "dr-rajesh-kulkarni", "orthopaedics",
                        "Consultant Orthopaedic & Joint Replacement Surgeon", "MBBS, MS (Ortho), Fellowship in Arthroplasty",
                        "MMC-2006-49205", 19, "mr,hi,en", 800,
                        "Joint replacement and trauma specialist trained at a Nagpur tertiary centre. Performs cemented and cement-free knee and hip replacement with next-day mobilisation protocols.",
                        "Knee replacement,Hip replacement,Trauma & fractures,Arthroscopy", 4.8, 401, 0, true, true, 3),
                new Dr("Dr. Priya Waghmare", "dr-priya-waghmare", "paediatrics",
                        "Consultant Paediatrician & Neonatologist", "MBBS, DCH, MD (Paediatrics)", "MMC-2011-58442", 14,
                        "mr,hi,en", 600,
                        "Cares for newborns and children up to 15 years, including NICU management of premature babies. Runs the hospital's immunisation and growth-tracking clinic every Saturday.",
                        "Newborn & NICU,Vaccination,Growth & nutrition,Asthma in children", 4.9, 377, 0, true, true, 4),
                new Dr("Dr. Vikram Patil", "dr-vikram-patil", "cardiology",
                        "Interventional Cardiologist", "MBBS, MD (Medicine), DM (Cardiology)", "MMC-2009-53881", 16,
                        "mr,hi,en", 1000,
                        "Interventional cardiologist performing angiography and angioplasty. Visits Katol three days a week and manages the chest-pain fast-track pathway with the emergency team.",
                        "Angioplasty,Angiography,Heart failure,Hypertension", 4.7, 288, 0, true, true, 5),
                new Dr("Dr. Meena Joshi", "dr-meena-joshi", "diabetology",
                        "Diabetologist & Endocrinologist", "MBBS, MD (Medicine), Fellowship in Diabetes", "MMC-2012-60124", 13,
                        "mr,hi,en", 600,
                        "Focused on making diabetes manageable in small-town India: simplified insulin schedules, Marathi diet counselling and family-based lifestyle programmes.",
                        "Diabetes,Thyroid,Insulin therapy,Diabetic foot care", 4.8, 254, 0, true, true, 6),
                new Dr("Dr. Sandeep Borkar", "dr-sandeep-borkar", "general-surgery",
                        "Consultant General & Laparoscopic Surgeon", "MBBS, MS (Surgery), FMAS", "MMC-2007-50417", 18,
                        "mr,hi,en", 800,
                        "Performs keyhole surgery for hernia, gallbladder, appendix and piles. Known in the region for single-day laparoscopic procedures and early discharge planning.",
                        "Laparoscopic hernia,Gallbladder surgery,Piles & fissure,Appendectomy", 4.7, 233, 0, false, true, 7),
                new Dr("Dr. Neha Raut", "dr-neha-raut", "dermatology",
                        "Consultant Dermatologist", "MBBS, MD (Dermatology)", "MMC-2014-64803", 11,
                        "mr,hi,en", 500,
                        "Medical and cosmetic dermatology with a special interest in paediatric skin conditions, hair disorders and laser treatment.",
                        "Acne & scars,Hair loss,Psoriasis,Laser & cosmetic care", 4.6, 189, 0, true, false, 8),
                new Dr("Dr. Ganesh Tidke", "dr-ganesh-tidke", "neurology",
                        "Consultant Neurologist", "MBBS, MD (Medicine), DM (Neurology)", "MMC-2010-56019", 15,
                        "mr,hi,en", 900,
                        "Manages stroke, epilepsy and headache disorders. Leads the stroke fast-track pathway and coordinates rehabilitation with the physiotherapy team.",
                        "Stroke,Epilepsy,Migraine,Paralysis & neuropathy", 4.7, 176, 0, true, false, 9),
                new Dr("Dr. Ashwini Kale", "dr-ashwini-kale", "ophthalmology",
                        "Consultant Ophthalmologist", "MBBS, MS (Ophthalmology)", "MMC-2013-62288", 12,
                        "mr,hi,en", 500,
                        "Cataract and diabetic retina specialist. Conducts the monthly free eye-camp at the Narkhed clinic.",
                        "Cataract (phaco),Diabetic retina,Glaucoma,Spectacles & squint", 4.7, 201, 1, false, false, 10),
                new Dr("Dr. Imran Shaikh", "dr-imran-shaikh", "pulmonology",
                        "Consultant Pulmonologist", "MBBS, MD (Pulmonary Medicine)", "MMC-2011-58990", 14,
                        "mr,hi,en", 700,
                        "Treats asthma, COPD and tuberculosis with structured inhaler technique training in Marathi for rural patients.",
                        "Asthma,COPD,Tuberculosis,Sleep apnoea", 4.6, 142, 0, true, false, 11),
                new Dr("Dr. Kavita More", "dr-kavita-more", "ent",
                        "Consultant ENT Surgeon", "MBBS, MS (ENT)", "MMC-2012-60775", 13,
                        "mr,hi,en", 600,
                        "Endoscopic sinus surgery, hearing rehabilitation and paediatric ENT care.",
                        "Sinusitis,Hearing loss,Tonsils & adenoids,Vertigo", 4.6, 131, 2, false, false, 12),
                new Dr("Dr. Prashant Uikey", "dr-prashant-uikey", "urology",
                        "Consultant Urologist", "MBBS, MS, MCh (Urology)", "MMC-2010-55734", 15,
                        "mr,hi,en", 900,
                        "Stone disease and prostate care with endoscopic and laser techniques; avoids open surgery wherever possible.",
                        "Kidney stones (RIRS/PCNL),Prostate,Urinary infection,Urodynamics", 4.7, 118, 0, true, false, 13),
                new Dr("Dr. Swati Nimbalkar", "dr-swati-nimbalkar", "nephrology",
                        "Consultant Nephrologist", "MBBS, MD (Medicine), DM (Nephrology)", "MMC-2013-62910", 12,
                        "mr,hi,en", 800,
                        "Runs the dialysis unit at Katol and manages chronic kidney disease with a diet-first approach.",
                        "Dialysis care,Chronic kidney disease,Kidney transplant work-up,Electrolyte disorders", 4.6, 96, 0, false, false, 14),
                new Dr("Dr. Harshad Vaidya", "dr-harshad-vaidya", "emergency-trauma",
                        "Head — Emergency & Trauma", "MBBS, MD (Emergency Medicine)", "MMC-2012-61040", 13,
                        "mr,hi,en", 0,
                        "Leads the 24×7 emergency department, ambulance response and disaster triage for the Katol–Narkhed belt.",
                        "Trauma & accidents,Cardiac emergency,Stroke fast-track,Poisoning", 4.9, 320, 0, false, true, 15),
                new Dr("Dr. Rupali Sonone", "dr-rupali-sonone", "physiotherapy",
                        "Chief Physiotherapist", "BPT, MPT (Ortho), Certified in Sports Rehab", "MOT-2015-3382", 10,
                        "mr,hi,en", 400,
                        "Post-operative rehabilitation, back pain and stroke recovery, with a home-visit service for elderly patients.",
                        "Post-surgery rehab,Back & neck pain,Stroke recovery,Home physiotherapy", 4.8, 165, 0, true, false, 16),
                new Dr("Dr. Nikhil Zade", "dr-nikhil-zade", "psychiatry",
                        "Consultant Psychiatrist", "MBBS, MD (Psychiatry)", "MMC-2014-64115", 11,
                        "mr,hi,en", 600,
                        "Adult and adolescent mental health, de-addiction support and family counselling in a stigma-free setting.",
                        "Anxiety & depression,Adolescent counselling,De-addiction,Sleep disorders", 4.7, 88, 1, true, false, 17),
                new Dr("Dr. Sneha Barve", "dr-sneha-barve", "dentistry",
                        "Consultant Dental Surgeon", "BDS, MDS (Conservative Dentistry)", "MDC-2015-7741", 10,
                        "mr,hi,en", 300,
                        "Pain-free root canal, cosmetic dentistry and a dedicated children's dental clinic.",
                        "Root canal,Crowns & bridges,Braces,Children's dentistry", 4.6, 74, 2, false, false, 18)
        );

        List<Doctor> out = new ArrayList<>();
        for (Dr r : rows) {
            out.add(doctors.save(Doctor.builder()
                    .name(r.name()).slug(r.slug())
                    .speciality(find(sps, r.sp()))
                    .facility(fs.get(Math.min(r.facilityIdx(), fs.size() - 1)))
                    .designation(r.desig()).qualifications(r.quals()).registrationNo(r.reg())
                    .experienceYears(r.exp()).languages(r.langs())
                    .consultationFee(BigDecimal.valueOf(r.fee()))
                    .bio(r.bio()).areasOfExpertise(r.expertise())
                    .rating(r.rating()).reviewCount(r.reviews())
                    .teleconsultEnabled(r.tele()).featured(r.featured())
                    .sortOrder(r.order()).active(true).build()));
        }
        return out;
    }

    private void seedSchedules(List<Doctor> ds, List<Facility> fs) {
        List<DoctorSchedule> out = new ArrayList<>();
        int idx = 0;
        for (Doctor d : ds) {
            idx++;
            Facility facility = d.getFacility() == null ? fs.get(0) : d.getFacility();
            // Deterministic but varied weekly pattern so the calendar looks real.
            List<DayOfWeek> morningDays = List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
            List<DayOfWeek> eveningDays = List.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY);
            if (idx % 2 == 0) {
                morningDays = List.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY);
                eveningDays = List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
            }
            for (DayOfWeek day : morningDays) {
                out.add(schedule(d, facility, day, LocalTime.of(9, 0), LocalTime.of(13, 0), 20, false));
            }
            for (DayOfWeek day : eveningDays) {
                out.add(schedule(d, facility, day, LocalTime.of(17, 0), LocalTime.of(20, 0), 20, false));
            }
            if (d.isTeleconsultEnabled()) {
                out.add(schedule(d, facility, DayOfWeek.SUNDAY, LocalTime.of(10, 0), LocalTime.of(12, 0), 20, true));
                out.add(schedule(d, facility, DayOfWeek.WEDNESDAY, LocalTime.of(20, 0), LocalTime.of(21, 30), 30, true));
            }
        }
        schedules.saveAll(out);
        log.info("Seeded {} weekly schedule rows.", out.size());
    }

    private DoctorSchedule schedule(Doctor d, Facility f, DayOfWeek day, LocalTime start, LocalTime end,
                                    int minutes, boolean tele) {
        return DoctorSchedule.builder()
                .doctor(d).facility(f).dayOfWeek(day)
                .startTime(start).endTime(end).slotMinutes(minutes)
                .maxPatientsPerSlot(1).teleconsult(tele).active(true).build();
    }

    // ------------------------------------------------------------------ packages

    private void seedPackages() {
        List<HealthPackage> out = new ArrayList<>();
        out.add(HealthPackage.builder()
                .name("Basic Wellness Check").nameMr("मूलभूत आरोग्य तपासणी").slug("basic-wellness-check")
                .tier("BASIC").price(new BigDecimal("899")).listPrice(new BigDecimal("1400"))
                .tagline("A sensible annual starting point").taglineMr("वार्षिक आरोग्य तपासणीची सुरुवात")
                .includes("""
                        Complete Blood Count (CBC)
                        Blood sugar (fasting)
                        Urine routine
                        Blood group & Rh
                        ESR
                        Physician consultation""")
                .testCount(38).description("For healthy adults under 35 who want a baseline reading of the parameters that matter most.")
                .descriptionMr("३५ वर्षांखालील निरोगी प्रौढांसाठी मूलभूत आरोग्य तपासणी.")
                .genderSuitability("all").minAge(18).maxAge(35)
                .fastingRequired(true).reportHours(24).homeCollection(true)
                .iconKey("shield").featured(false).sortOrder(1).active(true).build());

        out.add(HealthPackage.builder()
                .name("Essential Full Body Check").nameMr("अत्यावश्यक संपूर्ण तपासणी").slug("essential-full-body-check")
                .tier("ESSENTIAL").price(new BigDecimal("2499")).listPrice(new BigDecimal("3900"))
                .tagline("Our most-booked package").taglineMr("सर्वाधिक बुक होणारा पॅकेज")
                .includes("""
                        Complete Blood Count (CBC)
                        Fasting & post-meal blood sugar
                        HbA1c
                        Lipid profile (8 parameters)
                        Liver function test
                        Kidney function test
                        Thyroid profile (T3, T4, TSH)
                        Urine routine
                        ECG
                        Chest X-ray
                        Physician consultation""")
                .testCount(64).description("Covers heart, liver, kidney, thyroid and sugar in one visit — the right annual check for most working adults.")
                .descriptionMr("हृदय, यकृत, मूत्रपिंड, थायरॉईड व रक्तशर्करा — एका भेटीत संपूर्ण तपासणी.")
                .genderSuitability("all").minAge(25).maxAge(60)
                .fastingRequired(true).reportHours(24).homeCollection(true)
                .iconKey("clipboard").featured(true).sortOrder(2).active(true).build());

        out.add(HealthPackage.builder()
                .name("Women's Health Complete").nameMr("महिला आरोग्य संपूर्ण").slug("womens-health-complete")
                .tier("ADVANCED").price(new BigDecimal("3499")).listPrice(new BigDecimal("5200"))
                .tagline("Designed with our gynaecologists").taglineMr("स्त्रीरोग तज्ज्ञांच्या मार्गदर्शनाखाली")
                .includes("""
                        Everything in Essential Full Body
                        Iron studies & ferritin
                        Vitamin D & B12
                        Calcium (total & ionic)
                        Pap smear
                        Ultrasound abdomen & pelvis
                        Gynaecologist consultation""")
                .testCount(78).description("Adds anaemia, bone-health and cervical screening parameters that women in rural Maharashtra are most often missing.")
                .descriptionMr("स्त्रियांमधील रक्तक्षय, हाडांचे आरोग्य व सर्व्हायकल कॅन्सर तपासणी.")
                .genderSuitability("female").minAge(20).maxAge(70)
                .fastingRequired(true).reportHours(48).homeCollection(true)
                .iconKey("women").featured(true).sortOrder(3).active(true).build());

        out.add(HealthPackage.builder()
                .name("Senior Citizen Platinum").nameMr("ज्येष्ठ नागरिक प्लॅटिनम").slug("senior-citizen-platinum")
                .tier("PREMIUM").price(new BigDecimal("5999")).listPrice(new BigDecimal("8800"))
                .tagline("For parents above 55").taglineMr("५५ वर्षांवरील पालकांसाठी")
                .includes("""
                        Everything in Essential Full Body
                        2D Echo
                        Treadmill test (if advised)
                        PSA (men) / CA-125 (women)
                        Bone density (DEXA)
                        Ultrasound abdomen
                        Eye check-up
                        Audiometry
                        Physiotherapy assessment
                        Geriatric physician consultation""")
                .testCount(96).description("A full cardiac, bone, hearing and vision review with a geriatric physician explaining every report in Marathi.")
                .descriptionMr("हृदय, हाडे, ऐकणे व दृष्टी यांची संपूर्ण तपासणी; अहवाल मराठीत समजावून सांगितला जातो.")
                .genderSuitability("all").minAge(55).maxAge(100)
                .fastingRequired(true).reportHours(48).homeCollection(true)
                .iconKey("senior").featured(true).sortOrder(4).active(true).build());

        out.add(HealthPackage.builder()
                .name("Diabetes Care Profile").nameMr("मधुमेह तपासणी").slug("diabetes-care-profile")
                .tier("ESSENTIAL").price(new BigDecimal("1799")).listPrice(new BigDecimal("2600"))
                .tagline("Quarterly control review").taglineMr("तिमाही नियंत्रण तपासणी")
                .includes("""
                        Fasting & post-meal sugar
                        HbA1c
                        Lipid profile
                        Kidney function test
                        Urine albumin/creatinine ratio
                        Fundus examination
                        Diabetic foot assessment
                        Diabetologist consultation""")
                .testCount(52).description("Everything needed to know whether your current diabetes treatment is actually working.")
                .descriptionMr("मधुमेहावरील उपचार किती परिणामकारक आहेत याची संपूर्ण तपासणी.")
                .genderSuitability("all").minAge(18).maxAge(100)
                .fastingRequired(true).reportHours(24).homeCollection(true)
                .iconKey("droplet").featured(false).sortOrder(5).active(true).build());

        out.add(HealthPackage.builder()
                .name("Pre-Employment Fitness Check").nameMr("नोकरीपूर्व आरोग्य प्रमाणपत्र").slug("pre-employment-fitness")
                .tier("BASIC").price(new BigDecimal("699")).listPrice(new BigDecimal("1100"))
                .tagline("Report within 24 hours").taglineMr("२४ तासांत अहवाल")
                .includes("""
                        Physical examination
                        CBC & urine routine
                        Blood sugar
                        Chest X-ray
                        Vision & colour-blindness test
                        Fitness certificate""")
                .testCount(24).description("The standard certificate set requested by factories and offices across the Nagpur industrial belt.")
                .descriptionMr("नागपूर परिसरातील कारखाने व कार्यालयांसाठी आवश्यक आरोग्य प्रमाणपत्र.")
                .genderSuitability("all").minAge(18).maxAge(65)
                .fastingRequired(false).reportHours(24).homeCollection(false)
                .iconKey("certificate").featured(false).sortOrder(6).active(true).build());

        packages.saveAll(out);
    }

    // ------------------------------------------------------------------ lab tests

    private void seedLabTests() {
        record L(String en, String mr, String slug, String cat, int price, String sample, int hours,
                 boolean fasting, String params) {}
        List<L> rows = List.of(
                new L("Complete Blood Count", "संपूर्ण रक्त तपासणी", "cbc", "Haematology", 250, "Blood", 6, false, "24 parameters"),
                new L("Fasting Blood Sugar", "उपाशी रक्तशर्करा", "fasting-blood-sugar", "Diabetes", 80, "Blood", 4, true, "Glucose"),
                new L("HbA1c", "एचबीए१सी", "hba1c", "Diabetes", 400, "Blood", 12, false, "Glycated haemoglobin"),
                new L("Lipid Profile", "लिपिड प्रोफाइल", "lipid-profile", "Cardiac", 600, "Blood", 12, true, "8 parameters"),
                new L("Thyroid Profile (T3 T4 TSH)", "थायरॉईड प्रोफाइल", "thyroid-profile", "Hormones", 550, "Blood", 24, false, "T3, T4, TSH"),
                new L("Liver Function Test", "यकृत कार्य तपासणी", "liver-function-test", "Biochemistry", 700, "Blood", 24, true, "11 parameters"),
                new L("Kidney Function Test", "मूत्रपिंड कार्य तपासणी", "kidney-function-test", "Biochemistry", 650, "Blood", 24, false, "Urea, Creatinine, Uric acid, Electrolytes"),
                new L("Urine Routine", "लघवी तपासणी", "urine-routine", "Pathology", 120, "Urine", 4, false, "Physical, chemical, microscopy"),
                new L("Vitamin D (25-OH)", "व्हिटॅमिन डी", "vitamin-d", "Vitamins", 1200, "Blood", 36, false, "25-Hydroxy Vitamin D"),
                new L("Vitamin B12", "व्हिटॅमिन बी१२", "vitamin-b12", "Vitamins", 900, "Blood", 36, false, "Cobalamin"),
                new L("ECG", "इलेक्ट्रोकार्डियोग्राम", "ecg", "Cardiac", 300, "—", 1, false, "12-lead ECG"),
                new L("2D Echocardiography", "इकोकार्डिओग्राफी", "2d-echo", "Cardiac", 1800, "—", 2, false, "Structural & functional study"),
                new L("Ultrasound Whole Abdomen", "संपूर्ण पोटाचा सोनोग्राफी", "usg-whole-abdomen", "Radiology", 1200, "—", 4, true, "Abdomen & pelvis"),
                new L("Chest X-ray (PA view)", "छातीचा एक्स-रे", "chest-xray", "Radiology", 350, "—", 2, false, "Digital PA view"),
                new L("Dengue NS1 & IgM", "डेंग्यू तपासणी", "dengue-test", "Serology", 900, "Blood", 24, false, "NS1 antigen, IgM antibody"),
                new L("Widal / Typhidot", "टाइफॉइड तपासणी", "typhoid-test", "Serology", 450, "Blood", 24, false, "Enteric fever panel"),
                new L("Pregnancy Test (Beta hCG)", "गर्भधारणा तपासणी", "beta-hcg", "Hormones", 500, "Blood", 12, false, "Quantitative Beta hCG"),
                new L("Blood Group & Rh", "रक्तगट तपासणी", "blood-group", "Haematology", 150, "Blood", 4, false, "ABO & Rh typing")
        );
        List<LabTest> out = new ArrayList<>();
        for (L r : rows) {
            out.add(LabTest.builder()
                    .name(r.en()).nameMr(r.mr()).slug(r.slug()).category(r.cat())
                    .price(BigDecimal.valueOf(r.price())).sampleType(r.sample())
                    .reportHours(r.hours()).fastingRequired(r.fasting())
                    .parameters(r.params()).homeCollection(true).active(true).build());
        }
        labTests.saveAll(out);
    }

    // ------------------------------------------------------------------ hero + tiles

    private void seedHeroSlides() {
        heroSlides.saveAll(List.of(
                HeroSlide.builder()
                        .eyebrow("सerving Katol since 1998")
                        .headline("Vidarbha's trusted multi-speciality care, now one tap away")
                        .headlineMr("विदर्भातील विश्वासाचे रुग्णालय, आता एका क्लिकवर")
                        .subheadline("120 beds · 18 specialities · 24×7 emergency · cashless insurance at मुळाई रोड, कटोल")
                        .subheadlineMr("१२० खाटा · १८ विभाग · २४×७ आपत्कालीन सेवा · कटोल येथे कॅशलेस सुविधा")
                        .ctaLabel("Book an appointment").ctaUrl("/book")
                        .secondaryCtaLabel("Find a doctor").secondaryCtaUrl("/doctors")
                        .artKey("hospital-facade").gradient("linear-gradient(120deg,#063B33 0%,#0E7C66 55%,#17A88B 100%)")
                        .sortOrder(1).active(true).build(),
                HeroSlide.builder()
                        .eyebrow("New at Chinche Hospital")
                        .headline("A dedicated 24×7 trauma and cardiac emergency bay")
                        .headlineMr("२४×७ अपघात व हृदयरोग आपत्कालीन कक्ष")
                        .subheadline("Ambulance with a ventilator, an on-call interventional cardiologist and a golden-hour protocol for road accidents on the Katol–Nagpur highway.")
                        .subheadlineMr("व्हेंटिलेटरसह रुग्णवाहिका, २४ तास उपलब्ध हृदयरोग तज्ज्ञ आणि अपघातासाठी गोल्डन-अवर प्रोटोकॉल.")
                        .ctaLabel("Emergency: 108").ctaUrl("tel:108")
                        .secondaryCtaLabel("See emergency services").secondaryCtaUrl("/specialities/emergency-trauma")
                        .artKey("emergency").gradient("linear-gradient(120deg,#7A1F1A 0%,#C0392B 60%,#E4674F 100%)")
                        .sortOrder(2).active(true).build(),
                HeroSlide.builder()
                        .eyebrow("Preventive health")
                        .headline("Full-body check-ups from ₹899, with home sample collection")
                        .headlineMr("₹८९९ पासून संपूर्ण आरोग्य तपासणी, घरी नमुना संकलन")
                        .subheadline("Reports in Marathi and English, explained to you by a physician — not just handed over.")
                        .subheadlineMr("मराठी व इंग्रजी अहवाल, डॉक्टरांकडून सविस्तर स्पष्टीकरण.")
                        .ctaLabel("View health packages").ctaUrl("/health-packages")
                        .secondaryCtaLabel("Book a lab test").secondaryCtaUrl("/lab-tests")
                        .artKey("checkup").gradient("linear-gradient(120deg,#123A5C 0%,#2F6FB0 60%,#5AA0D6 100%)")
                        .sortOrder(3).active(true).build()));
    }

    private void seedQuickLinks() {
        record Q(String en, String mr, String desc, String descMr, String icon, String url, String group, int order) {}
        List<Q> rows = List.of(
                new Q("Book an Appointment", "अपॉइंटमेंट बुक करा", "OPD, video consult or surgery planning",
                        "ओ.पी.डी., व्हिडिओ सल्ला किंवा शस्त्रक्रिया नियोजन", "calendar", "/book", "HERO_SERVICES", 1),
                new Q("Health Check-ups", "आरोग्य तपासणी", "Full-body packages from ₹899",
                        "₹८९९ पासून संपूर्ण तपासणी", "shield", "/health-packages", "HERO_SERVICES", 2),
                new Q("Lab Tests", "प्रयोगशाळा तपासणी", "Home collection, reports in 6–24 hrs",
                        "घरी नमुना संकलन, ६–२४ तासांत अहवाल", "flask", "/lab-tests", "HERO_SERVICES", 3),
                new Q("Video Consultation", "व्हिडिओ सल्ला", "Talk to a doctor from home",
                        "घरबसल्या डॉक्टरांशी सल्ला", "video", "/book?service=TELE_CONSULTATION", "HERO_SERVICES", 4),
                new Q("Second Opinion", "दुसरा अभिप्राय", "Expert review of your reports",
                        "तज्ज्ञांकडून अहवालांचे पुनरावलोकन", "report", "/book?service=SECOND_OPINION", "HERO_SERVICES", 5),
                new Q("Emergency & Ambulance", "आपत्कालीन व रुग्णवाहिका", "24×7 trauma response on 108",
                        "२४×७ आपत्कालीन सेवा, क्रमांक १०८", "ambulance", "/emergency", "HERO_SERVICES", 6),
                new Q("Track Your Booking", "बुकिंग तपासा", "Status using your reference number",
                        "संदर्भ क्रमांकावरून स्थिती", "search-doc", "/track", "PATIENT_CORNER", 1),
                new Q("Cashless Insurance", "कॅशलेस विमा", "TPA helpdesk at the reception",
                        "स्वागत कक्षात टी.पी.ए. मदत केंद्र", "insurance", "/insurance", "PATIENT_CORNER", 2),
                new Q("Download Reports", "अहवाल डाउनलोड करा", "Lab and imaging reports online",
                        "प्रयोगशाळा व इमेजिंग अहवाल ऑनलाइन", "download", "/reports", "PATIENT_CORNER", 3),
                new Q("Blood Bank", "रक्तपेढी", "Donor registration and requests",
                        "दाता नोंदणी व विनंती", "blood", "/blood-bank", "PATIENT_CORNER", 4),
                new Q("Feedback & Grievance", "अभिप्राय व तक्रार", "Patient experience cell",
                        "रुग्ण अनुभव कक्ष", "feedback", "/contact?type=FEEDBACK", "PATIENT_CORNER", 5),
                new Q("Careers", "करियर", "Nursing, technician and admin roles",
                        "परिचारिका, तंत्रज्ञ व प्रशासकीय पदे", "career", "/careers", "PATIENT_CORNER", 6)
        );
        List<QuickLink> out = new ArrayList<>();
        for (Q r : rows) {
            out.add(QuickLink.builder()
                    .label(r.en()).labelMr(r.mr()).description(r.desc()).descriptionMr(r.descMr())
                    .iconKey(r.icon()).linkUrl(r.url()).linkGroup(r.group()).sortOrder(r.order()).active(true).build());
        }
        quickLinks.saveAll(out);
    }

    // ------------------------------------------------------------------ content

    private void seedBlogs() {
        LocalDateTime now = LocalDateTime.now();
        blogs.saveAll(List.of(
                Blog.builder()
                        .title("Monsoon in Vidarbha: six infections our OPD sees most, and how to prevent them")
                        .titleMr("विदर्भातील पाऊस: ओ.पी.डी.मध्ये सर्वाधिक दिसणारे सहा आजार व त्यांचा प्रतिबंध")
                        .slug("monsoon-infections-vidarbha-prevention"
                        ).excerpt("Leptospirosis from waterlogged fields, dengue from stored water, and typhoid from street food — what our physicians in Katol are diagnosing this season, and the practical steps that actually reduce your risk.")
                        .content("""
                                <p>Every year between June and October our outpatient department at Katol sees a predictable shift. Fevers that were mostly viral in summer turn into a mix of water-borne and vector-borne illness, and the patients walking in are increasingly farmers, farm labourers and schoolchildren.</p>
                                <h3>1. Leptospirosis</h3>
                                <p>Exposure to standing water in fields is the single biggest risk factor. If you have a high fever with severe calf pain and red eyes after wading through floodwater, do not wait three days — come in the same day. Early antibiotics change the outcome completely.</p>
                                <h3>2. Dengue</h3>
                                <p>Stored water in coolers, tyres and overhead tanks breeds Aedes mosquitoes. Empty and scrub coolers weekly. If fever crosses five days or you notice bleeding gums, get a platelet count.</p>
                                <h3>3. Typhoid and other enteric fevers</h3>
                                <p>Boil or chlorinate drinking water during heavy rain, when sewage lines overflow. Street food avoided for one season prevents a fortnight of illness.</p>
                                <h3>4. Malaria</h3>
                                <p>Fever with chills on alternate days deserves a rapid antigen test, not just a clinical guess. Our diagnostics centre runs both RDT and smear.</p>
                                <h3>5. Skin and fungal infections</h3>
                                <p>Damp clothing and shared towels drive ringworm and intertrigo. Dry thoroughly, change socks twice daily, and do not self-medicate with steroid creams — they make fungal infection worse.</p>
                                <h3>6. Respiratory flare-ups</h3>
                                <p>Asthma and COPD patients worsen with humidity and mould. Keep inhalers accessible, and review technique at every visit — most poor control is a technique problem, not a dose problem.</p>
                                <p>Prevention is cheaper than treatment, and most of it costs nothing. If someone in your family has a fever above three days, our OPD is open 8 AM to 8 PM and our emergency department never closes.</p>
                                """)
                        .author("Dr. Anand Chinche").category("Seasonal Health")
                        .tags("monsoon,infection,dengue,typhoid,prevention")
                        .readMinutes(6).status(ContentStatus.PUBLISHED)
                        .publishedAt(now.minusDays(3)).featured(true).viewCount(0L).build(),
                Blog.builder()
                        .title("Reading your own blood report: the seven numbers that actually matter")
                        .titleMr("तुमचा रक्त अहवाल स्वतः वाचा: खरोखर महत्त्वाचे सात आकडे")
                        .slug("understanding-your-blood-report")
                        .excerpt("Haemoglobin, HbA1c, creatinine, TSH, LDL, platelets and vitamin D. What each one tells you, what the normal range means for an Indian adult, and when a borderline value is genuinely worth acting on.")
                        .content("""
                                <p>Most patients receive a four-page lab report and understand none of it. You do not need a medical degree to read the seven numbers that carry almost all of the information.</p>
                                <h3>Haemoglobin</h3>
                                <p>Below 12 g/dL in women and 13 g/dL in men is anaemia. In our region the most common cause is iron deficiency, often alongside B12 deficiency in vegetarian diets. Both are correctable, and both cause fatigue that people wrongly accept as normal.</p>
                                <h3>HbA1c</h3>
                                <p>Your average blood sugar over three months. Below 5.7% is normal, 5.7–6.4% is pre-diabetes, 6.5% and above is diabetes. Pre-diabetes is reversible with weight loss and activity — this is the single most valuable number to catch early.</p>
                                <h3>Creatinine</h3>
                                <p>Reflects kidney filtering. Combined with age and sex it gives eGFR. A rising creatinine over successive tests matters far more than one isolated reading.</p>
                                <h3>TSH</h3>
                                <p>Thyroid function. Above 4.5 suggests underactive thyroid — a common, easily treated cause of weight gain, hair fall and low energy, particularly in women.</p>
                                <h3>LDL cholesterol</h3>
                                <p>The target depends on your risk. If you have diabetes or known heart disease, below 70 mg/dL. Otherwise below 100. Total cholesterol alone is misleading.</p>
                                <h3>Platelets</h3>
                                <p>Normal is roughly 1.5–4.0 lakh. A sudden drop with fever needs urgent evaluation.</p>
                                <h3>Vitamin D</h3>
                                <p>Widespread deficiency even in a sunny region, because most of us work indoors. Below 20 ng/mL needs supplementation, not just advice about sunlight.</p>
                                <p>Bring your report to any Chinche Hospital OPD and a physician will walk you through it — that conversation is included in the consultation fee, never billed separately.</p>
                                """)
                        .author("Dr. Meena Joshi").category("Understanding Reports")
                        .tags("blood report,haemoglobin,hba1c,cholesterol,thyroid")
                        .readMinutes(7).status(ContentStatus.PUBLISHED)
                        .publishedAt(now.minusDays(11)).featured(false).viewCount(0L).build(),
                Blog.builder()
                        .title("Knee replacement in a small town: what actually determines a good outcome")
                        .titleMr("लहान शहरातील सांधे बदली: यशस्वी निकाल कशावर अवलंबून असतो")
                        .slug("knee-replacement-outcomes-small-town")
                        .excerpt("Implant brand matters less than the rehabilitation plan. What our orthopaedic team does before, during and after surgery — and the honest recovery timeline we give patients.")
                        .content("""
                                <p>Patients usually ask about the implant. In practice, the implant is the least interesting part of a knee replacement. Outcomes are decided by three things: pre-operative strength, surgical technique, and whether rehabilitation actually happens.</p>
                                <h3>Before surgery</h3>
                                <p>Six weeks of quadriceps strengthening, haemoglobin corrected above 11, blood sugar under control, and any dental or urinary infection treated. Operating on an anaemic, uncontrolled diabetic patient is how infections start.</p>
                                <h3>During surgery</h3>
                                <p>Meticulous soft-tissue balance matters more than the brand on the box. We use laminar-flow operating theatres, antibiotic-loaded cement where indicated, and tranexamic acid to reduce transfusion need.</p>
                                <h3>After surgery — the part patients skip</h3>
                                <p>Standing on day one, walking with support by day two, a structured twelve-week physiotherapy programme, and full knee bend by three months. Patients who complete physiotherapy score dramatically better at one year than those who do not, regardless of implant.</p>
                                <h3>The honest timeline</h3>
                                <p>Walking independently in three to four weeks, stairs comfortably by six weeks, driving at eight to ten weeks, and a full return to farming or shop work by twelve weeks. Anyone promising faster is not telling you the whole truth.</p>
                                <p>Our physiotherapy team also visits homes within a 25 km radius of Katol for patients who cannot travel during the first fortnight.</p>
                                """)
                        .author("Dr. Rajesh Kulkarni").category("Surgery")
                        .tags("knee replacement,orthopaedics,physiotherapy,recovery")
                        .readMinutes(6).status(ContentStatus.PUBLISHED)
                        .publishedAt(now.minusDays(24)).featured(false).viewCount(0L).build(),
                Blog.builder()
                        .title("Pregnancy in Katol: an antenatal checklist we give every expecting mother")
                        .titleMr("कटोलमधील गरोदरपण: प्रत्येक मातेसाठी आमची अँटेनटल यादी")
                        .slug("antenatal-care-checklist-katol")
                        .excerpt("Visits, scans, supplements and warning signs — a plain-language schedule from confirmation to delivery, plus when to call us at 2 AM.")
                        .content("""
                                <p>Good pregnancy care is mostly schedule and attention. Here is the checklist we hand to every mother registering at Chinche Hospital.</p>
                                <h3>Visits</h3>
                                <p>Monthly until 28 weeks, fortnightly until 36 weeks, then weekly. Fewer visits than this is the most common gap we see in transferred cases.</p>
                                <h3>Scans and tests</h3>
                                <p>Dating scan at 7–9 weeks, NT scan at 11–13 weeks, anomaly scan at 18–22 weeks, growth scans in the third trimester. Blood group, haemoglobin, sugar (GTT at 24–28 weeks), thyroid, HIV, HBsAg and VDRL at booking.</p>
                                <h3>Supplements</h3>
                                <p>Folic acid before conception and through the first trimester, iron and calcium from the second trimester, vitamin D where deficient. Take iron with lemon juice, not with tea or milk.</p>
                                <h3>Call us immediately if</h3>
                                <p>Bleeding or leaking fluid, reduced fetal movement, severe headache with blurred vision, swelling of face and hands, fever above 100°F, or contractions before 37 weeks. Our emergency line is answered at night — that is what it is for.</p>
                                """)
                        .author("Dr. Sunita Deshmukh").category("Mother & Child")
                        .tags("pregnancy,antenatal,delivery,gynaecology")
                        .readMinutes(5).status(ContentStatus.PUBLISHED)
                        .publishedAt(now.minusDays(38)).featured(false).viewCount(0L).build()));
    }

    private void seedAnnouncements() {
        LocalDateTime now = LocalDateTime.now();
        announcements.saveAll(List.of(
                Announcement.builder()
                        .title("New 24×7 dialysis unit now operational at the Katol campus")
                        .titleMr("कटोल येथे २४×७ डायलिसिस कक्ष सुरू")
                        .slug("new-dialysis-unit-katol")
                        .summary("Eight stations with double-pass reverse osmosis water treatment, so patients from Katol, Narkhed and Saoner no longer travel to Nagpur thrice a week.")
                        .badge("NEW FACILITY")
                        .body("""
                                <p>The dialysis unit opens with eight stations, each with an individual monitor, dedicated nursing staff and double-pass RO water treatment. Hepatitis-B positive patients are handled in a physically separated bay.</p>
                                <p>Registered patients can book a recurring weekly slot through the reception desk or online. Under Mahatma Jyotiba Phule Jan Arogya Yojana, eligible patients are treated cashless.</p>
                                """)
                        .status(ContentStatus.PUBLISHED).publishedAt(now.minusDays(4)).pinned(true).active(true).build(),
                Announcement.builder()
                        .title("Free diabetes and blood-pressure camp — first Sunday of every month")
                        .titleMr("मोफत मधुमेह व रक्तदाब तपासणी शिबिर — दर महिन्याच्या पहिल्या रविवारी")
                        .slug("free-diabetes-bp-camp-monthly")
                        .summary("Screening, physician consultation and medication review at no cost, at the Katol OPD between 9 AM and 1 PM. Registration is not required.")
                        .badge("FREE CAMP")
                        .body("""
                                <p>Every first Sunday our diabetology and general medicine teams run a free screening camp: sugar, blood pressure, BMI, foot examination and a physician review of current medication.</p>
                                <p>Bring your existing prescriptions and last two reports if you have them. Around 250 people are seen each month, so please arrive early.</p>
                                """)
                        .status(ContentStatus.PUBLISHED).publishedAt(now.minusDays(16)).pinned(false).active(true).build(),
                Announcement.builder()
                        .title("Digital X-ray and colour Doppler now available at the Narkhed clinic")
                        .titleMr("नरखेड क्लिनिकमध्ये डिजिटल एक्स-रे व कलर डॉप्लर सुविधा")
                        .slug("digital-xray-doppler-narkhed")
                        .summary("Radiology capacity moves closer to villages north of Katol; reports are delivered digitally the same day.")
                        .badge("EXPANSION")
                        .body("""
                                <p>Narkhed clinic now houses a digital radiography system and a colour Doppler ultrasound, operated by visiting radiologists three days a week. Reports are available digitally on the same day and can be shared directly with your treating physician.</p>
                                """)
                        .status(ContentStatus.PUBLISHED).publishedAt(now.minusDays(41)).pinned(false).active(true).build(),
                Announcement.builder()
                        .title("Video consultation now available in Marathi with 14 specialists")
                        .titleMr("१४ तज्ज्ञांसोबत मराठीत व्हिडिओ सल्ला उपलब्ध")
                        .slug("video-consultation-marathi-launch")
                        .summary("Book a Sunday video slot from the website and receive a secure link by SMS fifteen minutes before your appointment.")
                        .badge("DIGITAL CARE")
                        .body("""
                                <p>Patients who cannot travel can now consult fourteen of our specialists by video, including our cardiologist, diabetologist, dermatologist and psychiatrist. Slots run on Sunday mornings and Wednesday evenings.</p>
                                <p>A secure link is sent by SMS and email fifteen minutes before your slot. Keep your reports photographed on your phone — you can share them during the call.</p>
                                """)
                        .status(ContentStatus.PUBLISHED).publishedAt(now.minusDays(58)).pinned(false).active(true).build()));
    }

    private void seedStories(List<Doctor> ds, List<Speciality> sps) {
        LocalDateTime now = LocalDateTime.now();
        stories.saveAll(List.of(
                SuccessStory.builder()
                        .title("A farmer from Narkhed walks again eleven weeks after a hip fracture")
                        .slug("hip-fracture-farmer-narkhed-recovery")
                        .summary("A 71-year-old farmer sustained a femoral neck fracture after a fall in his field. Surgery within 24 hours of arrival, followed by structured home physiotherapy, returned him to walking without support in eleven weeks.")
                        .patientLabel("71-year-old male").patientCity("Narkhed, Nagpur")
                        .doctor(findDoctor(ds, "dr-rajesh-kulkarni")).speciality(find(sps, "orthopaedics"))
                        .publishedAt(now.minusDays(20)).featured(true).active(true).build(),
                SuccessStory.builder()
                        .title("Twins delivered safely at 32 weeks in our NICU")
                        .slug("twins-delivered-32-weeks-nicu")
                        .summary("A twin pregnancy with pre-eclampsia was managed through antenatal steroids, caesarean delivery at 32 weeks and 26 days of neonatal intensive care. Both babies were discharged healthy and gaining weight.")
                        .patientLabel("Mother, 29 years").patientCity("Katol, Nagpur")
                        .doctor(findDoctor(ds, "dr-sunita-deshmukh")).speciality(find(sps, "obstetrics-gynaecology"))
                        .publishedAt(now.minusDays(52)).featured(true).active(true).build(),
                SuccessStory.builder()
                        .title("Chest pain dismissed as acidity turned out to be a 95% blockage")
                        .slug("chest-pain-acidity-95-percent-blockage")
                        .summary("A 54-year-old shopkeeper had treated his chest discomfort as gas for three weeks. An ECG at our OPD triggered the chest-pain fast-track; angioplasty the same evening restored flow to a 95% blocked artery.")
                        .patientLabel("54-year-old male").patientCity("Saoner, Nagpur")
                        .doctor(findDoctor(ds, "dr-vikram-patil")).speciality(find(sps, "cardiology"))
                        .publishedAt(now.minusDays(74)).featured(true).active(true).build(),
                SuccessStory.builder()
                        .title("Speech returned after a stroke caught within the golden hour")
                        .slug("stroke-golden-hour-recovery")
                        .summary("A 63-year-old teacher arrived 74 minutes after symptom onset with right-sided weakness and speech loss. Thrombolysis within the window, plus twelve weeks of rehabilitation, restored independent speech and walking.")
                        .patientLabel("63-year-old female").patientCity("Katol, Nagpur")
                        .doctor(findDoctor(ds, "dr-ganesh-tidke")).speciality(find(sps, "neurology"))
                        .publishedAt(now.minusDays(96)).featured(true).active(true).build(),
                SuccessStory.builder()
                        .title("A 4 kg kidney stone burden cleared without open surgery")
                        .slug("large-kidney-stone-burden-pcnl")
                        .summary("A 45-year-old with recurrent stones and a large staghorn calculus was treated with percutaneous nephrolithotomy in two sittings, avoiding open surgery and preserving kidney function.")
                        .patientLabel("45-year-old male").patientCity("Mowad, Nagpur")
                        .doctor(findDoctor(ds, "dr-prashant-uikey")).speciality(find(sps, "urology"))
                        .publishedAt(now.minusDays(118)).featured(false).active(true).build(),
                SuccessStory.builder()
                        .title("HbA1c from 11.2% to 6.8% in eight months, without insulin")
                        .slug("diabetes-reversal-eight-months")
                        .summary("A 48-year-old with poorly controlled type 2 diabetes was managed through structured diet counselling in Marathi, activity planning and medication adjustment — reaching 6.8% HbA1c in eight months.")
                        .patientLabel("48-year-old male").patientCity("Katol, Nagpur")
                        .doctor(findDoctor(ds, "dr-meena-joshi")).speciality(find(sps, "diabetology"))
                        .publishedAt(now.minusDays(140)).featured(false).active(true).build()));
    }

    private void seedTestimonials(List<Speciality> sps) {
        testimonials.saveAll(List.of(
                Testimonial.builder().patientName("सौ. मंगल राऊत").city("कटोल").relation("OPD patient")
                        .rating(5).quote("डॉक्टरांनी पूर्ण अहवाल मराठीत समजावून सांगितला. पहिल्यांदाच मला माझ्या तपासणीचा अर्थ कळला.")
                        .speciality(find(sps, "general-medicine")).sortOrder(1).active(true).build(),
                Testimonial.builder().patientName("Shrikant Thakre").city("Narkhed").relation("Knee replacement")
                        .rating(5).quote("I was told I would need to go to Nagpur for a knee replacement. I had it done here, walked the next day, and my physio came home twice a week.")
                        .speciality(find(sps, "orthopaedics")).sortOrder(2).active(true).build(),
                Testimonial.builder().patientName("अनिल देशमुख").city("सावनेर").relation("Emergency admission")
                        .rating(5).quote("रात्री २ वाजता अपघात झाला. रुग्णवाहिका २० मिनिटांत आली आणि तासाभरात शस्त्रक्रिया सुरू झाली. माझ्या भावाचा जीव वाचला.")
                        .speciality(find(sps, "emergency-trauma")).sortOrder(3).active(true).build(),
                Testimonial.builder().patientName("Fatima Shaikh").city("Katol").relation("Delivery")
                        .rating(5).quote("The labour room staff explained every step to my mother in Marathi. My delivery was normal and the billing was exactly what was quoted — no surprises.")
                        .speciality(find(sps, "obstetrics-gynaecology")).sortOrder(4).active(true).build(),
                Testimonial.builder().patientName("प्रकाश उईके").city("मुळाई").relation("Dialysis patient")
                        .rating(4).quote("आधी नागपुरासाठी आठवड्यातून तीन वेळा प्रवास करावा लागत असे. आता डायलिसिस इथेच होते, खर्च आणि वेळ दोन्ही वाचले.")
                        .speciality(find(sps, "nephrology")).sortOrder(5).active(true).build(),
                Testimonial.builder().patientName("Rohit Ambhore").city("Katol").relation("Lab & health check")
                        .rating(5).quote("Booked the full-body check online at night, sample collected from home by 7 AM, report on WhatsApp by evening. This is how a small-town hospital should work.")
                        .speciality(find(sps, "diabetology")).sortOrder(6).active(true).build()));
    }

    private void seedAwards() {
        awards.saveAll(List.of(
                Award.builder().title("NABH Entry-Level Accreditation").issuer("National Accreditation Board for Hospitals")
                        .awardYear("2025").description("Accredited for patient safety, infection control and quality of clinical records.")
                        .iconKey("accreditation").sortOrder(1).active(true).build(),
                Award.builder().title("NABL Accredited Medical Laboratory").issuer("National Accreditation Board for Testing")
                        .awardYear("2024").description("Diagnostics centre accredited for clinical chemistry, haematology and serology.")
                        .iconKey("lab-certified").sortOrder(2).active(true).build(),
                Award.builder().title("Jan Arogya Yojana Empanelled").issuer("Government of Maharashtra")
                        .awardYear("2023").description("Empanelled for cashless treatment under the state health coverage scheme.")
                        .iconKey("government").sortOrder(3).active(true).build(),
                Award.builder().title("Best Rural Multi-Speciality Hospital, Vidarbha").issuer("Vidarbha Healthcare Forum")
                        .awardYear("2024").description("Recognised for accessible tertiary care outside a metro.")
                        .iconKey("trophy").sortOrder(4).active(true).build(),
                Award.builder().title("Kayakalp Award — Swachhata & Quality").issuer("Ministry of Health & Family Welfare")
                        .awardYear("2023").description("Awarded for hygiene, waste management and patient amenities.")
                        .iconKey("swachhata").sortOrder(5).active(true).build(),
                Award.builder().title("Green OT Certified").issuer("Association of Healthcare Providers India")
                        .awardYear("2025").description("Operating theatres certified for energy efficiency and biomedical waste compliance.")
                        .iconKey("leaf").sortOrder(6).active(true).build()));
    }

    // ------------------------------------------------------------------ staff

    private void ensureAdminUser(PasswordEncoder encoder) {
        if (users.count() > 0) return;
        String rawPassword = System.getenv().getOrDefault("ADMIN_PASSWORD", "Chinche@2026");
        String adminEmail = System.getenv().getOrDefault("ADMIN_EMAIL", "admin@chinchehospital.in");

        users.save(AppUser.builder()
                .username("admin").password(encoder.encode(rawPassword))
                .fullName("Hospital Administrator").email(adminEmail)
                .role(UserRole.ROLE_ADMIN).enabled(true).build());
        users.save(AppUser.builder()
                .username("reception").password(encoder.encode("Reception@2026"))
                .fullName("Front Desk — Katol").email("frontdesk@chinchehospital.in")
                .role(UserRole.ROLE_RECEPTIONIST).enabled(true).build());
        log.info("Seeded staff accounts: admin / reception (passwords via ADMIN_PASSWORD env or defaults).");
    }

    // ------------------------------------------------------------------ helpers

    private static Speciality find(List<Speciality> list, String slug) {
        if (slug == null) return null;
        return list.stream().filter(s -> s.getSlug().equals(slug)).findFirst().orElse(null);
    }

    private static Doctor findDoctor(List<Doctor> list, String slug) {
        return list.stream().filter(d -> d.getSlug().equals(slug)).findFirst().orElse(null);
    }
}
