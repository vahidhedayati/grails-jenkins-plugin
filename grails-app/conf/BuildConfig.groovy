grails.project.work.dir = 'target'

grails.project.dependency.resolution = {

	inherits 'global'
	log 'warn'

	repositories {
		grailsCentral()
		mavenLocal()
		mavenCentral()
	}

	dependencies {
		build ('javax.websocket:javax.websocket-api:1.0') { export = false }
		
		runtime('org.codehaus.groovy.modules.http-builder:http-builder:0.5.1') {
			excludes 'xalan'
			excludes 'xml-apis'
			excludes 'groovy'
		}
		
		// Jira client
		//compile 'net.rcarz:jira-client:0.5', { 
		//		excludes ('junit')
		//}
	}

	plugins {
		//build (':rest:0.8') {
		//	excludes 'commons-beanutil'
		//}
		build ':release:3.0.1', ':rest-client-builder:2.0.3',  {
			export = false
		}
	}
}
