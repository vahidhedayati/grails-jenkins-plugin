package grails.plugin.jenkins

import grails.core.GrailsApplication
import grails.core.support.GrailsApplicationAware


class JenJirConfService implements GrailsApplicationAware {

	static transactional = false

	def config
	GrailsApplication grailsApplication

	// Jira config items
	String getJiraServer() {
		return config.jiraServer
	}

	String getJiraUser() {
		return config.jiraUser
	}

	String getJiraPass () {
		return config.jiraPass
	}

	String getJiraSendType() {
		return config.jiraSendType
	}

	String getJiracustomField() {
		return config.customField
	}


	boolean isConfigEnabled(String config) {
		return Boolean.valueOf(config ?: false)
	}

	int getHttpConnTimeOut() {
		return config.http.connection.timeout ?: 10
	}

	int getHttpSockTimeOut() {
		return config.http.socket.timeout ?: 30
	}

	void setGrailsApplication(GrailsApplication ga) {
		config = ga.config.jenkins
	}
}
