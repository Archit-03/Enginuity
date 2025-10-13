package com.project.enginuity.profile.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {
    @Autowired
    private UserDetailService userDetailService;
    @Autowired
    private JwtUtils jwtUtils;

    private final List<String> excludedUrls = List.of(
            "/login",
            "/register",
            "/resend-otp",
            "/verify-otp",
            "/send-reset-otp",
            "/reset-password",
            "/logout"
    );


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path=request.getServletPath();
        if (excludedUrls.contains(path)){
            filterChain.doFilter(request,response);
            return;
        }
        String authHeader=request.getHeader("Authorization");
        String jwtToken=null;
        String email=null;
        String userId=null;
        if(authHeader!=null&&authHeader.startsWith(("Bearer "))){
            jwtToken=authHeader.substring(7);
        }

        if(jwtToken==null || jwtToken.isEmpty()){
            Cookie[] cookies = request.getCookies();
            if(cookies!=null){
                for(Cookie cookie:cookies){
                    if("jwt".equals(cookie.getName())){
                        jwtToken=cookie.getValue();
                        break;
                    }
                }
            }
        }

        if(jwtToken!=null){
            email=jwtUtils.extractEmail(jwtToken);
            userId=jwtUtils.extractUserId(jwtToken);
            CustomPrincipal principal=new CustomPrincipal(userId,email);
            if(email!=null&& SecurityContextHolder.getContext().getAuthentication()==null){
                UserDetails userDetails=userDetailService.loadUserByUsername(email);
                if(jwtUtils.isValidToken(jwtToken,userDetails)){
                    UsernamePasswordAuthenticationToken authenticationToken=
                            new UsernamePasswordAuthenticationToken(principal,null,userDetails.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }
        }

        filterChain.doFilter(request, response);

    }
}
