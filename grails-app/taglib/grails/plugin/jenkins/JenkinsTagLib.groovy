package grails.plugin.jenkins

class JenkinsTagLib {
     static namespace = "jenkins"
   def grailsApplication
   
   // Logics to produce requirements put into taglib, controller page loads in taglib
   // saves writing twice and now end user can interact directly via taglib calls
   def connect =  { attrs, body ->
	   def template=attrs.remove('template')?.toString()
	   def divId=attrs.remove('divId')?.toString()
	   def jensuser=attrs.remove('jensuser')?.toString()
	   def jenspass=attrs.remove('jenspass')?.toString()
	   def jenspecial=attrs.remove('jenspecial')?.toString()
	   if (!attrs.jensjob) {
		   throwTagError("Tag [jenkins] is missing required attribute [jensjob]")
	   }
	   
	   // websocket url
	   def wshostname=attrs.wshostname ?: grailsApplication.config.jenkins.wshostname ?: 'localhost:8080'
	   
	   String jenserver=attrs.jenserver ?: 'localhost'
	   
	   //jenkins server
	   StringBuilder jenurl=new StringBuilder()
	   if (!jenserver.startsWith('http')) {
		   jenurl.append('http://'+jenserver)
	   }else {
	   		jenurl.append(jenserver)
	   }
	   
	   // If you defined a port append to url
	   if (attrs.jensport) {
		   jenurl.append(':'+attrs.jensport)
	   }
	   // Full url to jenkins server - main url
	   String jenfullserver=jenurl.toString()
	   
	   if (jenspecial) {
		   jenurl.append('/'+jenspecial)
	   }
	   // Full url to jenkins server - main url
	   String jensconurl=jenurl.toString()
	   
	   
	   
	   String jensjob=attrs.jensjob
	   String jensfolder=attrs.jensfolder ?: 'job'
	   // Relative UrI to get to folder/job (can now be appended to above) 
	   String jensurl='/'+jenspecial+'/'+jensfolder+'/'+jensjob+'/'
	   
	   // Configuration uris.
	   //Progressive page
	   // Default: '/logText/progressiveHtml'
	   String jensprogressive=attrs.jensprogressive ?: grailsApplication.config.jenkins.progressiveuri ?: '/logText/progressiveHtml'
	   
	   
	   //Ending uri for when building 
	   // Default: '/build?delay=0sec'
	   String jensbuildend=attrs.jensbuildend ?: grailsApplication.config.jenkins.buildend ?:  '/build?delay=0sec'
	   
	   // jens uri to get to full logs:
	   // Default: '/consoleFull'
	   String jensconlog=attrs.jensLog ?: grailsApplication.config.jenkins.consoleLog ?: '/consoleFull'
	   

	   
	   String appname=grailsApplication.metadata['app.name']
	   if (template) {
		   out << g.render(template:template, model: [divId:divId,jenfullserver:jenfullserver,jensconurl:jensconurl, jensjob:jensjob, jensuser:jensuser,jenspass:jenspass,appname:appname,wshostname:wshostname,jenserver:jenserver,jensurl:jensurl,jensprogressive:jensprogressive, jensbuildend:jensbuildend, jensconlog:jensconlog])
	   }else{   
	   		out << g.render(contextPath: pluginContextPath, template : '/jenkins/process', model: [divId:divId,jenfullserver:jenfullserver,jensconurl:jensconurl, jensjob:jensjob, jensuser:jensuser,jenspass:jenspass,appname:appname,wshostname:wshostname,jenserver:jenserver,jensurl:jensurl,jensprogressive:jensprogressive, jensbuildend:jensbuildend, jensconlog:jensconlog])
	   }
   }
   

}
