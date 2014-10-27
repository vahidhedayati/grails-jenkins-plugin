package grails.plugin.jenkins


class JenkinsController {
	
	def grailsApplication
	
	def index() { 
		def hideLoginPage=grailsApplication.config.jenkins.hideLoginPage
		
		if (hideLoginPage && hideLoginPage.toLowerCase().equals('yes')) {
			render "Index page disabled"
			return
		}
		
	}
	
	def process() {		
		String jenserver=params.jenserver
		String jensport=params.jensport
		String jensuser=params.jensuser
		String jenspass=params.jenspass
		String jensjob=params.jensjob
		String jensfolder=params.jensfolder
		String jensprefix=params.jensprefix
		String jenschoice=params.jenschoice
		
		def hideButtons=grailsApplication.config.jenkins.hideButtons ?: 'no'
		def hideTriggerButton=grailsApplication.config.jenkins.hideTriggerButton ?: 'no'
		def hideDashBoardButton=grailsApplication.config.jenkins.hideDashBoardButton ?: 'no'
		def hideBuildTimer=grailsApplication.config.jenkins.hideBuildTimer ?: 'no'
		
		[hideButtons:hideButtons,hideTriggerButton:hideTriggerButton, hideDashBoardButton:hideDashBoardButton, hideBuildTimer:hideBuildTimer,jenserver:jenserver,jensport:jensport,jensuser:jensuser,
		jenspass:jenspass,jensprefix:jensprefix,jensfolder:jensfolder,jensjob:jensjob,jenschoice:jenschoice]
	}

}
