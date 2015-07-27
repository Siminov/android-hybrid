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



package siminov.web.adapter.handlers;

import java.net.URLDecoder;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import siminov.core.Constants;
import siminov.core.database.DatabaseBundle;
import siminov.core.database.design.IDatabaseImpl;
import siminov.core.database.design.IQueryBuilder;
import siminov.core.exception.DatabaseException;
import siminov.core.exception.DeploymentException;
import siminov.core.exception.SiminovException;
import siminov.core.log.Log;
import siminov.core.model.DatabaseDescriptor;
import siminov.core.model.EntityDescriptor;
import siminov.core.model.EntityDescriptor.Attribute;
import siminov.core.model.EntityDescriptor.Relationship;
import siminov.core.resource.ResourceManager;
import siminov.web.adapter.IAdapter;
import siminov.web.model.WebSiminovDatas;
import siminov.web.model.WebSiminovDatas.WebSiminovData;
import siminov.web.model.WebSiminovDatas.WebSiminovData.WebSiminovValue;
import siminov.web.reader.WebSiminovDataReader;
import siminov.web.writter.WebSiminovDataWritter;


/**
 * It handles all request related to database.
 * LIKE: save, update, saveOrUpdate, delete.
 */
public class DatabaseHandler implements IAdapter {

	private static ResourceManager coreResourceManager = ResourceManager.getInstance();
	private static siminov.web.resource.ResourceManager webResourceManager = siminov.web.resource.ResourceManager.getInstance();

	
	/**
	 * Handles Database Save Request From Web.
	 * @param data Data Need To Be Save In Database.
	 * @throws DatabaseException If any exception occur while saving data in Database.
	 */
	public void save(final String data) throws DatabaseException {

		WebSiminovDatas webSiminovDatas = parseWebSiminovDatas(data);
		save(webSiminovDatas);
		
	}
	
	private void save(WebSiminovDatas webSiminovDatas) throws DatabaseException {
		
		Iterator<WebSiminovData> webDatas = webSiminovDatas.getWebSiminovDatas();
		while(webDatas.hasNext()) {
			save(webDatas.next());
		}
		
	}

	private void save(WebSiminovData webSiminovData) throws DatabaseException {
		
		String className = webSiminovData.getDataType();
		Iterator<WebSiminovValue> webValues = webSiminovData.getValues();
		
		Map<String, WebSiminovValue> webSiminovValues = new HashMap<String, WebSiminovValue>();
		while(webValues.hasNext()) {
			WebSiminovValue webSiminovValue = webValues.next();
			webSiminovValues.put(webSiminovValue.getType(), webSiminovValue);
		}
		
		
		EntityDescriptor entityDescriptor = getEntityDescriptor(className);
		DatabaseDescriptor databaseDescriptor = getDatabaseDescriptor(className);
		
		DatabaseBundle databaseBundle = coreResourceManager.getDatabaseBundle(databaseDescriptor.getDatabaseName());
		IDatabaseImpl database = databaseBundle.getDatabase();
		IQueryBuilder queryBuilder = databaseBundle.getQueryBuilder();
		
		String tableName = entityDescriptor.getTableName();
		
		Collection<String> columnNames = new LinkedList<String>();
		Collection<Object> columnValues = new LinkedList<Object>();

		Iterator<EntityDescriptor.Attribute> attributes = entityDescriptor.getAttributes();
		while(attributes.hasNext()) {
			Attribute attribute = attributes.next();
			
			String columnName = attribute.getColumnName();
			Object columnValue = webSiminovValues.get(attribute.getVariableName()).getValue();
			
			columnNames.add(columnName);
			columnValues.add(columnValue);
			
		}

			
		processManyToOneRelationship(webSiminovData, columnNames, columnValues);
		processManyToManyRelationship(webSiminovData, columnNames, columnValues);

		
		/*
		 * Add Parameters
		 */
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(IQueryBuilder.FORM_SAVE_BIND_QUERY_TABLE_NAME_PARAMETER, tableName);
		parameters.put(IQueryBuilder.FORM_SAVE_BIND_QUERY_COLUMN_NAMES_PARAMETER, columnNames.iterator());

		String query = queryBuilder.formSaveBindQuery(parameters);
		database.executeBindQuery(databaseDescriptor, entityDescriptor, query, columnValues.iterator());

		

		/*
		 * 5. Check for relationship's if any, IF EXISTS: process it, ELSE: return all objects.
		 */
		Iterator<EntityDescriptor.Relationship> relationships = entityDescriptor.getRelationships();
		while(relationships.hasNext()) {
			EntityDescriptor.Relationship relationship = relationships.next();
			
			boolean isLoad = relationship.isLoad();
			if(!isLoad) {
				continue;
			}
			
			String relationshipType = relationship.getType();
			if(relationshipType == null || relationshipType.length() <= 0) {
				continue;
			}
			
			if(relationshipType.equalsIgnoreCase(Constants.ENTITY_DESCRIPTOR_RELATIONSHIP_TYPE_ONE_TO_ONE)) {
				
				WebSiminovData referedData = null;
				Iterator<WebSiminovData> datas = webSiminovData.getDatas();
				while(datas.hasNext()) {
					
					WebSiminovData data = datas.next();
					
					String mappedClassName = webResourceManager.getMappedNativeClassName(data.getDataType());
					if(mappedClassName.equalsIgnoreCase(relationship.getReferTo())) {
						referedData = data;
						break;
					}
				}
				

				if(referedData == null) {
					continue;
				}

				saveOrUpdate(referedData);
			} else if(relationshipType.equalsIgnoreCase(Constants.ENTITY_DESCRIPTOR_RELATIONSHIP_TYPE_ONE_TO_MANY)) {
				
				Iterator<WebSiminovData> datas = webSiminovData.getDatas();
				while(datas.hasNext()) {
					
					WebSiminovData data = datas.next();
					
					String mappedClassName = webResourceManager.getMappedNativeClassName(data.getDataType());
					if(mappedClassName.equalsIgnoreCase(relationship.getReferTo())) {
						data.addData(webSiminovData);
						saveOrUpdate(data);
					}
				}
				
			} else if(relationshipType.equalsIgnoreCase(Constants.ENTITY_DESCRIPTOR_RELATIONSHIP_TYPE_MANY_TO_MANY)) {
				
				Iterator<WebSiminovData> datas = webSiminovData.getDatas();
				while(datas.hasNext()) {
					
					WebSiminovData data = datas.next();
					
					String mappedClassName = webResourceManager.getMappedNativeClassName(data.getDataType());
					if(mappedClassName.equalsIgnoreCase(relationship.getReferTo())) {
						data.addData(webSiminovData);
						saveOrUpdate(data);
					}
				}
			}
		}
	}

	
	
	/**
	 * Handles Database Update Request From Web.
	 * @param data Data Need To Be Update In Database.
	 * @throws DatabaseException If any exception occur while updating data in Database.
	 */
	public void update(String data) throws DatabaseException {
		
		WebSiminovDatas webSiminovDatas = parseWebSiminovDatas(data);
		update(webSiminovDatas);
		
	}

	private void update(WebSiminovDatas webSiminovDatas) throws DatabaseException {
		
		Iterator<WebSiminovData> webDatas = webSiminovDatas.getWebSiminovDatas();
		while(webDatas.hasNext()) {
			update(webDatas.next());
		}
		
	}
	
	private void update(WebSiminovData webSiminovData) throws DatabaseException {
		
		String className = webSiminovData.getDataType();
		Iterator<WebSiminovValue> webSiminovValue = webSiminovData.getValues();
		
		Map<String, WebSiminovValue> webSiminovValues = new HashMap<String, WebSiminovValue>();
		while(webSiminovValue.hasNext()) {
			WebSiminovValue webValue = webSiminovValue.next();
			webSiminovValues.put(webValue.getType(), webValue);
		}
		
		
		EntityDescriptor entityDescriptor = getEntityDescriptor(className);
		DatabaseDescriptor databaseDescriptor = getDatabaseDescriptor(className);

		DatabaseBundle databaseBundle = coreResourceManager.getDatabaseBundle(databaseDescriptor.getDatabaseName());
		IDatabaseImpl database = databaseBundle.getDatabase();
		IQueryBuilder queryBuilder = databaseBundle.getQueryBuilder();
		
		
		StringBuilder whereClause = new StringBuilder();
		String tableName = entityDescriptor.getTableName();
		
		Collection<String> columnNames = new LinkedList<String>();
		Collection<Object> columnValues = new LinkedList<Object>();

		Iterator<EntityDescriptor.Attribute> attributes = entityDescriptor.getAttributes();
		while(attributes.hasNext()) {
			Attribute attribute = attributes.next();
			
			String columnName = attribute.getColumnName();
			Object columnValue = webSiminovValues.get(attribute.getVariableName()).getValue();
			
			columnNames.add(columnName);
			columnValues.add(columnValue);
			
			if(attribute.isPrimaryKey()) {
				if(whereClause.length() == 0) {
					whereClause.append(columnName + "= '" + columnValue + "'");
				} else {
					whereClause.append(" AND " + columnName + "= '" + columnValue + "'");
				}
			}
		}

		
		processManyToOneRelationship(webSiminovData, whereClause);
		processManyToManyRelationship(webSiminovData, whereClause);

		processManyToOneRelationship(webSiminovData, columnNames, columnValues);
		processManyToManyRelationship(webSiminovData, columnNames, columnValues);
		
		

		/*
		 * Add Parameters
		 */
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(IQueryBuilder.FORM_UPDATE_BIND_QUERY_TABLE_NAME_PARAMETER, tableName);
		parameters.put(IQueryBuilder.FORM_UPDATE_BIND_QUERY_COLUMN_NAMES_PARAMETER, columnNames.iterator());
		parameters.put(IQueryBuilder.FORM_UPDATE_BIND_QUERY_WHERE_CLAUSE_PARAMETER, whereClause.toString());

		
		String query = queryBuilder.formUpdateBindQuery(parameters);

		Iterator<Object> values = columnValues.iterator();
		database.executeBindQuery(databaseDescriptor, entityDescriptor, query, values);

		
		
		/*
		 * 5. Check for relationship's if any, IF EXISTS: process it, ELSE: return all objects.
		 */
		Iterator<EntityDescriptor.Relationship> relationships = entityDescriptor.getRelationships();
		while(relationships.hasNext()) {
			EntityDescriptor.Relationship relationship = relationships.next();
			
			boolean isLoad = relationship.isLoad();
			if(!isLoad) {
				continue;
			}
			
			String relationshipType = relationship.getType();
			if(relationshipType == null || relationshipType.length() <= 0) {
				continue;
			}
			
			if(relationshipType.equalsIgnoreCase(Constants.ENTITY_DESCRIPTOR_RELATIONSHIP_TYPE_ONE_TO_ONE)) {
				
				WebSiminovData referedData = null;
				Iterator<WebSiminovData> datas = webSiminovData.getDatas();
				while(datas.hasNext()) {
					
					WebSiminovData data = datas.next();
					
					String mappedClassName = webResourceManager.getMappedNativeClassName(data.getDataType());
					if(mappedClassName.equalsIgnoreCase(relationship.getReferTo())) {
						referedData = data;
						break;
					}
				}
				

				if(referedData == null) {
					continue;
				}

				saveOrUpdate(referedData);
			} else if(relationshipType.equalsIgnoreCase(Constants.ENTITY_DESCRIPTOR_RELATIONSHIP_TYPE_ONE_TO_MANY)) {
				
				Iterator<WebSiminovData> datas = webSiminovData.getDatas();
				while(datas.hasNext()) {
					
					WebSiminovData data = datas.next();
					
					String mappedClassName = webResourceManager.getMappedNativeClassName(data.getDataType());
					if(mappedClassName.equalsIgnoreCase(relationship.getReferTo())) {
						data.addData(webSiminovData);
						saveOrUpdate(data);
					}
				}
				
			} else if(relationshipType.equalsIgnoreCase(Constants.ENTITY_DESCRIPTOR_RELATIONSHIP_TYPE_MANY_TO_MANY)) {
				
				Iterator<WebSiminovData> datas = webSiminovData.getDatas();
				while(datas.hasNext()) {
					
					WebSiminovData data = datas.next();
					
					String mappedClassName = webResourceManager.getMappedNativeClassName(data.getDataType());
					if(mappedClassName.equalsIgnoreCase(relationship.getReferTo())) {
						data.addData(webSiminovData);
						saveOrUpdate(data);
					}
				}
			}
		}
		
	}
	

	/**
	 * Handles Database Save Or Update Request From Web.
	 * @param data Data Need To Be Save Or Update In Database.
	 * @throws DatabaseException If any exception occur while saving or updating data in Database.
	 */
	public void saveOrUpdate(String data) throws DatabaseException {

		WebSiminovDatas webSiminovDatas = parseWebSiminovDatas(data);
		saveOrUpdate(webSiminovDatas);
		
	}
	
	private void saveOrUpdate(WebSiminovDatas webSiminovDatas) throws DatabaseException {
		
		Iterator<WebSiminovData> webDatas = webSiminovDatas.getWebSiminovDatas();
		
		while(webDatas.hasNext()) {
			WebSiminovData webSiminovData = webDatas.next();
			saveOrUpdate(webSiminovData);
		}
	}

	private void saveOrUpdate(WebSiminovData webSiminovData) throws DatabaseException {

		String className = webSiminovData.getDataType();
		Iterator<WebSiminovValue> webValues = webSiminovData.getValues();
		
		Map<String, WebSiminovValue> webSiminovValues = new HashMap<String, WebSiminovValue>();
		while(webValues.hasNext()) {
			WebSiminovValue webSiminovValue = webValues.next();
			webSiminovValues.put(webSiminovValue.getType(), webSiminovValue);
		}
		
		
		EntityDescriptor entityDescriptor = getEntityDescriptor(className);
		
		StringBuilder whereClause = new StringBuilder();
		Iterator<EntityDescriptor.Attribute> attributes = entityDescriptor.getAttributes();
		while(attributes.hasNext()) {
			Attribute attribute = attributes.next();
			
			if(attribute.isPrimaryKey()) {

				String columnName = attribute.getColumnName();
				Object columnValue = webSiminovValues.get(attribute.getVariableName()).getValue();

				if(whereClause.length() <= 0) {
					whereClause.append(columnName + "= '" + columnValue + "'");
				} else {
					whereClause.append(" AND " + columnName + "= '" + columnValue + "'");
				}
			}
			
		}

		
		processManyToOneRelationship(webSiminovData, whereClause);
		processManyToManyRelationship(webSiminovData, whereClause);
		

		if(whereClause == null || whereClause.length() <= 0) {
			save(webSiminovData);
			return;
		}

		
		/*
		 * 4. IF EXISTS: call update method, ELSE: call save method.
		 */
		int count = count(entityDescriptor, null, false, whereClause.toString(), null, null);
		if(count <= 0) {
			save(webSiminovData);
		} else {
			update(webSiminovData);
		}
	}
	

	
	/**
	 * Handles Database Delete Request From Web.
	 * @param className Web Model Class Name of which delete request is sent.
	 * @param whereClause Based on which tuple will be deleted from table.
	 * @param data Data Need To Be Delete In Database.
	 * @throws DatabaseException If any exception occur while deleting data in Database.
	 */
	public void delete(final String className, final String whereClause, final String data) throws DatabaseException {
		
		WebSiminovDatas webSiminovDatas = null;
		
		if(data != null && data.length() > 0) {
			webSiminovDatas = parseWebSiminovDatas(data);			
		
			delete(webSiminovDatas);
			return;
		}
		
		EntityDescriptor entityDescriptor = getEntityDescriptor(className);
		
		delete(entityDescriptor, whereClause);

	}

	private void delete(final WebSiminovDatas webSiminovDatas) throws DatabaseException {
		
		Iterator<WebSiminovData> webDatas = webSiminovDatas.getWebSiminovDatas();
		
		while(webDatas.hasNext()) {
			WebSiminovData webSiminovData = webDatas.next();
			
			String className = webSiminovData.getDataType();
			Iterator<WebSiminovValue> webValues = webSiminovData.getValues();
			
			Map<String, WebSiminovValue> webSiminovValues = new HashMap<String, WebSiminovValue>();
			while(webValues.hasNext()) {
				WebSiminovValue webSiminovValue = webValues.next();
				webSiminovValues.put(webSiminovValue.getType(), webSiminovValue);
			}
			
			EntityDescriptor entityDescriptor = getEntityDescriptor(className);
			
			StringBuilder whereClause = new StringBuilder();
			
			Collection<String> columnNames = new LinkedList<String>();
			Collection<Object> columnValues = new LinkedList<Object>();
	
			Iterator<EntityDescriptor.Attribute> attributes = entityDescriptor.getAttributes();
			while(attributes.hasNext()) {
				Attribute attribute = attributes.next();
				
				String columnName = attribute.getColumnName();
				Object columnValue = webSiminovValues.get(attribute.getVariableName()).getValue();
				
				columnNames.add(columnName);
				columnValues.add(columnValue);
				
				if(attribute.isPrimaryKey()) {
					if(whereClause.length() == 0) {
						whereClause.append(columnName + "= '" + columnValue + "'");
					} else {
						whereClause.append(" AND " + columnName + "= '" + columnValue + "'");
					}
				}
			}
	
			
			processManyToOneRelationship(webSiminovData, whereClause);
			processManyToManyRelationship(webSiminovData, whereClause);
			
			
			delete(entityDescriptor, whereClause.toString());
		}
	}

	private void delete(final EntityDescriptor entityDescriptor, final String whereClause) throws DatabaseException {
		
		DatabaseDescriptor databaseDescriptor = getDatabaseDescriptor(entityDescriptor.getClassName());
		
		DatabaseBundle databaseBundle = coreResourceManager.getDatabaseBundle(databaseDescriptor.getDatabaseName());
		IDatabaseImpl database = databaseBundle.getDatabase();
		IQueryBuilder queryBuilder = databaseBundle.getQueryBuilder();
		
		
		/*
		 * Add Parameters
		 */
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(IQueryBuilder.FORM_DELETE_QUERY_TABLE_NAME_PARAMETER, entityDescriptor.getTableName());
		parameters.put(IQueryBuilder.FORM_DELETE_QUERY_WHERE_CLAUSE_PARAMETER, whereClause.toString());
		
		
		String query = queryBuilder.formDeleteQuery(parameters);
		database.executeQuery(databaseDescriptor, entityDescriptor, query);

	}
	

	/**
	 * 
	 * @param data 
	 * @throws DatabaseException 
	 */
	
	/**
	 * Handles Database Select Request From Web.
	 * @param className Web Model Function Name.
	 * @param distinct Distinct tuples need to be fetched or not.
	 * @param whereClause Where Clause based on which tuples will be fetched from table.
	 * @param columnNames Name of Columns for which data needs to be fetched.
	 * @param groupBy Used to specify GROUP BY statement in conjunction with the aggregate functions to group the result-set by one or more columns.
	 * @param havingClause Used to specify HAVING clause to SQL because the WHERE keyword could not be used with aggregate functions.
	 * @param orderBy Used to specify ORDER BY keyword to sort the result-set.
	 * @param whichOrderBy Used to specify ORDER BY ASC OR DESC keyword to sort the result-set in ascending order.
	 * @param limit Used to specify the range of data need to fetch from table.
	 * @return Return Tuples Fetched From Table.
	 * @throws DatabaseException If any exception occur while selecting data in Database.
	 */
	public String select(final String className, final Boolean distinct, final String whereClause, final String[] columnNames, final String[] groupBy, final String havingClause, final String[] orderBy, final String whichOrderBy, final String limit) throws DatabaseException {
		
		EntityDescriptor entityDescriptor = getEntityDescriptor(className);
		select(entityDescriptor, distinct, whereClause, columnNames, groupBy, havingClause, orderBy, whichOrderBy, limit);

		WebSiminovDatas webSiminovDatas = select(entityDescriptor, distinct, whereClause, columnNames, groupBy, havingClause, orderBy, whichOrderBy, limit);
		
		String data = null;
		try {
			data = WebSiminovDataWritter.jsonBuidler(webSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(DatabaseHandler.class.getName(), "select", "SiminovException caught while building json, " + siminovException.getMessage());
			throw new DatabaseException(DatabaseHandler.class.getName(), "select", "SiminovException caught while building json, " + siminovException.getMessage());
		}

		return data;
	}

	
	private WebSiminovDatas select(final EntityDescriptor entityDescriptor, final Boolean distinct, final String whereClause, final String[] columnNames, final String[] groupBy, final String havingClause, final String[] orderBy, final String whichOrderBy, final String limit) throws DatabaseException {

		String className = entityDescriptor.getClassName();
		DatabaseDescriptor databaseDescriptor = getDatabaseDescriptor(className);
		
		DatabaseBundle databaseBundle = coreResourceManager.getDatabaseBundle(databaseDescriptor.getDatabaseName());
		IDatabaseImpl database = databaseBundle.getDatabase();
		IQueryBuilder queryBuilder = databaseBundle.getQueryBuilder();
		
		if(database == null) {
			Log.error(DatabaseHandler.class.getName(), "select", "No Database Instance Found For DATABASE-MAPPING: " + className);
			throw new DeploymentException(DatabaseHandler.class.getName(), "select", "No Database Instance Found For DATABASE-MAPPING: " + className);
		}
		
		Collection<String> columnNameCollection = new LinkedList<String>();
		Collection<String> groupByCollection = new LinkedList<String>();
		Collection<String> orderByCollection = new LinkedList<String>();
		
		while(columnNames != null && columnNames.length > 0) {
			for(int i = 0;i < columnNames.length;i++) {
				columnNameCollection.add(columnNames[i]);
			}
		} 
		
		while(groupBy != null && groupBy.length > 0) {
			for(int i = 0;i < groupBy.length;i++) {
				groupByCollection.add(groupBy[i]);
			}
		}
		
		while(orderBy != null && orderBy.length > 0) {
			for(int i = 0;i < orderBy.length;i++) {
				orderByCollection.add(orderBy[i]);
			}
		}

		
		
		/*
		 * Add Parameters
		 */
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(IQueryBuilder.FORM_SELECT_QUERY_TABLE_NAME_PARAMETER, entityDescriptor.getTableName());
		parameters.put(IQueryBuilder.FORM_SELECT_QUERY_DISTINCT_PARAMETER, false);
		parameters.put(IQueryBuilder.FORM_SELECT_QUERY_WHERE_CLAUSE_PARAMETER, whereClause);
		parameters.put(IQueryBuilder.FORM_SELECT_QUERY_COLUMN_NAMES_PARAMETER, columnNameCollection.iterator());
		parameters.put(IQueryBuilder.FORM_SELECT_QUERY_GROUP_BYS_PARAMETER, groupByCollection.iterator());
		parameters.put(IQueryBuilder.FORM_SELECT_QUERY_HAVING_PARAMETER, havingClause);
		parameters.put(IQueryBuilder.FORM_SELECT_QUERY_ORDER_BYS_PARAMETER, orderByCollection.iterator());
		parameters.put(IQueryBuilder.FORM_SELECT_QUERY_WHICH_ORDER_BY_PARAMETER, null);
		parameters.put(IQueryBuilder.FORM_SELECT_QUERY_LIMIT_PARAMETER, limit);

		
		Iterator<Map<String, Object>> datas = database.executeSelectQuery(getDatabaseDescriptor(className), entityDescriptor, queryBuilder.formSelectQuery(parameters));
		Collection<Map<String, Object>> datasBundle = new LinkedList<Map<String,Object>>();
		while(datas.hasNext()) {
			datasBundle.add(datas.next());
		}

		WebSiminovDatas webSiminovDatas = parseData(entityDescriptor, datasBundle.iterator());
		datas = datasBundle.iterator();
		
		Iterator<WebSiminovData> siminovDatas = webSiminovDatas.getWebSiminovDatas();
		while(siminovDatas.hasNext() && datas.hasNext()) {
			
			Map<String, Object> data = datas.next();
			WebSiminovData siminovData = siminovDatas.next();
			
			processOneToOneRelationship(siminovData);
			processOneToManyRelationship(siminovData);

			processManyToOneRelationship(siminovData, data);
			processManyToManyRelationship(siminovData, data);
		}
		

		return webSiminovDatas;
	}

		
	
	public String selectManual(final String className, final String query) throws DatabaseException {
		
		EntityDescriptor entityDescriptor = getEntityDescriptor(className);
		DatabaseDescriptor databaseDescriptor = getDatabaseDescriptor(className);

		DatabaseBundle databaseBundle = coreResourceManager.getDatabaseBundle(databaseDescriptor.getDatabaseName());
		IDatabaseImpl database = databaseBundle.getDatabase();
		
		if(database == null) {
			Log.error(DatabaseHandler.class.getName(), "select", "No Database Instance Found For DATABASE-MAPPING: " + className);
			throw new DeploymentException(DatabaseHandler.class.getName(), "select", "No Database Instance Found For DATABASE-MAPPING: " + className);
		}
		
		
		Iterator<Map<String, Object>> datas = database.executeSelectQuery(getDatabaseDescriptor(className), entityDescriptor, query);
		Collection<Map<String, Object>> datasBundle = new LinkedList<Map<String,Object>>();
		while(datas.hasNext()) {
			datasBundle.add(datas.next());
		}

		WebSiminovDatas webSiminovDatas = parseData(entityDescriptor, datasBundle.iterator());
		datas = datasBundle.iterator();
		
		Iterator<WebSiminovData> siminovDatas = webSiminovDatas.getWebSiminovDatas();
		while(siminovDatas.hasNext() && datas.hasNext()) {
			
			Map<String, Object> data = datas.next();
			WebSiminovData siminovData = siminovDatas.next();
			
			processOneToOneRelationship(siminovData);
			processOneToManyRelationship(siminovData);

			processManyToOneRelationship(siminovData, data);
			processManyToManyRelationship(siminovData, data);
			
		}
		
		
		String data = null;
		try {
			data = WebSiminovDataWritter.jsonBuidler(webSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(DatabaseHandler.class.getName(), "select", "SiminovException caught while building json, " + siminovException.getMessage());
			throw new DatabaseException(DatabaseHandler.class.getName(), "select", "SiminovException caught while building json, " + siminovException.getMessage());
		}

		return data;
	}
	
	private WebSiminovDatas lazyFetch(final EntityDescriptor entityDescriptor, final boolean distinct, final String whereClause, final Iterator<String> columnNames, final Iterator<String> groupBy, final String having, final Iterator<String> orderBy, final String whichOrderBy, final String limit) throws DatabaseException {
		
		DatabaseDescriptor databaseDescriptor = getDatabaseDescriptor(entityDescriptor.getClassName());
		DatabaseBundle databaseBundle = coreResourceManager.getDatabaseBundle(databaseDescriptor.getDatabaseName());
		
		IDatabaseImpl database = databaseBundle.getDatabase();
		IQueryBuilder queryBuilder = databaseBundle.getQueryBuilder();
		
		if(database == null) {
			Log.error(DatabaseHandler.class.getName(), "lazyFetch", "No Database Instance Found For DATABASE-MAPPING: " + entityDescriptor.getClassName());
			throw new DeploymentException(DatabaseHandler.class.getName(), "lazyFetch", "No Database Instance Found For DATABASE-MAPPING: " + entityDescriptor.getClassName());
		}

		
		
		/*
		 * Add Parameters
		 */
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(IQueryBuilder.FORM_SELECT_QUERY_TABLE_NAME_PARAMETER, entityDescriptor.getTableName());
		parameters.put(IQueryBuilder.FORM_SELECT_QUERY_DISTINCT_PARAMETER, false);
		parameters.put(IQueryBuilder.FORM_SELECT_QUERY_WHERE_CLAUSE_PARAMETER, whereClause);
		parameters.put(IQueryBuilder.FORM_SELECT_QUERY_COLUMN_NAMES_PARAMETER, columnNames);
		parameters.put(IQueryBuilder.FORM_SELECT_QUERY_GROUP_BYS_PARAMETER, groupBy);
		parameters.put(IQueryBuilder.FORM_SELECT_QUERY_HAVING_PARAMETER, having);
		parameters.put(IQueryBuilder.FORM_SELECT_QUERY_ORDER_BYS_PARAMETER, orderBy);
		parameters.put(IQueryBuilder.FORM_SELECT_QUERY_WHICH_ORDER_BY_PARAMETER, null);
		parameters.put(IQueryBuilder.FORM_SELECT_QUERY_LIMIT_PARAMETER, limit);

		return parseData(entityDescriptor, database.executeSelectQuery(getDatabaseDescriptor(entityDescriptor.getClassName()), entityDescriptor, queryBuilder.formSelectQuery(parameters)));
	
	}
	
	
	/**
	 * Handles Database Begin Transaction Request From Web.
	 * @param databaseDescriptorName Name of Database Descriptor.
	 * @throws DatabaseException If any exception occur while beginning transaction.
	 */
	public void beginTransaction(final String databaseDescriptorName) throws DatabaseException {
		
		DatabaseBundle databaseBundle = coreResourceManager.getDatabaseBundle(databaseDescriptorName);
		IDatabaseImpl database = databaseBundle.getDatabase();

		if(database == null) {
			Log.error(DatabaseHandler.class.getName(), "beginTransaction", "No Database Instance Found For CLASS: " + databaseDescriptorName);
			throw new DeploymentException(DatabaseHandler.class.getName(), "beginTransaction", "No Database Instance Found For CLASS: " + databaseDescriptorName);
		}
		
		database.executeMethod(Constants.SQLITE_DATABASE_BEGIN_TRANSACTION, null);
		
	}

	
	/**
	 * Handles Database Commit Transaction Request From Web.
	 * @param databaseDescriptorName Name of Database Descriptor.
	 * @throws DatabaseException If any error occur while committing transaction. 
	 */
	public void commitTransaction(final String databaseDescriptorName) throws DatabaseException {
		
		DatabaseBundle databaseBundle = coreResourceManager.getDatabaseBundle(databaseDescriptorName);
		IDatabaseImpl database = databaseBundle.getDatabase();

		if(database == null) {
			Log.error(DatabaseHandler.class.getName(), "commitTransaction", "No Database Instance Found For CLASS: " + databaseDescriptorName);
			throw new DeploymentException(DatabaseHandler.class.getName(), "commitTransaction", "No Database Instance Found For CLASS: " + databaseDescriptorName);
		}

		database.executeMethod(Constants.SQLITE_DATABASE_COMMIT_TRANSACTION, null);

	}
	
	
	/**
	 * Handles Database End Transaction Request From Web.
	 * @param databaseDescriptorName Name of Database Descriptor.
	 * @throws DatabaseException If any error occur while ending transaction.
	 */
	public void endTransaction(final String databaseDescriptorName) throws DatabaseException {
		
		DatabaseBundle databaseBundle = coreResourceManager.getDatabaseBundle(databaseDescriptorName);
		IDatabaseImpl database = databaseBundle.getDatabase();

		if(database == null) {
			Log.error(DatabaseHandler.class.getName(), "endTransaction", "No Database Instance Found For CLASS: " + databaseDescriptorName);
			throw new DeploymentException(DatabaseHandler.class.getName(), "endTransaction", "No Database Instance Found For CLASS: " + databaseDescriptorName);
		}
		
		try {
			database.executeMethod(Constants.SQLITE_DATABASE_END_TRANSACTION, null);
		} catch(DatabaseException databaseException) {
			Log.error(DatabaseHandler.class.getName(), "endTransaction", "DatabaseException caught while executing end transaction method, " + databaseException.getMessage());
		}
	}
	
	
	/**
	 * Handles Database Count Request From Web.
	 * @param className Web Model Class Name.
	 * @param column Name of Column For Which Count Needs To Be Find.
	 * @param distinct Distinct tuples needs to be calculated or not.
	 * @param whereClause Where Clause based on which Count needs to be found.
	 * @param groupBys Used to specify GROUP BY statement in conjunction with the aggregate functions to group the result-set by one or more columns.
	 * @param having Used to specify HAVING clause to SQL because the WHERE keyword could not be used with aggregate functions.
	 * @return Count Of Tuples.
	 * @throws DatabaseException If any error occur while getting count.
	 */
	public String count(final String className, final String column, final Boolean distinct, final String whereClause, final String[] groupBys, final String having) throws DatabaseException {

		EntityDescriptor entityDescriptor = getEntityDescriptor(className);
		int count = count(entityDescriptor, column, distinct, whereClause, groupBys, having);
		
		WebSiminovDatas webSiminovDatas = new WebSiminovDatas();
		WebSiminovData siminovData = new WebSiminovData();
		
		siminovData.setDataValue(String.valueOf(count));
		
		webSiminovDatas.addWebSiminovData(siminovData);

		String data = null;
		try {
			data = WebSiminovDataWritter.jsonBuidler(webSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(DatabaseHandler.class.getName(), "count", "SiminovException caught while building json, " + siminovException.getMessage());
			throw new DatabaseException(DatabaseHandler.class.getName(), "count", "SiminovException caught while building json, " + siminovException.getMessage());
		}

		return data;

	}
	
	
	private int count(final EntityDescriptor entityDescriptor, final String column, final Boolean distinct, final String whereClause, final String[] groupBys, final String having) throws DatabaseException {
		
		DatabaseDescriptor databaseDescriptor = getDatabaseDescriptor(entityDescriptor.getClassName());
		DatabaseBundle databaseBundle = coreResourceManager.getDatabaseBundle(databaseDescriptor.getDatabaseName());
		
		IDatabaseImpl database = databaseBundle.getDatabase();
		IQueryBuilder queryBuilder = databaseBundle.getQueryBuilder();
		
		if(database == null) {
			Log.error(DatabaseHandler.class.getName(), "count", "No Database Instance Found For DATABASE-MAPPING: " + entityDescriptor.getClassName());
			throw new DeploymentException(DatabaseHandler.class.getName(), "count", "No Database Instance Found For DATABASE-MAPPING: " + entityDescriptor.getClassName());
		}

		
		/*
		 * Add Parameters
		 */
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(IQueryBuilder.FORM_COUNT_QUERY_TABLE_NAME_PARAMETER, entityDescriptor.getTableName());
		parameters.put(IQueryBuilder.FORM_COUNT_QUERY_COLUMN_PARAMETER, null);
		parameters.put(IQueryBuilder.FORM_COUNT_QUERY_DISTINCT_PARAMETER, false);
		parameters.put(IQueryBuilder.FORM_COUNT_QUERY_WHERE_CLAUSE_PARAMETER, whereClause);
		parameters.put(IQueryBuilder.FORM_COUNT_QUERY_GROUP_BYS_PARAMETER, null);
		parameters.put(IQueryBuilder.FORM_COUNT_QUERY_HAVING_PARAMETER, null);
		
		
		String query = queryBuilder.formCountQuery(parameters);

		Iterator<Map<String, Object>> datas = database.executeSelectQuery(databaseDescriptor, entityDescriptor, query);
		while(datas.hasNext()) {
			Map<String, Object> data = datas.next();
			Collection<Object> parse = data.values();

			Iterator<Object> values = parse.iterator();
			while(values.hasNext()) {
				
				Object value = values.next();
				if(value instanceof Integer) {
					return ((Integer) value).intValue();
				} else if(value instanceof Long) {
					return ((Long) value).intValue();
				} else if(value instanceof Float) {
					return ((Float) value).intValue();
				}
				
			}
		}
		
		return 0;
	}
	

	
	/**
	 * Handles Database Average Request From Web.
	 * @param className Web Model Class Name.
	 * @param column Name of Column For Which Average Needs To Be Find.
	 * @param whereClause Where Clause based on which Average needs to be found.
	 * @param groupBys Used to specify GROUP BY statement in conjunction with the aggregate functions to group the result-set by one or more columns.
	 * @param having Used to specify HAVING clause to SQL because the WHERE keyword could not be used with aggregate functions.
	 * @return Average Of Tuples.
	 * @throws DatabaseException If any error occur while getting count.
	 */
	public String avg(final String className, final String columnName, final String whereClause, final String[] groupBy, final String having) throws DatabaseException {
		
		EntityDescriptor entityDescriptor = getEntityDescriptor(className);

		int avg = avg(entityDescriptor, columnName, whereClause, groupBy, having);

		WebSiminovDatas webSiminovDatas = new WebSiminovDatas();
		WebSiminovData siminovData = new WebSiminovData();
		
		siminovData.setDataValue(String.valueOf(avg));
		
		webSiminovDatas.addWebSiminovData(siminovData);

		String data = null;
		try {
			data = WebSiminovDataWritter.jsonBuidler(webSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(DatabaseHandler.class.getName(), "avg", "SiminovException caught while building json, " + siminovException.getMessage());
			throw new DatabaseException(DatabaseHandler.class.getName(), "avg", "SiminovException caught while building json, " + siminovException.getMessage());
		}

		return data;

	}
	
	private int avg(final EntityDescriptor entityDescriptor, final String columnName, final String whereClause, final String[] groupBy, final String having) throws DatabaseException {
		
		DatabaseDescriptor databaseDescriptor = getDatabaseDescriptor(entityDescriptor.getClassName());
		DatabaseBundle databaseBundle = coreResourceManager.getDatabaseBundle(databaseDescriptor.getDatabaseName());
		
		IDatabaseImpl database = databaseBundle.getDatabase();
		IQueryBuilder queryBuilder = databaseBundle.getQueryBuilder();
		
		if(database == null) {
			Log.error(DatabaseHandler.class.getName(), "avg", "No Database Instance Found For DATABASE-MAPPING: " + entityDescriptor.getClassName());
			throw new DeploymentException(DatabaseHandler.class.getName(), "avg", "No Database Instance Found For DATABASE-MAPPING: " + entityDescriptor.getClassName());
		}

		
		Collection<String> groupBys = new ArrayList<String>();
		if(groupBy != null && groupBy.length > 0) {
			for(int i = 0;i < groupBy.length;i++) {
				groupBys.add(groupBy[i]);
			}
		}
		

		/*
		 * Add Parameters
		 */
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(IQueryBuilder.FORM_AVG_QUERY_TABLE_NAME_PARAMETER, entityDescriptor.getTableName());
		parameters.put(IQueryBuilder.FORM_AVG_QUERY_COLUMN_PARAMETER, columnName);
		parameters.put(IQueryBuilder.FORM_AVG_QUERY_WHERE_CLAUSE_PARAMETER, whereClause);
		parameters.put(IQueryBuilder.FORM_AVG_QUERY_GROUP_BYS_PARAMETER, groupBys.iterator());
		parameters.put(IQueryBuilder.FORM_AVG_QUERY_HAVING_PARAMETER, having);

		
		String query = queryBuilder.formAvgQuery(parameters);

		Iterator<Map<String, Object>> datas = database.executeSelectQuery(databaseDescriptor, entityDescriptor, query);
		while(datas.hasNext()) {
			Map<String, Object> data = datas.next();
			Collection<Object> parse = data.values();

			Iterator<Object> values = parse.iterator();
			while(values.hasNext()) {

				Object value = values.next();
				if(value instanceof Integer) {
					return ((Integer) value).intValue();
				} else if(value instanceof Long) {
					return ((Long) value).intValue();
				} else if(value instanceof Float) {
					return ((Float) value).intValue();
				}
			
			}
		}
		
		return 0;
		
	}


	/**
	 * Handles Database Sum Request From Web.
	 * @param className Web Model Class Name.
	 * @param column Name of Column For Which Sum Needs To Be Find.
	 * @param whereClause Where Clause based on which Sum needs to be found.
	 * @param groupBys Used to specify GROUP BY statement in conjunction with the aggregate functions to group the result-set by one or more columns.
	 * @param having Used to specify HAVING clause to SQL because the WHERE keyword could not be used with Sum functions.
	 * @return Sum Of Tuples.
	 * @throws DatabaseException If any error occur while getting sum.
	 */
	public String sum(final String className, final String columnName, final String whereClause, final String[] groupBy, final String having) throws DatabaseException {
		
		EntityDescriptor entityDescriptor = getEntityDescriptor(className);

		int sum =  sum(entityDescriptor, columnName, whereClause, groupBy, having);
		
		WebSiminovDatas webSiminovDatas = new WebSiminovDatas();
		WebSiminovData siminovData = new WebSiminovData();
		
		siminovData.setDataValue(String.valueOf(sum));
		
		webSiminovDatas.addWebSiminovData(siminovData);

		String data = null;
		try {
			data = WebSiminovDataWritter.jsonBuidler(webSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(DatabaseHandler.class.getName(), "sum", "SiminovException caught while building json, " + siminovException.getMessage());
			throw new DatabaseException(DatabaseHandler.class.getName(), "sum", "SiminovException caught while building json, " + siminovException.getMessage());
		}

		return data;

	}
	
	private int sum(final EntityDescriptor entityDescriptor, final String columnName, final String whereClause, final String[] groupBy, final String having) throws DatabaseException {
		
		DatabaseDescriptor databaseDescriptor = getDatabaseDescriptor(entityDescriptor.getClassName());
		DatabaseBundle databaseBundle = coreResourceManager.getDatabaseBundle(databaseDescriptor.getDatabaseName());

		IDatabaseImpl database = databaseBundle.getDatabase();
		IQueryBuilder queryBuilder = databaseBundle.getQueryBuilder();
		
		if(database == null) {
			Log.error(DatabaseHandler.class.getName(), "sum", "No Database Instance Found For DATABASE-MAPPING: " + entityDescriptor.getClassName());
			throw new DeploymentException(DatabaseHandler.class.getName(), "sum", "No Database Instance Found For DATABASE-MAPPING: " + entityDescriptor.getClassName());
		}

		Collection<String> groupBys =  new ArrayList<String>();
		if(groupBy != null && groupBy.length > 0) {
			for(int i = 0;i < groupBy.length;i++) {
				groupBys.add(groupBy[i]);
			}
		}
		
		
		/*
		 * Add Parameters
		 */
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(IQueryBuilder.FORM_SUM_QUERY_TABLE_NAME_PARAMETER, entityDescriptor.getTableName());
		parameters.put(IQueryBuilder.FORM_SUM_QUERY_COLUMN_PARAMETER, columnName);
		parameters.put(IQueryBuilder.FORM_SUM_QUERY_WHERE_CLAUSE_PARAMETER, whereClause);
		parameters.put(IQueryBuilder.FORM_SUM_QUERY_GROUP_BYS_PARAMETER, groupBys.iterator());
		parameters.put(IQueryBuilder.FORM_SUM_QUERY_HAVING_PARAMETER, having);
		
		
		String query = queryBuilder.formSumQuery(parameters);

		Iterator<Map<String, Object>> datas = database.executeSelectQuery(databaseDescriptor, entityDescriptor, query);
		while(datas.hasNext()) {
			Map<String, Object> data = datas.next();
			Collection<Object> parse = data.values();

			Iterator<Object> values = parse.iterator();
			while(values.hasNext()) {

				Object value = values.next();
				if(value instanceof Integer) {
					return ((Integer) value).intValue();
				} else if(value instanceof Long) {
					return ((Long) value).intValue();
				} else if(value instanceof Float) {
					return ((Float) value).intValue();
				}

			}
		}
		
		return 0;
		
	}

	
	/**
	 * Handles Database Total Request From Web.
	 * @param className Web Model Class Name.
	 * @param column Name of Column For Which Total Needs To Be Find.
	 * @param whereClause Where Clause based on which Total needs to be found.
	 * @param groupBys Used to specify GROUP BY statement in conjunction with the aggregate functions to group the result-set by one or more columns.
	 * @param having Used to specify HAVING clause to SQL because the WHERE keyword could not be used with Total functions.
	 * @return Total Of Tuples.
	 * @throws DatabaseException If any error occur while getting total.
	 */
	public String total(final String className, final String columnName, final String whereClause, final String[] groupBy, final String having) throws DatabaseException {
		
		EntityDescriptor entityDescriptor = getEntityDescriptor(className);

		int total = total(entityDescriptor, columnName, whereClause, groupBy, having);
		
		WebSiminovDatas webSiminovDatas = new WebSiminovDatas();
		WebSiminovData siminovData = new WebSiminovData();
		
		siminovData.setDataValue(String.valueOf(total));
		
		webSiminovDatas.addWebSiminovData(siminovData);

		String data = null;
		try {
			data = WebSiminovDataWritter.jsonBuidler(webSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(DatabaseHandler.class.getName(), "total", "SiminovException caught while building json, " + siminovException.getMessage());
			throw new DatabaseException(DatabaseHandler.class.getName(), "total", "SiminovException caught while building json, " + siminovException.getMessage());
		}

		return data;

	}
	
	private int total(final EntityDescriptor entityDescriptor, final String columnName, final String whereClause, final String[] groupBy, final String having) throws DatabaseException {
		
		DatabaseDescriptor databaseDescriptor = getDatabaseDescriptor(entityDescriptor.getClassName());
		DatabaseBundle databaseBundle = coreResourceManager.getDatabaseBundle(databaseDescriptor.getDatabaseName());

		IDatabaseImpl database = databaseBundle.getDatabase();
		IQueryBuilder queryBuilder = databaseBundle.getQueryBuilder();
		
		if(database == null) {
			Log.error(DatabaseHandler.class.getName(), "total", "No Database Instance Found For DATABASE-MAPPING: " + entityDescriptor.getClassName());
			throw new DeploymentException(DatabaseHandler.class.getName(), "total", "No Database Instance Found For DATABASE-MAPPING: " + entityDescriptor.getClassName());
		}

		Collection<String> groupBys = new ArrayList<String>();
		if(groupBy != null && groupBy.length > 0) {
			for(int i = 0;i < groupBy.length;i++) {
				groupBys.add(groupBy[i]);
			}
		}
		
		
		/*
		 * Add Parameters
		 */
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(IQueryBuilder.FORM_TOTAL_QUERY_TABLE_NAME_PARAMETER, entityDescriptor.getTableName());
		parameters.put(IQueryBuilder.FORM_TOTAL_QUERY_COLUMN_PARAMETER, columnName);
		parameters.put(IQueryBuilder.FORM_TOTAL_QUERY_WHERE_CLAUSE_PARAMETER, whereClause);
		parameters.put(IQueryBuilder.FORM_TOTAL_QUERY_GROUP_BYS_PARAMETER, groupBys.iterator());
		parameters.put(IQueryBuilder.FORM_TOTAL_QUERY_HAVING_PARAMETER, having);
		
		
		String query = queryBuilder.formTotalQuery(parameters);

		Iterator<Map<String, Object>> datas = database.executeSelectQuery(databaseDescriptor, entityDescriptor, query);
		while(datas.hasNext()) {
			Map<String, Object> data = datas.next();
			Collection<Object> parse = data.values();

			Iterator<Object> values = parse.iterator();
			while(values.hasNext()) {

				Object value = values.next();
				if(value instanceof Integer) {
					return ((Integer) value).intValue();
				} else if(value instanceof Long) {
					return ((Long) value).intValue();
				} else if(value instanceof Float) {
					return ((Float) value).intValue();
				}

			}
		}
		
		return 0;
		
	}


	/**
	 * Handles Database Minimum Request From Web.
	 * @param className Web Model Class Name.
	 * @param column Name of Column For Which Minimum Needs To Be Find.
	 * @param whereClause Where Clause based on which Minimum needs to be found.
	 * @param groupBys Used to specify GROUP BY statement in conjunction with the aggregate functions to group the result-set by one or more columns.
	 * @param having Used to specify HAVING clause to SQL because the WHERE keyword could not be used with Minimum functions.
	 * @return Minimum Of Tuples.
	 * @throws DatabaseException If any error occur while getting minimum.
	 */
	public String min(final String className, final String columnName, final String whereClause, final String[] groupBy, final String having) throws DatabaseException {
		
		EntityDescriptor entityDescriptor = getEntityDescriptor(className);

		int min = min(entityDescriptor, columnName, whereClause, groupBy, having);
		
		WebSiminovDatas webSiminovDatas = new WebSiminovDatas();
		WebSiminovData siminovData = new WebSiminovData();
		
		siminovData.setDataValue(String.valueOf(min));

		webSiminovDatas.addWebSiminovData(siminovData);

		String data = null;
		try {
			data = WebSiminovDataWritter.jsonBuidler(webSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(DatabaseHandler.class.getName(), "min", "SiminovException caught while building json, " + siminovException.getMessage());
			throw new DatabaseException(DatabaseHandler.class.getName(), "min", "SiminovException caught while building json, " + siminovException.getMessage());
		}

		return data;

	}
	
	private int min(final EntityDescriptor entityDescriptor, final String columnName, final String whereClause, final String[] groupBy, final String having) throws DatabaseException {
		
		DatabaseDescriptor databaseDescriptor = getDatabaseDescriptor(entityDescriptor.getClassName());
		DatabaseBundle databaseBundle = coreResourceManager.getDatabaseBundle(databaseDescriptor.getDatabaseName());

		IDatabaseImpl database = databaseBundle.getDatabase();
		IQueryBuilder queryBuilder = databaseBundle.getQueryBuilder();
		
		if(database == null) {
			Log.error(DatabaseHandler.class.getName(), "min", "No Database Instance Found For DATABASE-MAPPING: " + entityDescriptor.getClassName());
			throw new DeploymentException(DatabaseHandler.class.getName(), "min", "No Database Instance Found For DATABASE-MAPPING: " + entityDescriptor.getClassName());
		}

		Collection<String> groupBys = new ArrayList<String>();
		if(groupBy != null && groupBy.length > 0) {
			for(int i = 0;i < groupBy.length;i++) {
				groupBys.add(groupBy[i]);
			}
		}
		
		
		/*
		 * Add Parameters
		 */
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(IQueryBuilder.FORM_MIN_QUERY_TABLE_NAME_PARAMETER, entityDescriptor.getTableName());
		parameters.put(IQueryBuilder.FORM_MIN_QUERY_COLUMN_PARAMETER, columnName);
		parameters.put(IQueryBuilder.FORM_MIN_QUERY_WHERE_CLAUSE_PARAMETER, whereClause);
		parameters.put(IQueryBuilder.FORM_MIN_QUERY_GROUP_BYS_PARAMETER, groupBys.iterator());
		parameters.put(IQueryBuilder.FORM_MIN_QUERY_HAVING_PARAMETER, having);
		

		String query = queryBuilder.formMinQuery(parameters);

		Iterator<Map<String, Object>> datas = database.executeSelectQuery(databaseDescriptor, entityDescriptor, query);
		while(datas.hasNext()) {
			Map<String, Object> data = datas.next();
			Collection<Object> parse = data.values();

			Iterator<Object> values = parse.iterator();
			while(values.hasNext()) {

				Object value = values.next();
				if(value instanceof Integer) {
					return ((Integer) value).intValue();
				} else if(value instanceof Long) {
					return ((Long) value).intValue();
				} else if(value instanceof Float) {
					return ((Float) value).intValue();
				}

			}
		}
		
		return 0;
		
	}
	

	/**
	 * Handles Database Maximum Request From Web.
	 * @param className Web Model Class Name.
	 * @param column Name of Column For Which Maximum Needs To Be Find.
	 * @param whereClause Where Clause based on which Maximum needs to be found.
	 * @param groupBys Used to specify GROUP BY statement in conjunction with the aggregate functions to group the result-set by one or more columns.
	 * @param having Used to specify HAVING clause to SQL because the WHERE keyword could not be used with Maximum functions.
	 * @return Maximum Of Tuples.
	 * @throws DatabaseException If any error occur while getting maximum.
	 */
	public String max(final String className, final String columnName, final String whereClause, final String[] groupBy, final String having) throws DatabaseException {
		
		EntityDescriptor entityDescriptor = getEntityDescriptor(className);

		int max = max(entityDescriptor, columnName, whereClause, groupBy, having);

		WebSiminovDatas webSiminovDatas = new WebSiminovDatas();
		WebSiminovData siminovData = new WebSiminovData();
		
		siminovData.setDataValue(String.valueOf(max));
		
		webSiminovDatas.addWebSiminovData(siminovData);

		String data = null;
		try {
			data = WebSiminovDataWritter.jsonBuidler(webSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(DatabaseHandler.class.getName(), "avg", "SiminovException caught while building json, " + siminovException.getMessage());
			throw new DatabaseException(DatabaseHandler.class.getName(), "avg", "SiminovException caught while building json, " + siminovException.getMessage());
		}

		return data;
		
	}
	

	private int max(final EntityDescriptor entityDescriptor, final String columnName, final String whereClause, final String[] groupBy, final String having) throws DatabaseException {
		
		DatabaseDescriptor databaseDescriptor = getDatabaseDescriptor(entityDescriptor.getClassName());
		DatabaseBundle databaseBundle = coreResourceManager.getDatabaseBundle(databaseDescriptor.getDatabaseName());

		IDatabaseImpl database = databaseBundle.getDatabase();
		IQueryBuilder queryBuilder = databaseBundle.getQueryBuilder();
		
		if(database == null) {
			Log.error(DatabaseHandler.class.getName(), "max", "No Database Instance Found For DATABASE-MAPPING: " + entityDescriptor.getClassName());
			throw new DeploymentException(DatabaseHandler.class.getName(), "max", "No Database Instance Found For DATABASE-MAPPING: " + entityDescriptor.getClassName());
		}

		Collection<String> groupBys = new ArrayList<String>();
		if(groupBy != null && groupBy.length > 0) {
			for(int i = 0;i < groupBy.length;i++) {
				groupBys.add(groupBy[i]);
			}
		}
		
		
		/*
		 * Add Parameters
		 */
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(IQueryBuilder.FORM_MAX_QUERY_TABLE_NAME_PARAMETER, entityDescriptor.getTableName());
		parameters.put(IQueryBuilder.FORM_MAX_QUERY_COLUMN_PARAMETER, columnName);
		parameters.put(IQueryBuilder.FORM_MAX_QUERY_WHERE_CLAUSE_PARAMETER, whereClause);
		parameters.put(IQueryBuilder.FORM_MAX_QUERY_GROUP_BYS_PARAMETER, groupBys.iterator());
		parameters.put(IQueryBuilder.FORM_MAX_QUERY_HAVING_PARAMETER, having);
		

		String query = queryBuilder.formMaxQuery(parameters);

		Iterator<Map<String, Object>> datas = database.executeSelectQuery(databaseDescriptor, entityDescriptor, query);
		while(datas.hasNext()) {
			Map<String, Object> data = datas.next();
			Collection<Object> parse = data.values();

			Iterator<Object> values = parse.iterator();
			while(values.hasNext()) {

				Object value = values.next();
				if(value instanceof Integer) {
					return ((Integer) value).intValue();
				} else if(value instanceof Long) {
					return ((Long) value).intValue();
				} else if(value instanceof Float) {
					return ((Float) value).intValue();
				}

			}
		}
		
		return 0;
		
	}
	

	
	/**
	 * Handles Database Group Concat Request From Web.
	 * @param className Web Model Class Name.
	 * @param column Name of Column For Which Group Concat Needs To Be Find.
	 * @param whereClause Where Clause based on which Group Concat needs to be found.
	 * @param groupBys Used to specify GROUP BY statement in conjunction with the aggregate functions to group the result-set by one or more columns.
	 * @param having Used to specify HAVING clause to SQL because the WHERE keyword could not be used with Group Concat functions.
	 * @return Group Concat Of Tuples.
	 * @throws DatabaseException If any error occur while getting Group Concat.
	 */
	public String groupConcat(final String className, final String columnName, final String delimiter, final String whereClause, final String[] groupBy, final String having) throws DatabaseException {

		EntityDescriptor entityDescriptor = getEntityDescriptor(className);

		String groupConcat = groupConcat(entityDescriptor, columnName, delimiter, whereClause, groupBy, having);
		
		WebSiminovDatas webSiminovDatas = new WebSiminovDatas();
		WebSiminovData siminovData = new WebSiminovData();
		
		siminovData.setDataValue(groupConcat);
		
		webSiminovDatas.addWebSiminovData(siminovData);

		String data = null;
		try {
			data = WebSiminovDataWritter.jsonBuidler(webSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(DatabaseHandler.class.getName(), "groupConcat", "SiminovException caught while building json, " + siminovException.getMessage());
			throw new DatabaseException(DatabaseHandler.class.getName(), "groupConcat", "SiminovException caught while building json, " + siminovException.getMessage());
		}

		return data;

	}
	
	private String groupConcat(final EntityDescriptor entityDescriptor, final String columnName, final String delimiter, final String whereClause, final String[] groupBy, final String having) throws DatabaseException {
		
		DatabaseDescriptor databaseDescriptor = getDatabaseDescriptor(entityDescriptor.getClassName());
		DatabaseBundle databaseBundle = coreResourceManager.getDatabaseBundle(databaseDescriptor.getDatabaseName());

		IDatabaseImpl database = databaseBundle.getDatabase();
		IQueryBuilder queryBuilder = databaseBundle.getQueryBuilder();
		
		if(database == null) {
			Log.error(DatabaseHandler.class.getName(), "groupConcat", "No Database Instance Found For DATABASE-MAPPING: " + entityDescriptor.getClassName());
			throw new DeploymentException(DatabaseHandler.class.getName(), "groupConcat", "No Database Instance Found For DATABASE-MAPPING: " + entityDescriptor.getClassName());
		}

		Collection<String> groupBys = new ArrayList<String>();
		if(groupBy != null && groupBy.length > 0) {
			for(int i = 0;i < groupBy.length;i++) {
				groupBys.add(groupBy[i]);
			}
		}
		
		
		/*
		 * Add Parameters
		 */
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(IQueryBuilder.FORM_GROUP_CONCAT_QUERY_TABLE_NAME_PARAMETER, entityDescriptor.getTableName());
		parameters.put(IQueryBuilder.FORM_GROUP_CONCAT_QUERY_COLUMN_PARAMETER, columnName);
		parameters.put(IQueryBuilder.FORM_GROUP_CONCAT_QUERY_WHERE_CLAUSE_PARAMETER, delimiter);
		parameters.put(IQueryBuilder.FORM_GROUP_CONCAT_QUERY_GROUP_BYS_PARAMETER, whereClause);
		parameters.put(IQueryBuilder.FORM_GROUP_CONCAT_QUERY_HAVING_PARAMETER, groupBys.iterator());
		parameters.put(IQueryBuilder.FORM_GROUP_CONCAT_QUERY_DELIMITER_PARAMETER, having);
		

		String query = queryBuilder.formGroupConcatQuery(parameters);

		Iterator<Map<String, Object>> datas = database.executeSelectQuery(databaseDescriptor, entityDescriptor, query);
		while(datas.hasNext()) {
			Map<String, Object> data = datas.next();
			Collection<Object> parse = data.values();

			Iterator<Object> values = parse.iterator();
			while(values.hasNext()) {
				return ((String) values.next());
			}
		}
		
		return "";
		
	}
	

	
	/**
	 * Handles Database Get Table Name Request From Web.
	 * @param className Web Model Class Name.
	 * @return Table Name.
	 * @throws DatabaseException If any error occur while get table name mapped to web model class name.
	 */
	public String getTableName(final String className) throws DatabaseException {
	
		EntityDescriptor entityDescriptor = getEntityDescriptor(className);

		String tableName = getTableName(entityDescriptor);
		
		WebSiminovDatas webSiminovDatas = new WebSiminovDatas();
		WebSiminovData siminovData = new WebSiminovData();
		
		siminovData.setDataValue(String.valueOf(tableName));
		
		webSiminovDatas.addWebSiminovData(siminovData);

		String data = null;
		try {
			data = WebSiminovDataWritter.jsonBuidler(webSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(DatabaseHandler.class.getName(), "getTableName", "SiminovException caught while building json, " + siminovException.getMessage());
			throw new DatabaseException(DatabaseHandler.class.getName(), "getTableName", "SiminovException caught while building json, " + siminovException.getMessage());
		}

		return data;
		
	}
	
	private String getTableName(final EntityDescriptor entityDescriptor) throws DatabaseException {
		return entityDescriptor.getTableName();
	}

	
	/**
	 * Handles Database Get Columns Names Request From Web.
	 * @param className Web Model Class Name.
	 * @return Column Names.
	 * @throws DatabaseException If any error occur while getting column names mapped to web model class name.
	 */
	public String getColumnNames(final String className) throws DatabaseException {
		
		EntityDescriptor entityDescriptor = getEntityDescriptor(className);

		Iterator<String> columnNames = getColumnNames(entityDescriptor);
		
		WebSiminovDatas webSiminovDatas = new WebSiminovDatas();

		while(columnNames.hasNext()) {
			String columnName = columnNames.next();
			
			WebSiminovData siminovData = new WebSiminovData();
			siminovData.setDataValue(columnName);
			
			webSiminovDatas.addWebSiminovData(siminovData);
		}

		
		String data = null;
		try {
			data = WebSiminovDataWritter.jsonBuidler(webSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(DatabaseHandler.class.getName(), "getTableName", "SiminovException caught while building json, " + siminovException.getMessage());
			throw new DatabaseException(DatabaseHandler.class.getName(), "getTableName", "SiminovException caught while building json, " + siminovException.getMessage());
		}

		return data;
		
	}
	
	public static final Iterator<String> getColumnNames(final EntityDescriptor entityDescriptor) throws DatabaseException {
		
		Iterator<EntityDescriptor.Attribute> attributes = entityDescriptor.getAttributes();

		Collection<String> columnNames = new ArrayList<String>();
		while(attributes.hasNext()) {
			columnNames.add(attributes.next().getColumnName());
		}

		/*
		 * Add ONE-TO-MANY And MANY-TO-MANY Relationship Columns.
		 */
		Iterator<Relationship> oneToManyRelationships = entityDescriptor.getManyToOneRelationships();
		Iterator<Relationship> manyToManyRelationships = entityDescriptor.getManyToManyRelationships();
		
		while(oneToManyRelationships.hasNext()) {
			Relationship oneToManyRelationship = oneToManyRelationships.next();
			EntityDescriptor referedEntityDescriptor = oneToManyRelationship.getReferedEntityDescriptor();
			if(referedEntityDescriptor == null) {
				referedEntityDescriptor = getEntityDescriptor(oneToManyRelationship.getReferTo());
				oneToManyRelationship.setReferedEntityDescriptor(referedEntityDescriptor);
			}

			Iterator<Attribute> parentAttributes = referedEntityDescriptor.getAttributes();
			while(parentAttributes.hasNext()) {
				Attribute attribute = parentAttributes.next();
				
				boolean isPrimary = attribute.isPrimaryKey();
				if(isPrimary) {
					columnNames.add(attributes.next().getColumnName());
				}
			}
		}
		
		while(manyToManyRelationships.hasNext()) {
			Relationship manyToManyRelationship = manyToManyRelationships.next();
			EntityDescriptor parentEntityDescriptor = manyToManyRelationship.getReferedEntityDescriptor();
			
			Iterator<Attribute> parentAttributes = parentEntityDescriptor.getAttributes();
			while(parentAttributes.hasNext()) {
				Attribute attribute = parentAttributes.next();
				
				boolean isPrimary = attribute.isPrimaryKey();
				if(isPrimary) {
					columnNames.add(attributes.next().getColumnName());
				}
			}
		}
		

		return columnNames.iterator();
		
	}
	

	
	/**
	 * Handles Database Get Columns Types Request From Web.
	 * @param className Web Model Class Name.
	 * @return Column Types.
	 * @throws DatabaseException If any error occur while getting column types mapped to web model class name.
	 */
	public String getColumnTypes(final String className) throws DatabaseException {
		
		EntityDescriptor entityDescriptor = getEntityDescriptor(className);

		return getColumnType(entityDescriptor);
		
	}
	
	public static final String getColumnType(final EntityDescriptor entityDescriptor) throws DatabaseException {
		
		Map<String, Object> columnTypes = new HashMap<String, Object> ();
		Iterator<EntityDescriptor.Attribute> attributes = entityDescriptor.getAttributes();
		
		while(attributes.hasNext()) {
			Attribute attribute = attributes.next();
			columnTypes.put(attribute.getColumnName(), attribute.getType());
		}
		
		/*
		 * Add ONE-TO-MANY And MANY-TO-MANY Relationship Columns.
		 */
		Iterator<Relationship> oneToManyRelationships = entityDescriptor.getManyToOneRelationships();
		Iterator<Relationship> manyToManyRelationships = entityDescriptor.getManyToManyRelationships();
		
		while(oneToManyRelationships.hasNext()) {
			Relationship oneToManyRelationship = oneToManyRelationships.next();
			EntityDescriptor referedEntityDescriptor = oneToManyRelationship.getReferedEntityDescriptor();
			if(referedEntityDescriptor == null) {
				referedEntityDescriptor = getEntityDescriptor(oneToManyRelationship.getReferTo());
				oneToManyRelationship.setReferedEntityDescriptor(referedEntityDescriptor);
			}

			
			Iterator<Attribute> parentAttributes = referedEntityDescriptor.getAttributes();
			while(parentAttributes.hasNext()) {
				Attribute attribute = parentAttributes.next();
				
				boolean isPrimary = attribute.isPrimaryKey();
				if(isPrimary) {
					columnTypes.put(attribute.getColumnName(), attribute.getType());
				}
			}
		}
		
		while(manyToManyRelationships.hasNext()) {
			Relationship manyToManyRelationship = manyToManyRelationships.next();
			EntityDescriptor parentEntityDescriptor = manyToManyRelationship.getReferedEntityDescriptor();
			
			Iterator<Attribute> parentAttributes = parentEntityDescriptor.getAttributes();
			while(parentAttributes.hasNext()) {
				Attribute attribute = parentAttributes.next();
				
				boolean isPrimary = attribute.isPrimaryKey();
				if(isPrimary) {
					columnTypes.put(attribute.getColumnName(), attribute.getType());
				}
			}
		}


		Collection<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
		values.add(columnTypes);
		
		WebSiminovDatas webSiminovDatas = parseData(entityDescriptor, values.iterator());
		
		String returnData = null;
		try {
			returnData = WebSiminovDataWritter.jsonBuidler(webSiminovDatas);		
		} catch(SiminovException siminovException) {
			Log.error(DatabaseHandler.class.getName(), "getColumnTypes", "SiminovException caught while building json output, " + siminovException.getMessage());
			throw new DatabaseException(DatabaseHandler.class.getName(), "getColumnTypes", "SiminovException caught while building json output, " + siminovException.getMessage());
		}
		
		return returnData;
		
	}
	
	
	/**
	 * Handles Database Get Primary Column Names Request From Web.
	 * @param className Web Model Class Name.
	 * @return Primary Column Names.
	 * @throws DatabaseException If any error occur while get primary column names mapped to web model class name.
	 */
	public String getPrimaryKeys(final String className) throws DatabaseException {
		
		EntityDescriptor entityDescriptor = getEntityDescriptor(className);

		Iterator<String> primaryKeys = getPrimaryKeys(entityDescriptor);
		
		WebSiminovDatas webSiminovDatas = new WebSiminovDatas();

		while(primaryKeys.hasNext()) {
			String columnName = primaryKeys.next();
			
			WebSiminovData siminovData = new WebSiminovData();
			siminovData.setDataValue(columnName);
			
			webSiminovDatas.addWebSiminovData(siminovData);
		}

		
		String data = null;
		try {
			data = WebSiminovDataWritter.jsonBuidler(webSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(DatabaseHandler.class.getName(), "getPrimaryKeys", "SiminovException caught while building json, " + siminovException.getMessage());
			throw new DatabaseException(DatabaseHandler.class.getName(), "getPrimaryKeys", "SiminovException caught while building json, " + siminovException.getMessage());
		}

		return data;

	}
	
	public static final Iterator<String> getPrimaryKeys(final EntityDescriptor entityDescriptor) throws DatabaseException {
		
		Iterator<Attribute> attributes = entityDescriptor.getAttributes();
		Collection<String> primaryKeys = new ArrayList<String>();

		while(attributes.hasNext()) {
			Attribute attribute = attributes.next();
			
			boolean isPrimary = attribute.isPrimaryKey();
			if(isPrimary) {
				primaryKeys.add(attribute.getColumnName());
			}
		}

		/*
		 * Add ONE-TO-MANY And MANY-TO-MANY Relationship Columns.
		 */
		Iterator<Relationship> oneToManyRelationships = entityDescriptor.getManyToOneRelationships();
		Iterator<Relationship> manyToManyRelationships = entityDescriptor.getManyToManyRelationships();
		
		while(oneToManyRelationships.hasNext()) {
			Relationship oneToManyRelationship = oneToManyRelationships.next();
			EntityDescriptor referedEntityDescriptor = oneToManyRelationship.getReferedEntityDescriptor();
			if(referedEntityDescriptor == null) {
				referedEntityDescriptor = getEntityDescriptor(oneToManyRelationship.getReferTo());
				oneToManyRelationship.setReferedEntityDescriptor(referedEntityDescriptor);
			}

			
			Iterator<Attribute> parentAttributes = referedEntityDescriptor.getAttributes();
			while(parentAttributes.hasNext()) {
				Attribute attribute = parentAttributes.next();
				
				boolean isPrimary = attribute.isPrimaryKey();
				if(isPrimary) {
					primaryKeys.add(attribute.getColumnName());
				}
			}
		}
		
		while(manyToManyRelationships.hasNext()) {
			Relationship manyToManyRelationship = manyToManyRelationships.next();
			EntityDescriptor parentEntityDescriptor = manyToManyRelationship.getReferedEntityDescriptor();
			
			Iterator<Attribute> parentAttributes = parentEntityDescriptor.getAttributes();
			while(parentAttributes.hasNext()) {
				Attribute attribute = parentAttributes.next();
				
				boolean isPrimary = attribute.isPrimaryKey();
				if(isPrimary) {
					primaryKeys.add(attribute.getColumnName());
				}
			}
		}
		
		return primaryKeys.iterator();
		
	}
	

	/**
	 * Handles Database Get Mandatory Column Names Request From Web.
	 * @param className Web Model Class Name.
	 * @return Mandatory Names.
	 * @throws DatabaseException If any error occur while getting mandatory column names mapped to web model class name.
	 */
	public String getMandatoryFields(final String className) throws DatabaseException {
		
		EntityDescriptor entityDescriptor = getEntityDescriptor(className);

		Iterator<String> mandatoryFields = getMandatoryFields(entityDescriptor);
		
		WebSiminovDatas webSiminovDatas = new WebSiminovDatas();

		while(mandatoryFields.hasNext()) {
			String columnName = mandatoryFields.next();
			
			WebSiminovData siminovData = new WebSiminovData();
			siminovData.setDataValue(columnName);
			
			webSiminovDatas.addWebSiminovData(siminovData);
		}

		
		String data = null;
		try {
			data = WebSiminovDataWritter.jsonBuidler(webSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(DatabaseHandler.class.getName(), "getMandatoryFields", "SiminovException caught while building json, " + siminovException.getMessage());
			throw new DatabaseException(DatabaseHandler.class.getName(), "getMandatoryFields", "SiminovException caught while building json, " + siminovException.getMessage());
		}

		return data;

	}
	
	public static final Iterator<String> getMandatoryFields(final EntityDescriptor entityDescriptor) throws DatabaseException {
		
		Iterator<Attribute> attributes = entityDescriptor.getAttributes();
		Collection<String> mandatoryFields = new ArrayList<String>();
		
		while(attributes.hasNext()) {
			Attribute attribute = attributes.next();
			
			if(attribute.isNotNull()) {
				mandatoryFields.add(attribute.getColumnName());
			}
		}
		

		/*
		 * Add ONE-TO-MANY And MANY-TO-MANY Relationship Columns.
		 */
		Iterator<Relationship> oneToManyRelationships = entityDescriptor.getManyToOneRelationships();
		Iterator<Relationship> manyToManyRelationships = entityDescriptor.getManyToManyRelationships();
		
		while(oneToManyRelationships.hasNext()) {
			Relationship oneToManyRelationship = oneToManyRelationships.next();
			EntityDescriptor referedEntityDescriptor = oneToManyRelationship.getReferedEntityDescriptor();
			if(referedEntityDescriptor == null) {
				referedEntityDescriptor = getEntityDescriptor(oneToManyRelationship.getReferTo());
				oneToManyRelationship.setReferedEntityDescriptor(referedEntityDescriptor);
			}

			
			Iterator<Attribute> parentAttributes = referedEntityDescriptor.getAttributes();
			while(parentAttributes.hasNext()) {
				Attribute attribute = parentAttributes.next();
				
				boolean isPrimary = attribute.isPrimaryKey();
				if(isPrimary) {
					if(attribute.isNotNull()) {
						mandatoryFields.add(attribute.getColumnName());
					}
				}
			}
		}
		
		while(manyToManyRelationships.hasNext()) {
			Relationship manyToManyRelationship = manyToManyRelationships.next();
			EntityDescriptor parentEntityDescriptor = manyToManyRelationship.getReferedEntityDescriptor();
			
			Iterator<Attribute> parentAttributes = parentEntityDescriptor.getAttributes();
			while(parentAttributes.hasNext()) {
				Attribute attribute = parentAttributes.next();
				
				boolean isPrimary = attribute.isPrimaryKey();
				if(isPrimary) {
					if(attribute.isNotNull()) {
						mandatoryFields.add(attribute.getColumnName());
					}
				}
			}
		}
		
		return mandatoryFields.iterator();
		
	}
	
	
	/**
	 * Handles Database Get Unique Column Names Request From Web.
	 * @param className Web Model Class Name.
	 * @return Unique Column Names.
	 * @throws DatabaseException If any error occur while getting unique column names. 
	 */
	public String getUniqueFields(final String className) throws DatabaseException {
		
		EntityDescriptor entityDescriptor = getEntityDescriptor(className);

		Iterator<String> uniqueFields = getUniqueFields(entityDescriptor);
		
		WebSiminovDatas webSiminovDatas = new WebSiminovDatas();

		while(uniqueFields.hasNext()) {
			String columnName = uniqueFields.next();
			
			WebSiminovData siminovData = new WebSiminovData();
			siminovData.setDataValue(columnName);
			
			webSiminovDatas.addWebSiminovData(siminovData);
		}

		
		String data = null;
		try {
			data = WebSiminovDataWritter.jsonBuidler(webSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(DatabaseHandler.class.getName(), "getUniqueFields", "SiminovException caught while building json, " + siminovException.getMessage());
			throw new DatabaseException(DatabaseHandler.class.getName(), "getUniqueFields", "SiminovException caught while building json, " + siminovException.getMessage());
		}

		return data;

	}
	
	public static final Iterator<String> getUniqueFields(final EntityDescriptor entityDescriptor) throws DatabaseException {
		
		Iterator<Attribute> attributes = entityDescriptor.getAttributes();
		Collection<String> uniqueFields = new ArrayList<String>();
		
		while(attributes.hasNext()) {
			Attribute attribute = attributes.next();
			
			boolean isUnique = attribute.isUnique();
			if(isUnique) {
				uniqueFields.add(attribute.getColumnName());
			}
		}
		
		/*
		 * Add ONE-TO-MANY And MANY-TO-MANY Relationship Columns.
		 */
		Iterator<Relationship> oneToManyRelationships = entityDescriptor.getManyToOneRelationships();
		Iterator<Relationship> manyToManyRelationships = entityDescriptor.getManyToManyRelationships();
		
		while(oneToManyRelationships.hasNext()) {
			Relationship oneToManyRelationship = oneToManyRelationships.next();
			EntityDescriptor referedEntityDescriptor = oneToManyRelationship.getReferedEntityDescriptor();
			if(referedEntityDescriptor == null) {
				referedEntityDescriptor = getEntityDescriptor(oneToManyRelationship.getReferTo());
				oneToManyRelationship.setReferedEntityDescriptor(referedEntityDescriptor);
			}

			
			Iterator<Attribute> parentAttributes = referedEntityDescriptor.getAttributes();
			while(parentAttributes.hasNext()) {
				Attribute attribute = parentAttributes.next();
				
				boolean isPrimary = attribute.isPrimaryKey();
				if(isPrimary) {

					boolean isUnique = attribute.isUnique();
					if(isUnique) {
						uniqueFields.add(attribute.getColumnName());
					}
				}
			}
		}
		
		while(manyToManyRelationships.hasNext()) {
			Relationship manyToManyRelationship = manyToManyRelationships.next();
			EntityDescriptor parentEntityDescriptor = manyToManyRelationship.getReferedEntityDescriptor();
			
			Iterator<Attribute> parentAttributes = parentEntityDescriptor.getAttributes();
			while(parentAttributes.hasNext()) {
				Attribute attribute = parentAttributes.next();
				
				boolean isPrimary = attribute.isPrimaryKey();
				if(isPrimary) {

					boolean isUnique = attribute.isUnique();
					if(isUnique) {
						uniqueFields.add(attribute.getColumnName());
					}
				}
			}
		}
		

		return uniqueFields.iterator();
		
	}
	
	
	/**
	 * Handle Database Get Foreign Column Names Request From Web.
	 * @param className Web Model Class Name.
	 * @return Foreign Column Names.
	 * @throws DatabaseException If any error occur while getting foreign column names.
	 */
	public String getForeignKeys(final String className) throws DatabaseException {
		
		EntityDescriptor entityDescriptor = getEntityDescriptor(className);

		Iterator<String> foreignKeys = getForeignKeys(entityDescriptor);
		
		WebSiminovDatas webSiminovDatas = new WebSiminovDatas();

		while(foreignKeys.hasNext()) {
			String columnName = foreignKeys.next();
			
			WebSiminovData siminovData = new WebSiminovData();
			siminovData.setDataValue(columnName);
			
			webSiminovDatas.addWebSiminovData(siminovData);
		}

		
		String data = null;
		try {
			data = WebSiminovDataWritter.jsonBuidler(webSiminovDatas);
		} catch(SiminovException siminovException) {
			Log.error(DatabaseHandler.class.getName(), "getForeignKeys", "SiminovException caught while building json, " + siminovException.getMessage());
			throw new DatabaseException(DatabaseHandler.class.getName(), "getForeignKeys", "SiminovException caught while building json, " + siminovException.getMessage());
		}

		return data;

	}
	
	public static final Iterator<String> getForeignKeys(final EntityDescriptor entityDescriptor) throws DatabaseException {
		
		Collection<String> foreignKeys = new LinkedList<String>();
		
		/*
		 * Add ONE-TO-MANY And MANY-TO-MANY Relationship Columns.
		 */
		Iterator<Relationship> oneToManyRelationships = entityDescriptor.getManyToOneRelationships();
		Iterator<Relationship> manyToManyRelationships = entityDescriptor.getManyToManyRelationships();
		
		while(oneToManyRelationships.hasNext()) {
			Relationship oneToManyRelationship = oneToManyRelationships.next();
			EntityDescriptor referedEntityDescriptor = oneToManyRelationship.getReferedEntityDescriptor();
			if(referedEntityDescriptor == null) {
				referedEntityDescriptor = getEntityDescriptor(oneToManyRelationship.getReferTo());
				oneToManyRelationship.setReferedEntityDescriptor(referedEntityDescriptor);
			}

			
			Iterator<Attribute> parentAttributes = referedEntityDescriptor.getAttributes();
			while(parentAttributes.hasNext()) {
				Attribute attribute = parentAttributes.next();
				
				boolean isPrimary = attribute.isPrimaryKey();
				if(isPrimary) {
					foreignKeys.add(attribute.getColumnName());
				}
			}
		}
		
		while(manyToManyRelationships.hasNext()) {
			Relationship manyToManyRelationship = manyToManyRelationships.next();
			EntityDescriptor parentEntityDescriptor = manyToManyRelationship.getReferedEntityDescriptor();
			
			Iterator<Attribute> parentAttributes = parentEntityDescriptor.getAttributes();
			while(parentAttributes.hasNext()) {
				Attribute attribute = parentAttributes.next();
				
				boolean isPrimary = attribute.isPrimaryKey();
				if(isPrimary) {
					foreignKeys.add(attribute.getColumnName());
				}
			}
		}
		
		return foreignKeys.iterator();
		
	}
	
	
	private WebSiminovDatas parseWebSiminovDatas(String data) throws DatabaseException {
		
		WebSiminovDataReader webSiminovDataParser = null; 
		data = URLDecoder.decode(data);
		
		try {
			webSiminovDataParser = new WebSiminovDataReader(data);
		} catch(SiminovException siminovException) {
			Log.error(DatabaseHandler.class.getName(), "parseWebSiminovDatas", "SiminovException caught while parsing js core data, " + siminovException.getMessage());
			throw new DatabaseException(DatabaseHandler.class.getName(), "parseWebSiminovDatas", "SiminovException caught while parsing js core data, " + siminovException.getMessage());
		}

		return webSiminovDataParser.getDatas();
	}
	
	public static final DatabaseDescriptor getDatabaseDescriptor(final String className) throws DatabaseException {
		return webResourceManager.getDatabaseDescriptorBasedOnClassName(className);
	}

	public static final EntityDescriptor getEntityDescriptor(final String className) throws DatabaseException {
		return webResourceManager.getEntityDescriptorBasedOnClassName(className);
	}
	
	/**
		Iterates the provided cursor, and returns tuples in form of actual objects.
	 */
	private static WebSiminovDatas parseData(final EntityDescriptor entityDescriptor, Iterator<Map<String, Object>> values) throws DatabaseException {
		
		Collection<Map<String, Object>> tuples = new LinkedList<Map<String, Object>>();
		WebSiminovDatas webSiminovDatas = new WebSiminovDatas();
		
		while(values.hasNext()) {
			Map<String, Object> value = values.next();
			
			WebSiminovData webSiminovData = new WebSiminovData();
			webSiminovData.setDataType(webResourceManager.getMappedWebClassName(entityDescriptor.getClassName()));
			
			Iterator<String> keys = value.keySet().iterator();
			while(keys.hasNext()) {
				
				String columnName = keys.next();
				if(!entityDescriptor.containsAttributeBasedOnColumnName(columnName)) {
					continue;
				}
				
				String variableName = entityDescriptor.getAttributeBasedOnColumnName(columnName).getVariableName();
				
				Object object = value.get(columnName);
				
				WebSiminovValue webSiminovValue = new WebSiminovValue();
				webSiminovValue.setType(variableName);

				if(object instanceof String) {
					webSiminovValue.setValue((String) object);
				} else if(object instanceof Long) {
					webSiminovValue.setValue(((Long) object).toString());
				} else if(object instanceof Float) {
					webSiminovValue.setValue(((Float) object).toString());
				} else if(object instanceof Blob) {
					webSiminovValue.setValue(((Blob) object).toString());
				}

				webSiminovData.addValue(webSiminovValue);
			}
			
			webSiminovDatas.addWebSiminovData(webSiminovData);
			tuples.add(value);
		}
		
		values = tuples.iterator();
		
		return webSiminovDatas;
	}

	
	static void processManyToOneRelationship(final WebSiminovData siminovData, final Collection<String> columnNames, final Collection<Object> columnValues) throws DatabaseException {
		
		EntityDescriptor entityDescriptor = getEntityDescriptor(siminovData.getDataType());
		Iterator<Relationship> manyToOneRelationships = entityDescriptor.getManyToOneRelationships();
		
		while(manyToOneRelationships.hasNext()) {
			Relationship manyToOneRelationship = manyToOneRelationships.next();
			EntityDescriptor referedEntityDescriptor = manyToOneRelationship.getReferedEntityDescriptor();
			if(referedEntityDescriptor == null) {
				referedEntityDescriptor = getEntityDescriptor(manyToOneRelationship.getReferTo());
				manyToOneRelationship.setReferedEntityDescriptor(referedEntityDescriptor);
			}

			WebSiminovData referedWebSiminovData = null;
			Iterator<WebSiminovData> datas = siminovData.getDatas();
			while(datas.hasNext()) {
				WebSiminovData data = datas.next();
				String referedClassName = webResourceManager.getMappedNativeClassName(data.getDataType());
				
				if(referedClassName.equalsIgnoreCase(referedEntityDescriptor.getClassName())) {
					referedWebSiminovData = data;
					break;
				}
			}

			if(referedWebSiminovData == null) {
				Log.error(DatabaseHandler.class.getName(), "processManyToOneRelationship", "Parent Object Not Set, Please Provide Proper Relationship. REFER-TO: " + manyToOneRelationship.getReferTo());
				throw new DatabaseException(DatabaseHandler.class.getName(), "processManyToOneRelationship", "Parent Object Not Set, Please Provide Proper Relationship. REFER-TO: " + manyToOneRelationship.getReferTo());
			}
			
			processManyToOneRelationship(referedWebSiminovData, columnNames, columnValues);
			
			Iterator<WebSiminovValue> webValues = referedWebSiminovData.getValues();
			
			Map<String, WebSiminovValue> webSiminovValues = new HashMap<String, WebSiminovValue>();
			while(webValues.hasNext()) {
				WebSiminovValue webSiminovValue = webValues.next();
				webSiminovValues.put(webSiminovValue.getType(), webSiminovValue);
			}
			
			Iterator<Attribute> parentAttributes = referedEntityDescriptor.getAttributes();
			while(parentAttributes.hasNext()) {
				Attribute attribute = parentAttributes.next();
				
				boolean isPrimary = attribute.isPrimaryKey();
				if(isPrimary) {
					columnNames.add(attribute.getColumnName());
					columnValues.add(webSiminovValues.get(attribute.getVariableName()).getValue());
				}
			}
		}
	}
	
	
	static void processManyToManyRelationship(final WebSiminovData siminovData, final Collection<String> columnNames, final Collection<Object> columnValues) throws DatabaseException {
		
		EntityDescriptor entityDescriptor = getEntityDescriptor(siminovData.getDataType());
		Iterator<Relationship> manyToManyRelationships = entityDescriptor.getManyToManyRelationships();
		
		while(manyToManyRelationships.hasNext()) {
			Relationship manyToManyRelationship = manyToManyRelationships.next();
			EntityDescriptor referedEntityDescriptor = manyToManyRelationship.getReferedEntityDescriptor();
			if(referedEntityDescriptor == null) {
				referedEntityDescriptor = getEntityDescriptor(manyToManyRelationship.getReferTo());
				manyToManyRelationship.setReferedEntityDescriptor(referedEntityDescriptor);
			}

			WebSiminovData referedWebSiminovData = null;
			Iterator<WebSiminovData> datas = siminovData.getDatas();
			while(datas.hasNext()) {
				WebSiminovData data = datas.next();
				String referedClassName = webResourceManager.getMappedNativeClassName(data.getDataType());
				
				if(referedClassName.equalsIgnoreCase(referedEntityDescriptor.getClassName())) {
					referedWebSiminovData = data;
					break;
				}
			}

			if(referedWebSiminovData == null) {
				Log.error(DatabaseHandler.class.getName(), "processManyToManyRelationship", "Parent Object Not Set, Please Provide Proper Relationship. REFER-TO: " + manyToManyRelationship.getReferTo());
				throw new DatabaseException(DatabaseHandler.class.getName(), "processManyToManyRelationship", "Parent Object Not Set, Please Provide Proper Relationship. REFER-TO: " + manyToManyRelationship.getReferTo());
			}
			
			processManyToManyRelationship(referedWebSiminovData, columnNames, columnValues);
			
			Iterator<WebSiminovValue> webValues = referedWebSiminovData.getValues();
			
			Map<String, WebSiminovValue> webSiminovValues = new HashMap<String, WebSiminovValue>();
			while(webValues.hasNext()) {
				WebSiminovValue webSiminovValue = webValues.next();
				webSiminovValues.put(webSiminovValue.getType(), webSiminovValue);
			}
			
			Iterator<Attribute> parentAttributes = referedEntityDescriptor.getAttributes();
			while(parentAttributes.hasNext()) {
				Attribute attribute = parentAttributes.next();
				
				boolean isPrimary = attribute.isPrimaryKey();
				if(isPrimary) {
					columnNames.add(attribute.getColumnName());
					columnValues.add(webSiminovValues.get(attribute.getVariableName()).getValue());
				}
			}
		}
	}
	
	
	static void processManyToOneRelationship(final WebSiminovData siminovData, final StringBuilder whereClause) throws DatabaseException {
		
		EntityDescriptor entityDescriptor = getEntityDescriptor(siminovData.getDataType());
		Iterator<Relationship> manyToOneRelationships = entityDescriptor.getManyToOneRelationships();
		
		while(manyToOneRelationships.hasNext()) {
			Relationship manyToOneRelationship = manyToOneRelationships.next();
			EntityDescriptor referedEntityDescriptor = manyToOneRelationship.getReferedEntityDescriptor();
			if(referedEntityDescriptor == null) {
				referedEntityDescriptor = getEntityDescriptor(manyToOneRelationship.getReferTo());
				manyToOneRelationship.setReferedEntityDescriptor(referedEntityDescriptor);
			}

			WebSiminovData referedWebSiminovData = null;
			Iterator<WebSiminovData> datas = siminovData.getDatas();
			while(datas.hasNext()) {
				WebSiminovData data = datas.next();
				String referedClassName = webResourceManager.getMappedNativeClassName(data.getDataType());
				
				if(referedClassName.equalsIgnoreCase(referedEntityDescriptor.getClassName())) {
					referedWebSiminovData = data;
					break;
				}
			}

			if(referedWebSiminovData == null) {
				Log.error(DatabaseHandler.class.getName(), "processManyToOneRelationship", "Parent Object Not Set, Please Provide Proper Relationship. REFER-TO: " + manyToOneRelationship.getReferTo());
				throw new DatabaseException(DatabaseHandler.class.getName(), "processManyToOneRelationship", "Parent Object Not Set, Please Provide Proper Relationship. REFER-TO: " + manyToOneRelationship.getReferTo());
			}
			
			processManyToOneRelationship(referedWebSiminovData, whereClause);
			
			Iterator<WebSiminovValue> webValues = referedWebSiminovData.getValues();
			
			Map<String, WebSiminovValue> webSiminovValues = new HashMap<String, WebSiminovValue>();
			while(webValues.hasNext()) {
				WebSiminovValue webSiminovValue = webValues.next();
				webSiminovValues.put(webSiminovValue.getType(), webSiminovValue);
			}
			
			Iterator<Attribute> parentAttributes = referedEntityDescriptor.getAttributes();
			while(parentAttributes.hasNext()) {
				Attribute attribute = parentAttributes.next();
				
				boolean isPrimary = attribute.isPrimaryKey();
				if(isPrimary) {
					String columnValue = webSiminovValues.get(attribute.getVariableName()).getValue();
					if(whereClause.length() <= 0) {
						whereClause.append(attribute.getColumnName() + "= '" + columnValue + "'");
					} else {
						whereClause.append(" AND " + attribute.getColumnName() + "= '" + columnValue + "'");
					}
				}
			}
		}
	}
	
	
	static void processManyToManyRelationship(final WebSiminovData siminovData, final StringBuilder whereClause) throws DatabaseException {
		
		EntityDescriptor entityDescriptor = getEntityDescriptor(siminovData.getDataType());
		Iterator<Relationship> manyToManyRelationships = entityDescriptor.getManyToManyRelationships();
		
		while(manyToManyRelationships.hasNext()) {
			Relationship manyToManyRelationship = manyToManyRelationships.next();
			EntityDescriptor referedEntityDescriptor = manyToManyRelationship.getReferedEntityDescriptor();
			if(referedEntityDescriptor == null) {
				referedEntityDescriptor = getEntityDescriptor(manyToManyRelationship.getReferTo());
				manyToManyRelationship.setReferedEntityDescriptor(referedEntityDescriptor);
			}

			WebSiminovData referedWebSiminovData = null;
			Iterator<WebSiminovData> datas = siminovData.getDatas();
			while(datas.hasNext()) {
				WebSiminovData data = datas.next();
				String referedClassName = webResourceManager.getMappedNativeClassName(data.getDataType());
				
				if(referedClassName.equalsIgnoreCase(referedEntityDescriptor.getClassName())) {
					referedWebSiminovData = data;
					break;
				}
			}

			if(referedWebSiminovData == null) {
				Log.error(DatabaseHandler.class.getName(), "processManyToManyRelationship", "Parent Object Not Set, Please Provide Proper Relationship. REFER-TO: " + manyToManyRelationship.getReferTo());
			}
			
			processManyToManyRelationship(referedWebSiminovData, whereClause);
			
			Iterator<WebSiminovValue> webValues = siminovData.getValues();
			
			Map<String, WebSiminovValue> webSiminovValues = new HashMap<String, WebSiminovValue>();
			while(webValues.hasNext()) {
				WebSiminovValue webSiminovValue = webValues.next();
				webSiminovValues.put(webSiminovValue.getType(), webSiminovValue);
			}
			
			Iterator<Attribute> parentAttributes = referedEntityDescriptor.getAttributes();
			while(parentAttributes.hasNext()) {
				Attribute attribute = parentAttributes.next();
				
				boolean isPrimary = attribute.isPrimaryKey();
				if(isPrimary) {
					String columnValue = webSiminovValues.get(attribute.getVariableName()).getValue();
					if(whereClause.length() <= 0) {
						whereClause.append(attribute.getColumnName() + "= '" + columnValue + "'");
					} else {
						whereClause.append(" AND " + attribute.getColumnName() + "= '" + columnValue + "'");
					}
				}
			}
		}
	}
	
	
	private void processOneToOneRelationship(final WebSiminovData webSiminovData) throws DatabaseException {

		EntityDescriptor entityDescriptor = getEntityDescriptor(webSiminovData.getDataType());
		Iterator<EntityDescriptor.Relationship> oneToOneRelationships = entityDescriptor.getOneToOneRelationships();
		
		while(oneToOneRelationships.hasNext()) {
			
			EntityDescriptor.Relationship oneToOneRelationship = oneToOneRelationships.next();

			boolean isLoad = oneToOneRelationship.isLoad();
			if(!isLoad) {
				continue;
			}

			
			StringBuilder whereClause = new StringBuilder();
			Iterator<String> foreignKeys = getPrimaryKeys(entityDescriptor);
			while(foreignKeys.hasNext()) {
				String foreignKey = foreignKeys.next();
				Object columnValue = null;
				
				WebSiminovValue value = webSiminovData.getValueBasedOnType(foreignKey);
				columnValue = value.getValue();

				if(whereClause.length() <= 0) {
					whereClause.append(foreignKey + "='" + columnValue.toString() + "'"); 
				} else {
					whereClause.append(", " + foreignKey + "='" + columnValue.toString() + "'");  
				}
			}

			EntityDescriptor referedEntityDescriptor = oneToOneRelationship.getReferedEntityDescriptor();
			if(referedEntityDescriptor == null) {
				referedEntityDescriptor = getEntityDescriptor(oneToOneRelationship.getReferTo());
				oneToOneRelationship.setReferedEntityDescriptor(referedEntityDescriptor);
			}

			
			WebSiminovDatas referedObject = lazyFetch(referedEntityDescriptor, false, whereClause.toString(), null, null, null, null, null, null);
			Iterator<WebSiminovData> siminovDatas = referedObject.getWebSiminovDatas();
			
			while(siminovDatas.hasNext()) {
				WebSiminovData siminovData = siminovDatas.next();
				webSiminovData.addData(siminovData);
			}
		}
		
	}


	private void processOneToManyRelationship(final WebSiminovData webSiminovData) throws DatabaseException {

		EntityDescriptor entityDescriptor = getEntityDescriptor(webSiminovData.getDataType());
		Iterator<EntityDescriptor.Relationship> oneToManyRelationships = entityDescriptor.getOneToManyRelationships();
		
		while(oneToManyRelationships.hasNext()) {
			
			EntityDescriptor.Relationship oneToManyRelationship = oneToManyRelationships.next();

			boolean isLoad = oneToManyRelationship.isLoad();
			if(!isLoad) {
				continue;
			}

			
			StringBuilder whereClause = new StringBuilder();
			Iterator<String> foreignKeys = getPrimaryKeys(entityDescriptor);
			while(foreignKeys.hasNext()) {
				String foreignKey = foreignKeys.next();
				Attribute attribute = entityDescriptor.getAttributeBasedOnColumnName(foreignKey);
				
				Object columnValue = null;
				
				WebSiminovValue value = webSiminovData.getValueBasedOnType(attribute.getVariableName());
				columnValue = value.getValue();

				if(whereClause.length() <= 0) {
					whereClause.append(foreignKey + "='" + columnValue.toString() + "'"); 
				} else {
					whereClause.append(" AND " + foreignKey + "='" + columnValue.toString() + "'");  
				}
			}

			EntityDescriptor referedEntityDescriptor = oneToManyRelationship.getReferedEntityDescriptor();
			if(referedEntityDescriptor == null) {
				referedEntityDescriptor = getEntityDescriptor(oneToManyRelationship.getReferTo());
				oneToManyRelationship.setReferedEntityDescriptor(referedEntityDescriptor);
			}

			
			WebSiminovDatas referedObject = lazyFetch(referedEntityDescriptor, false, whereClause.toString(), null, null, null, null, null, null);
			Iterator<WebSiminovData> siminovDatas = referedObject.getWebSiminovDatas();
			
			while(siminovDatas.hasNext()) {
				WebSiminovData siminovData = siminovDatas.next();
				webSiminovData.addData(siminovData);
			}
		}
	}
	
	
	private void processManyToOneRelationship(final WebSiminovData webSiminovData, Map<String, Object> data) throws DatabaseException {

		EntityDescriptor entityDescriptor = getEntityDescriptor(webSiminovData.getDataType());
		Iterator<Relationship> manyToOneRelationships = entityDescriptor.getManyToOneRelationships();

		while(manyToOneRelationships.hasNext()) {
			Relationship manyToOneRelationship = manyToOneRelationships.next();
			EntityDescriptor referedEntityDescriptor = manyToOneRelationship.getReferedEntityDescriptor();
			if(referedEntityDescriptor == null) {
				referedEntityDescriptor = getEntityDescriptor(manyToOneRelationship.getReferTo());
				manyToOneRelationship.setReferedEntityDescriptor(referedEntityDescriptor);
			}

			WebSiminovData referedObject = new WebSiminovData();
			referedObject.setDataType(webResourceManager.getMappedWebClassName(referedEntityDescriptor.getClassName()));
			
			processManyToOneRelationship(referedObject, data);

			if(manyToOneRelationship.isLoad()) {

				StringBuilder whereClause = new StringBuilder();

				Iterator<String> foreignKeys = getPrimaryKeys(referedEntityDescriptor);
				while(foreignKeys.hasNext()) {
					String foreignKey = foreignKeys.next();
					Attribute attribute = referedEntityDescriptor.getAttributeBasedOnColumnName(foreignKey);
					Object columnValue = data.get(attribute.getColumnName());

					if(whereClause.length() <= 0) {
						whereClause.append(foreignKey + "='" + columnValue.toString() + "'"); 
					} else {
						whereClause.append(" AND " + foreignKey + "='" + columnValue.toString() + "'");  
					}
				}
				
				WebSiminovDatas fetchedObjects = lazyFetch(referedEntityDescriptor, false, whereClause.toString(), null, null, null, null, null, null);
				referedObject = fetchedObjects.getWebSiminovDatas().next();
				
			} else {
				Iterator<String> foreignKeys = getPrimaryKeys(referedEntityDescriptor);
				while(foreignKeys.hasNext()) {
					String foreignKey = foreignKeys.next();
					Attribute attribute = referedEntityDescriptor.getAttributeBasedOnColumnName(foreignKey);

					Object columnValue = data.get(attribute.getColumnName());
					if(columnValue == null) {
						continue;
					}
					
					WebSiminovValue value = new WebSiminovValue();
					value.setType(foreignKey);
					value.setValue(columnValue.toString());
					
				}
			}
			

			if(referedObject == null) {
				Log.error(DatabaseHandler.class.getName(), "processManyToOneRelationship", "Unable To Create Parent Relationship. REFER-TO: " + manyToOneRelationship.getReferTo());
				throw new DatabaseException(DatabaseHandler.class.getName(), "processManyToOneRelationship", "Unable To Create Parent Relationship. REFER-TO: " + manyToOneRelationship.getReferTo());
			}

			webSiminovData.addData(referedObject);
		}
	}
	
	
	private void processManyToManyRelationship(final WebSiminovData webSiminovData, Map<String, Object> data) throws DatabaseException {

		EntityDescriptor entityDescriptor = getEntityDescriptor(webSiminovData.getDataType());
		Iterator<Relationship> manyToManyRelationships = entityDescriptor.getManyToManyRelationships();

		while(manyToManyRelationships.hasNext()) {
			Relationship manyToManyRelationship = manyToManyRelationships.next();
			EntityDescriptor referedEntityDescriptor = manyToManyRelationship.getReferedEntityDescriptor();
			if(referedEntityDescriptor == null) {
				referedEntityDescriptor = getEntityDescriptor(manyToManyRelationship.getReferTo());
				manyToManyRelationship.setReferedEntityDescriptor(referedEntityDescriptor);
			}

			WebSiminovData referedObject = new WebSiminovData();
			referedObject.setDataType(webResourceManager.getMappedWebClassName(referedEntityDescriptor.getClassName()));
			
			processManyToManyRelationship(referedObject, data);

			if(manyToManyRelationship.isLoad()) {

				StringBuilder whereClause = new StringBuilder();

				Iterator<String> foreignKeys = getPrimaryKeys(referedEntityDescriptor);
				while(foreignKeys.hasNext()) {
					String foreignKey = foreignKeys.next();
					Attribute attribute = referedEntityDescriptor.getAttributeBasedOnColumnName(foreignKey);
					Object columnValue = data.get(attribute.getColumnName());

					if(whereClause.length() <= 0) {
						whereClause.append(foreignKey + "='" + columnValue.toString() + "'"); 
					} else {
						whereClause.append(" AND " + foreignKey + "='" + columnValue.toString() + "'");  
					}
				}
				
				WebSiminovDatas fetchedObjects = lazyFetch(referedEntityDescriptor, false, whereClause.toString(), null, null, null, null, null, null);
				referedObject = fetchedObjects.getWebSiminovDatas().next();
				
			} else {
				Iterator<String> foreignKeys = getPrimaryKeys(referedEntityDescriptor);
				while(foreignKeys.hasNext()) {
					String foreignKey = foreignKeys.next();
					Attribute attribute = referedEntityDescriptor.getAttributeBasedOnColumnName(foreignKey);

					Object columnValue = data.get(attribute.getColumnName());
					if(columnValue == null) {
						continue;
					}
					
					WebSiminovValue value = new WebSiminovValue();
					value.setType(foreignKey);
					value.setValue(columnValue.toString());
					
				}
			}
			

			if(referedObject == null) {
				Log.error(DatabaseHandler.class.getName(), "processManyToManyRelationship", "Unable To Create Parent Relationship. REFER-TO: " + manyToManyRelationship.getReferTo());
				throw new DatabaseException(DatabaseHandler.class.getName(), "processManyToManyRelationship", "Unable To Create Parent Relationship. REFER-TO: " + manyToManyRelationship.getReferTo());
			}

			webSiminovData.addData(referedObject);
		}
	}
}