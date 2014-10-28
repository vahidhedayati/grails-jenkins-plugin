package grails.plugin.jenkins

import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import grails.converters.JSON
import grails.util.Holders
import groovy.json.JsonBuilder
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseException
import groovyx.net.http.Method

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
	private String jensApi='/api/json'
	HTTPBuilder http

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
		//userSession.getUserProperties().put("server", server);
		//userSession.getUserProperties().put("job", job);
		jsessions.add(userSession)
	}

	@OnMessage
	public String handleMessage(String message,Session userSession) throws IOException {
		//def job=userSession.getUserProperties().get("job").toString()
		//def server=userSession.getUserProperties().get("server").toString()

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
				//				hideBuildTimer=data.hideBuildTimer  ?: 'no'
				jenkinsConnect(userSession)
			}

			if (cmd.toString().equals('viewHistory')) {
				if (data.bid) {
					clearPage(userSession)
					def url2=jenserver+data.bid+jensconlog
					parseJobConsole(userSession,url2,data.bid)
				}
			}

			if (cmd.toString().equals('stopBuild')) {
				if (data.bid) {
					def url2=jenserver+stripDouble(data.bid+'/stop')
					jobControl(userSession,url2,data.bid)
					dashboard(userSession)
				}
			}
			/*
			 if (cmd.toString().equals('histref')) {
			 if (!hideBuildTimer.equals('yes')) {
			 livedashboard(userSession)
			 }
			 }
			 */
			if (cmd.toString().equals('cancelJob')) {
				if (data.bid) {
					cancelJob(userSession,data.bid)
				}
			}


			if (cmd.toString().equals('choose')) {
				jenschoice=data.jenschoice ?: 'build'
				switch (jenschoice) {
					case 'build':
						clearPage(userSession)
						buildJob(userSession)
						break
					case 'dashboard':
						dashboard(userSession)
						break
					case 'disconnect':
						disconnect(userSession)
						break
					default:
						disconnect(userSession)
					//userSession.getBasicRemote().sendText(echoJob(server,job) as String)
				}
			}
		}
	}

	private void cancelJob(Session userSession,String bid){
		def url=jenserver
		//cancelJob(userSession,url,bid)
		jobControl(userSession,url+bid,bid)
	}

	private void disconnect(Session userSession) {
		jsessions.remove(userSession)
		userSession.close()
	}

	private void clearPage(Session userSession) {
		def json = new JsonBuilder()
		json {
			delegate.clearPage "true"
		}
		userSession.getBasicRemote().sendText(json.toString())
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



	private void parseJobConsole(Session userSession,String url,String bid) {
		try {
			// Send user confirmation that you are going to parse Jenkins job
			userSession.getBasicRemote().sendText("\nAbout to parse ${url}\n")
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

			if (html."**".find{ it.toString().contains("fetchNext")}) {

				// Get api output for the job
				def apiurl=bid+jensApi
				parseApi(userSession,apiurl)

				def nurl=jenserver+bid+jensprogressive
				userSession.getBasicRemote().sendText('Attempting live poll')
				//This hogs websocket connection - so lets background it
				def asyncProcess = new Thread({parseLiveLogs(userSession,nurl)} as Runnable )
				asyncProcess.start()
				//parseLiveLogs(userSession,nurl)


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

	def parseApi(Session userSession,String uri) {
		http.get( path: uri ) { resp, json ->
			userSession.getBasicRemote().sendText(json.toString())
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
			def http1 = new HTTPBuilder("${nurl}")
			http1.getClient().getParams().setParameter("http.connection.timeout", new Integer(httpConnTimeOut))
			http1.getClient().getParams().setParameter("http.socket.timeout", new Integer(httpSockTimeOut))
			if (!jensuser) {
				http1.auth.basic "${jensuser}", "${jenspass}"
			}
			while (hasMore) {

				// If there is a text-size header set then create url appender
				if (csize) {
					ssize="?start=${csize}"
				}
				// Set old size to current size
				def osize=csize

				// Now get the url which may or may not have current header size
				def url=nurl+ssize
				http1?.request("${url}",GET,TEXT) { req ->

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
		getBuilds(userSession,jensurl)
	}
	/*
	 private livedashboard(Session userSession) {
	 getLiveBuilds(userSession,jensurl)
	 }
	 */
	private buildJob(Session userSession) {
		String url=jensurl
		def url1=url+jensbuildend
		def lastbid=getLastBuild(url)
		int currentBuild=currentJob(lastbid)
		String consolelog=jensconlog
		//String consolelog='/consoleText'
		try {
			userSession.getBasicRemote().sendText("\nBefore triggering Build ID: "+currentBuild+"\n..waiting\n")
			jobControl(userSession, jenserver+url1,url)
			dashboard(userSession)
			def lastbid1=getLastBuild(url)
			def newBuild=currentJob(lastbid1)
			boolean go=false
			int a=0
			while ((go==false)&&(a<11)) {
				a++
				newBuild=currentJob(lastbid1)
				userSession.getBasicRemote().sendText(".")
				sleep(100)
				if (newBuild > currentBuild) {
					go=true
				}
			}
			dashboard(userSession)
			if (go) {
				userSession.getBasicRemote().sendText("New Build ID: "+newBuild+"\nAttempting to parse logs:\n")
				//String url2=url+"/"+lastbid1+consolelog
				String url2=stripDouble(lastbid1+consolelog)
				//dashboard(userSession)
				parseJobConsole(userSession,url2,lastbid1)
				//dashboard(userSession)
			}else{
				userSession.getBasicRemote().sendText("\nBuild trigger failed\n")
			}
			dashboard(userSession)

		} catch (Exception e) {
			e.printStackTrace()
		}
	}

	private void jobControl(Session userSession,String url,String bid) {
		try {
			//userSession.getBasicRemote().sendText("About to stop ${url}\n")
			http = new HTTPBuilder("${url}")
			http.getClient().getParams().setParameter("http.connection.timeout", new Integer(httpConnTimeOut))
			http.getClient().getParams().setParameter("http.socket.timeout", new Integer(httpSockTimeOut))
			if (!jensuser) {
				http.auth.basic "${jensuser}", "${jenspass}"
			}
			def html = http.post([:])
			dashboard(userSession)
		}catch (Exception ex){
			log.info("Failed error: ${ex}")
		}
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
	/*
	 private def getLiveBuilds(Session userSession,String url) {
	 try {
	 def finalList=[:]
	 def col3
	 def html = http.get( path : "${url}")
	 def bn=html."**".findAll {it.@class.toString().contains("build-name")}
	 if (bn) {
	 if (html."**".find {it.@class.toString().contains("progress-bar")}) {
	 col3 = html."**".find {it.@class.toString().contains("progress-bar")}.collect {
	 [
	 bid :verifyBuild(it.@href.text()),
	 bprogress : it.@tooltip.text()
	 ]
	 }
	 }
	 }else{
	 bn=html."**".find {it.@class.toString().contains("progress-bar ")}
	 if (bn) {
	 col3 = bn.collect {
	 [
	 bid :verifyBuild(it.@href.text()),
	 bprogress : it.@tooltip.text()
	 ]
	 }
	 }
	 }
	 finalList.put("historytop",col3)
	 userSession.getBasicRemote().sendText((finalList as JSON).toString())
	 }catch(Exception e) {
	 log.error "Problem communicating with ${url}: ${e.message}"
	 }
	 }
	 */	

	private def getBuilds(Session userSession,String url) {
		try {
			def finalList=[:]
			def hList=[]
			def col1,col2,col3
			def html = http.get( path : "${url}")

			// Both jenkins old/new
			def sbn=html."**".findAll {it.@class.toString().contains("stop-button-link")}
			if (sbn) {
				col3 = sbn.collect {
					[
						bid :verifyBuild(it.@href.text()),
						bstatus : verifyStatus(it.@href.text().toString()),
						jobid :  verifyJob(it.@href.text().toString())
					]
				}
			}

			// New jenkins
			def bd=html."**".findAll {it.@class.toString().contains("build-details")}
			if (bd) {
				col1 = bd.collect {
					[
						bid : it.A[0].@href.text(),
						bdate :it.toString().trim()
					]
				}

				def bn=html."**".findAll {it.@class.toString().contains("build-name")}
				if (bn) {
					col2 = bn.collect {
						[
							bid :verifyBuild(it.A[0].@href.text()),
							//bstatus : verifyStatus(it.A[0].IMG[0].@class.text().toString()),
							bstatus : verifyStatus(it.A[0].IMG.@src.text().toString()),
							//bimgUrl : it.A[0].IMG[0].@src.text(),
							jobid : it.toString().trim().replaceAll('[\n|\r|#|\t| ]+', '').replaceAll("\\s","")
						]
					}
				}

			}else{
				// Going to try parse older html styles - provided by a default jenkins install
				bd=html."**".findAll {it.@class.toString().contains("tip model-link")}
				if (bd) {
					col1 = bd.collect {
						[
							bid : it.@href.text(),
							bdate :it.toString().trim()
						]
					}
				}
				def bn=html."**".findAll {it.@class.toString().contains("build-row no-wrap")}
				if (bn) {
					col2=bn.collect {
						[
							bid: verifyBuild(it.TD[0].A.@href.text().toString()),
							bstatus : verifyStatus(it.TD[0].A.IMG.@src.text().toString()),
							jobid: verifyJob(it.TD[0].A.@href.text().toString()).replaceAll("\\s","")
						]
					}
				}
			}
			if (col3) {
				finalList=[:]
				finalList.put("historyQueue",col3)
				userSession.getBasicRemote().sendText((finalList as JSON).toString())
			}
			if (col1&&col2) {
				def combined = (col1 + col2).groupBy { it.bid }.collect { it.value.collectEntries { it } }
				finalList=[:]
				finalList.put("history",combined)
				userSession.getBasicRemote().sendText((finalList as JSON).toString())
			}
			
			
		}catch(Exception e) {
			log.error "Problem communicating with ${url}: ${e.message}"
		}
	}


	private def verifyBuild(String bid) {
		if (bid && bid.endsWith('/console')) {
			bid=bid.substring(0,bid.indexOf('/console')+1)
		}else if (bid&& bid.endsWith('/stop')) {
			bid=bid.substring(0,bid.indexOf('/stop')+1)
		}	
		return bid
	}


	private def getLastBuild(String url) {
		def bid="";
		try {
			int go=0;
			def bdate,transaction=''
			def html = http.get(path : "${url}")
			def bd=html."**".findAll {it.@class.toString().contains("build-details")}
			if (bd) {
				bd.each {
					go++
					if (go==1) {
						bid=it.A[0].@href.text()
						bdate=it.toString().trim()
					}
				}
			}else{
				bd=html."**".find { it.@class.toString().contains("tip model-link") }
				bd.each {
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

	private int currentJob(String job) {
		if (job.endsWith('/')) {
			job=job.substring(0,job.length()-1)
		}
		job=job.substring(job.lastIndexOf('/')+1,job.length())
		//if (job.isNumber()) {
		return job as int
		//}
		//return 0
	}
	private String verifyJob(String job) {
		if (job.endsWith('/')) {
			job=job.substring(0,job.length()-1)
		}
		if (job.indexOf('=')>-1) {
			job=job.substring(job.indexOf('=')+1,job.length())
		}else if (job.endsWith('/console')) {
			job=job.substring(0,job.indexOf('/console'))	
			job=job.substring(job.lastIndexOf('/')+1,job.length())
		}else if (job.endsWith('/stop')) {
			job=job.substring(0,job.indexOf('/stop'))
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

	private String seperator(def input) {
		if (input &&(!input.toString().startsWith('/'))) {
			input='/'+input
		}
		return input
	}

	private String verifyStatus(String img) {
		String output="unknown"
		if (img.contains("blue_anime")||(img.contains("aborted_anime"))||(img.contains("job"))&&(img.contains("stop"))) {
			output="building"
		}else if (img.contains("red")) {
			output="failed"
		}else if (img.contains("blue")) {
			output="passed"
		}else if ((img.contains("queue"))||(img.contains("cancelItem"))) {
			output="queued"
		}else if ((img.contains("grey"))||(img.contains("aborted"))) {
			output="cancelled"
		}
		return output
	}

}
