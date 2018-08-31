package com.gwg.user.web.configuration;

import java.util.Arrays;
import java.util.List;

import com.gwg.user.web.handler.UserAccessDeniedHandler;
import com.gwg.user.web.handler.UserAuthenticationFailureHandler;
import com.gwg.user.web.handler.UserLogoutSuccessHandler;
import com.gwg.user.web.security.HttpForbiddenEntryPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.access.vote.UnanimousBased;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.gwg.user.web.handler.UserAuthenticationSuccessHandler;
import com.gwg.user.web.security.CustomAccessDecisionVoter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.cors.CorsUtils;

@Configuration
//基于标准的，并且允许简单的基于角色的约束。但是并不具备SpringSecurity的原生注解能力。要使用基于表达式的语法
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableWebSecurity// 开启Security，使用该注解后会创建过滤器链
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	private static Logger logger = LoggerFactory
			.getLogger(WebSecurityConfig.class);

	@Bean
	public UserDetailsService userDetailsService() {
		return new UserDetailsServiceImpl();
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService());
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		logger.info("配置spring security start ....");
		// UsernamePasswordAuthenticationFilter
		http.authorizeRequests()
				.antMatchers("/login").permitAll()
		        .accessDecisionManager(accessDecisionManager())//配置投票系统
		        //.antMatchers("/success.html").hasRole("ADMIN") //设置访问success.html页面需要拥有ROLE_ADMIN角色,这个可以配置到数据库里面，使用自定义的投票器来处理
		        //.antMatchers("/index.html").permitAll();// 访问index.html不要权限验证
				.anyRequest().authenticated();// 其他所有路径都需要权限校验

        http.cors().configurationSource()
		http.csrf().disable();// 默认开启，这里先显式关闭
		http.formLogin() // 内部注册 UsernamePasswordAuthenticationFilter
				//.loginPage("/login.html") // 前后端分离项目，注释掉 配置使用自定义的表单登录页面地址
				.loginProcessingUrl("/login")// form表单POST请求url提交地址，默认为/login
				.passwordParameter("password")// form表单用户名参数名
				.usernameParameter("username") // form表单密码参数名
				// 登录成功跳转到成功处理器,这个只能访问handler method , 可以使用successHandler替换
				//.successForwardUrl("/login-success")
				// 登录失败跳转到失败处理器，这个只能访问handler method，可以使用failureHandler替换
				//.failureForwardUrl("/login-error")
				/*
				 * 如果successForwardUrl 和 defaultSuccessUrl 都配置了，后面会把前面的配置覆盖掉
				 * successForwardUrl ：服务器端跳转 defaultSuccessUrl ： 重定向，即客户端跳转
				 */
				//.defaultSuccessUrl("/index.html") // 如果用户没有访问受保护的页面，默认跳转到页面。如果用户访问了受保护的页面，则直接展示受保护页面
				//.failureUrl("/error.html")
				/*
				 * 如果failureForwardUrl和failureHandler都配置了，后面的配置会把前面的配置覆盖掉，
				 * 注意这与failureUrl不同。
				 * 思考：failureHandler与failureUrl有什么区别？failureUrl是跳转页面
				 * ，而failureHandler是跳转hanlder method
				 */
				.failureHandler(authFailureHandler())
				// 如果successForwardUrl 和
				// defaultSuccessUrl，以及successHandler都配置，后面配置会把前面的覆盖掉
				.successHandler(authSuccessHandler())
				// .failureUrl("/login?error")
				.permitAll();// 允许所有用户都有权限访问登录页面
		
		/*
		 * 设置一个账号同时只允许一个地点登录
		 * http://blog.csdn.net/lee353086/article/details/52537812
		 */
		http.sessionManagement()
		    .maximumSessions(1)////只允许一个用户登录,如果同一个账户两次登录,那么第一个账户将被踢下线,跳转到登录页面  
		    .expiredUrl("/login.html");//session失效后跳转 

		//认证异常处理
		http.exceptionHandling()
				.authenticationEntryPoint(httpForbiddenEntryPoint())//当用户请求了一个受保护的资源，但是用户没通过认证，那么抛出异常
				.accessDeniedHandler(accessDeniedHandler())
				.accessDeniedPage("/deny.html");//配置访问拒绝页面

		// 登出
		http.logout()
				.deleteCookies("JSESSIONID") //从cookie中删除JSESSIONID
				.invalidateHttpSession(true) //配置在登出的时候，使HttpSession无效
				.logoutSuccessHandler(logoutSuccessHandler()) //成功登出之后
				.permitAll();

	}

	// 认证成功handler
	@Bean
	AuthenticationSuccessHandler authSuccessHandler() {
		return new UserAuthenticationSuccessHandler();
	}

	// 认证失败handler
	@Bean
	AuthenticationFailureHandler authFailureHandler() {
		return new UserAuthenticationFailureHandler();
	}

	// 登出handler
	@Bean
	LogoutSuccessHandler logoutSuccessHandler() {
		return new UserLogoutSuccessHandler();
	}

	//异常处理
	@Bean
	public AuthenticationEntryPoint httpForbiddenEntryPoint() {
		return new HttpForbiddenEntryPoint();
	}

	//授权失败处理
	@Bean
	AccessDeniedHandler accessDeniedHandler() {
		return new UserAccessDeniedHandler();
	}
	
	//配置具体使用的投票系统,SpringSecurity内置了3个基于AccessDecisionManager实现的投票系统：AffirmativeBased、ConsensusBased和UnanimousBased。
	@Bean
	AccessDecisionManager accessDecisionManager(){
		//使用Spring Security内置的投票者, 使用自定义的投票者
		List<AccessDecisionVoter<? extends Object>> voters = Arrays.asList(new RoleVoter(), new CustomAccessDecisionVoter());
		return new UnanimousBased(voters);
	}

}
