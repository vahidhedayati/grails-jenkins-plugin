package jenjir

import grails.plugin.jenkins.JenkinsEndPoint
import org.springframework.boot.context.embedded.ServletContextInitializer
import org.springframework.context.annotation.Bean

import javax.servlet.ServletContext
import javax.servlet.ServletException

class DefaultJenjirCfg {

	@Bean
	public ServletContextInitializer myInitializer() {
		return new ServletContextInitializer() {
			@Override
			public void onStartup(ServletContext servletContext) throws ServletException {
				servletContext.addListener(JenkinsEndPoint)
			}
		}
	}

}
