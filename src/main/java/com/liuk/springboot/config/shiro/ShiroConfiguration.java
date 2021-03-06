package com.liuk.springboot.config.shiro;

import com.liuk.springboot.common.SpringContextHolder;
import com.liuk.springboot.sys.entity.Menu;
import com.liuk.springboot.sys.entity.User;
import com.liuk.springboot.sys.service.IUserService;
import com.liuk.springboot.sys.utils.UserUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.codec.Hex;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ByteSource;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.DelegatingFilterProxy;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.util.*;

/**
 * shiro相关配置
 *
 * @author kl
 * @date 2018/6/19
 */
@Configuration
public class ShiroConfiguration {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /*@Autowired
    private IUserService userService;*/

    @Bean("shiroFilter")
    public ShiroFilterFactoryBean shiroFilterFactoryBean(SecurityManager securityManager){
        logger.info("ShiroConfiguration.shiroFilter()....");

        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(securityManager);

        Map<String, String> filterChainDefinitionMap = new LinkedHashMap<>();
        filterChainDefinitionMap.put("/assets/**","anon");
        filterChainDefinitionMap.put("/test/**","anon");
        filterChainDefinitionMap.put("/vendor/**","anon");
        filterChainDefinitionMap.put("/favicon.ico","anon");
        filterChainDefinitionMap.put("/logout","logout");
        filterChainDefinitionMap.put("/**", "user");
        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);
        shiroFilterFactoryBean.setLoginUrl("/login");
        shiroFilterFactoryBean.setSuccessUrl("/index");

        Map<String,Filter> filterMap = new HashMap<>();
        filterMap.put("authc",formAuthenticationFilter());
        shiroFilterFactoryBean.setFilters(filterMap);

        return shiroFilterFactoryBean;
    }

    @Bean
    public FormAuthenticationFilter formAuthenticationFilter(){

        /**
         * 配置登录成功后跳转到首页
         */
        return new FormAuthenticationFilter() {
            @Override
            protected boolean onLoginSuccess(AuthenticationToken token, Subject subject, ServletRequest request, ServletResponse response) throws Exception {
                WebUtils.issueRedirect(request, response, getSuccessUrl());
                return false;
            }

            @Override
            protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e, ServletRequest request, ServletResponse response) {
                System.out.println(e.getMessage());
                request.setAttribute("message",e.getMessage());
                return true;
            }
        };

    }


    @Bean
    public AuthorizingRealm authorizingRealm(){
        logger.info("authorizingRealm ...  ");

        AuthorizingRealm authorizingRealm = new AuthorizingRealm() {
            @Override
            protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
                logger.info("doGetAuthorizationInfo ...");

                User user = (User)principalCollection.getPrimaryPrincipal();
                System.out.println(user);
                SimpleAuthorizationInfo simpleAuthorizationInfo = new SimpleAuthorizationInfo();
                List<Menu> menus = UserUtils.getMenuList();
                menus.forEach(menu -> {
                    if (StringUtils.isNotBlank(menu.getPermission())){
                        simpleAuthorizationInfo.addStringPermissions(Arrays.asList(menu.getPermission().split(",")));
                    }
                });

                UserUtils.getRoleList(user).forEach(role -> {
                    simpleAuthorizationInfo.addRole(role.getEnname());
                });
                return simpleAuthorizationInfo;
            }

            @Override
            protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
                logger.info("doGetAuthenticationInfo ...");
                IUserService userService = SpringContextHolder.getBean(IUserService.class);
                User user = userService.getByLoginName(authenticationToken.getPrincipal().toString());
                if(user == null){
                    throw new AuthenticationException("message:用户不存在！");
                }
                System.out.println(user);
                byte[] salt = Hex.decode(user.getPassword().substring(0, 16));
                return new SimpleAuthenticationInfo(user,user.getPassword().substring(16),ByteSource.Util.bytes(salt),getName());
//                return new SimpleAuthenticationInfo(username,user.getPassword().substring(16),ByteSource.Util.bytes(salt),getName());
            }

            @Override
            protected void assertCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) throws AuthenticationException {
                CredentialsMatcher credentialsMatcher = getCredentialsMatcher();
                if(credentialsMatcher != null){
                    if(!credentialsMatcher.doCredentialsMatch(token,info)){
                        throw new IncorrectCredentialsException("message:账户名或密码错误,请重试！");
                    }
                }else {
                    logger.error("No CredentialsMatcher has Configured...");
                    throw new AuthenticationException("No CredentialsMatcher has Configured...");
                }
            }

            @PostConstruct
            public void initCredentialsMatcher(){
                HashedCredentialsMatcher matcher = new HashedCredentialsMatcher("MD5");
                matcher.setHashIterations(1024);
                setCredentialsMatcher(matcher);
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

    @Bean
    public FilterRegistrationBean filterRegistrationBean(){
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        // 设置 targetBeanName
        DelegatingFilterProxy delegatingFilterProxy = new DelegatingFilterProxy("shiroFilter");
        //DelegatingFilterProxy delegatingFilterProxy = new DelegatingFilterProxy();
        filterRegistrationBean.setFilter(delegatingFilterProxy);
        filterRegistrationBean.addInitParameter("targetFilterLifecycle","true");
        filterRegistrationBean.addUrlPatterns("/*");
        return filterRegistrationBean;
    }

    @Bean
    public LifecycleBeanPostProcessor lifecycleBeanPostProcessor(){
        logger.info("init lifecycleBeanPostProcessor ...");
        return new LifecycleBeanPostProcessor();
    }


    /*
     *
     * 启用 Shiro 注解
     *  AOP式方法级权限检查
     *
     */
    @Bean
    @DependsOn("lifecycleBeanPostProcessor")
    public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator(){
        logger.info("init defaultAdvisorAutoProxyCreator ...");
        DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator = new DefaultAdvisorAutoProxyCreator();
        defaultAdvisorAutoProxyCreator.setProxyTargetClass(true);
        return defaultAdvisorAutoProxyCreator;
    }

    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager){
        logger.info("init authorizationAttributeSourceAdvisor ...");
        AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor = new AuthorizationAttributeSourceAdvisor();
        authorizationAttributeSourceAdvisor.setSecurityManager(securityManager);
        return authorizationAttributeSourceAdvisor;
    }
}
