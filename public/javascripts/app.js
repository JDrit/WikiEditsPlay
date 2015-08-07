
var wikiApp = angular.module('wikiApp', ['ngRoute', 'ngResource', 'highcharts-ng', 'wikiControllers']);

wikiApp.filter('escape', function() {
    return window.encodeURIComponent;
});

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
                templateUrl: '/assets/partials/channel-overview.html'
            }).
            when('/page/:domain/:page*', {
                templateUrl: '/assets/partials/page-overview.html'
            }).
            otherwise({
                redirectTo: '/'
            });
    }]);
