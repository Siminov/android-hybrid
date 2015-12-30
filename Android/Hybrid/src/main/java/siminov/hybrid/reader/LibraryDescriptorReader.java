/**
 * [SIMINOV FRAMEWORK - HYBRID]
 * Copyright [2014-2016] [Siminov Software Solution LLP|support@siminov.com]
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

package siminov.hybrid.reader;

import java.io.File;
import java.io.InputStream;
import java.util.Iterator;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import siminov.hybrid.Constants;
import siminov.hybrid.model.LibraryDescriptor;
import siminov.core.exception.DeploymentException;
import siminov.core.log.Log;
import siminov.core.reader.SiminovSAXDefaultHandler;

/**
 * Exposes methods to parse Library Descriptor information as per define in LibraryDescriptor.xml file by application.
	<p>
		<pre>
		
Example:
	{@code
	<!-- DESIGN OF LibraryDescriptor.xml -->
	
	<library-descriptor>
	
	    <!-- General Properties Of Library -->
	    
	    <!-- Mandatory Field -->
		<property name="name">name_of_library</property>
		
		<!-- Optional Field -->
		<property name="description">description_of_library</property>
	
		
		
		<!-- Entity Descriptor Needed Under This Library Descriptor -->
		
		<!-- Optional Field -->
			<!-- Entity Descriptor -->
		<entity-descriptors>
			<entity-descriptor>name_of_database_descriptor.full_path_of_entity_descriptor_file</entity-descriptor>
		</entity-descriptors>
		 
		
		<!-- Service Descriptors -->
			
		<!-- Optional Field -->
			<!-- Service Descriptor -->
		<service-descriptors>
		    <service-descriptor>full_path_of_service-descriptor_file</service-descriptor>
		</service-descriptors>
		
		
		<!-- Sync Descriptors -->
		
		<!-- Optional Field -->
			<!-- Sync Descriptor -->
		<sync-descriptors>
		    <sync-descriptor>full_path_of_sync_descriptor_file</sync-descriptor>
		</sync-descriptors>
		
		
		<!-- Adapter Descriptors -->
		
		<!-- Optional Field -->
			<!-- Adapter Descriptor -->
		<adapter-descriptors>
		    <adapter-descriptor>full_path_of_adapter_descriptor_file</adapter-descriptor>
		</adapter-descriptors>	
			
		
	</library-descriptor>
	
	}
	
		</pre>
	</p>
 *
 */
public class LibraryDescriptorReader extends SiminovSAXDefaultHandler implements siminov.core.Constants, Constants {

	private String libraryName = null;
	
	private StringBuilder tempValue = new StringBuilder();
	private LibraryDescriptor libraryDescriptor = new LibraryDescriptor();
	
	private String propertyName = "";
	
	public LibraryDescriptorReader(final String libraryName) {

		if(libraryName == null || libraryName.length() <= 0) {
			Log.error(getClass().getName(), "Constructor", "Invalid Library Name Found.");
			throw new DeploymentException(getClass().getName(), "Constructor", "Invalid Library Name Found.");
		}
		
		this.libraryName = libraryName;
		this.libraryName = this.libraryName.replace(".", "/");
		
		
		InputStream libraryDescriptorStream = null;
		libraryDescriptorStream = getClass().getClassLoader().getResourceAsStream(this.libraryName + File.separator + siminov.core.Constants.LIBRARY_DESCRIPTOR_FILE_NAME);

		if(libraryDescriptorStream == null) {
			Log.error(getClass().getName(), "Constructor", "Invalid Library Descriptor Stream Found, LIBRARY-NAME: " + this.libraryName + File.separator + siminov.core.Constants.LIBRARY_DESCRIPTOR_FILE_NAME);
			throw new DeploymentException(getClass().getName(), "Constructor", "Invalid Library Descriptor Stream Found, LIBRARY-NAME: " + this.libraryName + File.separator + siminov.core.Constants.LIBRARY_DESCRIPTOR_FILE_NAME);
		}
		
		try {
			parseMessage(libraryDescriptorStream);
		} catch(Exception exception) {
			Log.error(getClass().getName(), "Constructor", "Exception caught while parsing LIBRARY-DESCRIPTOR: " + this.libraryName + ", " + exception.getMessage());
			throw new DeploymentException(getClass().getName(), "Constructor", "Exception caught while parsing LIBRARY-DESCRIPTOR: " + this.libraryName + ", " + exception.getMessage());
		}
		
		doValidation();
	}
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		
		tempValue = new StringBuilder();
		
		if(localName.equalsIgnoreCase(LIBRARY_DESCRIPTOR_LIBRARY_DESCRIPTOR)) {
			libraryDescriptor = new LibraryDescriptor();
		} else if(localName.equalsIgnoreCase(LIBRARY_DESCRIPTOR_PROPERTY)) {
			initializeProperty(attributes);
		} 
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		String value = new String(ch,start,length);
		
		if(value == null || value.length() <= 0 || value.equalsIgnoreCase(NEW_LINE)) {
			return;
		}
		
		value = value.trim();
		tempValue.append(value);
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		
		if(localName.equalsIgnoreCase(LIBRARY_DESCRIPTOR_PROPERTY)) {
			libraryDescriptor.addProperty(propertyName, tempValue.toString());
		} else if(localName.equalsIgnoreCase(LIBRARY_DESCRIPTOR_ENTITY_DESCRIPTOR)) {
			libraryDescriptor.addEntityDescriptorPath(tempValue.toString());
		} else if(localName.equalsIgnoreCase(HYBRID_LIBRARY_DESCRIPTOR_ADAPTER_DESCRIPTOR)) {
			libraryDescriptor.addAdapterDescriptorPath(tempValue.toString());
		}
		
		super.endElement(uri, localName, qName);
	}
	
	private void initializeProperty(final Attributes attributes) {
		propertyName = attributes.getValue(LIBRARY_DESCRIPTOR_NAME);
	}

	private void doValidation() throws DeploymentException {
		
		/*
		 * Validation for name field.
		 */
		String name = libraryDescriptor.getName();
		if(name == null || name.length() <= 0) {
			Log.error(getClass().getName(), "doValidation", "LIBRARY-NAME IS MANDATORY FIELD - LIBRARY-DESCRIPTOR: " + this.libraryName);
			throw new DeploymentException(getClass().getName(), "doValidation", "LIBRARY-NAME IS MANDATORY FIELD - LIBRARY-DESCRIPTOR: " + this.libraryName);
		}
	}

	/**
	 * Get library descriptor
	 * @return Library Descriptor
	 */
	public LibraryDescriptor getLibraryDescriptor() {
		return this.libraryDescriptor;
	}
	
}
