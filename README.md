jenkins 0.1
=========


Grails Jenkins plugin, will interact with Jenkins front end html interface using HTTPBuilder. Currently has two features that are by default available via a new controller that the plugin adds to your project:


Dependency, in your conf/BuildConfig.groovy under plugins add:
```groovy
	compile ":jenkins:0.1"
```

###### Plugin will work with tomcat 7.0.54 + (inc. 8) running java 1.7+


### Video
![Grails-jenkins-plugin video youtube](http://youtu.be/XfsrBAa8aAg)

![Part 2: update showing multiple builds](https://www.youtube.com/watch?v=CKv3TqWq4AQ)


### Walkthrough
once plugin dependency added to  BuildConfig, refreshed dependencies upon run-appp, you will be able to access this plugin via this url:
```
http://localhost:8080/yourapp/jenkins
```

This will load in the default index page which asks a few questions in order for you to interact with the given jenkins server/job. This can either be re-used or just make a direct connection using <jenkins:connect within your gsp.


The plugin functionality provides two core aspects of the Jenkins product within your grails app:

##### lists current build history items on right hand side
The build history info is sent via websockets. If you click an item it will display its log. It displays status of job whether it passed/failed/canelled/building or queued. If building it will additionally show running time and estimated time according to Jenkins, this was initially achieved by parsing page over and over, now moved locally as a javascript that works out difference of estimated time according to jobid/api/json estimateTime value set per jenkins job. You can click stop to send a stop to backend jenkins which will stop the build. Scheduled future builds will appeared as queued and you can also cancel them.
 

##### Build
This triggers a build and attempts to parse job output, since it is building the results by default are returned via Ajax, it uses websockets instead to grab ready chunks of log output and display back on your page.





#### How to use
![Connection details](https://raw.githubusercontent.com/vahidhedayati/grails-jenkins-plugin/master/documentation/connect.jpg)

##### Example : Using taglib to make multiple calls to jenkins:

Controller:
ExampleController.groovy:
```groovy
	def build() { 
		def goahead=params.goahead
		[goahead:goahead]	
	}
```

GSP Page for build.gsp
```gsp


<g:form method="post">
	<input type="hidden" name="goahead" value="yes">
	<input type="submit" value="Build Jenkins job">
</g:form>

<g:if test="${goahead.equals('yes') }">
	<jenkins:connect divId="firstId" jenserver="localhost" jensport="9090" jensuser="" jenspass="" 
	jensjob="my_build" jensprefix="" jensfolder="job" jenschoice="build" hideButtons="no" hideBuildTimer="no" />

	<jenkins:connect divId="secondId" jenserver="localhost" jensport="9090" jensuser="" jenspass="" 
	jensjob="my_build2" jensprefix="" jensfolder="job" jenschoice="build" hideButtons="no" hideBuildTimer="no"/>
</g:if>
```
 So we have a button that asks to trigger build - if when clicked - its a self posting form that sets goahead=yes
 
 Then on the same page if this valus equals yes to call taglibs: results below:


![Output from dual builds](https://raw.githubusercontent.com/vahidhedayati/grails-jenkins-plugin/master/documentation/double-build.jpg)




# Config.groovy variables required:

Configure properties by adding following to grails-app/conf/Config.groovy under the "wschat" key:

```groovy


/*
* This is the most important configuration 
* in my current version the hostname is being defined by tomcat start up setenv.sh
* In my tomcat setenv.sh I have
* HOSTNAME=$(hostname)
* JAVA_OPTS="$JAVA_OPTS -DSERVERURL=$HOSTNAME"
*
* Now as per below the hostname is getting set to this value
* if not defined wschat will default it localhost:8080
*
*/
jenkins.wshostname=System.getProperty('SERVERURL')+":8080"
// can be overridden via tag lib :  wshostname="something"


/* timeout 
* This is the default timeout value for websocket connection
* If you wish to get user to be timed out if inactive set this to a millisecond value
*/
jenkins.timeout=0

/* 
* Optional : not required - unless different to defaults
* Jenkins hide Login Pag: default  'no'
* choices : no/yes
* Choose if default index page from plugin can be loaded
*/
jenkins.hideLoginPage='no'



/* 
* Optional : not required - unless different to defaults
* Jenkins internal consoleLog : default  '/consoleFull'
*/
jenkins.consoleLog='/consoleFull'
// can be overridden via tag lib by definining: jensLog="something" 



/* 
* Optional : not required - unless different to defaults
* Jenkins internal buildend : default  '/build?delay=0sec'
*/
jenkins.buildend='/build?delay=0sec'
// can be overridden via tag lib : jensbuildend="something"


/* 
* Optional : not required - unless different to defaults
* Jenkins internal progressiveuri : default  '/logText/progressiveHtml'
*/
jenkins.progressiveuri='/logText/progressiveHtml'
// can be overridden via tag lib : jensprogressive="something"


/* 
* Optional : not required - unless different to defaults
* Jenkins hide build/dashboard buttons : default  'no'
* choices : no/yes
*/

jenkins.hideButtons='no'

/* 
* Optional : not required - unless different to defaults
* Jenkins hide build button : default  'no'
* choices : no/yes
*/
jenkins.hideTriggerButton='no'

/* 
* Optional : not required - unless different to defaults
* Jenkins hide dashboard/buildhistory button : default  'no'
* choices : no/yes
*/
jenkins.hideDashBoardButton='no'

  
```



When submitted, the controller has been set to recieve parameters and call a tag lib which can also be used by you guys to call a jenkins build on the fly from within your gsp.

```gsp
<jenkins:connect
divId="someId"
jenserver="${jenserver }"
jensport="${jensport}"
jensuser="${jensuser}"
jenspass="${jenspass}"
jensjob="${jensjob}"
jensprefix="${jensprefix}"
jensfolder="${jensfolder}"
jensport="${jensport}"
jenschoice="${jenschoice}"

/>
```

Optional override taglibs: (Refer to above Config.groovy to understand what these are:) 
```gsp
<jenkins:connect
....
hideButtons="${hideButtons }"
hideTriggerButton="${hideTriggerButton }"
hideDashBoardButton="${hideDashBoardButton }"
jensLog="something" 
wshostname="something"
jensprogressive="something"
jensLog="something"
/> 

 ```
 
So long as you provide the above values from within a gsp page it should load in the results back on the page.


You should be able to call it multiple times and provide different divId's for each call - to get multiple builds on one gsp page.

Tested on recent/older variants of Jenkins. May still fail on others, please post an issue with specific Jenkins version for me to look into.



# whilst running in PROD:

Whilst running this plugin on a tomcat server from an application that calls plugin, I have seen:
```
javax.websocket.DeploymentException: Multiple Endpoints may not be deployed to the same path [/WsChatEndpoint]
	at org.apache.tomcat.websocket.server.WsServerContainer.addEndpoint(WsServerContainer.java:209)
	at org.apache.tomcat.websocket.server.WsServerContainer.addEndpoint(WsServerContainer.java:268)
	at javax.websocket.server.ServerContainer$addEndpoint.call(Unknown Source)
```	
This does appear to be a warning and endpoint still works fine, and happens in tomcat... 7 + 8

# Current Bug(s)
If previous job has failed, current build will appear in red in Jenkins and causes plugin to also show build as failure, need to revisit parsing for this feature to work.
