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
 



Dependency, in your conf/BuildConfig.groovy under plugins add:
```groovy
	compile ":jenkins:0.1"
```

###### Plugin will work with tomcat 7.0.54 + (inc. 8) running java 1.7+




#### How to use
So when you access the default index page a variety of questions are presented in form input boxes, this is a further explanation of what is being asked:

Server hostname
```
Server to connect to : so this is the IP or HostName of your Jenkins i.e. 10.1.1.2
```

Server port
```
Port: i.e. 8080 
```

Folder/special folder 
```
folder: this is usually job, so if you have a default Jenkins running on 8080, so far we have:

10.1.1.2:8080/job/ 

If you have a different setup lets say by default it is  
 10.1.1.2:8080/jenkins/job
 
 
 Then you will find the jensprefix variable useful, for these situations provide a value in the index page of Jenkins if url is like above
 
 jensprefix: jenkins
````

JobName: Your jenkins job's name
```
jobname: This is your created job on Jenkins so for example : my_first_build
````

```
username and password are optional and need testing from my end, so far its basic 
authentication on Jenkins remote end, so you can leave these blank
```

The Choice: dashboard(shows history + you can view logs) OR Directly trigger build and watch logs 
```
Choice: 
	Dashboard (not done yet will hopefully be part of 0.1),
	Build - this works and should trigger a remote build followed by a tailing or live 
	log watch of current build that it triggered via your grails app
```




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

