package grails.plugin.jenkins

import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient

import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.protocol.HttpContext

class HBuilder {


	RESTClient httpConn(String host, String user, String key, int cTimeOut, int sTimeOut) {
		try {
			return createRestClient(host, user ?: '', key ?: '',  cTimeOut, sTimeOut)
		}
		catch (e)  {
			log.error("Failed error: $e", e)
		}
	}

	HttpResponseDecorator httpConn(String type, String host, String uri, String user, String key, int cTimeOut, int sTimeOut) {
		try {
			return createRestClient(host, user, key, cTimeOut, sTimeOut)."${type}"(path: uri)
		}
		catch (e) {
			log.error "Problem communicating with ${uri}: ${e.message}", e
		}
	}

	HttpResponseDecorator httpConn(String type, String url, String user, String key, int cTimeOut, int sTimeOut) {
		try {
			return createRestClient(url, user, key, cTimeOut, sTimeOut)."${type}"([:])
		}
		catch(e) {
			log.error "Problem communicating with ${url}: ${e.message}", e
		}
	}

	protected  RESTClient createRestClient(String url, String user, String key, int cTimeOut, int sTimeOut) {
		RESTClient http = new RESTClient(url)

		int connTimeOut=cTimeOut * 1000
		int sockTimeOut=sTimeOut * 1000
		http.client.params.setParameter("http.connection.timeout", new Integer(connTimeOut))
		http.client.params.setParameter("http.socket.timeout", new Integer(sockTimeOut))

		if (user && key) {
			http.client.addRequestInterceptor(new BasicAuthRequestInterceptor(user, key))
		}

		return http
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
