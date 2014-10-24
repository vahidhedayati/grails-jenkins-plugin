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
	private String jensbuildend,jensprogressive,jensconlog,jensurl,jenserver,jensuser,jenspass,jenschoice=''
	HTTPBuilder http = new HTTPBuilder()
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
		def validurl=verifyUrl(jenserver)
		if (validurl.toString().startsWith('Success')) {
			userSession.getBasicRemote().sendText('Jenkins plugin connected to: '+jenserver)
		}else{
			userSession.getBasicRemote().sendText('JenkinsConnect failed to connect to: '+jenserver)
		}
	}

	private String echoJob(String server,String job) {
		return server
	}


	private void ParseJobConsole(Session userSession,String url,String bid) {
		//String murl
		try {
			// Send user confirmation that you are going to parse Jenkins job
			userSession.getBasicRemote().sendText("About to parse "+url+"<br>")
			http = new HTTPBuilder("${url}")
			http.getClient().getParams().setParameter("http.connection.timeout", new Integer(httpConnTimeOut))
			http.getClient().getParams().setParameter("http.socket.timeout", new Integer(httpSockTimeOut))
			if (!jensuser) {
				http.auth.basic "${jensuser}", "${jenspass}"
			}
			def html = http.get([:])
			//def html = http.get(path :url)
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
				http?.request("${nurl}${ssize}",GET,TEXT) { req ->

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
		userSession.getBasicRemote().sendText("\n\n\n\n\nDashboard ");
		String url=jenserver+jensurl
		String consolelog=jensconlog
		getBuilds(userSession,url)
	}
	private buildJob(Session userSession) {
		String url=jenserver+jensurl
		def url1=url+jensbuildend
		def lastbid=getLastBuild(url)
		StringBuilder sb=new StringBuilder()
		String consolelog=jensconlog
		//String consolelog='/consoleText'
		sb.append("Before triggering Build ID: "+lastbid+"..sleeping for a few seconds<br>")
		sleep(300)
		//dashboard(userSession)
		// This kicks off the build
		try {
			http = new HTTPBuilder("${url1}")
			http.getClient().getParams().setParameter("http.connection.timeout", new Integer(httpConnTimeOut))
			http.getClient().getParams().setParameter("http.socket.timeout", new Integer(httpSockTimeOut))
			if (!jensuser) {
				http.auth.basic "${jensuser}", "${jenspass}"
			}

			def html = http.get([:])
			def lastbid1=getLastBuild(url)
			userSession.getBasicRemote().sendText("\n\n\n\n\nTriggering build\n\n\nCurrent Build ID: "+lastbid1+"<br>")
			//String url2=url+"/"+lastbid1+consolelog
			String url2=jenserver+"/"+lastbid1+consolelog
			dashboard(userSession)
			ParseJobConsole(userSession,url2,lastbid1)
			dashboard(userSession)
		} catch (Exception e) {
			e.printStackTrace()
		}
		return sb.toString()

	}

	private String verifyUrl(def nurl)  {
		def ret = [:]
		String result='Failed'
		if (nurl.toString().indexOf('[')>-1) {
			return result
		}
		try {
			if (nurl.toString().indexOf(' ')>-1) {
				nurl=nurl.toString().replace(' ','_')
			}
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
			http = new HTTPBuilder(url)
			http.getClient().getParams().setParameter("http.connection.timeout", new Integer(httpConnTimeOut))
			http.getClient().getParams().setParameter("http.socket.timeout", new Integer(httpSockTimeOut))
			if (!jensuser) {
				http.auth.basic "${jensuser}", "${jenspass}"
			}
			def html = http.get([:])
			int counter=0
			def finalList=[:]
			def hList=[]


			def col1 = html."**".findAll { it.@class.toString().contains("build-details")}
			.collect {
				[
					bid : it.A[0].@href.text(),

					bdate :it.toString().trim()
				]
			}

			def col2 = html."**".findAll { it.@class.toString().contains("build-name")}
			.collect {
				[
					bid : it.A[0].@href.text().substring(0,it.A[0].@href.text().indexOf('/console')+1),
					bstatus : verifyStatus(it.A[0].IMG[0].@class.text().toString()),
					bimgUrl : it.A[0].IMG[0].@src.text(),
					jobid : it.toString().trim().replaceAll('[\n|\r|#|\t| ]+', '')
				]
			}
			def combined = (col1 + col2).groupBy { it.bid }.collect { it.value.collectEntries { it } }
			finalList.put("history",combined)
			def myMsgj=finalList as JSON
			userSession.getBasicRemote().sendText(myMsgj.toString())

		}catch(Exception e) {
			log.error "Problem communicating with ${url}: ${e.message}"
		}
	}


	private def verifyStatus(String img) {
		def output
		if (img.contains("icon-blue-anime")) {
			output="building"
		}else if (img.contains("icon-red")) {
			output="failed"
		}else if (img.contains("icon-blue")) {
			output="passed"
		}

		return output
	}
	private def getLastBuild(String url) {
		def bid="";
		try {
			int go=0;
			def bdate,transaction=''
			http = new HTTPBuilder(url)
			http.getClient().getParams().setParameter("http.connection.timeout", new Integer(httpConnTimeOut))
			http.getClient().getParams().setParameter("http.socket.timeout", new Integer(httpSockTimeOut))
			if (!jensuser) {
				http.auth.basic "${jensuser}", "${jenspass}"
			}
			def html = http.get([:])
			html."**".find { it.@class.toString().contains("build-row no-wrap") || it.@class.toString().contains("pane build-details")}.each {
				go++
				if (go==1) {
					bid=it.A[0].@href.text()
					bdate=it.toString().trim()
				}
			}
		}catch(Exception e) {
			log.error "Problem communicating with ${url}: ${e.message}"
		}
		return  bid
	}
}
