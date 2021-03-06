package com.test.keycloak.config;

import com.test.keycloak.WebAccessDeniedHandler;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;

import org.keycloak.adapters.springsecurity.KeycloakSecurityComponents;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;

import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;



@EnableWebSecurity
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@ComponentScan(basePackageClasses = KeycloakSecurityComponents.class)
public class KeySecurityConfig extends KeycloakWebSecurityConfigurerAdapter {

    private final WebAccessDeniedHandler webAccessDeniedHandler;

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Autowired
    public KeySecurityConfig(WebAccessDeniedHandler webAccessDeniedHandler){
        this.webAccessDeniedHandler = webAccessDeniedHandler;
    }
    // Keycloak applicatoin.properties ?????? ???????????? ??????
    @Bean
    public KeycloakConfigResolver keycloakConfigResolver() {
        return new KeycloakSpringBootConfigResolver();
    }

    @Override
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
//        return new NullAuthenticatedSessionStrategy();
        return new RegisterSessionAuthenticationStrategy(sessionRegistry());        //AuthSession
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception{
        KeycloakAuthenticationProvider keycloakAuthenticationProvider = keycloakAuthenticationProvider();
        //Keycloak?????? ??????????????? Provider ????????????
        keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(new SimpleAuthorityMapper());

        auth.authenticationProvider(keycloakAuthenticationProvider);
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring()
                .antMatchers("/resources/**")       //??? ????????? ?????? ??????
                .antMatchers("/css/**")
                .antMatchers("/vendor/**")
                .antMatchers("/js/**")
                .antMatchers("/favicon*/**")
                .antMatchers("/img/**")
                .antMatchers("/image/**");
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        super.configure(http);

        http
                .authorizeRequests()
                .antMatchers("/user_manage").access("hasRole('ADMIN')")     //user_manage ????????? ADMIN ????????? ????????????
                .anyRequest().authenticated()
                .and()

                // "/" ????????? permitAll(false) ==>  authenticated() ??? ???????????????.
                .logout()
                .logoutUrl("/logout")       //logout url??????
                .logoutSuccessUrl("/")      //logout ??? ????????? ??????
                .and()
                .csrf().disable()
        ;
    }
}
