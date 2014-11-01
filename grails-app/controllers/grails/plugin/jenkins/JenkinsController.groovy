package grails.plugin.jenkins

class JenkinsController {

	def index() {
		def hideLoginPage = config.hideLoginPage

		if (hideLoginPage && hideLoginPage.toLowerCase().equals('yes')) {
			render "Index page disabled"
		}
	}

	def process(String jenserver, String jensport, String jensuser, String jenspass, String jensjob,
	            String jensfolder, String jensprefix, String jenschoice) {

		def hideButtons = config.hideButtons ?: 'no'
		def hideTriggerButton = config.hideTriggerButton ?: 'no'
		def hideDashBoardButton = config.hideDashBoardButton ?: 'no'

		[hideButtons:hideButtons, hideTriggerButton:hideTriggerButton, hideDashBoardButton:hideDashBoardButton,
	    jenserver:jenserver, jensport:jensport, jensuser:jensuser, jenspass:jenspass, jensprefix:jensprefix,
	    jensfolder:jensfolder, jensjob:jensjob, jenschoice:jenschoice]
	}

	private getConfig() {
		grailsApplication.config.jenkins
	}
}
