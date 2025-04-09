package revi1337.onsquad.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import revi1337.onsquad.crew_member.config.CrewTopMemberProperty;
import revi1337.onsquad.inrastructure.file.config.s3.properties.RecycleBinCleaningProperty;

@ConfigurationProperties("onsquad.api")
public record ApiProperties(
        @NestedConfigurationProperty CrewTopMemberProperty crewTopMembers,
        @NestedConfigurationProperty RecycleBinCleaningProperty cleanRecycleBin
) {
}
