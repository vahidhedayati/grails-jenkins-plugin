import grails.plugin.jenkins.JenkinsEndPoint

class JenjirGrailsPlugin {
	def version = "0.6"
	def grailsVersion = "2.0 > *"
	def title = "Jenjir plugin"
	def description = 'Plugin to interact with Jenkins : do real time builds. View build version history of a given job, derive summary information. Push Summary to Jira.'
	def documentation = "https://github.com/vahidhedayati/grails-jenkins-plugin"
	def license = "APACHE"
	def developers = [name: 'Vahid Hedayati', email: 'badvad@gmail.com']
	def issueManagement = [system: 'GITHUB', url: 'https://github.com/vahidhedayati/grails-jenkins-plugin/issues']
	def scm = [url: 'https://github.com/vahidhedayati/grails-jenkins-plugin']
	
	def doWithWebDescriptor = { xml ->
		def listenerNode = xml.'listener'
		listenerNode[listenerNode.size() - 1] + {
			'listener' {
				'listener-class'(JenkinsEndPoint.name)
			}
		}
	}
}
