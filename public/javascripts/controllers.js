

var wikiControllers = angular.module('wikiControllers', ['highcharts-ng', 'ui.bootstrap']);

wikiControllers.controller('searchController', ['$scope', '$http', function($scope, $http) {
    $scope.selected = undefined;
    $scope.domains = ["en.wikipedia", "wikidata.wikipedia"];
    $http.get('/api/all_domains').success(function (data) {
        $scope.domains = data;
    });
}]);

wikiControllers.controller('pageController', ['$scope', '$routeParams', '$http', function($scope, $routeParams, $http) {
    $scope.domain = $routeParams.domain;
    $scope.page = $routeParams.page;
    $scope.graphConfig = {
        useHighStocks: true,
        options: {
            chart: { type: 'line', zoomType: 'x' },
            navigator: { enabled:true },
            rangeSelector: {
                buttons: [
                    { type: 'hour', count: 24, text: '1d' },
                    { type: 'day', count: 3, text: '3d' },
                    { type: 'month', count: 1, text: '1m' },
                    { type: 'all',  text: 'All' }]
            },
            tooltip: { pointFormat: "{point.y:.0f} edits" }
        },
        yAxis: { title: { text: 'Page Edits' } },
        title: { text: 'Page Edits' },
        subtitle: { text: 'subtitle' },
        series: [{ name: 'Page edits', type: 'spline', data: [] }]
    };
    $http.get('/api/page_edits/' + $scope.domain + '/' + $scope.page).success(function (data) {
        $scope.graphConfig.series[0].data = data;
    });
}]);

wikiControllers.controller('main-controller',  ['$scope', '$routeParams', '$http', function($scope, $routeParams, $http) {

    $scope.graphConfig = {
        useHighStocks: true,
        options: {
            chart: { type: 'line', zoomType: 'x' },
            navigator: { enabled:true },
            rangeSelector: {
                buttons: [{ type: 'minute', count: 5, text: '5m' },
                    { type: 'hour', count: 1, text: '1h' },
                    { type: 'hour', count: 24, text: '1d' },
                    { type: 'day', count: 3, text: '3d' },
                    { type: 'all',  text: 'All' }]
            },
            tooltip: { pointFormat: "{point.y:.0f} edits" }
        },
        yAxis: { title: { text: 'Page Edits' } },
        title: { text: 'Total Page Edits' },
        subtitle: { text: 'subtitle' },
        series: [{ name: 'Page edits', type: 'spline', data: [] }]
    };
    $("#wikidata-link").removeClass("active");
    $("#en-link").removeClass("active");
    $http.get('/api/total_edits').success(function (data) {
        $scope.graphConfig.series[0].data = data;
    });
}]);

wikiControllers.controller('top-controller',  ['$scope', '$routeParams', '$http',
        function($scope, $routeParams, $http) {
    var lastMove = new Date().getTime();

    $scope.domain = $routeParams.domain;
    $scope.graphConfig = {
        useHighStocks: true,
        options: {
            chart: {
                type: 'line',
                zoomType: 'x'
            },
            navigator: { enabled:true },
            rangeSelector: {
                buttons: [{ type: 'minute', count: 5, text: '5m' },
                    { type: 'hour', count: 1, text: '1h' },
                    { type: 'hour', count: 24, text: '1d' },
                    { type: 'day', count: 3, text: '3d' },
                    { type: 'all', text: 'All' }]
            },
            tooltip: { pointFormat: "{point.y:.0f} edits/hr" }
        },
        yAxis: { title: { text: 'Page Edits' } },
        xAxis: {
            events:{
                afterSetExtremes: function(){
                    if (new Date().getTime() - lastMove > 1000) {
                        $http.get('/api/top_pages_range/' + $scope.domain + '?start=' + this.min + '&end=' + this.max).success(function (response) {
                            $scope.pages = response;
                        });
                        $http.get('/api/top_users_range/' + $scope.domain + '?start' + this.min + '&end=' + this.max).success(function (response) {
                            $scope.users = response;
                        });
                    }
                    lastMove = new Date().getTime();
                }
            }
        },
        title: { text: 'Number of Pages Being Edited per Hour' },
        subtitle: { text: 'subtitle' },
        series: [{ name: 'Page edits', type: 'spline', data: [] }]
    };

    if ($scope.domain == "en.wikipedia") {
        $("#wikidata-link").removeClass("active");
        $("#en-link").addClass("active");
    } else {
        $("#en-link").removeClass("active");
        $("#wikidata-link").addClass("active");
    }

    $http.get('/api/channel_edits/' + $scope.domain).success(function (data) {
        $scope.graphConfig.series[0].data = data;
    });
}]);