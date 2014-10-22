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
			
		if (cmd.toString().equals('choose')) {
			jenschoice=data.jenschoice ?: 'build'
			switch (jenschoice) {
				case 'build':
					buildJob(userSession)
					break
					default:
					//	dosomething
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


	private void ParseJobConsole(Session userSession,String url,String murl,String bid) {
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
					html.findAll { it.toString()}.each {
						if (it.toString().indexOf('Started')>-1) {
							start=true
						}
						if (start)  {
							userSession.getBasicRemote().sendText(it.toString())
						}
						if (it.toString().indexOf('Finished')>-1) {
							start=false
						}

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

	


	private buildJob(Session userSession) {
		String url=jenserver+jensurl
		def url1=url+jensbuildend
		def output=getBuildStatus (url)
		def lastbid=getLastBuild(url)
		StringBuilder sb=new StringBuilder()
		String consolelog=jensconlog
		//String consolelog='/consoleText'
		sb.append("Before triggering build "+output+"<br> Last Build ID: "+lastbid+"<br>")
		// This kicks off the build
			try {
				http = new HTTPBuilder("${url1}")
				http.getClient().getParams().setParameter("http.connection.timeout", new Integer(httpConnTimeOut))
				http.getClient().getParams().setParameter("http.socket.timeout", new Integer(httpSockTimeOut))
				if (!jensuser) {
					http.auth.basic "${jensuser}", "${jenspass}"
				}
				
				def html = http.get([:])
				def output1=getBuildStatus(url)
				def lastbid1=getLastBuild(url)
				userSession.getBasicRemote().sendText("After trigger build : "+output1+"<br>Current Build ID: "+lastbid1+"<br>")
				//String url2=url+"/"+lastbid1+consolelog
				String url2=jenserver+"/"+lastbid1+consolelog
				ParseJobConsole(userSession,url2,url,lastbid1)
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

	private def getLastBuild(url) {
		def bid="";
		def html=""
		try {
			http = new HTTPBuilder(url)
			http.getClient().getParams().setParameter("http.connection.timeout", new Integer(httpConnTimeOut))
			http.getClient().getParams().setParameter("http.socket.timeout", new Integer(httpSockTimeOut))
			if (!jensuser) {
				http.auth.basic "${jensuser}", "${jenspass}"
			}
			 html = http.get([:])
			//html = http.get( path : url)
		}catch(Exception e) {
			log.error "Problem1 communicating with ${url}: ${e.message}"
		}
		try {
			int go=0;
			def bdate,transaction=''
			html."**".find { it.@class.toString().contains("build-row no-wrap") || it.@class.toString().contains("pane build-details")}.each {
				go++
				if (go==1) {
					bid= it.A[0].@href.text()
					/*if (bid&&(bid.indexOf('/')>-1)) {
						if (bid.endsWith('/')) {
							bid=bid.substring(0,bid.length()-1)
						}
						bid=bid.substring(bid.lastIndexOf('/')+1,bid.length())
					}*/
					bdate=it.toString().trim()
					//def out1=it.toString().trim()
					//out1 = out1.replace("\n", "").replace("\r", "");
					//out1 = out1.replaceAll("\\s+", " ").trim();
					//out1 = out1.replace(' #', '').trim();
					//out1 = out1.replace('#', '').trim();
					//if (!bid) {
					//bid=out1.substring(0, out1.indexOf(' '))
					//}
					//if (!bid)
					//if (it.tip model-link inside
					//if (bdate) {
					//	bdate=out1.substring(bid.size(), out1.length()).trim()
					//if (bdate =~ /[^0-9]$/) {
					//	bdate=bdate.substring(0, bdate.length()-1)
					//}
					//}
					//bid=bid.trim()
					//if (bid =~ /^[^0-9]/) {
					//	bid=bid.substring(1,bid.length()).trim()
					//}
				}
			}
		}catch(Exception e) {
			log.error "Problem communicating with ${url}: ${e.message}"
		}
		return  bid
	}


	private def getBuildStatus (url) {
		String output
		String	selection="Built"
		String rselection="Building"
		try {
			http = new HTTPBuilder("${url}")
			http.getClient().getParams().setParameter("http.connection.timeout", new Integer(httpConnTimeOut))
			http.getClient().getParams().setParameter("http.socket.timeout", new Integer(httpSockTimeOut))
			if (!jensuser) {
				http.auth.basic "${jensuser}", "${jenspass}"
			}
			//def html = http.get(path:url)
			def html = http.get([:])
			int go=0
			int go1=0
			int go2=0
			html."**".find { it.@class.toString().contains("build-row no-wrap transitive") || it.@class.toString().contains("build-row transitive")}.each {
				go1=1;
				output="<font color=red>Currently "+rselection+" :"+it+"</font><br>"
			}
			go=0;
			if (go==0) {
				html."**".find { it.@class.toString().contains("build-row no-wrap") || it.@class.toString().contains("pane build-details") }.each {
					go++
					if (go==1) {
						it."**".find { it.@class.toString().contains("tip")}.each {
							go2++
							if (go2==1) {
								it."**".find { it.toString()}.each {
									output=output+"<font color=green>Last "+selection+": "+it+"</font><br>"
								}
							}
						}
					}
				}
			}
		}catch(Exception e) {
			log.error "Problem communicating with ${url}: ${e.message}"
		}
		if (!output) { output = "No "+selection+" information found!" }
		return output
	}


}
