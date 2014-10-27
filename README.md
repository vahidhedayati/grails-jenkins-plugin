jenkins 0.1
=========


Grails Jenkins plugin, will interact with Jenkins front end html interface using HTTPBuilder. Currently has two features that are by default available via a new controller that the plugin adds to your project:

### Video

[Grails-jenkins-plugin video youtube](http://youtu.be/XfsrBAa8aAg)

### Walkthrough
So once you had added plugin to your buildconfig, refreshed dependencies upon run-appp, you will be able to access this plugin via this url:
```
http://localhost:8080/yourapp/jenkins
```

This will load in the default index page (Refer to how to use further down) which asks a few questions in order for you to interact with an available jenkins server from within your grails application, you can also make a direct call via a taglib provided by the plugin from within your gsps (multiple times to multiple jenkins server all in one go).



The plugin functionality provides two core aspects of the Jenkins product within your grails app:

1. lists current build history on right hand side, if you click on dashboard or trigger a build.


The build history is being sent back via WebSockets and is called upon you clicking build + when build completes. The history is as is on Jenkins so if building it will show up building on here. If it passed/failed/cancelled and so forth.


You can click the specific historical item to view its logs, which again uses HTTPBuilder to grab out build logs and send via WebSockets back to the web page.

2. Trigger a build, this triggers a build and reuses feature to view log, but since this is building, it will trigger liveWatch which grabs the chunks back as Jenkins builds it and presents it back via WebSockets. Its a re-make of Jenkins does locally through its own console interface but using WebSockets instead of Ajax.

It also displays estimated time for build and how long it has been running.

All modes are now captured / building/success/failed/queued/cancelled.

You are also able to stop a runnning build job or a future queued item using this plugin.

 



Dependency, in your conf/BuildConfig.groovy under plugins add:
```groovy
	compile ":jenkins:0.1"
```

###### Plugin will work with tomcat 7.0.54 + (inc. 8) running java 1.7+




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


/* 
* Optional : not required - unless different to defaults
* Jenkins hide active estimated time for build to finish : default  'no' 
* choices : no/yes
*/
jenkins.hideBuildTimer='no'
  
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
hideButtons="${hideButtons }"
hideTriggerButton="${hideTriggerButton }"
hideDashBoardButton="${hideDashBoardButton }"
hideBuildTimer="${hideBuildTimer }"
jensLog="something" 
wshostname="something"
jensprogressive="something"
jensLog="something" 

 ```
 
So long as you provide the above values from within a gsp page it should load in the results back on the page..


You should be able to call it multiple times and provide different divId's for each call - to get multiple builds on one gsp page - not tried it myself.

I have tested this against a few variants of jenkins and it works according to these types:
Default jenkins - more recent/older variants.
Ubuntu jenkins - (has a dark theme black menu bar) working on this also.
May still fail on others, please post an issue with specific Jenkins version for me to look into.



# whilst running in PROD:

Whilst running this plugin on a tomcat server from an application that calls plugin, I have seen:
```
javax.websocket.DeploymentException: Multiple Endpoints may not be deployed to the same path [/WsChatEndpoint]
	at org.apache.tomcat.websocket.server.WsServerContainer.addEndpoint(WsServerContainer.java:209)
	at org.apache.tomcat.websocket.server.WsServerContainer.addEndpoint(WsServerContainer.java:268)
	at javax.websocket.server.ServerContainer$addEndpoint.call(Unknown Source)
```	
This does appear to be a warning and endpoint still works fine, and happens in tomcat... 7 + 8

