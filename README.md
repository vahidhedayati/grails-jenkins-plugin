jenkins 0.1
=========

Grails Jenkins plugin, will interact with jenkins front end html interface using HTTPBuilder and interacts response back via websockets.



Dependency, in your conf/BuildConfig.groovy under plugins add:
```groovy
	compile ":jenkins:0.1"
```

###### Plugin will work with tomcat 7.0.54 + (inc. 8) running java 1.7+


#### How to use
once installed, you will have a new controller called jenkins, the default index action will load a form which will as a bunch of questions:

```
Server to connect to : so this is the ip or hostname of your jenkins i.e. 10.1.1.2
```

```
Port: i.e. 8080 
```

```
folder: this is usually job, so if you have a default jenkins running on 8080, so far we have:

10.1.1.2:8080/job/ 

If you have a different setup lets say by default it is  
 10.1.1.2:8080/jenkins/job
 
 Then set folder as :
 jenkins/job
````

```
jobname: This is your created job on jenkins so for example : my_first_build
````

```
username and password are optional and need testing from my end, so far its basic 
authentication on jenkins remote end, so you can leave these blank
```

```
Choice: 
	Dashboard (not done yet will hopefully be part of 0.1),
	Build - this works and should trigger a remote build followed by a tailing or live 
	log watch of current build that it triggered via your grails app
```






When submitted, the controller has been set to recieve parameters and call a tag lib which can also be used by you guys to call a jenkins build on the fly from within your gsp.

```
<jenkins:connect
divId="someId"
jenserver="${jenserver }"
jensport="${jensport}"
jensuser="${jensuser}"
jenspass="${jenspass}"
jenserver="${jenserver}"
jensjob="${jensjob}"
jensfolder="${jensfolder}"
jensport="${jensport}"
/>
```

So long as you provide the above values from within a gsp page it should load in the results back on the page..


You should be able to call it multiple times and provide different divId's for each call - to get multiple builds on one gsp page - not tried it myself.

Finally this is still being tested and it may not work on all versions of jenkins. I will try to get as much testing done on variations of jenkins before releasing it as a plugin on grails (if it is accepted ofcourse).




# whilst running in PROD:

Whilst running this plugin on a tomcat server from an application that calls plugin, I have seen:
```
javax.websocket.DeploymentException: Multiple Endpoints may not be deployed to the same path [/WsChatEndpoint]
	at org.apache.tomcat.websocket.server.WsServerContainer.addEndpoint(WsServerContainer.java:209)
	at org.apache.tomcat.websocket.server.WsServerContainer.addEndpoint(WsServerContainer.java:268)
	at javax.websocket.server.ServerContainer$addEndpoint.call(Unknown Source)
```	
This does appear to be a warning and endpoint still works fine, and happens in tomcat... 7 + 8

