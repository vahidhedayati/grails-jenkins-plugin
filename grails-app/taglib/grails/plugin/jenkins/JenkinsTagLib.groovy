package grails.plugin.jenkins

import grails.util.Metadata

class JenkinsTagLib {

	static namespace = "jen"

	def grailsApplication
	def jenkinsService

	// Logics to produce requirements put into taglib, controller page loads in taglib
	// saves writing twice and now end user can interact directly via taglib calls
	def connect = { attrs ->

		if (!attrs.jensjob) {
			throwTagError("Tag [jenkins] is missing required attribute [jensjob]")
		}

		String template = attrs.remove('template')
		String divId = attrs.remove('divId')
		String jensuser = attrs.remove('jensuser')
		String jenspass = attrs.remove('jenspass')
		String jenschoice = attrs.remove('jenschoice')

		//String jensprefix = attrs.remove('jensprefix')

		def config = grailsApplication.config.jenkins

		// websocket url
		String wshostname = attrs.wshostname ?: config.wshostname ?: 'localhost:8080'

		String jenserver = attrs.jenserver ?: 'localhost'

		//jenkins server
		StringBuilder jenurl = new StringBuilder()
		if (jenserver.startsWith('http')) {
			jenurl.append(jenserver)
		}else {
			jenurl.append('http://').append(jenserver)
		}

		// If you defined a port append to url
		if (attrs.jensport) {
			jenurl.append(':').append(attrs.jensport)
		}
		// Full url to jenkins server - main url
		String jenfullserver = jenurl
		String jensprefix = ''
		if (attrs.jensprefix) {
			jensprefix = jenkinsService.seperator(attrs?.jensprefix)
			jenurl.append(jensprefix)
		}
		// Full url to jenkins server - main url
		String jensconurl = jenurl

		//String jensjob = attrs.jensjob
		//String jensfolder = attrs.jensfolder ?: 'job'
		// Relative UrI to get to folder/job (can now be appended to above)
		String jensurl = jensprefix + jenkinsService.seperator(attrs?.jensfolder) + jenkinsService.seperator(attrs?.jensjob)

		// Configuration uris.
		//Progressive page
		// Default: '/logText/progressiveHtml'
		String jensprogressive = attrs.jensprogressive ?: config.progressiveuri ?: '/logText/progressiveHtml'

		//Ending uri for when building
		// Default: '/build?delay=0sec'
		String jensbuildend = attrs.jensbuildend ?: config.buildend ?:  '/build?delay=0sec'

		// jens uri to get to full logs:
		// Default: '/consoleFull'
		String jensconlog = attrs.jensLog ?: config.consoleLog ?: '/consoleFull'

		String hideButtons = attrs.hideButtons ?: config.hideButtons ?: 'no'
		String hideTriggerButton = attrs.hideTriggerButton ?: config.hideTriggerButton ?: 'no'
		String hideDashBoardButton = attrs.hideDashBoardButton ?: config.hideDashBoardButton ?: 'no'
		// String hideBuildTimer = attrs.hideBuildTimer ?: config.hideBuildTimer ?: 'no'


		//println "--->> ${jenfullserver} --- ${jensconurl} "
		String appname = Metadata.current.getApplicationName()
		def model = [hideButtons:hideButtons, hideTriggerButton:hideTriggerButton, hideDashBoardButton:hideDashBoardButton,
		             jenschoice:jenschoice, divId:divId, jenfullserver:jenfullserver, jensconurl:jensconurl,
		             jensjob:attrs.jensjob, jensuser:jensuser, jenspass:jenspass, appname:appname, wshostname:wshostname,
		             jenserver:jenserver, jensurl:jensurl, jensprogressive:jensprogressive, jensbuildend:jensbuildend,
		             jensconlog:jensconlog]
		if (template) {
			out << g.render(template:template, model: model)
		}else{
			out << g.render(contextPath: pluginContextPath, template: '/jenkins/process', model: model)
		}
	}

	def dirconnect = {attrs ->

		if (!attrs.jensjob) {
			throwTagError("Tag [jenkins] is missing required attribute [jensjob]")
		}

		String jensurl = attrs.remove('jensurl')
		String jenserver = url.host
		String jensuser = attrs.remove('jensuser')
		String jenspass = attrs.remove('jenspass')

		String validurl = jenkinsService.verifyUrl(jensurl, jenserver, jensuser, jenspass)
		if (!validurl.startsWith('Success')) {
			return
		}

		def url = new URL(jensurl)
		String template = attrs.remove('template')
		String divId = attrs.remove('divId')
		String jenschoice = attrs.remove('jenschoice')
		String jensauthority = url.authority
		String jenspath = url.path
		String jensprot = url.protocol
		//String jensport = url.port

		String jensconurl = jensprot + '://' + jensauthority

		def config = grailsApplication.config.jenkins

		def wshostname = attrs.wshostname ?: config.wshostname ?: 'localhost:8080'
		String jensprogressive = attrs.jensprogressive ?: config.progressiveuri ?: '/logText/progressiveHtml'
		String jensbuildend = attrs.jensbuildend ?: config.buildend ?:  '/build?delay=0sec'
		String jensconlog = attrs.jensLog ?: config.consoleLog ?: '/consoleFull'
		String hideButtons = attrs.hideButtons ?: config.hideButtons ?: 'no'
		String hideTriggerButton = attrs.hideTriggerButton ?: config.hideTriggerButton ?: 'no'
		String hideDashBoardButton = attrs.hideDashBoardButton ?: config.hideDashBoardButton ?: 'no'
		String appname = Metadata.current.getApplicationName()

		def model = [hideButtons:hideButtons, hideTriggerButton:hideTriggerButton, hideDashBoardButton:hideDashBoardButton,
		             jenschoice:jenschoice, divId:divId, jenfullserver:jensconurl, jensconurl:jensconurl, jensjob:attrs.jensjob,
		             jensuser:jensuser, jenspass:jenspass, appname:appname, wshostname:wshostname, jenserver:jenserver,
		             jensurl:jenspath, jensprogressive:jensprogressive, jensbuildend:jensbuildend, jensconlog:jensconlog]
		if (template) {
			out << g.render(template:template, model: model)
		}else{
			out << g.render(contextPath: pluginContextPath, template: '/jenkins/process', model: model)
		}
	}
}
