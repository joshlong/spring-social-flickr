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
<script type="text/javascript" src="${pageContext.request.contextPath}/js/angular-resource.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/jso.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/bootstrap.js"></script>

<style type="text/css">
    .album {
        border: 1px solid black;
        height: 100px;
        display: inline-block;
        background-color:  #f5f5dc;
        margin-right: 10px;
    }

    .albumCoverMissing {
        width: 100px;

    }

    .albumRow {
        border: 1px solid black;
        margin-bottom: 10px;
    }

        /*http://meyerweb.com/eric/articles/webrev/200006b.html*/
        /*
        .cell {
            display: inline-block;
            height: 30px;
            vertical-align: baseline;
        }

        .id {
            width: 50px;
            text-align: right;
            padding-right: 10px;
        }

        .ln {
            display: inline-block;
            width: 230px;
        }

        .hr > span {
            padding-top: 8px;
        }

        .tr {
            display: block;
            height: 45px;
            font-weight: bold;
        }

        .tr > * > input {
            margin-top: 8px;
        }

        .fn {
            display: inline-block;
            width: 230px;
        }

        .btns {
            width: 200px
        }*/
</style>
<script lang="">
    /***
     *
     * controllers that make up the client-side business logic of the application.
     *
     * handles all the pages through OAuth secured services,
     * which - while not obscured - require multi-factor authentication to do anything useful.
     */


    $.ajaxSetup({
        cache:false
    });

    var appName = 'flickr';
    var module = angular.module(appName, ['ngResource', 'ui']);


    module.value('ui.config', {
        // The ui-jq directive namespace
        jq:{
            // The Tooltip namespace
            tooltip:{
                // Tooltip options. This object will be used as the defaults
                placement:'right'
            }
        }
    });

    // idea try moving the module.run logic into this ajaxUtils object and then try separating this out into a separate object
    module.factory('ajaxUtils', function () {
        var contentType = 'application/json; charset=utf-8' ,
                dataType = 'json',
                errorCallback = function (e) {
                    alert('error trying to connect to ');
                };


        var baseUrl = (function () {
            var defaultPorts = {"http:":80, "https:":443};
            return window.location.protocol + "//" + window.location.hostname
                    + (((window.location.port)
                    && (window.location.port != defaultPorts[window.location.protocol]))
                    ? (":" + window.location.port) : "");
        })();

        var sendDataFunction = function (ajaxFunction, argsProcessor, url, _method, data, cb) {
            var d = data || {};
            var argFunc = argsProcessor || function (a) {
                return a;
            };
            var isPost = (_method || '').toLowerCase() == 'post';

            if (!isPost) {
                d['_method'] = _method;
            }

            var arg = {
                type:'POST',
                url:url,
                data:d,
                cache:false,
                dataType:dataType,
                success:cb,
                error:errorCallback
            };

            if (!isPost) {
                arg['headers'] = {'_method':_method};
            }
            ajaxFunction(argFunc(arg));
        };
        var noopArgsProcessor = function (e) {
            return e;
        };

        return {

            url:function (u) {
                return baseUrl + u;
            },
            put:function (url, data, cb) {
                sendDataFunction($.ajax, noopArgsProcessor, url, 'PUT', data, cb);
            },
            post:function (url, data, cb) {
                sendDataFunction($.ajax, noopArgsProcessor, url, 'POST', data, cb);
            },
            get:function (url, data, cb) {
                $.ajax({
                    type:'GET',
                    url:url,
                    cache:false,
                    dataType:dataType,
                    contentType:contentType,
                    success:cb,
                    error:errorCallback
                });
            }
        };
    });

    module.factory('batchImportService', function (ajaxUtils) {
        var batchEntryUrl = '/batch/';
        return {

            getAlbums:function (cb) {
                ajaxUtils.get(ajaxUtils.url(batchEntryUrl + '/albums'), {}, function (albums) {
                    for (var i = 0; i < albums.length; i++) {
                        albums[i].hasCoverImage = !(albums [i].url == null || albums [i].url == '');
                    }
                    cb(albums);
                });
            }

        }; //todo
    });


    function BatchImportController($rootScope, $scope, $q, $timeout, ajaxUtils, batchImportService) {

        var rowSize = 5; // how many albums to display in a given row
        console.log('starting the batch import controller');

        $scope.albums = [];

        function renderAlbums(albums) {
            var matrix = [];


            var cr = [];
            for (var i = 0; i < albums.length; i++) {
                albums[i].uid = 1 + i;
                if ((i % rowSize ) == 0) {
                    matrix.push(cr);
                    cr = [];
                }
                cr.push(albums[i]);
                if (i == (albums.length ) - 1) {
                    matrix.push(cr);
                    cr = [];
                }
            }
            $scope.albums = matrix;
        }

        // todo put this in a setTimeout loop
        function refreshAlbums() {
            batchImportService.getAlbums(function (albums) {
                console.log('inside getAlbums() callback with ' + albums.length + '.')
                $scope.$apply(function () {
                    renderAlbums(albums);
                });
            });
        }

        refreshAlbums()
        //setInterval(refreshAlbums, 1000) ;
    }
</script>
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

<div class="importConsole" ng-controller="BatchImportController">


    <%-- this screen lets the user start a download
        and then periodically refreshes the status of the downloads
    --%>


    <%-- supports rendering a grid of 5xN with each album being shown and the import status being given
    --%>

    <div ng-repeat="row in albums" class="albumRow">
        <div ng-repeat="album in row" class="album">
            <div ng-show="!album.hasCoverImage" class="albumCoverMissing">No Image Available</div>
            <img src="{{album.url}}" ng-show="album.hasCoverImage" height="100"/>
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
