package grails.plugin.jenkins

import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import grails.converters.JSON
import groovy.json.JsonBuilder
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient

import javax.servlet.ServletContext
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

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.web.context.ServletContextHolder as SCH
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes as GA
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@WebListener
@ServerEndpoint("/JenkinsEndPoint/{server}/{job}")
class JenkinsEndPoint implements ServletContextListener {

	private final Logger log = LoggerFactory.getLogger(getClass().name)

	private JenService jenService
	private GrailsApplication grailsApplication
	private int httpConnTimeOut = 10*1000
	private int httpSockTimeOut = 30*1000

	static final Set<Session> jsessions = ([] as Set).asSynchronized()

	private String customParams,jensbuildend, jensprogressive, jensconlog, jensurl, jenserver, jensuser,jenspass,jenschoice,jensconurl = ''
	private String jensApi = '/api/json'
	private String userbase = '/user/'
	private String userend = '/configure'

	RESTClient http

	void contextInitialized(ServletContextEvent event) {
		ServletContext servletContext = event.servletContext
		final ServerContainer serverContainer = servletContext.getAttribute("javax.websocket.server.ServerContainer")
		try {
			// Adding this conflicts with listener added via plugin descriptor
			// Whilst it works as run-app - in production this causes issues
			//serverContainer.addEndpoint(JenkinsEndPoint)

			def ctx = servletContext.getAttribute(GA.APPLICATION_CONTEXT)

			//jenkinsService = ctx.jenkinsService

			grailsApplication = ctx.grailsApplication

			def config = grailsApplication.config
			int defaultMaxSessionIdleTimeout = config.jenkins.timeout ?: 0
			serverContainer.defaultMaxSessionIdleTimeout = defaultMaxSessionIdleTimeout
		}
		catch (IOException e) {
			log.error e.message, e
		}
	}

	void contextDestroyed(ServletContextEvent event) {
	}

	@OnOpen
	void whenOpening(Session userSession, EndpointConfig c, @PathParam("server") String server, @PathParam("job") String job) {
		userSession.userProperties.server = server
		userSession.userProperties.job = job
		jsessions.add(userSession)
		
		def ctx= SCH.servletContext.getAttribute(GA.APPLICATION_CONTEXT)
		jenService = ctx.jenService
		
		grailsApplication= ctx.grailsApplication
	}

	@OnMessage
	String handleMessage(String message, Session userSession) throws IOException {
		//def job = userSession.userProperties.get("job").toString()
		//def server = userSession.userProperties.get("server").toString()
		def data = JSON.parse(message)
		if (!data) {
			return
		}

		String cmd = data.cmd
		if (cmd.equals('connect')) {
			jenserver = data.jenserver
			jensurl = data.jensurl
			jensuser = data.jensuser
			jensconurl = data.jensconurl
			jenspass = data.jenspass
			customParams = data.customParams
			jensconlog = data.jensconlog ?: '/consoleFull'
			jensprogressive = data.jensprogressive ?: '/logText/progressiveHtml'
			jensbuildend = data.jensbuildend  ?: '/build?delay=0sec'
			jenkinsConnect(userSession)
		}

		if (cmd.equals('viewHistory')) {
			if (data.bid) {
				clearPage(userSession)
				String url2 = jenserver+data.bid+jensconlog
				parseJobConsole(userSession,url2,data.bid)
			}
		}

		if (cmd.equals('stopBuild')) {
			if (data.bid) {
				jenService.jobControl(userSession,jenService.stripDouble(data.bid+'/stop'),data.bid,jenserver, jensuser, jenspass)
				dashboard(userSession)
			}
		}

		if (cmd.equals('cancelJob')) {
			if (data.bid) {
				cancelJob(userSession,data.bid)
			}
		}

		if (cmd.equals('choose')) {
			jenschoice = data.jenschoice ?: 'build'
			switch (jenschoice) {
				case 'build':
					clearPage(userSession)
					buildJob(userSession)
					break
				case 'dash':
					dashboard(userSession)
					break
				case 'dashboard':
					dashboardButton(userSession)
					break
				default:
					disconnect(userSession)
			}
		}
	}

	private void cancelJob(Session userSession,String bid){
		jenService.jobControl(userSession,bid,bid,jenserver, jensuser, jenspass)
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
		userSession.basicRemote.sendText(json.toString())
	}

	private void jenkinsConnect(Session userSession) {
		
		String validurl = jenService.verifyUrl(jensconurl, jenserver, jensuser, jenspass)
		if (!validurl.startsWith('Success')) {
			userSession.basicRemote.sendText('JenkinsConnect failed to connect to: '+jensconurl)
			return
		}
			http = jenService.httpConn(jenserver, jensuser, jenspass)

		userSession.basicRemote.sendText('Jenkins plugin connected to: ' + jensconurl)
		// try to get apiToken if only user has provided
		if (jensuser && !jenspass) {
			jenspass = jenService.returnToken(jensuser, jenserver, userbase, userend)
		}
	}

	private String echoJob(String server,String job) {
		return server
	}

	private void parseJobConsole(Session userSession, String url, String bid) {
		// Send user confirmation that you are going to parse Jenkins job
		userSession.basicRemote.sendText("\nAbout to parse ${url}\n")
		HttpResponseDecorator html1 = jenService.httpConn('get', jenserver, url, jensuser, jenspass)
		def html = html1?.data
		boolean start, start1 = false
		// If we have a class of console output then set start1 to true
		// This means the job has actually finished building and jenkins content is now static
		if (html."**".findAll{ it.@class.toString().contains("console-output")}) {
			start1 = true
		}

		// However if we see fetchNext within the html then it is likely to be building
		// So lets attempt to connect to progressiveHtml page within jenkins
		if (html."**".find{ it.toString().contains("fetchNext") ||it.@id.toString().contains("out")}) {

			// Get api output for the job
			parseApi(userSession,bid + jensApi)

			//This hogs websocket connection - so lets background it
			def asyncProcess = new Thread({parseLiveLogs(userSession, bid + jensprogressive)} as Runnable)
			asyncProcess.start()
			//parseLiveLogs(userSession,bid+jensprogressive)

			// Sendback liveUrl back through sockets
			// If user wishes they can interact with it this way
			// Due to CORS - the method was changed to use
			// httpbuilder this end to do remoteURL processing
			//def json = new JsonBuilder()
			//json {
			//	delegate.liveUrl "${jenserver}${url}"
			//}
			//userSession.asyncRemote.sendText(json.toString())
		}

		//This means job has finished and jenkins has static results showing in /consoleFull
		if (start1) {
			html."**".findAll{ it.@class.toString().contains("console-output")}.each {
				userSession.basicRemote.sendText(it.toString())
			}
		}
	}
	private dashboardButton(Session userSession) {
		clearPage(userSession)
		def job = userSession.userProperties.get("job").toString()
		def server = userSession.userProperties.get("server").toString()
		userSession.basicRemote.sendText("""
Welcome to Grails Jenkins Plugin
You are connected to: $job 
Running on Jenkins Host: $server
""")
		
		getBuilds(userSession, jensurl)
	}
	private dashboard(Session userSession) {
		getBuilds(userSession, jensurl)
	}

	private int currentBuildId(String url) {
		def lastbid = getLastBuild(url)
		//int currentBuild = jenkinsService.currentJob(lastbid)
		if (lastbid) {
			jenService.currentJob(lastbid) as int
		}
	}
	

	
	private buildJob(Session userSession) {
		String url  = jensurl
		def url1 = url + jensbuildend
		
		int currentBuild = currentBuildId(url)
		
		String consolelog = jensconlog
		try {
			userSession.basicRemote.sendText("\nBefore triggering Build ID: $currentBuild\n..waiting\n")
			jenService.jobControl(userSession, url1, url,jenserver, jensuser, jenspass)
			boolean go = false
			int a = 0
			def lastbid1, newBuild
			while (!go && a < 6) {
				a++
				//newBuild = currentBuildId(url)
				lastbid1 = getLastBuild(url)
				if (lastbid1) {
					newBuild = jenService.currentJob(lastbid1)
					userSession.basicRemote.sendText("[${newBuild}].")
					sleep(1000)
					if (newBuild > currentBuild) {
						
						/*
						* Get hold of config from grails if we have a processurl
						* a url defined upon success of a jenkins job that receives values 
						* and does something with built jobs 
						*/
						def config = grailsApplication.config
						String processurl = config.jenkins.processurl
						String wsprocessurl = config.jenkins.wsprocessurl
						String wsprocessname = config.jenkins.wsprocessname
						if ((wsprocessurl||processurl) && currentBuild) {
							//This hogs websocket connection - so lets background it
							def asyncProcess = new Thread({jenService.workOnBuild(userSession,processurl,wsprocessurl,wsprocessname,newBuild,url,jenserver, jensuser, jenspass,customParams,jensurl,jensApi)} as Runnable)
							//def asyncProcess = new Thread({jenService.workOnBuild(null,processurl,newBuild,url,jenserver, jensuser, jenspass,customParams,jensurl,jensApi)} as Runnable)
							asyncProcess.start()
						}
						
						go = true
					}
				}
				else {
					a--;
				}
			}
			dashboard(userSession)
			if (go) {
				userSession.basicRemote.sendText("New Build ID: $newBuild\nAttempting to parse logs:\n")
				sleep(3000)
				String url2 = jenService.stripDouble(lastbid1 + consolelog)
				parseJobConsole(userSession, url2, lastbid1)
			}
			else {
				userSession.basicRemote.sendText("\nBuild triggered failed to capture it running")
			}
			dashboard(userSession)
		}
		catch (e) {
			log.error e.message, e
		}
	}


	private getBuilds(Session userSession,String url) {
		try {
			def finalList = [:]
			def hList = []
			def col1,col2,col3
			HttpResponseDecorator html1 = http.get(path: "$url")
			def html = html1?.data
			def sbn = html."**".findAll {it.@class.toString().contains("stop-button-link")}
			if (sbn) {
				col3 = sbn.collect {
					[
						bid: jenService.verifyBuild(it.@href.text()),
						bstatus : jenService.verifyStatus(it.@href.text().toString()),
						jobid :  jenService.verifyJob(it.@href.text().toString())
					]
				}
			}
			// New jenkins
			def bd = html."**".findAll {it.@class.toString().contains("build-details")}
			if (bd) {
				col1 = bd.collect {
					[
						bid : it.A[0].@href.text(),
						bdate :it.toString().trim()
					]
				}
				def bn = html."**".findAll {it.@class.toString().contains("build-name")}
				if (bn) {
					col2 = bn.collect {
						[
							bid: jenService.verifyBuild(it.A[0].@href.text()),
							//bstatus : verifyStatus(it.A[0].IMG[0].@class.text().toString()),
							bstatus : jenService.verifyStatus(it.A[0].IMG.@src.text().toString()),
							//bimgUrl : it.A[0].IMG[0].@src.text(),
							jobid : it.toString().trim().replaceAll('[\n|\r|#|\t| ]+', '').replaceAll("\\s","")
						]
					}
				}
			}else{
				// Going to try parse older html styles - provided by a default jenkins install
				bd = html."**".findAll {it.@class.toString().contains("tip model-link")}
				if (bd) {
					col1 = bd.collect {
						[
							bid : it.@href.text(),
							bdate :it.toString().trim()
						]
					}
				}
				def bn = html."**".findAll {it.@class.toString().contains("build-row no-wrap")}
				if (bn) {
					col2 = bn.collect {
						[
							bid: jenService.verifyBuild(it.TD[0].A.@href.text().toString()),
							bstatus : jenService.verifyStatus(it.TD[0].A.IMG.@src.text().toString()),
							jobid: jenService.verifyJob(it.TD[0].A.@href.text().toString()).replaceAll("\\s","")
						]
					}
				}
			}
			if (col3) {
				finalList = [historyQueue: col3]
				userSession.basicRemote.sendText((finalList as JSON).toString())
			}
			if (col1&&col2) {
				def combined = (col1 + col2).groupBy { it.bid }.collect { it.value.collectEntries { it } }
				finalList = [history: combined]
				userSession.basicRemote.sendText((finalList as JSON).toString())
			}
		}
		catch(e) {
			log.error "Problem communicating with ${url}: ${e.message}", e
		}
	}

	private String getLastBuild(String url) {
		String bid = ""
		String bdate
		try {
			int go = 0
			HttpResponseDecorator html1 = http.get(path: "${url}")
			def html = html1?.data
			def bd = html."**".findAll {it.@class.toString().contains("build-details")}
			if (bd) {
				bd.each {
					go++
					if (go == 1) {
						bid = it.A[0].@href.text()
						bdate = it.toString().trim()
					}
				}
			}
			else {
				bd = html."**".find { it.@class.toString().contains("tip model-link") }
				bd.each {
					go++
					if (go == 1) {
						bid = it.@href.text()
						bdate = it.toString().trim()
					}
				}
			}
		}
		catch(e) {
			log.error "Problem communicating with ${url}: ${e.message}", e
		}
		return  bid
	}

	/*
	 * Parse Jenkins API url - grab all but only using a few json values
	 *  to calculate estimated duration of build
	 */
	def parseApi(Session userSession,String uri) {
		http.get(path: uri) { resp, json -> userSession.basicRemote.sendText(json.toString()) }
	}

	/*
	 *fetchNext javascript function, it grabs the current size returned from header:
	 *X-Text-Size and pushes it back as start={value}
	 *this ensures the response returned via websockets only contains
	 *relevant chunk
	 */
	def parseLiveLogs(Session userSession, String nurl) {
		boolean hasMore = true
		def csize, consoleAnnotator
		String ssize = ''
		userSession.basicRemote.sendText("Attempting live poll")
		def http1 = jenService.httpConn(jenserver, jensuser, jenspass)

		// while hasMore is true -
		// jenkins html page defined as header values
		// goes away once full results are returned

		nurl=jenService.stripDouble(nurl)
		while (hasMore) {
			
			// Set old size to current size
			def osize = csize

			//http1?.request("$jenserver$url", GET, TEXT) { req ->}
			http1?.request(GET, TEXT) { req ->
				uri.path = "$nurl"
				if (csize) {
					uri.query= [ start: "${csize}"]
				}
				
				// On success get latest output back from headers
				if (jensuser && jenspass) {
					headers.'Authorization' = 'Basic ' + "$jensuser:$jenspass".bytes.encodeBase64()
				}
				
				response.failure = { resp, reader ->
					hasMore = false
				}
				response.success = { resp, reader ->
					hasMore = resp.headers.'X-More-Data'
					csize = resp.headers.'X-Text-Size'
					consoleAnnotator = resp.headers.'X-ConsoleAnnotator'
					// If the current size is there and larger than osize value send to websocket
					if (csize && csize > osize) {
						userSession.basicRemote.sendText(reader.text as String)
					}
				}
			}
		}
	}
}
