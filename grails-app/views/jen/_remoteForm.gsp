<g:if test="${divId && remoteController && remoteAction }">
<g:formRemote  name="remote_${divId}" update="${divId}"  url="[controller: '${remoteController}', action: '${remoteAction}']" >
	<input type="hidden" name="result" value="${result}">
	<input type="hidden" name="buildUrl" value="${buildUrl}">
	<input type="hidden" name="buildId" value="${buildId}">
	<input type="hidden" name="user" value="${user}">
	<input type="hidden" name="token" value="${token}">
	<input type="hidden" name="customParams" value="${customParams}">
	<input type="hidden" name="job" value="${job}">
	<input type="hidden" name="server" value="${server}">
	<input type="hidden" name="files" value="${files}">
	<input type="submit" name="doit" value="${wsprocessname}">
</g:formRemote>
</g:if>