/*
 * $Id: IBXMLReader.java,v 1.12 2009/05/26 15:53:05 valdas Exp $
 *
 * Copyright (C) 2001 Idega hf. All Rights Reserved.
 *
 * This software is the proprietary information of Idega hf.
 * Use is subject to license terms.
 *
 */
package com.idega.builder.business;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.faces.component.UIComponent;

import com.idega.builder.dynamicpagetrigger.util.DPTCrawlable;
import com.idega.builder.dynamicpagetrigger.util.DPTCrawlableContainer;
import com.idega.builder.tag.BuilderPage;
import com.idega.core.component.business.ComponentRegistry;
import com.idega.core.component.business.ICObjectBusiness;
import com.idega.core.component.business.ICObjectComponentInfo;
import com.idega.core.component.data.ICObject;
import com.idega.core.component.data.ICObjectBMPBean;
import com.idega.core.component.data.ICObjectHome;
import com.idega.core.component.data.ICObjectInstance;
import com.idega.core.component.data.ICObjectInstanceHome;
import com.idega.data.IDOCreateException;
import com.idega.data.IDOLookup;
import com.idega.data.IDOLookupException;
import com.idega.event.ObjectInstanceCacher;
import com.idega.idegaweb.IWMainApplication;
import com.idega.presentation.Page;
import com.idega.presentation.PresentationObject;
import com.idega.presentation.PresentationObjectContainer;
import com.idega.presentation.Table;
import com.idega.repository.data.RefactorClassRegistry;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.reflect.PropertyCache;
import com.idega.xml.XMLAttribute;
import com.idega.xml.XMLElement;
import com.idega.xml.XMLException;

/**
 * <p>
 * This is the main class for parsing the 'IBXML' document format in the Builder.
 * <p>
 * @author <a href="tryggvi@idega.is">Tryggvi Larusson</a>,
 * <a href="palli@idega.is">Pall Helgason</a>
 * @version 1.0
 */
public class IBXMLReader {
	
	protected static final String ID_PREFIX = "id";

	public static final Logger logger = Logger.getLogger(IBXMLReader.class.getName());
	
	public static final String UUID_PREFIX = ICObjectBusiness.UUID_PREFIX;
	
	/**
	 *<p>
	 *Constructor only used by BuilderLogic
	 *</p>
	 */
	IBXMLReader() {
	}

	/**
	 *
	 */
	private void setAllBuilderControls(UIComponent parent, boolean setTo) {
		//List list = parent.getChildren();
		//if (list != null) {
			//Iterator it = list.iterator();
			Iterator it = parent.getFacetsAndChildren();
			while (it.hasNext()) {
				try{
					UIComponent obj = (UIComponent) it.next();
					if(obj instanceof PresentationObject){
						((PresentationObject)obj).setUseBuilderObjectControl(setTo);
						((PresentationObject)obj).setBelongsToParent(true);
						//if (obj instanceof PresentationObjectContainer) {
						//	setAllBuilderControls((PresentationObjectContainer) obj, setTo);
						//}
						setAllBuilderControls(obj, setTo);
					}
				}
				catch(ClassCastException cce){
					cce.printStackTrace();
				}
			}
		//}
	}

	/**
	 *
	 */
	Page getPopulatedPage(IBXMLPage ibxml) {
		Page parentContainer = null;
		String pageKey = null;
		XMLElement root = ibxml.getRootElement();
		if (root == null) {
			System.err.println("IBXML Root element is null");
			return null;
		}
		XMLElement pageXML = ibxml.getPageElement(root);
		List pageAttr = pageXML.getAttributes();
		Iterator attr = pageAttr.iterator();

		boolean hasTemplate = false;
		boolean isTemplate = false;
		boolean isLocked = true;
		String dptRootPage = null;

		// Parse the page attributes
		while (attr.hasNext()) {
			XMLAttribute at = (XMLAttribute) attr.next();
			if (at.getName().equalsIgnoreCase(IBXMLConstants.TEMPLATE_STRING)) {
				hasTemplate = true;
				String pageId = at.getValue();
				//parentContainer = getBuilderLogic().getPageCacher().getPage(at.getValue());
				parentContainer = getBuilderLogic().getPageCacher().getComponentBasedPage(pageId).getNewPageCloned();
				parentContainer.setIsExtendingTemplate();
				parentContainer.setTemplateId(at.getValue());
				setAllBuilderControls(parentContainer, false);
			}
			else if (at.getName().equalsIgnoreCase(IBXMLConstants.PAGE_TYPE)) {
				String value = at.getValue();
				if (value.equals(IBXMLConstants.PAGE_TYPE_TEMPLATE) || value.equals(IBXMLConstants.PAGE_TYPE_DPT_TEMPLATE)) {
					isTemplate = true;
				}
			}
			else if (at.getName().equalsIgnoreCase(IBXMLConstants.ID_STRING)) {
				pageKey = at.getValue();
			}
			else if (at.getName().equalsIgnoreCase(IBXMLConstants.REGION_LOCKED)) {
				if (at.getValue().equals("true")) {
					isLocked = true;
				}
				else {
					isLocked = false;
				}
			} 
			else if (at.getName().equalsIgnoreCase(IBXMLConstants.DPT_ROOTPAGE_STRING)) {
				dptRootPage = at.getValue();
			}
		}

		//If the page does not extend a template it has no parent container
		if (!hasTemplate) {
			parentContainer = new BuilderPage();
		}
		else {
			setTemplateObjectsForPage(ibxml);
		}

		if (isLocked) {
			parentContainer.lock();
		}
		else {
			parentContainer.unlock();
		}

		//Set the type of the page
		if (isTemplate) {
			parentContainer.setIsTemplate();
			ibxml.setType(IBXMLConstants.PAGE_TYPE_TEMPLATE);
		}
		else {
			parentContainer.setIsPage();
			ibxml.setType(IBXMLConstants.PAGE_TYPE_PAGE);
		}

		//sets the id of the page
		try {
			int id = Integer.parseInt(pageKey);
			parentContainer.setPageID(id);
		}
		catch (NumberFormatException e) {
			try {
				parentContainer.setPageID(Integer.parseInt(ibxml.getPageKey()));
			}
			catch (NumberFormatException ex) {
				//      System.err.println("NumberFormatException - ibxml.getKey():"+ibxml.getKey()+" not Integer");
			}
		}
		
		//sets dptRootpageID
		try {
			if(dptRootPage!=null) {
				parentContainer.getDynamicPageTrigger().setRootPage(dptRootPage);
			}
		}
		catch (NumberFormatException e) {
			e.printStackTrace();
		}
		
		parentContainer.setTitle(ibxml.getName());

		if (pageXML.hasChildren()) {
			List children = pageXML.getChildren();
			Iterator it = children.iterator();

			while (it.hasNext()) {
				XMLElement child = (XMLElement) it.next();
				
				if (child.getName().equalsIgnoreCase(IBXMLConstants.PROPERTY_STRING)) {
					setProperty(child, parentContainer);
				}
				else if (child.getName().equalsIgnoreCase(IBXMLConstants.ELEMENT_STRING) || child.getName().equalsIgnoreCase(IBXMLConstants.MODULE_STRING)) {
					if (!parentContainer.getIsExtendingTemplate()) {
						parseElement(child, parentContainer, ibxml);
					}
					else if (!parentContainer.isLocked()) {
						parseElement(child, parentContainer, ibxml);
					}
				}
				else if (child.getName().equalsIgnoreCase(IBXMLConstants.REGION_STRING)) {
					parseRegion(child, parentContainer, ibxml);
				}
				else if (child.getName().equalsIgnoreCase(IBXMLConstants.CHANGE_PAGE_LINK)) {
					changeDPTCrawlableLinkedPageProperty(child, parentContainer);
				}
				else if (child.getName().equalsIgnoreCase(IBXMLConstants.CHANGE_ROOT_PAGE)) {
					changeDPTCrawlableCollectionLinkedPagesProperties(child, parentContainer);
				}
				else if (child.getName().equals(IBXMLConstants.CHANGE_IC_INSTANCE_ID)) {
					changeInstanceId(child, parentContainer);
				}
				else {
					System.err.println("Unknown tag in xml description file : " + child.getName());
				}
			}
		}

		return (parentContainer);
	}

	/**
	 *
	 */
	void parseRegion(XMLElement reg, PresentationObjectContainer regionParent, CachedBuilderPage ibxml) {
		List regionAttrList = reg.getAttributes();
		UIComponent newRegionParent = regionParent;
		if ((regionAttrList == null) || (regionAttrList.isEmpty())) {
			System.err.println("Table region has no attributes");
			return;
		}

		int x = 1;
		int y = 1;
		boolean isLocked = true;

		XMLAttribute locked = reg.getAttribute(IBXMLConstants.REGION_LOCKED);
		if (locked != null) {
			if (locked.getValue().equalsIgnoreCase("true")) {
				isLocked = true;
			}
			else {
				isLocked = false;
			}
		}

		XMLAttribute label = reg.getAttribute(IBXMLConstants.LABEL_STRING);

		XMLAttribute regionIDattr = reg.getAttribute(IBXMLConstants.ID_STRING);
		String regionID = null;
		if (regionIDattr != null) {
			regionID = regionIDattr.getValue();
			try {
				Integer.parseInt(regionID);
				XMLAttribute regionAttrX = reg.getAttribute(IBXMLConstants.X_REGION_STRING);
				if (regionAttrX != null) {
					try {
						x = regionAttrX.getIntValue();
					}
					catch (XMLException e) {
						System.err.println("Unable to convert x region attribute to integer");
						x = 1;
					}
				}
				XMLAttribute regionAttrY = reg.getAttribute(IBXMLConstants.Y_REGION_STRING);
				if (regionAttrY != null) {
					try {
						y = regionAttrY.getIntValue();
					}
					catch (XMLException e) {
						System.err.println("Unable to convert y region attribute to integer");
						y = 1;
					}
				}
			}
			catch (NumberFormatException e) {
				//Integer.parseInt(regionID.substring(0, regionID.indexOf(".")));
				int indexOfDot = regionID.indexOf(".");
				if(indexOfDot!=-1){
					String theRest = regionID.substring(indexOfDot + 1, regionID.length());
					x = Integer.parseInt(theRest.substring(0, theRest.indexOf(".")));
					y = Integer.parseInt(theRest.substring(theRest.indexOf(".") + 1, theRest.length()));
				}
			}
		}

		boolean parseChildren = true;
		boolean emptyParent = false;
		/*if (regionParent instanceof HtmlPage) {
			HtmlPage hPage = (HtmlPage)regionParent;
			HtmlPageRegion regionContainer = hPage.getRegion(regionID);
			newRegionParent = regionContainer;
			//regionContainer.setRegionId(regionID);
			//hPage.add(newRegionParent,regionID);
		}
		else 
		*/
		if (regionParent instanceof com.idega.presentation.Page) {
			if ((regionID == null) || (regionID.equals(""))) {
				System.err.println("Missing id attribute for region tag");
				return;
			}
			if (((Page) regionParent).getIsExtendingTemplate()) {
				
				newRegionParent = regionParent.getContainedObject(regionID);
					
				if (newRegionParent == null) {
					if (label != null) {
						newRegionParent = regionParent.getContainedLabeledObject(label.getValue());
						if (newRegionParent == null) {
							parseChildren = false;
						}
					}
					else {
						parseChildren = false;
					}
				}
				else {
					if ( ((PresentationObject)newRegionParent).getBelongsToParent() &&  ((PresentationObjectContainer)newRegionParent).isLocked()){
						parseChildren = false;
					}
					else{
						emptyParent = true;
					}
				}
			}
		}
		else if (regionParent instanceof com.idega.presentation.Table) {
			if (isLocked) {
				((Table) regionParent).lock(x, y);
			}
			else {
				((Table) regionParent).unlock(x, y);
			}

			if (label != null) {
				((Table) regionParent).setLabel(label.getValue(), x, y);
			}

			newRegionParent = ((Table) regionParent).containerAt(x, y);
		}

		if (parseChildren) {
			if (reg.hasChildren()) {
				if (emptyParent){
					((PresentationObjectContainer)newRegionParent).empty();
				}
				
				List children = reg.getChildren();
				Iterator childrenIt = children.iterator();

				while (childrenIt.hasNext()) {
					XMLElement element = (XMLElement)childrenIt.next();
					parseElement(element , (PresentationObjectContainer) newRegionParent, ibxml);
				}
			}
		}
	}

	/**
	 *Sets properties from the xml on the object via reflection or getAttributes().put(..) or PresentationObject.setProperty(...)
	 */
	void setProperty(XMLElement property, UIComponent object) {
		String key = null;
		List values = new ArrayList();

		
		//1. First check for <property name="" value="">:
		
		String propertyName = property.getAttributeValue(IBXMLConstants.NAME_STRING);
		String propertyValue = property.getAttributeValue(IBXMLConstants.VALUE_STRING);
		
		if(propertyValue!=null && propertyValue !=null){
			values.add(propertyValue);
			setComponentProperty(object, propertyName, values);
			return;
		}
		
		//2. If this isn't set after this check for children of the <property> element, this is the older way:
		
		List li = property.getChildren();
		Iterator it = li.iterator();

		while (it.hasNext()) {
			XMLElement e = (XMLElement) it.next();

			if (e.getName().equalsIgnoreCase(IBXMLConstants.NAME_STRING)) {
				key = e.getTextTrim();
			}
			else if (e.getName().equalsIgnoreCase(IBXMLConstants.VALUE_STRING)) {
				values.add(e.getTextTrim());
			}
		}

		if (key != null) {
			//key is MethodIdentifier
			if (key.startsWith(IBXMLConstants.METHOD_STRING)) {
				try {
					setReflectionProperty(object, key, values);
				}
				catch(Exception e) {
					e.printStackTrace();	
				}
			}
			else {
				//Backward compatability and possibly good for beanproperties, used by Image,Page and Table at least...
				//NOT into PropertyCache....
				if(object instanceof PresentationObject){
					//depracated stuff, the method in PO does the same as for a UIComponent in the "else part" 
					//but is overridden by ancient classes that do different thing with it
					String[] vals = new String[values.size()];
					for (int i = 0; i < values.size(); i++){
						vals[i] = (String) values.get(i);
					}
					((PresentationObject)object).setProperty(key,vals);
				}
				else{
					//UIComponent
					object.getAttributes().put(key,values);
				}
			}
		}
	}

	/**
	 *
	 */
	void setReflectionProperty(UIComponent instance, String methodIdentifier, List stringValues) {
		ComponentPropertyHandler.getInstance().setReflectionProperty(instance, methodIdentifier, stringValues);
	}
	
	void setComponentProperty(UIComponent instance, String componentProperty, List stringValues) {
		ComponentPropertyHandler.getInstance().setComponentProperty(instance, componentProperty, stringValues);
	}

	/**
	 *
	 */
	public UIComponent parseElement(XMLElement el, UIComponent parent, CachedBuilderPage ibxml) {
	
		UIComponent firstUICInstance = null;
		
		List at = el.getAttributes();
		boolean isLocked = true;

		if ((at == null) || (at.isEmpty())) {
			System.err.println("No attributes specified");
			return null;
		}
		String className = null;
		//String icObjectInstanceId = null;
		String componentId = null;
		String icObjectId = null;
		ICObjectInstance icObjectInstance = null;
		String label = null;
		
		Iterator it = at.iterator();
		
		//get the attributes for the module tag
		while (it.hasNext()) {
			XMLAttribute attr = (XMLAttribute) it.next();
			if (attr.getName().equalsIgnoreCase(IBXMLConstants.CLASS_STRING)) {
				className = attr.getValue();
			}
			else if (attr.getName().equalsIgnoreCase(IBXMLConstants.ID_STRING)) {
				//icObjectInstanceId = attr.getValue();
				componentId=attr.getValue();
			}
			else if (attr.getName().equalsIgnoreCase(IBXMLConstants.IC_OBJECT_ID_STRING)) {
				icObjectId = attr.getValue();
			}
			else if (attr.getName().equalsIgnoreCase(IBXMLConstants.REGION_LOCKED)) {
				if (attr.getValue().equals("false")) {
					isLocked = false;
				}
				else {
					isLocked = true;
				}
			}
			else if (attr.getName().equalsIgnoreCase(IBXMLConstants.LABEL_STRING)) {
				label = attr.getValue();
			}
		}

		
		try {
			//first create an instance
			//try to do it first by the classname (definately an UIComponent and maybe a PresentationObject)
			if (className != null) {
				makeSureObjectExists(className);
				
				if(componentId!=null){
					String pageKey = "";
					try{
						pageKey = ibxml.getPageKey();
						icObjectInstance = getICObjectInstanceFromComponentId(componentId,className,pageKey);
						
						ICObject icObject = icObjectInstance.getObject();
						if (icObject != null) {
							Class<? extends UIComponent> objectClass = icObject.getObjectClass();
							firstUICInstance = objectClass.newInstance();
						}
					}
					catch(Exception e){
						System.err.println("[IBXMLReader] " + e.getMessage() + ": pageKey=" + pageKey + ";icObjectInstanceID=" + icObjectInstance);
					}
				}
				
				//finally try to instanciate just from class:
				if(firstUICInstance==null){
					try{
						firstUICInstance = (UIComponent) RefactorClassRegistry.forName(className).newInstance();
					}
					catch (Exception e) {
						e.printStackTrace(System.err);
						throw new Exception("Invalid Class tag for module: '"+className+"'");
					}
				}
			}
			//else if(icObjectInstanceId!=null){
			//else if
			
			if(firstUICInstance!=null){
				setInstanceId(ibxml, firstUICInstance, componentId, icObjectId, icObjectInstance);	
				
				
				//TODO JSF Compat IS this necesery?
				//This is a hack to refresh the property cache so that we don't get old properties.
				//(this is used in JSF state restoring for PresentationObjects)
				String objectCacheKey = BuilderLogic.getInstance().getInstanceId(firstUICInstance);
				PropertyCache.getInstance().clearPropertiesForKey(objectCacheKey);
			//////
			}
			
			//TODO are there any similar UIComponent containers we need to check for?
			if (firstUICInstance instanceof PresentationObjectContainer) {
				if (isLocked) {
					((PresentationObjectContainer) firstUICInstance).lock();
				}
				else {
					((PresentationObjectContainer) firstUICInstance).unlock();
					if(label!=null){
						((PresentationObjectContainer) firstUICInstance).setLabel(label);
					}
				}
			}

			if (firstUICInstance instanceof com.idega.presentation.Table) {
				com.idega.presentation.Table table = (com.idega.presentation.Table) firstUICInstance;
				if(parent instanceof PresentationObjectContainer){
					((PresentationObjectContainer)parent).add(table);
				}
				else{
					parent.getChildren().add(table);
				}
				if (el.hasChildren()) {
					List children = el.getChildren();
					Iterator itr = children.iterator();

					while (itr.hasNext()) {
						XMLElement child = (XMLElement) itr.next();
						if (child.getName().equalsIgnoreCase(IBXMLConstants.PROPERTY_STRING)) {
							setProperty(child, table);
						}
						else if (child.getName().equalsIgnoreCase(IBXMLConstants.ELEMENT_STRING) || child.getName().equalsIgnoreCase(IBXMLConstants.MODULE_STRING)) {
							parseElement(child, table, ibxml);
						}
						else if (child.getName().equalsIgnoreCase(IBXMLConstants.REGION_STRING)) {
							parseRegion(child, table, ibxml);
						}
						else {
							System.err.println("Unknown tag in xml description file : " + child.getName());
						}
					}
				}
			}
			else {
				//Add the component to its parent
				try {
					if(parent instanceof PresentationObjectContainer){
						((PresentationObjectContainer)parent).add(firstUICInstance);
					}
					else{
						parent.getChildren().add(firstUICInstance);
					}
				}
				catch (Exception e) {
					e.printStackTrace(System.err);
					if (parent != null) {
						System.err.println("ParentID: " + parent.getId());
					}
					if (firstUICInstance != null){
						System.err.println("InstanceID: " + BuilderLogic.getInstance().getInstanceId(firstUICInstance));
					}
				}
				
				//set the properties for it or do the same for its children
				if (el.hasChildren()) {
					List children = el.getChildren();
					Iterator itr = children.iterator();

					while (itr.hasNext()) {
						XMLElement child = (XMLElement) itr.next();
						if (child.getName().equalsIgnoreCase(IBXMLConstants.PROPERTY_STRING)) {
							setProperty(child, firstUICInstance);
						}
						else if (child.getName().equalsIgnoreCase(IBXMLConstants.ELEMENT_STRING) || child.getName().equalsIgnoreCase(IBXMLConstants.MODULE_STRING)) {
							parseElement(child, firstUICInstance, ibxml);
						}
						else if (child.getName().equalsIgnoreCase(IBXMLConstants.REGION_STRING)) {
							parseRegion(child, (PresentationObjectContainer) firstUICInstance, ibxml);
						}
						else {
							System.err.println("Unknown tag in xml description file : " + child.getName());
						}
					}
				}
			}
		}
		catch (ClassNotFoundException e) {
			System.err.println("The specified class can not be found: " + className);
			e.printStackTrace();
		}
		catch (java.lang.IllegalAccessException e2) {
			System.err.println("Illegal access");
			e2.printStackTrace();
		}
		catch (java.lang.InstantiationException e3) {
			System.err.println("Unable to instanciate class: " + className);
			e3.printStackTrace();
		}
		catch (Exception e4) {
			System.err.println("Exception");
			e4.printStackTrace();
		}
		
		
		return firstUICInstance;
	}
	
	private void makeSureObjectExists(String className) {
		ICObject icObject = null;
		try {
			icObject = getICObjectHome().findByClassName(className);
		} catch (FinderException e) {
			logger.log(Level.WARNING, "Object '" + className + "' isn't registered in database, trying to register.");
		}
		if (icObject == null) {
			UIComponent component = null;
			try {
				component = (UIComponent) RefactorClassRegistry.forName(className).newInstance();
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Error while getting instance from class: " + className, e);
				return;
			}
			
			ICObject newICObject = null;
			try {
				newICObject = ICObjectBusiness.getInstance().createICObject();
			} catch (IDOCreateException e) {
				logger.log(Level.SEVERE, "Error while inserting new record in ic_object table", e);
			}
			if (newICObject == null) {
				return;
			}
			
			String name = null;
			String objectType = null;
			String bundleIdentifier = null;
			if (component instanceof PresentationObject) {
				PresentationObject po = (PresentationObject) component;
				
				name = po.getBuilderName(CoreUtil.getIWContext());
				bundleIdentifier = po.getBundleIdentifier();
				objectType = po.getComponentType();
			}
			
			if (name == null) {
				if (className.indexOf(CoreConstants.DOT) != -1) {
					name = className.substring(className.lastIndexOf(CoreConstants.DOT) + 1);
				}
			}
			newICObject.setName(name == null ? className : name);
			newICObject.setClassName(className);
			newICObject.setObjectType(objectType == null ? ICObjectBMPBean.COMPONENT_TYPE_JSFUICOMPONENT : objectType);
			newICObject.setBundleIdentifier(bundleIdentifier == null ? CoreConstants.CORE_IW_BUNDLE_IDENTIFIER : bundleIdentifier);
			newICObject.store();
			
			ComponentRegistry registry = ComponentRegistry.getInstance(IWMainApplication.getDefaultIWMainApplication());
			try {
				registry.registerComponent(new ICObjectComponentInfo(newICObject));
			} catch (ClassNotFoundException e) {
				logger.log(Level.SEVERE, "Error while registering component in ComponentRegistry", e);
			}
			
			logger.log(Level.INFO, "New ic_object for '" + className + "' was created successfully.");
			
			getBuilderLogic().clearAllCaches();
		}
	}

	public int getICObjectInstanceIdFromComponentId(String componentId, String className,String pageKey){
		try {
			return Integer.parseInt(componentId);
		}
		catch (NumberFormatException e) {
			ICObjectInstance instance = getICObjectInstanceFromComponentId(componentId,className,pageKey);
			return instance.getID();
		}
	}
	
	/**
	 * Creates a new ICObjectInstance if none is found for the componentId (UUID) and className
	 * @param componentId The unique id of the object
	 * @param className The class name of the object
	 * @param pageKey The page id or URI of the page the object is in
	 * @return
	 */
	public ICObjectInstance getICObjectInstanceFromComponentId(String componentId, String className, String pageKey) {
		ICObjectInstanceHome icoHome = getICObjectInstanceHome();
		// first try it as a number (old school) then as a uuid/uniquestring
		try {
			int id = Integer.parseInt(componentId);
			ICObjectInstance instance;
			try {
				instance = icoHome.findByPrimaryKey(id);
				return instance;
			}
			catch (FinderException ex) {
				throw new RuntimeException(ex);
			}
		}
		catch (NumberFormatException e) {
			String uniqueId = componentId;
			if (componentId.startsWith(UUID_PREFIX)) {
				uniqueId = componentId.substring(UUID_PREFIX.length(), componentId.length());
			}
			try {
				ICObjectInstance ico = icoHome.findByUniqueId(uniqueId);
				return ico;
			}
			catch (FinderException exe) {
				ICObjectInstance instance;
				try {
					instance = icoHome.create();
					instance.setUniqueId(uniqueId);
					if (pageKey != null) {
						instance.setIBPageByKey(pageKey);
					}
					if(className!=null){
						try {
							ICObject ico = getICObjectHome().findByClassName(className);
							instance.setICObject(ico);
						}
						catch (FinderException e1) {
							e1.printStackTrace();
						}
					}
					
					instance.store();
					return instance;
				}
				catch (CreateException e1) {
					throw new RuntimeException(e1);
				}
			}
		}
	}
	
	/**
	 * <p>
	 * TODO tryggvil describe method getICObjectHome
	 * </p>
	 * 
	 * @return
	 */
	public ICObjectInstanceHome getICObjectInstanceHome() {
		try {
			return (ICObjectInstanceHome) IDOLookup.getHome(ICObjectInstance.class);
		}
		catch (IDOLookupException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * <p>
	 * TODO tryggvil describe method getICObjectHome
	 * </p>
	 * @return
	 */
	public ICObjectHome getICObjectHome() {
		try {
			return (ICObjectHome) IDOLookup.getHome(ICObject.class);
		}
		catch (IDOLookupException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @param ibxml
	 * @param firstUICInstance
	 * @param icObjectInstanceId
	 * @param icObjectId
	 * @param icObjectInstance
	 * @return
	 */
	private void setInstanceId(CachedBuilderPage ibxml, UIComponent firstUICInstance, String componentId, String sIcObjectId, ICObjectInstance icObjectInstance) {
		//quick check to see if this is a legal ID (cannot start with a number)
		if(componentId!=null && Character.isDigit(componentId.charAt(0)) ){
			componentId = ID_PREFIX+componentId;
		}
		
		if(firstUICInstance instanceof PresentationObject){
			PresentationObject presentationObject = (PresentationObject) firstUICInstance;

			int icObjectInstanceId=-1;
			int icObjectId=-1;
			
			
			//presentationObject.setICObjectInstance(icObjectInstance);
			if(icObjectInstance!=null){
				icObjectInstanceId=((Integer)icObjectInstance.getPrimaryKey()).intValue();
			}
			
			if (sIcObjectId == null) {
				//presentationObject.setICObject(icObjectInstance.getObject());
				if(icObjectInstance!=null){
					ICObject ico = icObjectInstance.getObject();
					if(ico!=null){
						icObjectId=((Integer)ico.getPrimaryKey()).intValue();
					}
				}
			}
			else {
				icObjectId=Integer.parseInt(sIcObjectId);
			}
			
			//TODO JSF COMPAT FIND OUT WHAT THIS IS for??
			// added by gummi@idega.is // - cache ObjectInstance
			//NOT SURE BUT I THINK THIS WAS TO NOT STORE THE PAGE IT SELF?, EIKI dec 2008
			if (!"0".equals(componentId)) {
				cacheObjectInstance(ibxml, componentId, presentationObject);
			}
			
			presentationObject.setBuilderIds(componentId, icObjectInstanceId, icObjectId);
		}
		else{
			//set the instance id for a UIComponent
			firstUICInstance.setId(componentId);
		}
	}
	void changeDPTCrawlableCollectionLinkedPagesProperties(XMLElement change, PresentationObjectContainer parent) {
		List regionAttrList = change.getAttributes();
		if ((regionAttrList == null) || (regionAttrList.isEmpty())) {
			System.err.println("Table region has no attributes");
			return;
		}

		XMLAttribute id = change.getAttribute(IBXMLConstants.LINK_ID_STRING);
		XMLAttribute newPageIds = change.getAttribute(IBXMLConstants.LINK_TO);

		int iId = -1;
		try {
			iId = id.getIntValue();
		} catch (XMLException e1) {
			e1.printStackTrace();
		}
		
		List li = parent.getChildrenRecursive();
		if (li != null) {
			Iterator it = li.iterator();
			while (it.hasNext()) {
				PresentationObject obj = (PresentationObject) it.next();
				if (obj instanceof DPTCrawlableContainer) {
					DPTCrawlableContainer l = (DPTCrawlableContainer) obj;
					if (l.getICObjectInstanceID() == iId) {
						try {
							l.setRootId(newPageIds.getIntValue());
						} catch (XMLException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	void changeDPTCrawlableLinkedPageProperty(XMLElement change, PresentationObjectContainer parent) {
		List regionAttrList = change.getAttributes();
		if ((regionAttrList == null) || (regionAttrList.isEmpty())) {
			System.err.println("Table region has no attributes");
			return;
		}

		XMLAttribute id = change.getAttribute(IBXMLConstants.LINK_ID_STRING);
		XMLAttribute newPageLink = change.getAttribute(IBXMLConstants.LINK_TO);

		int intId = -1;
		int intNewPage = -1;
		try {
			intId = id.getIntValue();
			intNewPage = newPageLink.getIntValue();
		}
		catch (com.idega.xml.XMLException e) {
			e.printStackTrace();
		}
		List li = parent.getChildrenRecursive();
		if (li != null) {
			Iterator it = li.iterator();
			while (it.hasNext()) {
				PresentationObject obj = (PresentationObject) it.next();
				if (obj instanceof DPTCrawlable) {
					DPTCrawlable l = (DPTCrawlable) obj;
					if (intId == l.getICObjectInstanceID()) {
						l.setLinkedDPTPageID(intNewPage);
					}
				}
			}
		}
	}

	/**
	 *
	 */
	void changeInstanceId(XMLElement change, Page page) {
		int from = -1, to = -1;
		try {
			from = change.getAttribute(IBXMLConstants.IC_INSTANCE_ID_FROM).getIntValue();
			to = change.getAttribute(IBXMLConstants.IC_INSTANCE_ID_TO).getIntValue();
		}
		catch (XMLException e) {
			e.printStackTrace();
			return;
		}
		
		if (from != -1 && to != -1) {
			List children = page.getChildrenRecursive();
			if (children != null) {
				Iterator it = children.iterator();
				while (it.hasNext()) {
					PresentationObject obj = (PresentationObject) it.next();
					if (obj.getICObjectInstanceID() == from) {
						obj.changeInstanceIdForInheritedObject(to);
						ObjectInstanceCacher.changeObjectInstanceID(page, Integer.toString(from), Integer.toString(to), obj);
						return;
					}
				}
			}
		}
	}
	

	public void setTemplateObjectsForPage(CachedBuilderPage ibxml){
	  cacheObjectInstance(ibxml, null, null);
	}

	public void cacheObjectInstance(CachedBuilderPage ibxml, String instanceKey, PresentationObject objectInstance){
	  if(instanceKey != null){
		ObjectInstanceCacher.putObjectIntanceInCache(instanceKey,objectInstance);
	  }
	  //System.err.println("Cashing objectInstance: "+instanceKey);
	  String pageKey = ibxml.getPageKey();
	  String templatePageKey = ibxml.getTemplateKey();
	  
	  ObjectInstanceCacher.copyInstancesFromPageToPage(templatePageKey,pageKey);

	  //System.err.println("Cashing objectInstance: "+instanceKey+" on page "+ ibxml.getKey()+" extending: "+ibxml.getTemplateId());
	  if(instanceKey != null){
		ObjectInstanceCacher.getObjectInstancesCachedForPage(ibxml.getPageKey()).put(instanceKey,objectInstance);
	  }
	}
	
	protected BuilderLogic getBuilderLogic(){
		return BuilderLogic.getInstance();
	}
	
}