package siminov.hybrid.adapter.handlers;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import siminov.connect.design.service.IService;
import siminov.connect.exception.ServiceException;
import siminov.hybrid.Constants;
import siminov.hybrid.model.HybridSiminovDatas;
import siminov.hybrid.model.HybridSiminovDatas.HybridSiminovData;
import siminov.hybrid.reader.HybridSiminovDataReader;
import siminov.hybrid.service.GenericService;
import siminov.orm.exception.SiminovException;
import siminov.orm.log.Log;

public class ServiceHandler {

	public void invoke(String data) throws ServiceException {

		HybridSiminovDataReader hybridSiminovDataParser = null; 
		data = URLDecoder.decode(data);
		
		try {
			hybridSiminovDataParser = new HybridSiminovDataReader(data);
		} catch(SiminovException siminovException) {
			Log.loge(ServiceHandler.class.getName(), "invoke", "SiminovException caught while parsing siminov hybrid core data, " + siminovException.getMessage());
			throw new ServiceException(ServiceHandler.class.getName(), "invoke", "SiminovException caught while parsing siminov hybrid core data, " + siminovException.getMessage());
		}

		HybridSiminovDatas hybridSiminovDatas = hybridSiminovDataParser.getDatas();
		
		HybridSiminovData serviceHybridData = hybridSiminovDatas.getHybridSiminovDataBasedOnDataType(Constants.SIMINOV_SERVICE_ADAPTER_INVOKE_HANDLER_SERVICE_PARAMETER);
		HybridSiminovData apiHybridData = hybridSiminovDatas.getHybridSiminovDataBasedOnDataType(Constants.SIMINOV_SERVICE_ADAPTER_INVOKE_HANDLER_API_PARAMETER);
	
		HybridSiminovData inlineResourcesHybridDatas = hybridSiminovDatas.getHybridSiminovDataBasedOnDataType(Constants.SIMINOV_SERVICE_ADAPTER_INVOKE_HANDLER_INLINE_RESOURCES);
		Iterator<HybridSiminovData>	inlineResourcesHybridData = inlineResourcesHybridDatas.getDatas();
				
				
		String service = serviceHybridData.getDataValue();
		String api = apiHybridData.getDataValue();
		
		Map<String, String> inlineResources = new HashMap<String, String>();
		while(inlineResourcesHybridData.hasNext()) {
			
			HybridSiminovData inlineResourceHybridData = inlineResourcesHybridData.next();
			inlineResources.put(inlineResourceHybridData.getDataType(), inlineResourceHybridData.getDataValue());
		}
		
		
		IService genericService = new GenericService();
		
		genericService.setService(service);
		genericService.setApi(api);
		
		for(String inlineResource: inlineResources.keySet()) {
			genericService.addResource(inlineResource, inlineResources.get(inlineResource));
		}
		
		genericService.invoke();
	}
}
