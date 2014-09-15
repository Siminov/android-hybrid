package siminov.hybrid.service;

import java.util.Iterator;

import siminov.connect.connection.design.IConnectionRequest;
import siminov.connect.connection.design.IConnectionResponse;
import siminov.connect.exception.ServiceException;
import siminov.connect.model.ServiceDescriptor;
import siminov.connect.model.ServiceDescriptor.API;
import siminov.connect.service.NameValuePair;
import siminov.connect.service.Service;
import siminov.connect.service.design.IService;
import siminov.hybrid.Constants;
import siminov.hybrid.adapter.Adapter;
import siminov.hybrid.adapter.constants.HybridServiceHandler;
import siminov.hybrid.events.SiminovEventHandler;
import siminov.hybrid.model.HybridSiminovDatas;
import siminov.hybrid.model.HybridSiminovDatas.HybridSiminovData;
import siminov.hybrid.resource.Resources;
import siminov.hybrid.writter.HybridSiminovDataWritter;
import siminov.orm.exception.SiminovException;
import siminov.orm.log.Log;
import siminov.orm.utils.ClassUtils;

public class GenericService extends Service {

	public void onStart() {
		
		siminov.connect.resource.Resources connectResources = siminov.connect.resource.Resources.getInstance();
		
		ServiceDescriptor serviceDescriptor = getServiceDescriptor();
		if(serviceDescriptor == null) {
			serviceDescriptor = connectResources.requiredServiceDescriptorBasedOnName(getService());
			setServiceDescriptor(serviceDescriptor);
		}
		

		API api = serviceDescriptor.getApi(getApi());
		String apiHandler = api.getHandler();

		/*
		 * Invoke Native Handler
		 */
		IService service = getNativeHandler(apiHandler);
		if(service != null) {
			service.onStart();
		}
		
		
		/*
		 * Invoke Web Handler
		 */
		HybridSiminovDatas hybridSiminovDatas = new HybridSiminovDatas();
		
		HybridSiminovData hybridAPIHandler = new HybridSiminovData();
		hybridAPIHandler.setDataType(HybridServiceHandler.ISERVICE_API_HANDLER);
		hybridAPIHandler.setDataValue(apiHandler);

		hybridSiminovDatas.addHybridSiminovData(hybridAPIHandler);

		//Triggered Event
		HybridSiminovData triggeredEvent = new HybridSiminovData();
		
		triggeredEvent.setDataType(HybridServiceHandler.TRIGGERED_EVENT);
		triggeredEvent.setDataValue(HybridServiceHandler.ISERVICE_ON_SERVICE_START);
		
		hybridSiminovDatas.addHybridSiminovData(triggeredEvent);
		
		//Add Resources
		HybridSiminovData serviceResources = new HybridSiminovData();
		serviceResources.setDataType(HybridServiceHandler.ISERVICE_RESOURCES);
		
		
		Iterator<NameValuePair> resources = getResources();
		while(resources.hasNext()) {
			NameValuePair resource = resources.next();
			
			HybridSiminovData serviceResource = new HybridSiminovData();
			serviceResource.setDataType(resource.getName());
			serviceResource.setDataValue(resource.getValue().toString());
			
			serviceResources.addData(serviceResource);
		}

		hybridSiminovDatas.addHybridSiminovData(serviceResources);

		
		String data = null;
		try {
			data = HybridSiminovDataWritter.jsonBuidler(hybridSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(SiminovEventHandler.class.getName(), "onServiceStart", "SiminovException caught while generating json: " + siminovException.getMessage());
		}
		
		
		Adapter adapter = new Adapter();
		adapter.setAdapterName(Constants.HYBRID_SIMINOV_SERVICE_EVENT_HANDLER_ADAPTER);
		adapter.setHandlerName(Constants.HYBRID_SIMINOV_SERVICE_EVENT_HANDLER_TRIGGER_EVENT_HANDLER);
		
		adapter.addParameter(data);
		
		adapter.invoke();
	}

	public void onQueue() {

		siminov.connect.resource.Resources connectResources = siminov.connect.resource.Resources.getInstance();
		
		ServiceDescriptor serviceDescriptor = getServiceDescriptor();
		if(serviceDescriptor == null) {
			serviceDescriptor = connectResources.requiredServiceDescriptorBasedOnName(getService());
			setServiceDescriptor(serviceDescriptor);
		}
		

		API api = serviceDescriptor.getApi(getApi());
		String apiHandler = api.getHandler();

		/*
		 * Invoke Native Handler
		 */
		IService service = getNativeHandler(apiHandler);
		if(service != null) {
			service.onQueue();
		}
		
		
		/*
		 * Invoke Web Handler
		 */
		HybridSiminovDatas hybridSiminovDatas = new HybridSiminovDatas();
		
		HybridSiminovData hybridAPIHandler = new HybridSiminovData();
		hybridAPIHandler.setDataType(HybridServiceHandler.ISERVICE_API_HANDLER);
		hybridAPIHandler.setDataValue(apiHandler);

		hybridSiminovDatas.addHybridSiminovData(hybridAPIHandler);

		//Triggered Event
		HybridSiminovData triggeredEvent = new HybridSiminovData();
		
		triggeredEvent.setDataType(HybridServiceHandler.TRIGGERED_EVENT);
		triggeredEvent.setDataValue(HybridServiceHandler.ISERVICE_ON_SERVICE_QUEUE);
		
		hybridSiminovDatas.addHybridSiminovData(triggeredEvent);
		
		//Add Resources
		HybridSiminovData serviceResources = new HybridSiminovData();
		serviceResources.setDataType(HybridServiceHandler.ISERVICE_RESOURCES);
		
		
		Iterator<NameValuePair> resources = getResources();
		while(resources.hasNext()) {
			NameValuePair resource = resources.next();
			
			HybridSiminovData serviceResource = new HybridSiminovData();
			serviceResource.setDataType(resource.getName());
			serviceResource.setDataValue(resource.getValue().toString());
			
			serviceResources.addData(serviceResource);
		}

		hybridSiminovDatas.addHybridSiminovData(serviceResources);

		
		String data = null;
		try {
			data = HybridSiminovDataWritter.jsonBuidler(hybridSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(SiminovEventHandler.class.getName(), "onServiceQueue", "SiminovException caught while generating json: " + siminovException.getMessage());
		}
		
		
		Adapter adapter = new Adapter();
		adapter.setAdapterName(Constants.HYBRID_SIMINOV_SERVICE_EVENT_HANDLER_ADAPTER);
		adapter.setHandlerName(Constants.HYBRID_SIMINOV_SERVICE_EVENT_HANDLER_TRIGGER_EVENT_HANDLER);
		
		adapter.addParameter(data);
		
		adapter.invoke();
	}

	public void onPause() {

		siminov.connect.resource.Resources connectResources = siminov.connect.resource.Resources.getInstance();
		
		ServiceDescriptor serviceDescriptor = getServiceDescriptor();
		if(serviceDescriptor == null) {
			serviceDescriptor = connectResources.requiredServiceDescriptorBasedOnName(getService());
			setServiceDescriptor(serviceDescriptor);
		}
		

		API api = serviceDescriptor.getApi(getApi());
		String apiHandler = api.getHandler();

		/*
		 * Invoke Native Handler
		 */
		IService service = getNativeHandler(apiHandler);
		if(service != null) {
			service.onPause();
		}
		
		
		/*
		 * Invoke Web Handler
		 */
		HybridSiminovDatas hybridSiminovDatas = new HybridSiminovDatas();
		
		HybridSiminovData hybridAPIHandler = new HybridSiminovData();
		hybridAPIHandler.setDataType(HybridServiceHandler.ISERVICE_API_HANDLER);
		hybridAPIHandler.setDataValue(apiHandler);

		hybridSiminovDatas.addHybridSiminovData(hybridAPIHandler);
		
		//Triggered Event
		HybridSiminovData triggeredEvent = new HybridSiminovData();
		
		triggeredEvent.setDataType(HybridServiceHandler.TRIGGERED_EVENT);
		triggeredEvent.setDataValue(HybridServiceHandler.ISERVICE_ON_SERVICE_PAUSE);
		
		hybridSiminovDatas.addHybridSiminovData(triggeredEvent);
		
		
		//Add Resources
		HybridSiminovData serviceResources = new HybridSiminovData();
		serviceResources.setDataType(HybridServiceHandler.ISERVICE_RESOURCES);
		
		
		Iterator<NameValuePair> resources = getResources();
		while(resources.hasNext()) {
			NameValuePair resource = resources.next();
			
			HybridSiminovData serviceResource = new HybridSiminovData();
			serviceResource.setDataType(resource.getName());
			serviceResource.setDataValue(resource.getValue().toString());
			
			serviceResources.addData(serviceResource);
		}

		hybridSiminovDatas.addHybridSiminovData(serviceResources);


		String data = null;
		try {
			data = HybridSiminovDataWritter.jsonBuidler(hybridSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(SiminovEventHandler.class.getName(), "onServicePause", "SiminovException caught while generating json: " + siminovException.getMessage());
		}
		
		
		Adapter adapter = new Adapter();
		adapter.setAdapterName(Constants.HYBRID_SIMINOV_SERVICE_EVENT_HANDLER_ADAPTER);
		adapter.setHandlerName(Constants.HYBRID_SIMINOV_SERVICE_EVENT_HANDLER_TRIGGER_EVENT_HANDLER);
		
		adapter.addParameter(data);
		
		adapter.invoke();
	}

	public void onResume() {

		siminov.connect.resource.Resources connectResources = siminov.connect.resource.Resources.getInstance();
		
		ServiceDescriptor serviceDescriptor = getServiceDescriptor();
		if(serviceDescriptor == null) {
			serviceDescriptor = connectResources.requiredServiceDescriptorBasedOnName(getService());
			setServiceDescriptor(serviceDescriptor);
		}
		

		API api = serviceDescriptor.getApi(getApi());
		String apiHandler = api.getHandler();

		/*
		 * Invoke Native Handler
		 */
		IService service = getNativeHandler(apiHandler);
		if(service != null) {
			service.onResume();
		}
		
		
		/*
		 * Invoke Web Handler
		 */
		HybridSiminovDatas hybridSiminovDatas = new HybridSiminovDatas();
		
		HybridSiminovData hybridAPIHandler = new HybridSiminovData();
		hybridAPIHandler.setDataType(HybridServiceHandler.ISERVICE_API_HANDLER);
		hybridAPIHandler.setDataValue(apiHandler);

		hybridSiminovDatas.addHybridSiminovData(hybridAPIHandler);
		
		//Triggered Event
		HybridSiminovData triggeredEvent = new HybridSiminovData();
		
		triggeredEvent.setDataType(HybridServiceHandler.TRIGGERED_EVENT);
		triggeredEvent.setDataValue(HybridServiceHandler.ISERVICE_ON_SERVICE_RESUME);
		
		hybridSiminovDatas.addHybridSiminovData(triggeredEvent);
		
		//Add Resources
		HybridSiminovData serviceResources = new HybridSiminovData();
		serviceResources.setDataType(HybridServiceHandler.ISERVICE_RESOURCES);
		
		
		Iterator<NameValuePair> resources = getResources();
		while(resources.hasNext()) {
			NameValuePair resource = resources.next();
			
			HybridSiminovData serviceResource = new HybridSiminovData();
			serviceResource.setDataType(resource.getName());
			serviceResource.setDataValue(resource.getValue().toString());
			
			serviceResources.addData(serviceResource);
		}

		hybridSiminovDatas.addHybridSiminovData(serviceResources);

		
		String data = null;
		try {
			data = HybridSiminovDataWritter.jsonBuidler(hybridSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(SiminovEventHandler.class.getName(), "onServiceResume", "SiminovException caught while generating json: " + siminovException.getMessage());
		}
		
		
		Adapter adapter = new Adapter();
		adapter.setAdapterName(Constants.HYBRID_SIMINOV_SERVICE_EVENT_HANDLER_ADAPTER);
		adapter.setHandlerName(Constants.HYBRID_SIMINOV_SERVICE_EVENT_HANDLER_TRIGGER_EVENT_HANDLER);
		
		adapter.addParameter(data);
		
		adapter.invoke();
	}

	public void onFinish() {

		siminov.connect.resource.Resources conncetResources = siminov.connect.resource.Resources.getInstance();
		
		ServiceDescriptor serviceDescriptor = getServiceDescriptor();
		if(serviceDescriptor == null) {
			serviceDescriptor = conncetResources.requiredServiceDescriptorBasedOnName(getService());
			setServiceDescriptor(serviceDescriptor);
		}
		

		API api = serviceDescriptor.getApi(getApi());
		String apiHandler = api.getHandler();

		/*
		 * Invoke Native Handler
		 */
		IService service = getNativeHandler(apiHandler);
		if(service != null) {
			service.onFinish();
		}
		
		
		/*
		 * Invoke Web Handler
		 */
		HybridSiminovDatas hybridSiminovDatas = new HybridSiminovDatas();
		
		HybridSiminovData hybridAPIHandler = new HybridSiminovData();
		hybridAPIHandler.setDataType(HybridServiceHandler.ISERVICE_API_HANDLER);
		hybridAPIHandler.setDataValue(apiHandler);

		hybridSiminovDatas.addHybridSiminovData(hybridAPIHandler);
		
		//Triggered Event
		HybridSiminovData triggeredEvent = new HybridSiminovData();
		
		triggeredEvent.setDataType(HybridServiceHandler.TRIGGERED_EVENT);
		triggeredEvent.setDataValue(HybridServiceHandler.ISERVICE_ON_SERVICE_PAUSE);
		
		hybridSiminovDatas.addHybridSiminovData(triggeredEvent);
		
		//Add Resources
		HybridSiminovData serviceResources = new HybridSiminovData();
		serviceResources.setDataType(HybridServiceHandler.ISERVICE_RESOURCES);
		
		
		Iterator<NameValuePair> resources = getResources();
		while(resources.hasNext()) {
			NameValuePair resource = resources.next();
			
			HybridSiminovData serviceResource = new HybridSiminovData();
			serviceResource.setDataType(resource.getName());
			serviceResource.setDataValue(resource.getValue().toString());
			
			serviceResources.addData(serviceResource);
		}

		hybridSiminovDatas.addHybridSiminovData(serviceResources);

		
		String data = null;
		try {
			data = HybridSiminovDataWritter.jsonBuidler(hybridSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(SiminovEventHandler.class.getName(), "onServicePause", "SiminovException caught while generating json: " + siminovException.getMessage());
		}
		
		
		Adapter adapter = new Adapter();
		adapter.setAdapterName(Constants.HYBRID_SIMINOV_SERVICE_EVENT_HANDLER_ADAPTER);
		adapter.setHandlerName(Constants.HYBRID_SIMINOV_SERVICE_EVENT_HANDLER_TRIGGER_EVENT_HANDLER);
		
		adapter.addParameter(data);
		
		adapter.invoke();
	}

	public void onApiInvoke(IConnectionRequest connectionRequest) {

		siminov.connect.resource.Resources connectResources = siminov.connect.resource.Resources.getInstance();
		Resources hybridResources = Resources.getInstance();
		
		ServiceDescriptor serviceDescriptor = getServiceDescriptor();
		if(serviceDescriptor == null) {
			serviceDescriptor = connectResources.requiredServiceDescriptorBasedOnName(getService());
			setServiceDescriptor(serviceDescriptor);
		}
		

		API api = serviceDescriptor.getApi(getApi());
		String apiHandler = api.getHandler();

		/*
		 * Invoke Native Handler
		 */
		IService service = getNativeHandler(apiHandler);
		if(service != null) {
			service.onApiInvoke(connectionRequest);
		}
		
		
		/*
		 * Invoke Web Handler
		 */
		HybridSiminovDatas hybridSiminovDatas = new HybridSiminovDatas();
		
		//Triggered Event
		HybridSiminovData triggeredEvent = new HybridSiminovData();
		
		triggeredEvent.setDataType(HybridServiceHandler.TRIGGERED_EVENT);
		triggeredEvent.setDataValue(HybridServiceHandler.ISERVICE_ON_SERVICE_API_INVOKE);
		
		hybridSiminovDatas.addHybridSiminovData(triggeredEvent);
		
		HybridSiminovData hybridAPIHandler = new HybridSiminovData();
		hybridAPIHandler.setDataType(HybridServiceHandler.ISERVICE_API_HANDLER);
		hybridAPIHandler.setDataValue(apiHandler);

		hybridSiminovDatas.addHybridSiminovData(hybridAPIHandler);
		
		hybridSiminovDatas.addHybridSiminovData(hybridResources.generateHybridConnectionRequest(connectionRequest));

		//Add Resources
		HybridSiminovData serviceResources = new HybridSiminovData();
		serviceResources.setDataType(HybridServiceHandler.ISERVICE_RESOURCES);
		
		
		Iterator<NameValuePair> resources = getResources();
		while(resources.hasNext()) {
			NameValuePair resource = resources.next();
			
			HybridSiminovData serviceResource = new HybridSiminovData();
			serviceResource.setDataType(resource.getName());
			serviceResource.setDataValue(resource.getValue().toString());
			
			serviceResources.addData(serviceResource);
		}

		hybridSiminovDatas.addHybridSiminovData(serviceResources);

		
		String data = null;
		try {
			data = HybridSiminovDataWritter.jsonBuidler(hybridSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(SiminovEventHandler.class.getName(), "onServiceApiInvoke", "SiminovException caught while generating json: " + siminovException.getMessage());
		}
		
		
		Adapter adapter = new Adapter();
		adapter.setAdapterName(Constants.HYBRID_SIMINOV_SERVICE_EVENT_HANDLER_ADAPTER);
		adapter.setHandlerName(Constants.HYBRID_SIMINOV_SERVICE_EVENT_HANDLER_TRIGGER_EVENT_HANDLER);
		
		adapter.addParameter(data);
		
		adapter.invoke();
	}

	public void onApiFinish(IConnectionResponse connectionResponse) {

		siminov.connect.resource.Resources connectResources = siminov.connect.resource.Resources.getInstance();
		Resources hybridResources = Resources.getInstance();
		
		ServiceDescriptor serviceDescriptor = getServiceDescriptor();
		if(serviceDescriptor == null) {
			serviceDescriptor = connectResources.requiredServiceDescriptorBasedOnName(getService());
			setServiceDescriptor(serviceDescriptor);
		}
		

		API api = serviceDescriptor.getApi(getApi());
		String apiHandler = api.getHandler();

		/*
		 * Invoke Native Handler
		 */
		IService service = getNativeHandler(apiHandler);
		if(service != null) {
			service.onApiFinish(connectionResponse);
		}
		
		
		/*
		 * Invoke Web Handler
		 */
		HybridSiminovDatas hybridSiminovDatas = new HybridSiminovDatas();
		
		HybridSiminovData hybridAPIHandler = new HybridSiminovData();
		hybridAPIHandler.setDataType(HybridServiceHandler.ISERVICE_API_HANDLER);
		hybridAPIHandler.setDataValue(apiHandler);

		hybridSiminovDatas.addHybridSiminovData(hybridAPIHandler);
		
		//Triggered Event
		HybridSiminovData triggeredEvent = new HybridSiminovData();
		
		triggeredEvent.setDataType(HybridServiceHandler.TRIGGERED_EVENT);
		triggeredEvent.setDataValue(HybridServiceHandler.ISERVICE_ON_SERVICE_API_FINISH);
		
		hybridSiminovDatas.addHybridSiminovData(triggeredEvent);

		hybridSiminovDatas.addHybridSiminovData(hybridResources.generateHybridConnectionResponse(connectionResponse));
		
		
		//Event Resources
		HybridSiminovData serviceResources = new HybridSiminovData();
		serviceResources.setDataType(HybridServiceHandler.ISERVICE_RESOURCES);
		
		Iterator<NameValuePair> resources = getResources();
		while(resources.hasNext()) {
			NameValuePair resource = resources.next();
			
			HybridSiminovData serviceResource = new HybridSiminovData();
			serviceResource.setDataType(resource.getName());
			serviceResource.setDataValue(resource.getValue().toString());
			
			serviceResources.addData(serviceResource);
		}

		hybridSiminovDatas.addHybridSiminovData(serviceResources);
		
		
		String data = null;
		try {
			data = HybridSiminovDataWritter.jsonBuidler(hybridSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(SiminovEventHandler.class.getName(), "onServiceApiFinish", "SiminovException caught while generating json: " + siminovException.getMessage());
		}
		
		
		Adapter adapter = new Adapter();
		adapter.setAdapterName(Constants.HYBRID_SIMINOV_SERVICE_EVENT_HANDLER_ADAPTER);
		adapter.setHandlerName(Constants.HYBRID_SIMINOV_SERVICE_EVENT_HANDLER_TRIGGER_EVENT_HANDLER);
		
		adapter.addParameter(data);
		
		adapter.invoke();
	}

	public void onTerminate(ServiceException serviceException) {

		siminov.connect.resource.Resources conncetResources = siminov.connect.resource.Resources.getInstance();
		
		ServiceDescriptor serviceDescriptor = getServiceDescriptor();
		if(serviceDescriptor == null) {
			serviceDescriptor = conncetResources.requiredServiceDescriptorBasedOnName(getService());
			setServiceDescriptor(serviceDescriptor);
		}
		

		API api = serviceDescriptor.getApi(getApi());
		String apiHandler = api.getHandler();

		/*
		 * Invoke Native Handler
		 */
		IService service = getNativeHandler(apiHandler);
		if(service != null) {
			service.onFinish();
		}
		
		
		/*
		 * Invoke Web Handler
		 */
		HybridSiminovDatas hybridSiminovDatas = new HybridSiminovDatas();
		
		HybridSiminovData hybridAPIHandler = new HybridSiminovData();
		hybridAPIHandler.setDataType(HybridServiceHandler.ISERVICE_API_HANDLER);
		hybridAPIHandler.setDataValue(apiHandler);

		hybridSiminovDatas.addHybridSiminovData(hybridAPIHandler);
		
		//Triggered Event
		HybridSiminovData triggeredEvent = new HybridSiminovData();
		
		triggeredEvent.setDataType(HybridServiceHandler.TRIGGERED_EVENT);
		triggeredEvent.setDataValue(HybridServiceHandler.ISERVICE_ON_SERVICE_TERMINATE);
		
		hybridSiminovDatas.addHybridSiminovData(triggeredEvent);
		
		//Add Resources
		HybridSiminovData serviceResources = new HybridSiminovData();
		serviceResources.setDataType(HybridServiceHandler.ISERVICE_RESOURCES);
		
		
		Iterator<NameValuePair> resources = getResources();
		while(resources.hasNext()) {
			NameValuePair resource = resources.next();
			
			HybridSiminovData serviceResource = new HybridSiminovData();
			serviceResource.setDataType(resource.getName());
			serviceResource.setDataValue(resource.getValue().toString());
			
			serviceResources.addData(serviceResource);
		}

		hybridSiminovDatas.addHybridSiminovData(serviceResources);

		
		String data = null;
		try {
			data = HybridSiminovDataWritter.jsonBuidler(hybridSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(SiminovEventHandler.class.getName(), "onServicePause", "SiminovException caught while generating json: " + siminovException.getMessage());
		}
		
		
		Adapter adapter = new Adapter();
		adapter.setAdapterName(Constants.HYBRID_SIMINOV_SERVICE_EVENT_HANDLER_ADAPTER);
		adapter.setHandlerName(Constants.HYBRID_SIMINOV_SERVICE_EVENT_HANDLER_TRIGGER_EVENT_HANDLER);
		
		adapter.addParameter(data);
		
		adapter.invoke();
	}
	
	private IService getNativeHandler(String apiHandler) {
		
		Class<?> classObject = null;
		try {
			classObject = Class.forName(apiHandler);
		} catch(Exception exception) {
			Log.debug(ClassUtils.class.getName(), "getNativeHandlerEvent", "Exception caught while creating service native handler object, API-HANDLER: " + apiHandler + ", " + exception.getMessage());
			return null;
		}
		

		Object object = null;
		try {
			object = classObject.newInstance();
		} catch(Exception exception) {
			Log.debug(ClassUtils.class.getName(), "getNativeHandlerEvent", "Exception caught while creating service native handler object, API-HANDLER: " + apiHandler + ", " + exception.getMessage());
			return null;
		}

		return (IService) object;
	}

	public void onRestart() {
		
	}
}
