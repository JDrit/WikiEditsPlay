var phonecatControllers = angular.module('phonecatControllersg', []);

app.controller('topController', function($scope, $http) {
    $http.get("/api/top_pages/en.wikipedia").success(function(response) {
        $scope.pages = response;
    });
    $http.get("/api/top_users/en.wikipedia").success(function(response) {
        $scope.users = response;
    });
});