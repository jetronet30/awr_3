package com.jaba.awr_3.security.servvices;

import com.jaba.awr_3.security.mod.SeMod;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class UserAdd implements UserDetailsService {

    private final SecurityService securityService;

    // Constructor Injection (რეკომენდებული)
    public UserAdd(SecurityService securityService) {
        this.securityService = securityService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        
        // ვეძებთ მომხმარებელს ჩვენი JSON ფაილიდან
        SeMod seMod = securityService.getSByUsername(username);

        if (seMod == null) {
            throw new UsernameNotFoundException("მომხმარებელი ვერ მოიძებნა: " + username);
        }

        // Role-ის მიხედვით Authority-ის შექმნა
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + seMod.getRole())
        );

        // UserDetails-ის შექმნა (აქ password უკვე hashed უნდა იყოს)
        return new User(
                seMod.getUsername(),
                seMod.getPassword(),        // ეს უნდა იყოს BCrypt hashed პაროლი
                authorities
        );
    }
}