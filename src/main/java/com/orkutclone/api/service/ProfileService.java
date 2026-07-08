package com.orkutclone.api.service;

import com.orkutclone.api.dto.profile.*;
import com.orkutclone.api.model.*;
import com.orkutclone.api.repository.*;
import com.orkutclone.api.validation.AllowedProfileValues;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final UserProfileGeneralRepository generalRepository;
    private final UserProfileSocialRepository socialRepository;
    private final UserProfileContactRepository contactRepository;
    private final UserProfileProfessionalRepository professionalRepository;
    private final UserProfilePersonalRepository personalRepository;
    private final com.orkutclone.api.support.AvatarStorageService avatarStorage;

    private static final String DATING_INTEREST = "namoro";
    private static final long MAX_AVATAR_SIZE = 10L * 1024 * 1024;
    private static final int MIN_AVATAR_DIMENSION = 32;
    private static final Set<String> ALLOWED_FORMATS = Set.of("png", "jpg", "gif", "bmp");

    // ── General ──

    @Transactional(readOnly = true)
    public GeneralProfileDTO getGeneral() {
        return toGeneralDTO(getOrCreateGeneral());
    }

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public GeneralProfileDTO updateGeneral(GeneralProfileDTO dto) {
        UserProfileGeneral g = getOrCreateGeneral();

        if (dto.firstName() != null) { requireNotBlank(dto.firstName(), "firstName"); g.setFirstName(dto.firstName()); }
        if (dto.lastName() != null) { requireNotBlank(dto.lastName(), "lastName"); g.setLastName(dto.lastName()); }
        AllowedProfileValues.validateOption(dto.gender(), AllowedProfileValues.GENDER, "gender");
        if (dto.gender() != null) { requireNotBlank(dto.gender(), "gender"); g.setGender(dto.gender()); }
        AllowedProfileValues.validateOption(dto.relationshipStatus(), AllowedProfileValues.RELATIONSHIP_STATUS, "relationshipStatus");
        if (dto.relationshipStatus() != null) g.setRelationshipStatus(dto.relationshipStatus());
        AllowedProfileValues.validateOption(dto.birthMonth(), AllowedProfileValues.BIRTH_MONTHS, "birthMonth");
        if (dto.birthMonth() != null) g.setBirthMonth(dto.birthMonth());
        AllowedProfileValues.validateOption(dto.birthDay(), AllowedProfileValues.BIRTH_DAYS, "birthDay");
        if (dto.birthDay() != null) g.setBirthDay(dto.birthDay());
        if (dto.birthDatePrivacy() != null) g.setBirthDatePrivacy(dto.birthDatePrivacy());
        if (dto.birthYear() != null) g.setBirthYear(dto.birthYear());
        if (dto.birthYearPrivacy() != null) g.setBirthYearPrivacy(dto.birthYearPrivacy());
        if (dto.city() != null) g.setCity(dto.city());
        if (dto.state() != null) g.setState(dto.state());
        if (dto.zipCode() != null) g.setZipCode(dto.zipCode());
        AllowedProfileValues.validateOption(dto.country(), AllowedProfileValues.COUNTRIES, "country");
        if (dto.country() != null) { requireNotBlank(dto.country(), "country"); g.setCountry(dto.country()); }
        AllowedProfileValues.validateOptionList(dto.languages(), AllowedProfileValues.LANGUAGES, "languages");
        if (dto.languages() != null) replaceCollection(g.getLanguages(), dto.languages());
        if (dto.languagesPrivacy() != null) g.setLanguagesPrivacy(dto.languagesPrivacy());
        if (dto.highSchool() != null) g.setHighSchool(dto.highSchool());
        if (dto.highSchoolPrivacy() != null) g.setHighSchoolPrivacy(dto.highSchoolPrivacy());
        if (dto.college() != null) g.setCollege(dto.college());
        if (dto.collegePrivacy() != null) g.setCollegePrivacy(dto.collegePrivacy());
        if (dto.company() != null) g.setCompany(dto.company());
        if (dto.companyPrivacy() != null) g.setCompanyPrivacy(dto.companyPrivacy());
        AllowedProfileValues.validateOptionList(dto.interestedIn(), AllowedProfileValues.INTERESTED_IN, "interestedIn");
        if (dto.interestedIn() != null) replaceCollection(g.getInterestedIn(), dto.interestedIn());
        AllowedProfileValues.validateOption(dto.datingPreference(), AllowedProfileValues.DATING_PREFERENCE, "datingPreference");
        if (dto.datingPreference() != null) g.setDatingPreference(dto.datingPreference());

        // "interessado(a) em [gênero]" (datingPreference) só faz sentido junto de "namoro"
        // em interestedIn. Se "namoro" for removido, a preferência também é descartada.
        if (!g.getInterestedIn().contains(DATING_INTEREST)) {
            g.setDatingPreference(null);
        }

        generalRepository.save(g);
        syncUserName(g);
        return toGeneralDTO(g);
    }

    // ── Social ──

    @Transactional(readOnly = true)
    public SocialProfileDTO getSocial() {
        return toSocialDTO(getOrCreateSocial());
    }

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public SocialProfileDTO updateSocial(SocialProfileDTO dto) {
        UserProfileSocial s = getOrCreateSocial();

        AllowedProfileValues.validateOption(dto.children(), AllowedProfileValues.CHILDREN, "children");
        if (dto.children() != null) s.setChildren(dto.children());
        AllowedProfileValues.validateOption(dto.ethnicity(), AllowedProfileValues.ETHNICITY, "ethnicity");
        if (dto.ethnicity() != null) s.setEthnicity(dto.ethnicity());
        AllowedProfileValues.validateOption(dto.religion(), AllowedProfileValues.RELIGION, "religion");
        if (dto.religion() != null) s.setReligion(dto.religion());
        AllowedProfileValues.validateOption(dto.politicalView(), AllowedProfileValues.POLITICAL_VIEW, "politicalView");
        if (dto.politicalView() != null) s.setPoliticalView(dto.politicalView());
        AllowedProfileValues.validateOption(dto.sexualOrientation(), AllowedProfileValues.SEXUAL_ORIENTATION, "sexualOrientation");
        if (dto.sexualOrientation() != null) s.setSexualOrientation(dto.sexualOrientation());
        if (dto.sexualOrientationPrivacy() != null) s.setSexualOrientationPrivacy(dto.sexualOrientationPrivacy());
        AllowedProfileValues.validateOptionList(dto.humor(), AllowedProfileValues.HUMOR, "humor");
        if (dto.humor() != null) replaceCollection(s.getHumor(), dto.humor());
        AllowedProfileValues.validateOptionList(dto.style(), AllowedProfileValues.STYLE, "style");
        if (dto.style() != null) replaceCollection(s.getStyle(), dto.style());
        AllowedProfileValues.validateOption(dto.smoking(), AllowedProfileValues.SMOKING, "smoking");
        if (dto.smoking() != null) s.setSmoking(dto.smoking());
        AllowedProfileValues.validateOption(dto.drinking(), AllowedProfileValues.DRINKING, "drinking");
        if (dto.drinking() != null) s.setDrinking(dto.drinking());
        AllowedProfileValues.validateOption(dto.pets(), AllowedProfileValues.PETS, "pets");
        if (dto.pets() != null) s.setPets(dto.pets());
        AllowedProfileValues.validateOption(dto.livingWith(), AllowedProfileValues.LIVING_WITH, "livingWith");
        if (dto.livingWith() != null) s.setLivingWith(dto.livingWith());
        if (dto.hometown() != null) s.setHometown(dto.hometown());
        if (dto.website() != null) s.setWebsite(dto.website());
        if (dto.aboutMe() != null) s.setAboutMe(dto.aboutMe());
        if (dto.passions() != null) s.setPassions(dto.passions());
        if (dto.sports() != null) s.setSports(dto.sports());
        if (dto.activities() != null) s.setActivities(dto.activities());
        if (dto.books() != null) s.setBooks(dto.books());
        if (dto.music() != null) s.setMusic(dto.music());
        if (dto.tvShows() != null) s.setTvShows(dto.tvShows());
        if (dto.movies() != null) s.setMovies(dto.movies());
        if (dto.cuisines() != null) s.setCuisines(dto.cuisines());

        socialRepository.save(s);
        return toSocialDTO(s);
    }

    // ── Contact ──

    @Transactional(readOnly = true)
    public ContactProfileDTO getContact() {
        return toContactDTO(getOrCreateContact());
    }

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public ContactProfileDTO updateContact(ContactProfileDTO dto) {
        UserProfileContact c = getOrCreateContact();

        if (dto.primaryEmail() != null) c.setPrimaryEmail(dto.primaryEmail());
        if (dto.primaryEmailPrivacy() != null) c.setPrimaryEmailPrivacy(dto.primaryEmailPrivacy());
        if (dto.secondaryEmails() != null) {
            c.getSecondaryEmails().clear();
            dto.secondaryEmails().forEach(se ->
                    c.getSecondaryEmails().add(new SecondaryEmail(se.email(), se.privacy())));
        }
        if (dto.im1() != null) c.setIm1(dto.im1());
        if (dto.im1Privacy() != null) c.setIm1Privacy(dto.im1Privacy());
        if (dto.im2() != null) c.setIm2(dto.im2());
        if (dto.im2Privacy() != null) c.setIm2Privacy(dto.im2Privacy());
        if (dto.homePhone() != null) c.setHomePhone(dto.homePhone());
        if (dto.homePhonePrivacy() != null) c.setHomePhonePrivacy(dto.homePhonePrivacy());
        if (dto.mobilePhone() != null) c.setMobilePhone(dto.mobilePhone());
        if (dto.mobilePhonePrivacy() != null) c.setMobilePhonePrivacy(dto.mobilePhonePrivacy());
        if (dto.address1() != null) c.setAddress1(dto.address1());
        if (dto.address2() != null) c.setAddress2(dto.address2());
        if (dto.addressCity() != null) c.setAddressCity(dto.addressCity());
        if (dto.addressState() != null) c.setAddressState(dto.addressState());
        if (dto.addressZipCode() != null) c.setAddressZipCode(dto.addressZipCode());
        if (dto.addressCountry() != null) c.setAddressCountry(dto.addressCountry());

        contactRepository.save(c);
        return toContactDTO(c);
    }

    // ── Professional ──

    @Transactional(readOnly = true)
    public ProfessionalProfileDTO getProfessional() {
        return toProfessionalDTO(getOrCreateProfessional());
    }

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public ProfessionalProfileDTO updateProfessional(ProfessionalProfileDTO dto) {
        UserProfileProfessional p = getOrCreateProfessional();

        AllowedProfileValues.validateOption(dto.education(), AllowedProfileValues.EDUCATION_LEVEL, "education");
        if (dto.education() != null) p.setEducation(dto.education());
        if (dto.school() != null) p.setSchool(dto.school());
        if (dto.college() != null) p.setCollege(dto.college());
        if (dto.course() != null) p.setCourse(dto.course());
        if (dto.degree() != null) p.setDegree(dto.degree());
        if (dto.year() != null) p.setGraduationYear(dto.year());
        if (dto.profession() != null) p.setProfession(dto.profession());
        if (dto.sector() != null) p.setSector(dto.sector());
        if (dto.company() != null) p.setCompany(dto.company());
        if (dto.jobDescription() != null) p.setJobDescription(dto.jobDescription());
        if (dto.workPhone() != null) p.setWorkPhone(dto.workPhone());
        if (dto.professionalSkills() != null) p.setProfessionalSkills(dto.professionalSkills());
        if (dto.professionalInterests() != null) p.setProfessionalInterests(dto.professionalInterests());

        professionalRepository.save(p);
        return toProfessionalDTO(p);
    }

    // ── Personal ──

    @Transactional(readOnly = true)
    public PersonalProfileDTO getPersonal() {
        return toPersonalDTO(getOrCreatePersonal());
    }

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public PersonalProfileDTO updatePersonal(PersonalProfileDTO dto) {
        UserProfilePersonal p = getOrCreatePersonal();

        AllowedProfileValues.validateOption(dto.eyeColor(), AllowedProfileValues.EYE_COLOR, "eyeColor");
        if (dto.eyeColor() != null) p.setEyeColor(dto.eyeColor());
        AllowedProfileValues.validateOption(dto.hairColor(), AllowedProfileValues.HAIR_COLOR, "hairColor");
        if (dto.hairColor() != null) p.setHairColor(dto.hairColor());
        if (dto.height() != null) p.setHeight(dto.height());
        AllowedProfileValues.validateOption(dto.bodyType(), AllowedProfileValues.BODY_TYPE, "bodyType");
        if (dto.bodyType() != null) p.setBodyType(dto.bodyType());
        AllowedProfileValues.validateOption(dto.appearance(), AllowedProfileValues.APPEARANCE, "appearance");
        if (dto.appearance() != null) p.setAppearance(dto.appearance());
        AllowedProfileValues.validateOption(dto.bodyArt(), AllowedProfileValues.BODY_ART, "bodyArt");
        if (dto.bodyArt() != null) p.setBodyArt(dto.bodyArt());
        if (dto.perfectMatch() != null) p.setPerfectMatch(dto.perfectMatch());
        AllowedProfileValues.validateOptionList(dto.attractions(), AllowedProfileValues.ATTRACTIONS, "attractions");
        if (dto.attractions() != null) replaceCollection(p.getAttractions(), dto.attractions());
        if (dto.cantStand() != null) p.setCantStand(dto.cantStand());
        if (dto.idealFirstDate() != null) p.setIdealFirstDate(dto.idealFirstDate());
        if (dto.pastRelationshipsLessons() != null) p.setPastRelationshipsLessons(dto.pastRelationshipsLessons());
        if (dto.whatStandsOut() != null) p.setWhatStandsOut(dto.whatStandsOut());
        AllowedProfileValues.validateOption(dto.favoriteBodyPart(), AllowedProfileValues.FAVORITE_BODY_PART, "favoriteBodyPart");
        if (dto.favoriteBodyPart() != null) p.setFavoriteBodyPart(dto.favoriteBodyPart());
        if (dto.fiveEssentials() != null) p.setFiveEssentials(dto.fiveEssentials());
        if (dto.inMyRoom() != null) p.setInMyRoom(dto.inMyRoom());

        personalRepository.save(p);
        return toPersonalDTO(p);
    }

    // ── Avatar ──

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public AvatarResponse uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No image file provided");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read uploaded file");
        }

        if (data.length > MAX_AVATAR_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Image exceeds maximum size of 10MB");
        }

        // O formato é determinado pelos bytes reais (não pelo Content-Type declarado),
        // o que impede spoofing de MIME type.
        String extension = validateImageAndResolveExtension(data);
        String url = avatarStorage.store(data, extension);

        User user = userRepository.findById(authenticatedUser().getId()).orElseThrow();
        String previous = user.getProfilePicture();
        user.setProfilePicture(url);
        userRepository.save(user);

        if (previous != null && !previous.equals(url)) {
            avatarStorage.delete(previous);
        }

        return new AvatarResponse(url);
    }

    private String validateImageAndResolveExtension(byte[] data) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (iis == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read image data");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid image format. Supported: PNG, JPG, GIF, BMP");
            }
            ImageReader reader = readers.next();
            try {
                String extension = normalizeExtension(reader.getFormatName());
                if (!ALLOWED_FORMATS.contains(extension)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid image format. Supported: PNG, JPG, GIF, BMP");
                }
                reader.setInput(iis);
                if (reader.getWidth(0) < MIN_AVATAR_DIMENSION || reader.getHeight(0) < MIN_AVATAR_DIMENSION) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Image must be at least 32x32 pixels");
                }
                return extension;
            } finally {
                reader.dispose();
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to process image");
        }
    }

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public void deleteAvatar() {
        User user = userRepository.findById(authenticatedUser().getId()).orElseThrow();
        String previous = user.getProfilePicture();
        user.setProfilePicture(null);
        userRepository.save(user);
        avatarStorage.delete(previous);
    }

    private static String normalizeExtension(String imageType) {
        String type = imageType.toLowerCase();
        return type.equals("jpeg") ? "jpg" : type;
    }

    // ── Get-or-create helpers ──

    private UserProfile getOrCreateCoreProfile() {
        User user = authenticatedUser();
        return profileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserProfile profile = new UserProfile();
                    profile.setUser(user);
                    return profileRepository.save(profile);
                });
    }

    private UserProfileGeneral getOrCreateGeneral() {
        UserProfile core = getOrCreateCoreProfile();
        return generalRepository.findByProfileId(core.getId())
                .orElseGet(() -> {
                    UserProfileGeneral g = new UserProfileGeneral();
                    g.setProfile(core);
                    return generalRepository.save(g);
                });
    }

    private UserProfileSocial getOrCreateSocial() {
        UserProfile core = getOrCreateCoreProfile();
        return socialRepository.findByProfileId(core.getId())
                .orElseGet(() -> {
                    UserProfileSocial s = new UserProfileSocial();
                    s.setProfile(core);
                    return socialRepository.save(s);
                });
    }

    private UserProfileContact getOrCreateContact() {
        UserProfile core = getOrCreateCoreProfile();
        return contactRepository.findByProfileId(core.getId())
                .orElseGet(() -> {
                    UserProfileContact c = new UserProfileContact();
                    c.setProfile(core);
                    return contactRepository.save(c);
                });
    }

    private UserProfileProfessional getOrCreateProfessional() {
        UserProfile core = getOrCreateCoreProfile();
        return professionalRepository.findByProfileId(core.getId())
                .orElseGet(() -> {
                    UserProfileProfessional p = new UserProfileProfessional();
                    p.setProfile(core);
                    return professionalRepository.save(p);
                });
    }

    private UserProfilePersonal getOrCreatePersonal() {
        UserProfile core = getOrCreateCoreProfile();
        return personalRepository.findByProfileId(core.getId())
                .orElseGet(() -> {
                    UserProfilePersonal p = new UserProfilePersonal();
                    p.setProfile(core);
                    return personalRepository.save(p);
                });
    }

    // ── Utilities ──

    private User authenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private void syncUserName(UserProfileGeneral g) {
        if (g.getFirstName() != null || g.getLastName() != null) {
            User user = userRepository.findById(g.getProfile().getUser().getId()).orElseThrow();
            String first = g.getFirstName() != null ? g.getFirstName() : "";
            String last = g.getLastName() != null ? g.getLastName() : "";
            user.setName((first + " " + last).trim());
            userRepository.save(user);
        }
    }

    private void requireNotBlank(String value, String fieldName) {
        if (value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " cannot be empty");
        }
    }

    private void replaceCollection(List<String> target, List<String> source) {
        target.clear();
        target.addAll(source);
    }

    // ── DTO conversions ──

    private GeneralProfileDTO toGeneralDTO(UserProfileGeneral g) {
        return new GeneralProfileDTO(
                g.getFirstName(), g.getLastName(), g.getGender(), g.getRelationshipStatus(),
                g.getBirthMonth(), g.getBirthDay(), g.getBirthDatePrivacy(),
                g.getBirthYear(), g.getBirthYearPrivacy(),
                g.getCity(), g.getState(), g.getZipCode(), g.getCountry(),
                List.copyOf(g.getLanguages()), g.getLanguagesPrivacy(),
                g.getHighSchool(), g.getHighSchoolPrivacy(),
                g.getCollege(), g.getCollegePrivacy(),
                g.getCompany(), g.getCompanyPrivacy(),
                List.copyOf(g.getInterestedIn()), g.getDatingPreference()
        );
    }

    private SocialProfileDTO toSocialDTO(UserProfileSocial s) {
        return new SocialProfileDTO(
                s.getChildren(), s.getEthnicity(), s.getReligion(), s.getPoliticalView(),
                s.getSexualOrientation(), s.getSexualOrientationPrivacy(),
                List.copyOf(s.getHumor()), List.copyOf(s.getStyle()),
                s.getSmoking(), s.getDrinking(), s.getPets(), s.getLivingWith(),
                s.getHometown(), s.getWebsite(), s.getAboutMe(), s.getPassions(),
                s.getSports(), s.getActivities(), s.getBooks(), s.getMusic(),
                s.getTvShows(), s.getMovies(), s.getCuisines()
        );
    }

    private ContactProfileDTO toContactDTO(UserProfileContact c) {
        List<SecondaryEmailDTO> emails = c.getSecondaryEmails().stream()
                .map(se -> new SecondaryEmailDTO(se.getEmail(), se.getPrivacy()))
                .toList();
        return new ContactProfileDTO(
                c.getPrimaryEmail(), c.getPrimaryEmailPrivacy(), emails,
                c.getIm1(), c.getIm1Privacy(), c.getIm2(), c.getIm2Privacy(),
                c.getHomePhone(), c.getHomePhonePrivacy(),
                c.getMobilePhone(), c.getMobilePhonePrivacy(),
                c.getAddress1(), c.getAddress2(),
                c.getAddressCity(), c.getAddressState(),
                c.getAddressZipCode(), c.getAddressCountry()
        );
    }

    private ProfessionalProfileDTO toProfessionalDTO(UserProfileProfessional p) {
        return new ProfessionalProfileDTO(
                p.getEducation(), p.getSchool(), p.getCollege(),
                p.getCourse(), p.getDegree(), p.getGraduationYear(),
                p.getProfession(), p.getSector(), p.getCompany(),
                p.getJobDescription(), p.getWorkPhone(),
                p.getProfessionalSkills(), p.getProfessionalInterests()
        );
    }

    private PersonalProfileDTO toPersonalDTO(UserProfilePersonal p) {
        return new PersonalProfileDTO(
                p.getEyeColor(), p.getHairColor(), p.getHeight(),
                p.getBodyType(), p.getAppearance(), p.getBodyArt(),
                p.getPerfectMatch(), List.copyOf(p.getAttractions()),
                p.getCantStand(), p.getIdealFirstDate(),
                p.getPastRelationshipsLessons(), p.getWhatStandsOut(),
                p.getFavoriteBodyPart(), p.getFiveEssentials(), p.getInMyRoom()
        );
    }
}
