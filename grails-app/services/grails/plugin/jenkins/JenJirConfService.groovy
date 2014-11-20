package grails.plugin.jenkins


class JenJirConfService {
	
	static transactional = false
	
	def grailsApplication
		
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

	def getConfig() {
		grailsApplication.config.jenkins
	}
}
