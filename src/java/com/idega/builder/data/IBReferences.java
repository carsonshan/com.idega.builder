package com.idega.builder.data;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.idega.builder.app.IBApplication;
import com.idega.builder.business.XMLConstants;
import com.idega.core.builder.data.ICPage;
import com.idega.idegaweb.IWBundle;
import com.idega.io.serialization.StorableHolder;
import com.idega.presentation.IWContext;
import com.idega.util.xml.XMLData;
import com.idega.xml.XMLElement;

/**
 * <p>Title: idegaWeb</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: idega Software</p>
 * @author <a href="thomas@idega.is">Thomas Hilbig</a>
 * @version 1.0
 * Created on Mar 24, 2004
 */
public class IBReferences {
	
	public static final String EXPORT_DEFINITION = "exportdefinition.xml";
	
	private Map moduleReference = null;
	
	public IBReferences(IWContext iwc) throws IOException  {
		initialize(iwc);
	}
	
	private void initialize(IWContext iwc) throws IOException {
		moduleReference = new HashMap();
		IWBundle bundle = iwc.getIWMainApplication().getBundle(IBApplication.IB_BUNDLE_IDENTIFIER);
		String exportDefinitionPath = bundle.getRealPathWithFileNameString(EXPORT_DEFINITION);
		XMLData exportDefinition = XMLData.getInstanceForFile(exportDefinitionPath);
		XMLElement root = exportDefinition.getDocument().getRootElement();
		List children = root.getChildren(XMLConstants.EXPORT_MODULE);
		Iterator iterator = children.iterator();
		while (iterator.hasNext()) {
			XMLElement module = (XMLElement) iterator.next();
			IBReference reference = new IBReference(module, iwc);
			moduleReference.put(reference.getModuleClass(), reference);
		}
	}

	public StorableHolder createSourceFromElement(XMLElement metaDataFileElement) throws IOException {
		String moduleName = metaDataFileElement.getTextTrim(XMLConstants.FILE_MODULE);
		IBReference reference = (IBReference) moduleReference.get(moduleName);
		if (reference == null) {
			// shouldn't happen
			return null;
		}
		String name = metaDataFileElement.getTextTrim(XMLConstants.FILE_NAME);
		String parameterId = metaDataFileElement.getTextTrim(XMLConstants.FILE_PARAMETER_ID);
		String value = metaDataFileElement.getTextTrim(XMLConstants.FILE_VALUE);
		IBReference.Entry entry = reference.getReferenceByName(name, parameterId);
		return entry.createSource(value);
	}
		
			
	
	public void checkElementForReferencesNoteNecessaryModules(XMLElement element,IBExportImportData metadata) throws IOException {
		String nameOfElement = element.getName();
		// is it a module or a page?
		if (XMLConstants.MODULE_STRING.equalsIgnoreCase(nameOfElement) || 
				XMLConstants.PAGE_STRING.equalsIgnoreCase(nameOfElement)) {
			// ask for the class
			String moduleClass = element.getAttributeValue(XMLConstants.CLASS_STRING);
			// special case: pages aren't modules
			if (moduleClass == null) {
				moduleClass = ICPage.class.getName();
			}
			// mark the module as necessary
			metadata.addNecessaryModule(moduleClass);
			if (moduleReference.containsKey(moduleClass)) {
				IBReference reference = (IBReference) moduleReference.get(moduleClass);
				Collection entries = reference.getEntries();
				Iterator iterator = entries.iterator();
				while (iterator.hasNext()) {
					IBReference.Entry entry = (IBReference.Entry) iterator.next();
					entry.addSource(element, metadata);
				}
			}
		}
		List children = element.getChildren();
		if (children != null) {
			Iterator childrenIterator = children.iterator();
			while (childrenIterator.hasNext()) {
				XMLElement childElement = (XMLElement) childrenIterator.next();
				checkElementForReferencesNoteNecessaryModules(childElement, metadata);
			}
		}
	}
	

}
