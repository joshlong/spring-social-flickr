<!doctype html>
<html ng-app="flickr">
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
    <title> Console
    </title>
    <script type="text/javascript" src="${pageContext.request.contextPath}/web/js/jquery.js"></script>
    <script type="text/javascript" src="${pageContext.request.contextPath}/web/js/jquery-ui.js"></script>
    <script type="text/javascript" src="${pageContext.request.contextPath}/web/js/jquery-filedrop.js"></script>
    <script type="text/javascript" src="${pageContext.request.contextPath}/web/js/jquery-masked.js"></script>
    <script type="text/javascript" src="${pageContext.request.contextPath}/web/js/angular.js"></script>
    <script type="text/javascript" src="${pageContext.request.contextPath}/web/js/angular-ui.js"></script>
    <script type="text/javascript" src="${pageContext.request.contextPath}/web/js/angular-resource.js"></script>
    <script type="text/javascript" src="${pageContext.request.contextPath}/web/js/jso.js"></script>
    <script type="text/javascript" src="${pageContext.request.contextPath}/web/js/bootstrap.js"></script>
</head>
<body>
Welcome to Spring Social Flickr, ${flickrUser}!
<br/>
${messages}
<br>

<h1>Testing Photo Template</h1>
<hr>
<h2>Batch Download Console</h2>
   <a href="${pageContext.request.contextPath}/batch/start">Start Download..</a>

<%-- this screen lets the user start a download
    and then periodically refreshes the status of the downloads
--%>
<div>


</div>


<hr>
<h2>add tags to photo</h2>

<form action="addtags" method="POST">
    <label>Tags(comma separated) : </label><input type="text" name="tags"/>
    <label>Photo Id : </label><input type="text" name="photoid"/>
    <input type="submit" value="add tags"/>
</form>

<h2>delete photo</h2>

<form action="deletephoto" method="POST">
    <label>Photo Id : </label><input type="text" name="photoid"/>
    <input type="submit" value="delete photo"/>
</form>

<h2>upload photo</h2>

<form action="uploadphoto" method="POST" enctype="multipart/form-data">
    <label>Photo : </label>
    <input type="file" name="photo"/>
    title: <input type="text" name="title"/> <br/>
    description: <textarea rows="10" name="description"></textarea> <br/>
    <input type="submit" value="upload photo"/>

    <b>${photoId}</b>

</form>

</body>
</html>
