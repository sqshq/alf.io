(function () {
    "use strict";

    var BASE_TEMPLATE_URL = "/admin/partials";
    var BASE_STATIC_URL = "/resources/angularTemplates/admin/partials";

    var admin = angular.module('adminApplication', ['ui.bootstrap', 'ui.router', 'adminDirectives', 'adminServices', 'utilFilters']);

    admin.config(function($stateProvider, $urlRouterProvider) {
        $urlRouterProvider.otherwise("/");
        $stateProvider
            .state('index', {
                url: "/",
                templateUrl: BASE_TEMPLATE_URL + "/index.html"
            })
            .state('index.new-organization', {
                url: "new-organization",
                views: {
                    "newOrganization": {
                        templateUrl: BASE_STATIC_URL + "/main/edit-organization.html",
                        controller: 'CreateOrganizationController'
                    }
                }
            })
            .state('index.new-user', {
                url: "new-user",
                views: {
                    "newUser": {
                        templateUrl: BASE_STATIC_URL + "/main/edit-user.html",
                        controller: 'CreateUserController'
                    }
                }
            })
            .state('event', {
                abstract: true,
                url: '/event',
                templateUrl: BASE_STATIC_URL + "/event/index.html"
            })
            .state('event.new', {
                url: '/new',
                templateUrl: BASE_STATIC_URL + "/event/edit-event.html",
                controller: 'CreateEventController'
            })
    });

    var validationResultHandler = function(form, deferred) {
        return function(validationResult) {
            if(validationResult.errorCount > 0) {
                angular.forEach(validationResult.validationErrors, function(error) {
                    form.$setError(error.fieldName, error.message);
                });
                deferred.reject("invalid form");
            }
            deferred.resolve();
        };
    };

    var validationPerformer = function($q, validator, data, form) {
        var deferred = $q.defer();
        validator(data).success(validationResultHandler(form, deferred)).error(function(error) {
            deferred.reject(error);
        });
        return deferred.promise;
    };

    admin.controller('CreateOrganizationController', function($scope, $state, $rootScope, $q, OrganizationService) {
        $scope.organization = {};
        $scope.save = function(form, organization) {
            if(!form.$valid) {
                return;
            }
            validationPerformer($q, OrganizationService.checkOrganization, organization, form).then(function() {
                OrganizationService.createOrganization(organization).success(function() {
                    $rootScope.$emit('ReloadOrganizations', {});
                    $state.go("index");
                });
            }, angular.noop);
        };
        $scope.cancel = function() {
            $state.go("index");
        };
    });

    admin.controller('CreateUserController', function($scope, $state, $rootScope, $q, OrganizationService, UserService) {
        $scope.user = {};
        $scope.organizations = {};
        OrganizationService.getAllOrganizations().success(function(result) {
            $scope.organizations = result;
        });

        $scope.save = function(form, user) {
            if(!form.$valid) {
                return;
            }
            validationPerformer($q, UserService.checkUser, user, form).then(function() {
                UserService.createUser(user).success(function() {
                    $rootScope.$emit('ReloadUsers', {});
                    $state.go("index");
                });
            }, angular.noop);
        };

        $scope.cancel = function() {
            $state.go("index");
        };

    });

    admin.controller('CreateEventController', function($scope, $state, $rootScope, $q, OrganizationService, EventService) {
        $scope.event = {
            start: {},
            end: {}
        };
        $scope.organizations = {};

        OrganizationService.getAllOrganizations().success(function(result) {
            $scope.organizations = result;
        });

        $scope.evaluateBarType = function(index) {
            var barClasses = ['danger', 'warning', 'info', 'success'];
            if(index < barClasses.length) {
                return barClasses[index];
            }
            return index % 2 == 0 ? 'info' : 'success';
        };

        $scope.calcBarValue = function(categorySeats, eventSeats) {
            return Math.floor((categorySeats / eventSeats + 0.00001) * 10000) / 100;
        };

        $scope.event.ticketCategories = [];

        $scope.addCategory = function() {
            var lastCategory = _.last($scope.event.ticketCategories);
            var inceptionDate = moment();
            if(angular.isDefined(lastCategory)) {
                inceptionDate = moment(lastCategory.inception.date).add(1, 'days');
            }
            var category = {
                inception: {
                    date: inceptionDate.toDate()
                }, expiration: {
                    date: inceptionDate.add(1, 'days').toDate()
                }
            };
            $scope.event.ticketCategories.push(category);
        };

        $scope.canAddCategory = function(categories) {
            return _.every(categories, function(category) {
                return angular.isDefined(category.name) &&
                    angular.isDefined(category.seats) &&
                    category.seats > 0 &&
                    angular.isDefined(category.expiration.date);
            });
        };

        $scope.save = function(form, event) {
            validationPerformer($q, EventService.checkEvent, event, form).then(function() {
                EventService.createEvent(event).success(function() {
                    $state.go("index");
                });
            }, angular.noop);
        };

        $scope.cancel = function() {
            $state.go("index");
        };
    });

    admin.controller('MessageBarController', function($scope, $rootScope) {
        $rootScope.$on('Message', function(m) {
            $scope.message = m;
        });
    });

})();