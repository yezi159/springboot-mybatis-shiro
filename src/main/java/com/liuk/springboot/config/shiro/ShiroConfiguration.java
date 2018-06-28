package com.liuk.springboot.config.shiro;

import com.liuk.springboot.entity.User;
import com.liuk.springboot.service.UserService;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * shiro相关配置
 * Created by kl on 2018/6/19.
 */
@Configuration
public class ShiroConfiguration {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private UserService userService;

    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(SecurityManager securityManager){
        logger.info("ShiroConfiguration.shiroFilter()....");

        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(securityManager);

        Map<String, String> filterChainDefinitionMap = new LinkedHashMap<>();
        ///static/** = anon
        filterChainDefinitionMap.put("/assets/**","anon");
        filterChainDefinitionMap.put("/vendor/**","anon");
        filterChainDefinitionMap.put("/favicon.ico","anon");
        filterChainDefinitionMap.put("/logout","logout");
//        filterChainDefinitionMap.put("login","authc");
        filterChainDefinitionMap.put("/**", "authc");
        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);
        shiroFilterFactoryBean.setLoginUrl("/login");
        shiroFilterFactoryBean.setSuccessUrl("/index");
        return shiroFilterFactoryBean;
    }



    @Bean
    public AuthorizingRealm authorizingRealm(){
        logger.info("authorizingRealm ...  ");

        AuthorizingRealm authorizingRealm = new AuthorizingRealm() {
            @Override
            protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
                logger.info("doGetAuthorizationInfo ...");
                System.out.println(principalCollection);
                return new SimpleAuthorizationInfo();
            }

            @Override
            protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
                logger.info("doGetAuthenticationInfo ...");
                System.out.println("=====================");
                System.out.println(authenticationToken);
                User user = userService.get("1");
                System.out.println(user);
                String username = (String) authenticationToken.getPrincipal();
                String pawwword = "12345";
                SimpleAuthenticationInfo authenticationInfo = new SimpleAuthenticationInfo(username,pawwword,getName());
//                authenticationInfo.se
                return authenticationInfo;
            }
        };
        return authorizingRealm;
    }

    @Bean
    public SecurityManager securityManager(){
        logger.info("SecurityManager ...");
        DefaultWebSecurityManager defaultWebSecurityManager = new DefaultWebSecurityManager();
        defaultWebSecurityManager.setRealm(authorizingRealm());
        return defaultWebSecurityManager;
    }

}
