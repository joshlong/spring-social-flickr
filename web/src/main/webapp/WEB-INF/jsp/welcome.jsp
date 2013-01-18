<!doctype html>
<html ng-app="flickr">
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title> Console
</title>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery-ui.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery-filedrop.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery-masked.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/angular.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/angular-ui.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/angular-sanitize.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/angular-resource.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/jso.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/bootstrap.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/flickr.js"></script>

<style type="text/css">
    .album {
        border: 0;
        margin: 0;
        padding: 0;
        vertical-align: top;
        display: inline-block;
        margin-right: 10px;
    }

    .albumInfo {
        width: 150px;

    }

    .albumTitle {
        font-weight: bold;
    }

    .albumCoverMissing {
        margin: 0;
        padding: 0;
        display: block;
        text-align: center;
        vertical-align: middle;
        margin-top: auto;
        height: 160px;
        border: 1px solid black;
        margin-bottom: auto;
        width: 150px;
    }

    .albumRow {
        margin-bottom: 10px;
        font-size: smaller;
        font-family: sans-serif;
        padding: 0;
    }

    .albumImage {
        height: 160px;

    }


</style>

</head>
<body>
Welcome to Spring Social Flickr, ${flickrUser}!
<br/>
${messages}
<br>

<h1>Testing Photo Template</h1>
<hr>
<h2>Batch Download Console</h2>

<div class="importConsole" ng-controller="BatchImportController">

    <a ng-click="launch()" ng-show=" !started  ">Start Batch Download..</a>
    <a ng-click="stop()" ng-show=" started  ">Stop Batch Download..</a>

    <div ng-repeat="row in albums" class="albumRow">
        <div ng-repeat="album in row" class="album">

            <div class="albumImage">
                <img src="{{ album.coverImageUrl }}" width="150"/>
            </div>
            <div class="albumInfo">
                <div class="albumTitle">{{ album.title }}</div>
                <div style="height: 20px;">
                    {{album.countOfPhotos}} photos.
                    {{album.photosDownloaded}} imported.
                </div>

            </div>
        </div>
    </div>


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
