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
var module = angular.module(appName, ['ngResource' , 'ngSanitize', 'ui']);


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
                    albums[i].hasCoverImage = !(albums [i].primaryImageUrl == null || albums [i].primaryImageUrl == '');
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
                function urlForImage(album) {
                    var url = album.hasCoverImage ? album.primaryImageUrl : '/imgs/image-not-found.gif';
                    return url;
                }

                for (var i = 0; i < albums.length; i++)
                    albums[i].coverImageUrl = urlForImage(albums[i]);


                renderAlbums(albums);
            });
        });
    }


    refreshAlbums(); // get things started

    // then follow up with a refresh
    setInterval(refreshAlbums, 10 * 1000);
}