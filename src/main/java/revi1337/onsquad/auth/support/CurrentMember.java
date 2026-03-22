package revi1337.onsquad.auth.support;

import java.util.Collection;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import revi1337.onsquad.member.application.dto.MemberSummary;
import revi1337.onsquad.member.domain.vo.UserType;

public record CurrentMember(
        Long id,
        String email,
        String password,
        UserType userType,
        Collection<? extends GrantedAuthority> authorities
) implements UserDetails {

    public static CurrentMember of(Long id, String email, String password, UserType userType) {
        return new CurrentMember(id, email, password, userType, Set.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    public static CurrentMember of(Long id, String email, UserType userType) {
        return new CurrentMember(id, email, null, userType, Set.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    public static CurrentMember from(MemberSummary summary) {
        return CurrentMember.of(summary.id(), summary.email(), summary.password(), summary.userType());
    }

    public MemberSummary summary() {
        return new MemberSummary(id, email, password, userType);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
