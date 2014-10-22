package grails.plugin.jenkins

import javax.swing.text.html.HTML

class JenkinsController {
	
	def index() { 
		
	}
	
	def process() {
		
		String jenserver=params.jenserver
		String jensport=params.jensport
		String jensuser=params.jensuser
		String jenspass=params.jenspass
		String jensjob=params.jensjob
		String jensfolder=params.jensfolder
		String jenschoice=params.jenschoice
		[jenserver:jenserver,jensport:jensport,jensuser:jensuser,
		jenspass:jenspass,jensfolder:jensfolder,jensjob:jensjob,jenschoice:jenschoice]
		
		
	}

}
