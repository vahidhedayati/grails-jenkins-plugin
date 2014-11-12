jenkins 0.1
=========


Grails Jenkins plugin, will interact with Jenkins front end html interface using HTTPBuilder and push summary information to Jira if configured.

You can use websocket feature to watch live builds / view historical build information or trigger a background process that will do the build. So long as userID is provided and Jenkins has authentication enabled, it will attempt to log in as that user without a password (by grabbing token off of Jenkins) This in short means you now have visibility of who triggered a build via Jenkins.

Hooks/Triggers can be put in place to do two additional tasks.

After build trigger known as processurl or if via websocket wsprocessurl 

After build summary - which with correct Jira details it will push this summary to all defined tickets within the changelog.  


Dependency, in your conf/BuildConfig.groovy under plugins add:
```groovy
	compile ":jenkins:0.1"
```

###### Plugin will work with tomcat 7.0.54 + (inc. 8) running java 1.7+


### Video
[Part 1 : Older look](http://youtu.be/XfsrBAa8aAg)

[Part 2 : Update showing multiple builds, basic grails build in Jenkins](https://www.youtube.com/watch?v=CKv3TqWq4AQ)

[Part 3 : Update showing Jenkins authentication](https://www.youtube.com/watch?v=LOREp25Vz2Y)

[Part 4 : Non token authentication, custom Parameters](https://www.youtube.com/watch?v=bO3s8e4Qakc)

[part 5 : Automated build/deploy via jssh websockets async non websocket build ](https://www.youtube.com/watch?v=665QHQ-8q0U)

[part 6 : Automated summary to Jira - From Jenkins change log to Jira ticket(s)](https://www.youtube.com/watch?v=5LYrnobvLns)

[Test website testjenkins - used in videos](https://github.com/vahidhedayati/testjenkins)


### Walkthrough
once plugin dependency added to  BuildConfig, refreshed dependencies upon run-appp, you will be able to access this plugin via this url:
```
http://localhost:8080/yourapp/jen
```

This will load in the default index page which asks a few questions in order for you to interact with the given jenkins server/job. This can either be re-used or just make a direct connection using <jen:connect within your gsp.


The plugin adds the following functionality to your existing grails application:

##### lists current build history items on right hand side
The build history info is sent via WebSockets. If you click an item the Jenkins console logs will be displayed. It displays status of job whether it passed/failed/cancelled/building or queued. If building it will additionally show running time and estimated time according to Jenkins, this was initially achieved by parsing page over and over, now moved locally as a JavaScript that works out difference of estimated time according to jobid/api/json estimateTime value set per Jenkins job. You can click stop to send a stop to backend Jenkins which will stop the build. Scheduled future builds will appeared as queued and you can also cancel them.


##### Build
This triggers a build and attempts to parse the live Jenkins console output, since it is building the results on Jenkins by default are returned using Ajax. The plugin attempts to do a similar thing but using WebSockets, it grab ready chunks of log output and display back on your page.


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


<g:form>
	<input type="hidden" name="goahead" value="yes">
	<input type="submit" value="Build Jenkins job">
</g:form>

<g:if test="${goahead.equals('yes') }">
	<jen:connect divId="firstId" jenserver="localhost" jensport="9090" jensuser="" jenspass=""
	jensjob="my_build" jensprefix="" jensfolder="job" jenschoice="build" hideButtons="no" hideBuildTimer="no" />

	<jen:connect divId="secondId" jenserver="localhost" jensport="9090" jensuser="" jenspass=""
	jensjob="my_build2" jensprefix="" jensfolder="job" jenschoice="build" hideButtons="no" hideBuildTimer="no"/>
</g:if>
```
 So we have a button that asks to trigger build - if when clicked - its a self posting form that sets goahead=yes

 Then on the same page if this valus equals yes to call taglibs: results below:


![Output from dual builds](https://raw.githubusercontent.com/vahidhedayati/grails-jenkins-plugin/master/documentation/double-build.jpg)




# Config.groovy variables required:

Configure properties by adding following to grails-app/conf/Config.groovy under the "jenkins" key:

```groovy
/*
* This is the most important configuration
* in my current version the hostname is being defined by tomcat start up setenv.sh
* In my tomcat setenv.sh I have
* HOSTNAME = $(hostname)
* JAVA_OPTS="$JAVA_OPTS -DSERVERURL=$HOSTNAME"
*
* Now as per below the hostname is getting set to this value
* if not defined wschat will default it localhost:8080
*
*/
jenkins.wshostname = System.getProperty('SERVERURL')+":8080"
// can be overridden via tag lib :  wshostname="something"


/* timeout
* This is the default timeout value for websocket connection
* If you wish to get user to be timed out if inactive set this to a millisecond value
*/
jenkins.timeout = 0

/*
* Optional : not required - unless different to defaults
* Jenkins hide Login Pag: default  'no'
* choices : no/yes
* Choose if default index page from plugin can be loaded
*/
jenkins.hideLoginPage = 'no'



/*
* Optional : not required - unless different to defaults
* Jenkins internal consoleLog : default  '/consoleFull'
*/
jenkins.consoleLog = '/consoleFull'
// can be overridden via tag lib by definining: jensLog="something"



/*
* Optional : not required - unless different to defaults
* Jenkins internal buildend : default  '/build?delay=0sec'
*/
jenkins.buildend = '/build?delay=0sec'
// can be overridden via tag lib : jensbuildend="something"


/*
* Optional : not required - unless different to defaults
* Jenkins internal progressiveuri : default  '/logText/progressiveHtml'
*/
jenkins.progressiveuri = '/logText/progressiveHtml'
// can be overridden via tag lib : jensprogressive="something"


/*
* Optional : not required - unless different to defaults
* Jenkins hide build/dashboard buttons : default  'no'
* choices : no/yes
*/

jenkins.hideButtons = 'no'

/*
* Optional : not required - unless different to defaults
* Jenkins hide build button : default  'no'
* choices : no/yes
*/
jenkins.hideTriggerButton = 'no'

/*
* Optional : not required - unless different to defaults
* Jenkins hide dashboard/buildhistory button : default  'no'
* choices : no/yes
*/
jenkins.hideDashBoardButton = 'no'

/*
* Optional : your own custom processing url for when builds are triggered
* provide a full url back to a controll action so that when it completes a build
* notification is sent to controller and you can then call  further services on output
*/
jenkins.processurl = "http://localhost:8080/testjenkins/test/parseJenPlugin"


/*
* Optional : your own custom processing url for when builds are triggered
* provide a full url back to a controll action so that when it completes an extra 
* action button is provided
* the process url could in theory call another end point to lets say jssh and do a live deployment
*/
jenkins.wsprocessname = "Deploy"
jenkins.wsprocessurl = "http://localhost:8080/testjenkins/test/parseJenPluginDeploy"



// Auto submit wsprocess url ?
jenkins.autosubmit = yes"



// This is an important option to enable
// With this enabled - after a build has completed and has a result
// So long as your jira details are correct 
// and the ticket convention is as below appears in changes logs...
// Then this enabled with the jira info below should mean your build history should appear 
// under all logged tickets under changes screen of a given build id.
jenkins.showsummary = "yes"

/*
* Jira configuration - refer to summary section below:
*  Don't enable any of this if you are not looking to push anything to Jira.
*/
jenkins.sendtoJira = 'yes'

jenkins.jiraServer = 'http://jira-test.yourdomain.com'
jenkins.jiraUser = 'automation_account'
jenkins.jiraPass = 'automation_account_password'

/*
* This is the url usually to access the ticket for viewing - used to test if ticket is valid
* if not defined will default to /browse/
*/
jenkins.jira.AccessUri = "/browse/"


/* 
 * There are various send types :
 * comment -- adds the summary output as a comment to valid jira ticket 
 * customfield -- adds the summary output to provided customfield ID - please note customfield must have correct screen perms for it to work
 * updatecustomfield -- gets current input if different to new input adds them together to customfield
 * description -- updates ticket description with the summary
 * comdesc -- updates ticket description and adds a comment both containing the summary
 */
jenkins.jiraSendType = 'customfield' 
// If you have defined working option customfield then define the customfield id for this configuration item:
jenkins.customField = '12330' // the id of your customfield



/*
* API ChangeSet - send this as part of summary?
*/

jenkins.sendChangeSet = true  // true/false - by default false


/*
* API culprits - send this as part of summary?
*/

jenkins.sendCulprits = true  // true/false - by default false
```



When submitted, the controller has been set to recieve parameters and call a tag lib which can also be used by you guys to call a jenkins build on the fly from within your gsp.

```gsp
<jen:connect
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
<jen:connect
....
hideButtons="${hideButtons }"
hideTriggerButton="${hideTriggerButton }"
hideDashBoardButton="${hideDashBoardButton }"
jensLog="something"
wshostname="something"
jensprogressive="something"
jensLog="something"
customParams="[appId: '123', appName: 'crazyApp', appEnv: 'test' ]"
processurl="http://your_process_url/controller/action"
wsprocessurl="http://your_process_url/controller/action"
wsprocessname="Deploy code"
/>
 ```
customParams - if you have configured a processurl in your config you can pass values back


Results are typically returned to process url like this:
```
[result:SUCCESS, token:9cf496bb07021a1d788f8838159291cf, buildUrl:http://localhost:9090/job/my_build/175, customParams:{appId=123, appName=crazyApp, appEnv=test}, buildId:175, job:/job/my_build, server:http://localhost:9090, user:cc, action:parseJenPlugin, format:null, controller:test]
```

##### Refer further down on information on how to retrieve customParams


So long as you provide the above values from within a gsp page it should load in the results back on the page.


You should be able to call it multiple times and provide different divId's for each call - to get multiple builds on one gsp page.

Tested on recent/older variants of Jenkins. May still fail on others, please post an issue with specific Jenkins version for me to look into.



 Alternative more direct connect tag lib call:
 ```gsp
 <jen:dirconnect
		divId="someId"
		jensurl="http://jenkins-server:port/job/jobname"
		jensuser="current_user"
		jenschoice="dashboard"
		jensjob="jobname"
	/>
```

Optional - if you have configured a processurl in your config you can pass values back
All optional above should work
```gsp
processurl="http://your_process_url/controller/action"
wsprocessurl="http://your_process_url/controller/action"
wsprocessname="Deploy code"
customParams="[appId: '123', appName: 'crazyApp', appEnv: 'test' ]"
```



#### Summary information:

There is a new option called summary that appears next to the build ID's this Summary tries to grab information from 3 segments of Jenkins and if configured will push this information to a customfield on Jira.

It queries: 

The build Logs and tries to grab working folder, produced file and a line called last trans if it exists.
Change screen - grabs all related build changes to be pushed through 
Api summary - a variety of information from the api output.

The most important aspect of this is that within the changes logs, it looks for a ticket ID either seperated by  : or - 

so

``` 
AB-1102 : Description  
```

or :

```
AB-1102 - Description
```


Where AB-1102 will be the ticket number, this will then update this jira ticket with the summary provided 

![Jira ticket number](https://raw.githubusercontent.com/vahidhedayati/grails-jenkins-plugin/master/documentation/jen-changes.jpg)
 
Refer to above configuration items for the required jenkins configuration in your config.groovy.

It will parse through the changes logs, and for each ticket found - it will attempt to push the response to all tickets.

For the summary information to work properly I found I had to add two blank configuration items to my config.groovy:
 
```
jenkins.processurl=""

jenkins.wsprocessurl=""
```

These could actually be filled with a value, its just if not defined it returns some groovy object 


####Async Build (Non Websocket)
This will trigger a service that does a background build, whilst building it will check for completion, once completed it will trigger process url 
and send back results to it.
```gsp
<jen:asyncBuild

	url="http://host:post/job/JOB_NAME"
	
	customParams="[appId:'MyCurrentJob', appDetails: 'Something']"
	
	
	jensuser="MyUserId"
	
	
	processurl="http://localhost:8080/testjenkins/test/myresults"
	
	
	/>
```	

	
	
	

The processurl - is a background process that has no interaction with your front end view, runs in the background. When a job completes it returns its status plus a variety of other parameters to the given url.

The wsprocessurl - is a url which wsprocessname is the display name for the link within your websocket connected page. Once the job is completed a button is provided on the same websocket page to trigger the next controller/action which could in short be another taglib call that calls yet another websocket to process something else.
You can enable both processurl and wsprocessurl - they could be doing different things if needs be. It would not be a good idea to call the same controller/action since it will then lead to duplicated actions.


### Authenticated Jenkins howto:

Once you have configured global security of some form on your Jenkins server. Authentication should work via Jenkins so long as you either provide just the username to the initial form, or via the taglib call.

##### The plugin will attempt to grab the user authToken from the given server, if it can successfully retrieve this without authentication then the userToken is automatically set.

So by simply providing a valid username, the plugin will try do the rest and authenticate as the given user. With this you can easily gain a better overview of who is trigerring the build on Jenkins backend.

Whilst building if the current user has got authenticated then the user will appear above build logs otherwise current user will show anonymous.

##### To manually define authToken per user(known as jenspass)

First thing first, you need to enable authentication on Jenkins, our systems uses AD plugin and connects a user through to AD.
Once a user has logged in then goto:

1. Your Jenkins server:

http://your_jenkins:port/user/USERID/configure

Click on show API Token (This is an example token)
9a997cc1a954ac3a5ac59ea97c17a851

With this information now login using the front end using the username and the token as the password - this now triggers builds as the user.


###### customParams Retrieval
This is our example parseJenplugin call, the results are actually in JSON format, so as per what fed in in above example. I am now extracting each value on processurl: 
```groovy
def parseJenPlugin() { 
		println ":::> ${params} <:::"
		def pp=params.customParams
		def apps
		if (pp) {
			def data = JSON.parse(params.customParams)
			def appId=data?.appId
			def appName=data?.appName
			def appEnv=data?.appEnv
			println "--- Our Custom values passed from initial taglib call are:"
			println " AppID: $appId | AppName: $appName | AppEnv: $appEnv"
			/*
			 *    <jen:connect divId="firstId" 
					.....
					customParams="[appId: '123', appName: 'crazyApp', appEnv: 'test' ]"
					/>
					
					which has produced:
					
					:::> [result:SUCCESS, token:9cf496bb07021a1d788f8838159291cf, buildUrl:http://localhost:9090/job/my_build/182, customParams:{appId=123, appName=crazyApp, appEnv=test}, buildId:182, job:/job/my_build, server:http://localhost:9090, user:cc, action:parseJenPlugin, format:null, controller:test] <:::
--- Our Custom values passed from initial taglib call are:
 AppID: 123 | AppName: crazyApp | AppEnv: test

					
			 * 
			 */
			
			
		}
		render ""
	}
```


### Issues/Bugs

##### Prefix jenkins servers.
If you have a server with a prefix then you will find the quick connect method will not work for you, you need to use the manaully full detail connection method since the prefix is required for the url and uri.
