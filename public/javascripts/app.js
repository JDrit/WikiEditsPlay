
var wikiApp = angular.module('wikiApp', ['ngRoute', 'ngResource', 'highcharts-ng', 'wikiControllers']);

wikiApp.config(['$routeProvider',
    function($routeProvider) {
        $routeProvider.
            when('/', {
                templateUrl: '/assets/partials/wiki-overview.html'
            }).
            when('/domain', {
                redirectTo: '/domain/en.wikipedia'
            }).
            when('/domain/:domain', {
                templateUrl: '/assets/partials/channel-overview.html',
                controller: 'top-controller'
            }).
            otherwise({
                redirectTo: '/'
            });
    }]);