package grails.plugin.jenkins

import groovyx.net.http.HTTPBuilder

import javax.swing.text.html.HTML


class JenParserService {
	private int httpConnTimeOut = 10*1000;
	private int httpSockTimeOut = 30*1000;
	
	
    def Parse(String url) {
		
		//sb.append("<pre>")
		def http = new HTTPBuilder("${url}")
		http.getClient().getParams().setParameter("http.connection.timeout", new Integer(httpConnTimeOut))
		http.getClient().getParams().setParameter("http.socket.timeout", new Integer(httpSockTimeOut))
		//if ((jensuser!='') || (jensuser!=null)) {
		////	http.auth.basic "${jensuser}", "${jenspass}"
		//}
		def html = http.get([:])
		return html
		
    }
}
