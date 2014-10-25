package grails.plugin.jenkins

import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import grails.util.Holders
import groovy.json.JsonBuilder
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseException
import grails.converters.JSON
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import javax.servlet.annotation.WebListener
import javax.websocket.EndpointConfig
import javax.websocket.OnMessage
import javax.websocket.OnOpen
import javax.websocket.Session
import javax.websocket.server.PathParam
import javax.websocket.server.ServerContainer
import javax.websocket.server.ServerEndpoint

@WebListener

@ServerEndpoint("/JenkinsEndPoint/{server}/{job}")
class JenkinsEndPoint implements ServletContextListener{
	private int httpConnTimeOut = 10*1000;
	private int httpSockTimeOut = 30*1000;
	static final Set<Session> jsessions = Collections.synchronizedSet(new HashSet<Session>())
	private String jensbuildend,jensprogressive,jensconlog,jensurl,jenserver,jensuser,jenspass,jenschoice,jensconurl=''
	HTTPBuilder http 
	//= new HTTPBuilder()
	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		final ServerContainer serverContainer =	org.codehaus.groovy.grails.web.context.ServletContextHolder.getServletContext().getAttribute("javax.websocket.server.ServerContainer")
		try {
			serverContainer?.addEndpoint(JenkinsEndPoint.class)
			def config=Holders.config
			int DefaultMaxSessionIdleTimeout=config.jenkins.timeout  ?: 0
			serverContainer.setDefaultMaxSessionIdleTimeout(DefaultMaxSessionIdleTimeout as int)
		} catch (IOException e) {
			e.printStackTrace()
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
	}

	@OnOpen
	public void whenOpening(Session userSession,EndpointConfig c,@PathParam("server") String server,@PathParam("job") String job) {
		userSession.getUserProperties().put("server", server);
		userSession.getUserProperties().put("job", job);
		jsessions.add(userSession)
	}

	@OnMessage
	public String handleMessage(String message,Session userSession) throws IOException {
		def job=userSession.getUserProperties().get("job").toString()
		def server=userSession.getUserProperties().get("server").toString()

		def data=JSON.parse(message)
		if (data) {
			def cmd=data.cmd
			if (cmd.toString().equals('connect')) {
				jenserver=data.jenserver
				jensurl=data.jensurl
				jensuser=data.jensuser
				jensconurl=data.jensconurl
				jenspass=data.jenspass
				jensconlog=data.jensconlog ?: '/consoleFull'
				jensprogressive=data.jensprogressive ?: '/logText/progressiveHtml'
				jensbuildend=data.jensbuildend  ?: '/build?delay=0sec'
				jenkinsConnect(userSession)
			}

			if (cmd.toString().equals('viewHistory')) {
				if (data.bid) {
					def url2=jenserver+data.bid+jensconlog
					ParseJobConsole(userSession,url2,data.bid)
				}
			}
			if (cmd.toString().equals('choose')) {
				jenschoice=data.jenschoice ?: 'build'
				switch (jenschoice) {
					case 'build':
						buildJob(userSession)
						dashboard(userSession)
						break
					case 'dashboard':
						dashboard(userSession)
						break
					default:
						userSession.getBasicRemote().sendText(echoJob(server,job) as String)
				}
			}
		}
	}

	private void jenkinsConnect(Session userSession) {
		def url=jensconurl
		def validurl=verifyUrl(url)
		if (validurl.toString().startsWith('Success')) {
			userSession.getBasicRemote().sendText('Jenkins plugin connected to: '+jensconurl)
		}else{
			userSession.getBasicRemote().sendText('JenkinsConnect failed to connect to: '+jensconurl)
		}
	}

	private String echoJob(String server,String job) {
		return server
	}

	private void ParseJobConsole(Session userSession,String url,String bid) {
		try {
			// Send user confirmation that you are going to parse Jenkins job
			userSession.getBasicRemote().sendText("About to parse ${url}\n")		
			def html = http.get(path :"${url}")
			boolean start=false
			boolean start1=false

			// If we have a class of console output then set start1 to true
			// This means the job has actually finished building and jenkins content is now static
			if (html."**".findAll{ it.@class.toString().contains("console-output")}) {
				start1=true
			}

			// However if we see fetchNext within the html then it is likely to be building
			// So lets attempt to connect to progressiveHtml page within jenkins
			if (html."**".findAll{ it.toString().contains("fetchNext")}) {
				//def nurl=murl+'/'+bid+'/logText/progressiveHtml'
				def nurl=jenserver+bid+jensprogressive
				userSession.getBasicRemote().sendText('Still building..Attempting live poll')
				parseLiveLogs(userSession,nurl)

				// Sendback liveUrl back through sockets
				// If user wishes they can interact with it this way
				// Due to CORS - the method was changed to use
				// httpbuilder this end to do remoteURL processing
				def json = new JsonBuilder()
				json {
					delegate.liveUrl "${nurl}"
				}
				userSession.getBasicRemote().sendText(json.toString())

			}

			//This means job has finished and jenkins has static results showing in /consoleFull
			if (start1) {
				html."**".findAll{ it.@class.toString().contains("console-output")}.each {
					userSession.getBasicRemote().sendText(it.toString())
				}
			}
		}catch(Exception e) {
			log.error "Problem communicating with ${url}: ${e.message}"
		}
	}

	// This is a httpbuilder module put in to imitate the ajaxrequest posted via jenkins
	// fetchNext javascript function, it grabs the current size returned from header:
	// X-Text-Size and pushes it back as start={value}
	// this ensures the response returned via websockets only contains
	//relevant chunk
	def parseLiveLogs(Session userSession,String nurl) {
		try {
			boolean hasMore=true
			def csize
			def consoleAnnotator
			String ssize=''
			// So while hasMore is true -
			// this again controlled via the jenkins html page which has it in header or not
			// goes away once full results are returned
			while (hasMore) {

				// If there is a text-size header set then create url appender
				if (csize) {
					ssize="?start=${csize}"
				}
				// Set old size to current size
				def osize=csize

				// Now get the url which may or may not have current header size
				def url=nurl+ssize
				//stripDouble(nurl+ssize)
				http?.request("${url}",GET,TEXT) { req ->

					// On success get latest output back from headers
					response.success = { resp, reader ->
						hasMore=resp.headers.'X-More-Data'
						csize=resp.headers.'X-Text-Size'
						consoleAnnotator=resp.headers.'X-ConsoleAnnotator'
						// If the current size is there and larger than osize value send to websocket
						if (csize && csize > osize) {
							userSession.getBasicRemote().sendText(reader.text as String)
						}
					}
				}
			}
		}catch(Exception e) {
			log.error "Problem communicating with ${nurl}: ${e.message}"
		}
	}

	private dashboard(Session userSession) {
		userSession.getBasicRemote().sendText("\nDashboard clicked.");
		//String url=jenserver+jensurl
		String url=jensurl
		String consolelog=jensconlog
		getBuilds(userSession,url)
	}
	
	private buildJob(Session userSession) {
		String url=jensurl
		def url1=url+jensbuildend
		def lastbid=getLastBuild(url)
		String consolelog=jensconlog
		//String consolelog='/consoleText'
		try {
			triggerBuild(jenserver+url1)
			userSession.getBasicRemote().sendText("\nBefore triggering Build ID: "+lastbid+"\nAttempting build ${url1}\n..sleeping for a few seconds\n")
			sleep(2000)
			def lastbid1=getLastBuild(url)
			userSession.getBasicRemote().sendText("\nTriggering build\nCurrent Build ID: "+lastbid1+"<br>")
			//String url2=url+"/"+lastbid1+consolelog
			String url2=stripDouble(lastbid1+consolelog)
			dashboard(userSession)
			ParseJobConsole(userSession,url2,lastbid1)
			dashboard(userSession)
		} catch (Exception e) {
			e.printStackTrace()
		}
	}
	
	private void triggerBuild(String url) {
		http = new HTTPBuilder("${url}")
		http.getClient().getParams().setParameter("http.connection.timeout", new Integer(httpConnTimeOut))
		http.getClient().getParams().setParameter("http.socket.timeout", new Integer(httpSockTimeOut))
		if (!jensuser) {
			http.auth.basic "${jensuser}", "${jenspass}"
		}
		def html = http.get([:])
	}

	private String verifyUrl(def nurl)  {
		def ret = [:]
		String result='Failed'
		try {
			boolean goahead=true
			try {
				http = new HTTPBuilder("${nurl}")
				http.getClient().getParams().setParameter("http.connection.timeout", new Integer(httpConnTimeOut))
				http.getClient().getParams().setParameter("http.socket.timeout", new Integer(httpSockTimeOut))
				if (!jensuser) {
					http.auth.basic "${jensuser}", "${jenspass}"
				}
			}catch (Exception ex){
				result="Failed error: ${ex}"
				return result
				goahead=false
			}catch (Throwable ex) {
				result="Failed error: ${ex}"
				return result
				goahead=false
			}catch ( HttpResponseException ex ) {
				result="Failed error: ${ex.statusCode}"
				return result
				goahead=false
			}
			if (goahead) {
				http?.request(GET,TEXT) { req ->
					response.success = { resp ->
						result="Success"
					}
					response.failure = { resp ->
						result="Failed error: ${resp.statusLine.statusCode}"
					}
				}
			}
		}catch ( HttpResponseException ex ) {
			result="Failed error: ${ex.statusCode}"
			return result
		}
		return result
	}

	private def getBuilds(Session userSession,String url) {
		try {
			int go=0;
			def bdate,bid,transaction=''
			int counter=0
			def finalList=[:]
			def hList=[]
			def col1
			def col2
			def html = http.get( path : "${url}")	
			if (html."**".findAll {it.@class.toString().contains("build-details")}) {
				col1 = html."**".findAll { it.@class.toString().contains("build-details") }
				.collect {
					[
						bid : it.A[0].@href.text(),
						bdate :it.toString().trim()
					]
				}
				if (html."**".findAll {it.@class.toString().contains("build-name")}) {
					col2 = html."**".findAll {it.@class.toString().contains("build-name")}.collect {
						[
							bid : it.A[0].@href.text().substring(0,it.A[0].@href.text().indexOf('/console')+1),
							//bstatus : verifyStatus(it.A[0].IMG[0].@class.text().toString()),
							bstatus : verifyStatus(it.A[0].IMG[0].@src.text().toString()),
							//bimgUrl : it.A[0].IMG[0].@src.text(),
							jobid : it.toString().trim().replaceAll('[\n|\r|#|\t| ]+', '')
						]
					}
				}
			}else{
				// Going to try parse uglier html styles - provided by a default jenkins install
				if (html."**".findAll {it.@class.toString().contains("tip model-link")}) {
					col1 = html."**".findAll { it.@class.toString().contains("tip model-link") }
					.collect {
						[
							bid : it.@href.text(),
							bdate :it.toString().trim()
						]
					}
				}

				if (html."**".findAll {it.@class.toString().contains("build-row no-wrap")}) {
					col2=html."**".findAll {it.@class.toString().contains("build-row no-wrap")}.collect {
						[
							bid: verifyBuild(it.TD[0].A.@href.text().toString()),
							bstatus : verifyStatus(it.TD[0].A.IMG.@src.text().toString()),
							jobid: verifyJob(it.TD[0].A.@href.text().toString())
						]
					}

				}

			}
			def combined = (col1 + col2).groupBy { it.bid }.collect { it.value.collectEntries { it } }
			finalList.put("history",combined)
			def myMsgj=finalList as JSON
			userSession.getBasicRemote().sendText(myMsgj.toString())
		}catch(Exception e) {
			log.error "Problem communicating with ${url}: ${e.message}"
		}
	}


	private def verifyBuild(String bid) {
		if (bid && (bid.indexOf('/console')>-1)) {
			bid=bid.substring(0,bid.indexOf('/console')+1)
		}
		return bid
	}


	private def getLastBuild(String url) {
		def bid="";
		try {
			int go=0;
			def bdate,transaction=''
			def html = http.get(path : "${url}")
			if (html."**".findAll {it.@class.toString().contains("build-details")}) {
				html."**".find { it.@class.toString().contains("build-details")}.each {
					go++
					if (go==1) {
						bid=it.A[0].@href.text()
						bdate=it.toString().trim()
					}
				}
			}else{
				html."**".findAll { it.@class.toString().contains("tip model-link") }.each {
					go++
					if (go==1) {
						bid=it.@href.text()
						bdate=it.toString().trim()
					}
				}
			}

		}catch(Exception e) {
			log.error "Problem communicating with ${url}: ${e.message}"
		}
		return  bid
	}
	
	private String verifyJob(String job) {
		if (job.indexOf('/')>-1) {
			job=job.substring(0,job.indexOf('/console'))
			if (job.endsWith('/')) {
				job=job.substring(0,job.length()-1)
			}
			job=job.substring(job.lastIndexOf('/')+1,job.length())
		}
		return job
	}
	
	private String stripDouble(String url) {
		if (url.indexOf('//')>-1) {
			url=url.replace('//','/')
		}
		return url
	}
	
	private String verifyStatus(String img) {
		String output="unknown"
		if (img.contains("blue_anime")) {
			output="building"
		}else if (img.contains("red")) {
			output="failed"
		}else if (img.contains("blue")) {
			output="passed"
		}else if ((img.contains("grey"))||(img.contains("aborted"))) {
			output="cancelled"
		}
		return output
	}

}
