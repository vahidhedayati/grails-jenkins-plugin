package grails.plugin.jenkins

import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient

import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.protocol.HttpContext

class JenService {

	static transactional = false

	String verifyUrl(String nurl, String server, user, pass) {
		String result = 'Failed'
		RESTClient http
		try {
			try {
				http = httpConn(server, user, pass)
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

	String returnToken(String user, String jenserver, String userbase, String userend) {
		String url = userbase + user + userend
		def token
		int go = 0
		try {
			RESTClient http1 = new RESTClient(jenserver)
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
			log.info "Failed error: ${e}", e
		}
		return token
	}

	RESTClient httpConn(String host, String user, String key) {
		try {
			return createRestClient(host, user, key)
		}
		catch (e)  {
			log.error("Failed error: $e", e)
		}
	}

	HttpResponseDecorator httpConn(String type, String host, String uri, String user, String key) {
		try {
			return createRestClient(host, user, key)."${type}"(path: uri)
		}
		catch (e) {
			log.error "Problem communicating with ${uri}: ${e.message}", e
		}
	}

	HttpResponseDecorator httpConn(String type, String url, String user, String key) {
		try {
			return createRestClient(url, user, key)."${type}"([:])
		}
		catch(e) {
			log.error "Problem communicating with ${url}: ${e.message}", e
		}
	}

	protected RESTClient createRestClient(String url, String user, String key) {
		RESTClient http = new RESTClient(url)
		if (user && key) {
			http.client.addRequestInterceptor(new BasicAuthRequestInterceptor(user, key))
		}
		http
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

class BasicAuthRequestInterceptor implements HttpRequestInterceptor {

	final String user
	final String key

	BasicAuthRequestInterceptor(String user, String key) {
		this.user = user
		this.key = key
	}

	void process(HttpRequest httpRequest, HttpContext httpContext) {
		httpRequest.addHeader('Authorization', 'Basic ' + "$user:$key".bytes.encodeBase64())
	}
}
