(function() {

  'use strict';

  angular.module('dhib.quickstart.service.modal', ['ui.bootstrap'])
    .service('ModalService', ModalService)
    .controller('loadDataModalController', LoadDataModalController)
    .controller('entityModalController', EntityModalController)
    .controller('flowModalController', FlowModalController);

  function ModalService($uibModal) {
    var self = this;

    angular.extend(self, {
      openLoadDataModal: openLoadDataModal,
      openEntityModal: openEntityModal,
      openFlowModal: openFlowModal
    });

    function openLoadDataModal() {
      var modalInstance = $uibModal.open({
        animation: true,
        templateUrl: 'top/modal/loadDataModal.html',
        controller: 'loadDataModalController',
        size: 'sm',
        backdrop: 'static',
        keyboard: true
      });

      return modalInstance.result;
    }

    function openEntityModal() {
      var modalInstance = $uibModal.open({
        animation: true,
        templateUrl: 'top/modal/entityModal.html',
        controller: 'entityModalController',
        size: 'md',
        backdrop: 'static',
        keyboard: true
      });

      return modalInstance.result;
    }

    function openFlowModal(entityName, flowType, extension) {
      var modalInstance = $uibModal.open({
        animation: true,
        templateUrl: 'top/modal/flowModal.html',
        controller: 'flowModalController',
        size: 'sm',
        backdrop: 'static',
        keyboard: true,
        resolve: {
          'entityName': function() {
            return entityName;
          },
          'flowType': function() {
            return flowType;
          },
          'extension': function() {
            return extension;
          }
        }
      });

      return modalInstance.result;
    }
  }

  function LoadDataModalController($scope, $uibModalInstance, DataHub) {
    $scope.inputPath = 'input';
    $scope.dataFormat = 'documents';
    $scope.collection = null;

    $scope.ok = function() {
      $uibModalInstance.close({
        inputPath: $scope.inputPath,
        dataFormat: $scope.dataFormat,
        collection: $scope.collection
      });
    };

    $scope.cancel = function() {
      $uibModalInstance.dismiss();
    };

    $scope.onSelection = function(node, selected) {
      $scope.inputPath = node.path;
    };

    $scope.onNodeToggle = function(node, expanded) {
      if (expanded) {
        $scope.searchPath(node.path, node);
      }
    };

    function isLeaf(node) {
      return false;
    }

    $scope.treeOptions = {
      nodeChildren: 'children',
      dirSelectable: true,
      multiSelection: false,
      isLeaf: isLeaf,
      injectClasses: {
        ul: 'a1',
        li: 'a2',
        liSelected: 'a7',
        iExpanded: 'a3',
        iCollapsed: 'a4',
        iLeaf: 'a5',
        label: 'a6',
        labelSelected: 'a8'
      }
    };

    $scope.searchPath = function(basePath, node) {
      DataHub.searchPath(basePath).success(function(data) {
        if (node == null) { // jshint ignore:line
          //initialize root
          $scope.dataForTheTree = data.paths.slice();
        } else {
          node.children = data.paths;
        }
      });
    };

    $scope.dataForTheTree = [];
    //initialize root
    $scope.searchPath('');
  }

  function EntityModalController($scope, $uibModalInstance, DataHub) {
    $scope.entityForm = {};
    $scope.errorMessage = null;
    $scope.hasError = false;

    $scope.ok = function() {
      DataHub.saveEntity($scope.entityForm)
        .success(function() {
          $scope.hasError = false;

          $uibModalInstance.close($scope.entityForm);
        })
        .error(function(error) {
          $scope.hasError = true;
          $scope.errorMessage = error.message;
        })
        .finally(function() {
          $scope.loading = false;
        });
    };

    $scope.cancel = function() {
      $uibModalInstance.dismiss();
    };
  }

  function FlowModalController($scope, $uibModalInstance, DataHub, entityName, flowType, extension) {
    $scope.flowForm = {
      'entityName': entityName,
      'flowType': flowType,
      'extension': extension
    };
    $scope.errorMessage = null;
    $scope.hasError = false;

    $scope.ok = function() {
      $scope.loading = true;

      DataHub.saveFlow($scope.flowForm)
        .success(function() {
          $scope.hasError = false;

          $uibModalInstance.close($scope.flowForm);
        })
        .error(function(error) {
          $scope.hasError = true;
          $scope.errorMessage = error.message;
        })
        .finally(function() {
          $scope.loading = false;
        });
    };

    $scope.cancel = function() {
      $uibModalInstance.dismiss();
    };
  }

})();