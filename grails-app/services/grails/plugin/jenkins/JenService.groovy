package grails.plugin.jenkins

import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import groovy.json.JsonBuilder
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient

import javax.websocket.Session

class JenService {

	static transactional = false
	def grailsApplication
	//def hBuilderService
	
	def jenSummaryService
	
	HBuilder hBuilder=new HBuilder()

	private String jensApi = '/api/json'
	private String userbase = '/user/'
	private String userend = '/configure'

	// Simply ensures URL is a successful URL.
	String verifyUrl(String nurl, String server, String user, String pass) {
		String result = 'Failed'

		RESTClient http
		try {
			try {
				http = hBuilder.httpConn(server, user ?: '', pass ?: '')
			}
			catch (HttpResponseException e) {
				return "Failed error: $e.statusCode"
			}
			catch (e)  {
				return "Failed error: ${e}"
			}
			catch (Throwable e) {
				return "Failed error: $e"
			}
			http?.request(GET, TEXT) { req ->
				//http?.request(GET, HTML) { req ->
				response.success = { resp ->
					result = "Success"
				}
				response.failure = { resp ->
					result = "Failed error: ${resp.statusLine.statusCode}"
				}
			}
		}
		catch (HttpResponseException e) {
			result = "Failed error: $e.statusCode"
		}
		return result
	}

	//This returns the Jenkins APIToken for a given username
	String returnToken(String user, String jenserver) {
		String url = userbase + user + userend
		def token
		int go = 0
		try {
			def http1 = hBuilder.httpConn(jenserver, '', '')
			HttpResponseDecorator html1 = http1.get(path: "${url}")
			def html = html1?.data
			def bd = html."**".findAll {it.@id.toString().contains("apiToken")}
			if (bd) {
				bd.each {
					go++
					if (go == 1) {
						token = it.@value.text()
					}
				}
			}
		}
		catch (e)  {
			log.error "Failed error: ${e}", e
		}
		return token
	}

	def asyncBuilder(String url, String jensuser, String jenspass,  String processurl, String customParams) {

		def aurl = new URL(url)
		String jensauthority = aurl.authority
		String jensprot = aurl.protocol
		String jensurl=aurl.path
		String jenserver=jensprot + '://' + jensauthority

		def config = grailsApplication.config.jenkins
		def jensbuildend = config.jensbuildend  ?: '/build?delay=0sec'

		if (jensuser && !jenspass) {
			jenspass = returnToken(jensuser, jenserver)
		}

		def url1 = jenserver + jensurl + jensbuildend
		// current Build ID
		int currentBuild = currentBuildId(jensurl, jenserver, jensuser, jenspass)

		// Kicks off build
		jobControl(url1,jensuser, jenspass)
		// Increment existing buildID with 1
		int newBuild=currentBuild+1

		// Sleep a while
		sleep(6000)

		// Kick off a job to asynchronously check build and if we have result to trigger processurl
		def asyncProcess = new Thread({workOnBuild(null,processurl,'','',newBuild,
			jensurl,jenserver, jensuser, jenspass,customParams,jensurl,jensApi)} as Runnable)
		asyncProcess.start()

	}

	def asyncBuilder(String jensurl, String jenserver, String jensuser, 
		String jenspass,  String processurl, String customParams) {

		def config = grailsApplication.config.jenkins
		def jensbuildend = config.jensbuildend  ?: '/build?delay=0sec'

		def url1 = jensurl + jensbuildend

		if (jensuser && !jenspass) {
			jenspass = returnToken(jensuser, jenserver)
		}


		// current Build ID
		int currentBuild = currentBuildId(jensurl, jenserver, jensuser, jenspass)

		// Kicks off build
		jobControl(url1,jensuser, jenspass)
		// Increment existing buildID with 1
		int newBuild=currentBuild+1



		// Sleep a while
		sleep(6000)

		// Kick off a job to asynchronously check build and if we have result to trigger processurl
		def asyncProcess = new Thread({workOnBuild(null,processurl,'','',
			newBuild,jensurl,jenserver, jensuser, jenspass,customParams,
			jensurl,jensApi)} as Runnable)
		asyncProcess.start()

	}

	private int currentBuildId(String url, String jenserver, String jensuser, String jenspass) {
		def lastbid = getLastBuild(url, jenserver, jensuser ?: '', jenspass ?: '')
		//int currentBuild = jenkinsService.currentJob(lastbid)
		if (lastbid) {
			def cj=currentJob(lastbid)
			if (cj && cj.toString().isInteger()) {
				cj as int
			}
		}
	}

	private String getLastBuild(String url, String jenserver, String jensuser, String jenspass) {
		String bid = ""
		String bdate
		try {
			int go = 0
			RESTClient http = hBuilder.httpConn(jenserver, jensuser, jenspass)
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

	// This posts some form of action to Jenkins builds etc
	HttpResponseDecorator jobControl(String url, String jensuser, String jenspass ) {
		HttpResponseDecorator html1 = hBuilder.httpConn('post',  url, jensuser ?: '', jenspass ?: '')
	}

	// This posts some form of action to Jenkins builds etc
	HttpResponseDecorator jobControl(String url, String bid, String jenserver, String jensuser, String jenspass ) {
		HttpResponseDecorator html1 = hBuilder.httpConn('post', jenserver + url, jensuser ?: '', jenspass ?: '')
	}

	//This is an asynchronous task that is given a new BuildID, it will poll the
	// api page and once it has a result it will return this back to your own
	// defined processcontrol url.
	def workOnBuild(Session userSession, String processurl, String wsprocessurl,
		 String wsprocessname, int bid,String uri, String jenserver, String jensuser,
		  String jenspass, String customParams,	String jensurl, String jensApi) {

		boolean go = false
		def result
		int max = 120
		int a = 0

		def ubi=stripDouble(uri+"/"+bid.toString())
		String url=ubi+jensApi

		def http1 = hBuilder.httpConn(jenserver, jensuser, jenspass)

		try {

			while (!go && a < max) {

				a++

				http1.get(path: "${url}") { resp, json ->
					result = json.result
					if (result && result != 'null') {
						
						// We have a result -->
						// So lets check if user has enabled showhistory
						// Load up build history which also calls jira calls if enabled
						def config = grailsApplication.config.jenkins
						
						(config?.showhistory == "yes") {
							def output=jenSummaryService.jenSummary(http1,jenserver,bid.toString())
							if (userSession) { 
								userSession.basicRemote.sendText(output)
							}
						}
						
						if (userSession && wsprocessurl) {
							def ajson = new JsonBuilder()
							ajson.feedback{
								delegate.wsprocessurl "$wsprocessurl"
								delegate.wsprocessname "$wsprocessname"
								delegate.result "$result"
								delegate.buildUrl  "$jenserver$ubi"
								delegate.buildId "${bid as String}"
								delegate.customParams "${customParams}"
								delegate.server  "${jenserver}"
								delegate.user "${jensuser}"
								delegate.token "${jenspass}"
								delegate.job "${jensurl}"
							}
							userSession.basicRemote.sendText(ajson.toString())
						}
						go = true
					}
					sleep(10000)
				}
			}

		}catch (e) {
			log.error "Problem communicating with ${jenserver + url}: ${e.message}", e
		}

		if (result) {
			def http2 = hBuilder.httpConn(processurl,'','')
			http2.request( POST ) { req ->
				requestContentType = URLENC
				body = [
					result : result,
					buildUrl : jenserver+ubi,
					buildId: bid as String,
					customParams: customParams,
					server : jenserver,
					user : jensuser,
					token : jenspass,
					job : jensurl
				]
				response.success = { resp ->
					log.debug "Process URL Success! ${resp.status}"
				}

				response.failure = { resp ->
					log.error "Process URL failed with status ${resp.status}"
				}
			}
		}
	}


	String verifyStatus(String img) {
		String output = "unknown"
		if (img.contains("blue_anime") || (img.contains("aborted_anime")) || (img.contains("job")) && (img.contains("stop"))) {
			output = "building"
		}else if (img.contains("red")) {
			output = "failed"
		}else if (img.contains("blue")) {
			output = "passed"
		}else if ((img.contains("queue")) || (img.contains("cancelItem"))) {
			output = "queued"
		}else if ((img.contains("grey")) || (img.contains("aborted"))) {
			output = "cancelled"
		}
		return output
	}

	int currentJob(String job) {
		if (job && job.endsWith('/')) {
			job = job.substring(0, job.length()-1)
			job = job.substring(job.lastIndexOf('/')+1, job.length())
			if (job.isInteger()) {
				return job as int
			}
			return 0
		}
		return 0
	}

	String verifyJob(String job) {
		if (job) {
			if (job.endsWith('/')) {
				job = job.substring(0, job.length()-1)
			}
			if (job.indexOf('=')>-1) {
				job = job.substring(job.indexOf('=')+1, job.length())
			}else if (job.endsWith('/console')) {
				job = job.substring(0, job.indexOf('/console'))
				job = job.substring(job.lastIndexOf('/')+1, job.length())
			}else if (job.endsWith('/stop')) {
				job = job.substring(0, job.indexOf('/stop'))
				job = job.substring(job.lastIndexOf('/')+1, job.length())
			}
			return job
		}
	}

	String verifyBuild(String bid) {
		if (bid && bid.endsWith('/console')) {
			bid = bid.substring(0, bid.indexOf('/console')+1)
		}else if (bid &&  bid.endsWith('/stop')) {
			bid = bid.substring(0, bid.indexOf('/stop')+1)
		}
		return bid
	}


	def returnArrary(String csv) {
		List<String> block
		if (csv.toString().indexOf(',')>-1) {
			block =csv.split(',').collect { it.trim() }
			return block
		}
		return csv
	}

	String stripDouble(String url) {
		if (url) {
			if (url.indexOf('//')>-1) {
				url = url.replace('//', '/')
			}
			return url
		}
	}

	String seperator(input) {
		if (input && !input.toString().startsWith('/')) {
			input = '/' + input
		}
		return input
	}
}

