/** 
 * [SIMINOV FRAMEWORK]
 * Copyright [2015] [Siminov Software Solution LLP|support@siminov.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/



/**
	It contain all class related to Siminov Framework resource.
	
	@module Resource
*/

var win;
var dom;

try {

    if(!window) {
    	window = global || window;
    }

	win = window;
	dom = window['document'];
} catch(e) {
	win = Ti.App.Properties;
}


if(dom == undefined) {
    var Callback = require('../Callback');
    var Adapter = require('../Adapter/Adapter');
    var Constants = require('../Constants');
    var Transaction = require('../Database/Transaction');
    var SIDatasHelper = require('../ReaderWriter/SIDatasHelper');
}


var resourceManager = null;

ResourceManager.getInstance = function() {
    
    if(resourceManager == null) {
        resourceManager = new ResourceManager();
    }
    
    return resourceManager;
}



var getInstance = function() {
    
    if(resourceManager == null) {
        resourceManager = new ResourceManager();
    }
    
    return resourceManager;
}


/**
	It handles and provides all resources needed by SIMINOV HYBRID.
	
	@module Resource
	@class Resources
	@constructor
	
*/
function ResourceManager() {
		
    /**
        Get Application Descriptor object of application.
	 		
        @method getApplicationDescriptor
        @return {ApplicationDescriptor} Application Descriptor.
    */
    var getApplicationDescriptor = function() {

        var callback = arguments && arguments[0];
        var transaction = arguments && arguments[1];

        var adapter = new Adapter();
        adapter.setAdapterName(Constants.RESOURCE_ADAPTER);
        adapter.setHandlerName(Constants.RESOURCE_GET_APPLICATION_DESCRIPTOR_HANDLER);
	
	
        if(transaction) {
            adapter.setCallback(getApplicationDescriptorCallback);
            adapter.setAdapterMode(Adapter.REQUEST_ASYNC_MODE);
				
            transaction.addRequest(adapter);
        } else if(callback) {
            adapter.setCallback(getApplicationDescriptorCallback);
            adapter.setAdapterMode(Adapter.REQUEST_ASYNC_MODE);
				
            Adapter.invoke(adapter);
        } else {
            var data = Adapter.invoke(adapter);
            return getApplicationDescriptorCallback(data);
        }
	
	
        function getApplicationDescriptorCallback(data) {
			
            var datas = dom == undefined?JSON.parse(eval('(' + data + ')')):JSON.parse(data);
            var applicationDescriptor = SIDatasHelper.toModels(datas);
		        
            if(callback) {
                callback && callback.onSuccess && callback.onSuccess(applicationDescriptor[0]);
            } else {
                return applicationDescriptor[0];
            }
        }
    };
	
	
    var getApplicationDescriptorAsync = function(callback, transaction) {
        this.getApplicationDescriptor(callback?callback:new Callback(), transaction);
    };
		
		
    /**
        Get Database Descriptor based on database descriptor name provided as per defined in Database Descriptor file.
				
        Example: DatabaseDescriptor.si.xml
				
            <database-descriptor>
				
                <property name="database_name">SIMINOV-HYBRID-SAMPLE</property>
					
            </database-descriptor>
		 
        @method getDatabaseDescriptor
        @param databaseDescriptorName Database Descriptor object based on database descriptor name provided.
        @return {DatabaseDescriptor} Database Descriptor
    */
    var getDatabaseDescriptor = function(databaseName) {
	
        var callback = arguments && arguments[1];
	
        var adapter = new Adapter();
        adapter.setAdapterName(Constants.RESOURCE_ADAPTER);
        adapter.setHandlerName(Constants.RESOURCE_GET_DATABASE_DESCRIPTOR_BASED_ON_NAME_HANDLER);
	
        adapter.addParameter(databaseName);
	
			
        if(callback) {
            adapter.setCallback(getDatabaseDescriptorCallback);
            adapter.setAdapterMode(Adapter.REQUEST_ASYNC_MODE);
				
            Adapter.invoke(adapter);
        } else {
            var data = Adapter.invoke(adapter);
            return getDatabaseDescriptorCallback(data);
        }
		
			
        function getDatabaseDescriptorCallback(data) {
			
            var datas = JSON.parse(eval('(' + data + ')'));
            var databaseDescriptor = SIDatasHelper.toModels(datas);

            if(callback) {
                callback && callback.onSuccess && callback.onSuccess(databaseDescriptor[0]);
            } else {
                return databaseDescriptor[0];
            }
        }
    };
	    
	    
    var getDatabaseDescriptorAsync = function(databaseName, callback, transaction) {
        this.getDatabaseDescriptor(databaseName, callback?callback:new Callback(), transaction);
    };
	
	
    var getDatabaseDescriptorBasedOnClassName = function(className) {
			
        var callback = arguments && arguments[1];
        var transaction = arguments && arguments[2];
	
        var adapter = new Adapter();
        adapter.setAdapterName(Constants.RESOURCE_ADAPTER);
        adapter.setHandlerName(Constants.RESOURCE_GET_DATABASE_DESCRIPTOR_BASED_ON_CLASS_NAME_HANDLER);
	
        adapter.addParameter(className);
	
			
        if(transaction) {
            var parameters = adapter.getParameters();
			
            var siminovDatas = Object.create(HybridSiminovDatas);
            siminovDatas.datas = new Array();
				
            for(var i = 0;i < parameters.length;i++) {
				
                var parameter = parameters[i];
                if(parameter != undefined) {
                    parameter = encodeURI(parameters[i]);
                } else {
                    parameter = "";
                }
					
                var siminovData = Object.create(HybridSiminovDatas.HybridSiminovData);
                siminovData.value = parameter;
			        
                siminovDatas.datas.push(siminovData);
            }
		
            adapter.removeParameters();
            adapter.addParameter(JSON.stringify(siminovDatas));
			
            adapter.setCallback(getDatabaseDescriptorBasedOnClassNameCallback);
            transaction.addRequest(adapter);
        } else if(callback) {
            adapter.setCallback(getDatabaseDescriptorBasedOnClassNameCallback);
            adapter.setAdapterMode(Adapter.REQUEST_ASYNC_MODE);
				
            Adapter.invoke(adapter);
        } else {
            var data = Adapter.invoke(adapter);
            return getDatabaseDescriptorBasedOnClassNameCallback(data);
        }
		
			
        function getDatabaseDescriptorBasedOnClassNameCallback(data) {
			
            var datas = JSON.parse(eval('(' + data + ')'));
            var databaseDescriptor = SIDatasHelper.toModels(datas);

            if(callback) {
                callback && callback.onSuccess && callback.onSuccess(databaseDescriptor[0]);
            } else {
                return databaseDescriptor[0];
            }
        }
    };
		
		
    var getDatabaseDescriptorBasedOnClassNameAsync = function(databaseName, callback, transaction) {
        this.getDatabaseDescriptorBasedOnClassName(databaseName, callback?callback:new Callback(), transaction);
    };


    var getDatabaseDescriptorBasedOnTableName = function(tableName) {
			
        var callback = arguments && arguments[1];
        var transaction = arguments && arguments[2];
	
        var adapter = new Adapter();
        adapter.setAdapterName(Constants.RESOURCE_ADAPTER);
        adapter.setHandlerName(Constants.RESOURCE_GET_DATABASE_DESCRIPTOR_BASED_ON_TABLE_NAME_HANDLER);
	
        adapter.addParameter(tableName);
	
			
        if(transaction) {
            var parameters = adapter.getParameters();
			
            var siminovDatas = Object.create(HybridSiminovDatas);
            siminovDatas.datas = new Array();
				
            for(var i = 0;i < parameters.length;i++) {
				
                var parameter = parameters[i];
                if(parameter != undefined) {
                    parameter = encodeURI(parameters[i]);
                } else {
                    parameter = "";
                }
					
                var siminovData = Object.create(HybridSiminovDatas.HybridSiminovData);
                siminovData.value = parameter;
			        
                siminovDatas.datas.push(siminovData);
            }
		
            adapter.removeParameters();
            adapter.addParameter(JSON.stringify(siminovDatas));
			
            adapter.setCallback(getDatabaseDescriptorBasedOnTableNameCallback);
            transaction.addRequest(adapter);
        } else if(callback) {
            adapter.setCallback(getDatabaseDescriptorBasedOnTableNameCallback);
            adapter.setAdapterMode(Adapter.REQUEST_ASYNC_MODE);
				
            Adapter.invoke(adapter);
        } else {
            var data = Adapter.invoke(adapter);
            return getDatabaseDescriptorBasedOnTableNameCallback(data);
        }
		
			
        function getDatabaseDescriptorBasedOnTableNameCallback(data) {
			
            var datas = JSON.parse(eval('(' + data + ')'));
            var databaseDescriptor = SIDatasHelper.toModels(datas);

            if(callback) {
                callback && callback.onSuccess && callback.onSuccess(databaseDescriptor[0]);
            } else {
                return databaseDescriptor[0];
            }
        }
    };
		
		
    var getDatabaseDescriptorBasedOnTableNameAsync = function(databaseName, callback, transaction) {
        this.getDatabaseDescriptorBasedOnTableName(databaseName, callback?callback:new Callback(), transaction);
    };



    /**
        Get Entity Descriptor based on mapped class name provided.

        @method getEntityDescriptorBasedOnClassName
        @param className {String} POJO class name.
        @return {EntityDescriptor} Entity Descriptor object in respect to mapped class name.
    */
    var getEntityDescriptorBasedOnClassName = function(className) {
	
        var callback = arguments && arguments[1];
        var transaction = arguments && arguments[2];
	
        var adapter = new Adapter();
        adapter.setAdapterName(Constants.RESOURCE_ADAPTER);
        adapter.setHandlerName(Constants.RESOURCE_GET_ENTITY_DESCRIPTOR_BASED_ON_CLASS_NAME_HANDLER);
	
        adapter.addParameter(className);
	
	
        if(transaction) {
            var parameters = adapter.getParameters();
			
            var siminovDatas = Object.create(HybridSiminovDatas);
            siminovDatas.datas = new Array();
				
            for(var i = 0;i < parameters.length;i++) {
				
                var parameter = parameters[i];
                if(parameter != undefined) {
                    parameter = encodeURI(parameters[i]);
                } else {
                    parameter = "";
                }
					
                var siminovData = Object.create(HybridSiminovDatas.HybridSiminovData);
                siminovData.value = parameter;
			        
                siminovDatas.datas.push(siminovData);
            }
		
            adapter.removeParameters();
            adapter.addParameter(JSON.stringify(siminovDatas));
			
            adapter.setCallback(getEntityDescriptorBasedOnClassNameCallback);
            transaction.addRequest(adapter);
        } else if(callback) {
            adapter.setCallback(getEntityDescriptorBasedOnClassNameCallback);
            adapter.setAdapterMode(Adapter.REQUEST_ASYNC_MODE);
				
            Adapter.invoke(adapter);
        } else {
            var data = Adapter.invoke(adapter);
            return getEntityDescriptorBasedOnClassNameCallback(data);
        }
	
        function getEntityDescriptorBasedOnClassNameCallback(data) {
			
            var datas = JSON.parse(eval('(' + data + ')'));
            var entityDescriptor = SIDatasHelper.toModels(datas);

            if(callback) {
                callback && callback.onSuccess && callback.onSuccess(entityDescriptor[0]);
            } else {
                return entityDescriptor[0];
            }
        }
    };
	    
	    
    var getEntityDescriptorBasedOnClassNameAsync = function(className, callback, transaction) {
        this.getEntityDescriptorBasedOnClassName(className, callback?callback:new Callback(), transaction);
    };
	
		
    /**
        Get Entity Descriptor based on table name provided.

        @method getEntityDescriptorBasedOnTableName
        @param tableName {String} Name of table.
        @return {EntityDescriptor} Entity Descriptor object in respect to table name.
    */
    var getEntityDescriptorBasedOnTableName = function(tableName) {
	
        var callback = arguments && arguments[1];
        var transaction = arguments && arguments[2];
	
        var adapter = new Adapter();
        adapter.setAdapterName(Constants.RESOURCE_ADAPTER);
        adapter.setHandlerName(Constants.RESOURCE_GET_ENTITY_DESCRIPTOR_BASED_ON_TABLE_NAME_HANDLER);
	
        adapter.addParameter(tableName);
	
			
        if(transaction) {
            var parameters = adapter.getParameters();
			
            var siminovDatas = Object.create(HybridSiminovDatas);
            siminovDatas.datas = new Array();
				
            for(var i = 0;i < parameters.length;i++) {
				
                var parameter = parameters[i];
                if(parameter != undefined) {
                    parameter = encodeURI(parameters[i]);
                } else {
                    parameter = "";
                }
					
                var siminovData = Object.create(HybridSiminovDatas.HybridSiminovData);
                siminovData.value = parameter;
			        
                siminovDatas.datas.push(siminovData);
            }
		
            adapter.removeParameters();
            adapter.addParameter(JSON.stringify(siminovDatas));
			
            adapter.setCallback(getEntityDescriptorBasedOnTableNameCallback);
            transaction.addRequest(adapter);
        } else if(callback) {
            adapter.setCallback(getEntityDescriptorBasedOnTableNameCallback);
            adapter.setAdapterMode(Adapter.REQUEST_ASYNC_MODE);
				
            Adapter.invoke(adapter);
        } else {
            var data = Adapter.invoke(adapter);
            return getEntityDescriptorBasedOnTableNameCallback(data);
        }
	
	
        function getEntityDescriptorBasedOnTableNameCallback(data) {
			
            var datas = JSON.parse(eval('(' + data + ')'));
            var entityDescriptor = SIDatasHelper.toModels(datas);
		        
            if(callback) {
                callback && callback.onSuccess && callback.onSuccess(entityDescriptor[0]);
            } else {
                return entityDescriptor[0];
            }
        }
    };
	    
	    
    var getEntityDescriptorBasedOnTableNameAsync = function(tableName, callback, transaction) {
        this.getEntityDescriptorBasedOnTableName(tableName, callback, transaction);
    };
    
    
    return {
        getApplicationDescriptor: getApplicationDescriptor,
        getApplicationDescriptorAsync: getApplicationDescriptorAsync,
        getDatabaseDescriptor: getDatabaseDescriptor,
        getDatabaseDescriptorAsync: getDatabaseDescriptorAsync,
    	getDatabaseDescriptorBasedOnClassName: getDatabaseDescriptorBasedOnClassName,
        getDatabaseDescriptorBasedOnClassNameAsync: getDatabaseDescriptorBasedOnClassNameAsync,
        getDatabaseDescriptorBasedOnTableName: getDatabaseDescriptorBasedOnTableName,
        getDatabaseDescriptorBasedOnTableNameAsync: getDatabaseDescriptorBasedOnTableNameAsync,
        getEntityDescriptorBasedOnClassName: getEntityDescriptorBasedOnClassName,
        getEntityDescriptorBasedOnClassNameAsync: getEntityDescriptorBasedOnClassNameAsync,
        getEntityDescriptorBasedOnTableName: getEntityDescriptorBasedOnTableName,
        getEntityDescriptorBasedOnTableNameAsync: getEntityDescriptorBasedOnTableNameAsync
    };
}


if(dom == undefined) {
    exports.getInstance = getInstance;    
}

