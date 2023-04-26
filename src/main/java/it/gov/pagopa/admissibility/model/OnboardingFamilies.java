package it.gov.pagopa.admissibility.model;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.enums.OnboardingFamilyEvaluationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Document("onboarding_families")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true, builderMethodName = "hiddenBuilder", buildMethodName = "hiddenBuild")
public class OnboardingFamilies {
    @Id
    private String id;
    private String familyId;
    private String initiativeId;
    private Set<String> memberIds;
    private OnboardingFamilyEvaluationStatus status;
    private List<OnboardingRejectionReason> onboardingRejectionReasons;
    private LocalDateTime createDate;

    public OnboardingFamilies(@NonNull Family family, @NonNull String initiativeId){
        this.id=buildId(family, initiativeId);

        this.familyId= family.getFamilyId();
        this.initiativeId=initiativeId;
        this.memberIds=family.getMemberIds();
    }

    public static String buildId(Family family, String initiativeId) {
        return buildId(family.getFamilyId(), initiativeId);
    }

    public static String buildId(String familyId, String initiativeId) {
        return "%s_%s".formatted(familyId, initiativeId);
    }

    @SuppressWarnings("squid:S1452")
    public static OnboardingFamiliesBuilder<?,?> builder(Family family, String initiativeId){
        return OnboardingFamilies.hiddenBuilder()
                .id(buildId(family.getFamilyId(), initiativeId))
                .familyId(family.getFamilyId())
                .initiativeId(initiativeId)
                .memberIds(family.getMemberIds());
    }

    @SuppressWarnings("squid:S1610") // suppressing conversion of abstract class into interface: this class is handled by lombok SuperBuilder
    public abstract static class OnboardingFamiliesBuilder<C extends OnboardingFamilies, B extends OnboardingFamiliesBuilder<C, B>>  {

        public B family(Family family){
            this.familyId=family.getFamilyId();
            this.memberIds=family.getMemberIds();

            this.id=buildId(this.familyId, this.initiativeId);
            return self();
        }

        public B familyId(String familyId){
            this.familyId=familyId;
            this.id=buildId(this.familyId, this.initiativeId);
            return self();
        }

        public B initiativeId(String initiativeId){
            this.initiativeId=initiativeId;
            this.id=buildId(this.familyId, this.initiativeId);
            return self();
        }

        public C build() {
            C out = this.hiddenBuild();
            out.setId(buildId(out.getFamilyId(), out.getInitiativeId()));
            return out;
        }
    }
}
