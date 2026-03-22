package revi1337.onsquad.member.infrastructure;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import revi1337.onsquad.common.application.file.AbstractFileStorageManager;
import revi1337.onsquad.common.application.file.FileStorageManager;
import revi1337.onsquad.infrastructure.aws.s3.config.S3BucketProperties;

@Component
public class MemberFileStorageManager extends AbstractFileStorageManager {

    public MemberFileStorageManager(@Qualifier("cacheableS3StorageManager") FileStorageManager fileStorageManager, S3BucketProperties s3BucketProperties) {
        super(fileStorageManager, s3BucketProperties.getActualMemberAssets());
    }
}
