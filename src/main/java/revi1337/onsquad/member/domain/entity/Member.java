package revi1337.onsquad.member.domain.entity;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import revi1337.onsquad.common.domain.BaseEntity;
import revi1337.onsquad.member.domain.model.ProfileSpec;
import revi1337.onsquad.member.domain.vo.Address;
import revi1337.onsquad.member.domain.vo.Email;
import revi1337.onsquad.member.domain.vo.Introduce;
import revi1337.onsquad.member.domain.vo.Mbti;
import revi1337.onsquad.member.domain.vo.Nickname;
import revi1337.onsquad.member.domain.vo.Password;
import revi1337.onsquad.member.domain.vo.PasswordPolicy;
import revi1337.onsquad.member.domain.vo.UserType;

@Getter
@NoArgsConstructor(access = PROTECTED)
@Table(uniqueConstraints = {@UniqueConstraint(name = "uk_member_email", columnNames = "email")})
@Entity
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Embedded
    private Email email;

    @Embedded
    private Password password;

    @Embedded
    private Nickname nickname;

    @Embedded
    private Introduce introduce;

    @Embedded
    private Address address;

    private String profileImage;

    private String kakaoLink;

    @Enumerated(STRING)
    @Column(nullable = false)
    private UserType userType;

    @Enumerated(STRING)
    private Mbti mbti;

    public static Member general(String email, String password, String nickname, String address, String addressDetail) {
        return Member.builder()
                .email(new Email(email))
                .password(Password.of(password, PasswordPolicy.RAW))
                .nickname(new Nickname(nickname))
                .address(new Address(address, addressDetail))
                .userType(UserType.GENERAL)
                .build();
    }

    public static Member oauth2(String email, String password, String nickname, String image, UserType userType) {
        return Member.builder()
                .email(new Email(email))
                .password(Password.of(password, PasswordPolicy.BCRYPT))
                .nickname(new Nickname(nickname))
                .image(image)
                .userType(userType)
                .build();
    }

    @Builder(access = PRIVATE)
    private Member(Email email, Password password, Nickname nickname, Introduce introduce,
                   Address address, String image, String kakaoLink, UserType userType, Mbti mbti) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.introduce = introduce;
        this.address = address;
        this.profileImage = image;
        this.kakaoLink = kakaoLink;
        this.userType = userType;
        this.mbti = mbti;
    }

    public void updateProfile(ProfileSpec spec) {
        this.nickname = new Nickname(spec.getNickname());
        this.introduce = new Introduce(spec.getIntroduce());
        this.mbti = Mbti.parse(spec.getMbti());
        this.kakaoLink = spec.getKakaoLink();
        this.address = new Address(spec.getAddress(), spec.getAddressDetail());
    }

    public void updateImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void deleteImage() {
        this.profileImage = null;
    }

    public void updatePassword(String password, PasswordPolicy policy) {
        this.password = this.password.update(password, policy);
    }

    public boolean hasImage() {
        return profileImage != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Member member)) {
            return false;
        }
        return id != null && Objects.equals(getId(), member.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
