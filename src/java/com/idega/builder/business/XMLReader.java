/*
 * $Id: XMLReader.java,v 1.7 2001/09/18 17:19:45 palli Exp $
 *
 * Copyright (C) 2001 Idega hf. All Rights Reserved.
 *
 * This software is the proprietary information of Idega hf.
 * Use is subject to license terms.
 *
 */
package com.idega.builder.business;

import com.idega.core.data.ICObjectInstance;
import com.idega.jmodule.object.Page;
import com.idega.jmodule.object.ModuleObjectContainer;
import com.idega.jmodule.object.ModuleObject;
import com.idega.jmodule.object.Table;
import java.util.List;
import java.util.Iterator;
import java.util.Vector;
import org.jdom.Element;
import org.jdom.Attribute;

/**
 * @author <a href="tryggvi@idega.is">Tryggvi Larusson</a>,<a href="palli@idega.is">Pall Helgason</a>
 * @version 1.0
 */
public class XMLReader {
  private XMLReader() {
  }

  static Page getPopulatedPage(IBXMLPage ibxml) {
    Page parentContainer = null;
    String pageKey = null;
    Element root = ibxml.getRootElement();
    Element pageXML = root.getChild(XMLConstants.PAGE_STRING);
    List pageAttr = pageXML.getAttributes();
    Iterator attr = pageAttr.iterator();

    boolean hasTemplate = false;

    // Parse the page attributes
    while(attr.hasNext()) {
      Attribute at = (Attribute)attr.next();
      if (at.getName().equalsIgnoreCase(XMLConstants.TEMPLATE_STRING)) {
        hasTemplate = true;
        parentContainer = PageCacher.getPage(at.getValue());
        ((Page)parentContainer).setIsTemplate();
        //parseXML(at.getValue(),verifyPage,parent);
      }
      else if (at.getName().equalsIgnoreCase(XMLConstants.ID_STRING)) {
        pageKey = (String)at.getValue();
      }
    }

    if (!hasTemplate) {
      parentContainer = new Page();
    }

    if (pageXML.hasChildren()) {
      List children = pageXML.getChildren();
      Iterator it = children.iterator();

      while (it.hasNext()) {
        Element child = (Element)it.next();
        if (child.getName().equalsIgnoreCase(XMLConstants.PROPERTY_STRING)){
          setProperties(child,parentContainer);
        }
        else if (child.getName().equalsIgnoreCase(XMLConstants.ELEMENT_STRING) || child.getName().equalsIgnoreCase(XMLConstants.MODULE_STRING)) {
            if (hasTemplate)
                System.err.println("Using element or module on top level in a page having a template");
            else
                //parseElement(child,parentContainer,null);
                if(parentContainer ==null){
                    System.err.println("pc ==null");
                }

            parseElement(child,parentContainer);
        }
        else if (child.getName().equalsIgnoreCase(XMLConstants.REGION_STRING)) {
            parseRegion(child,parentContainer);
        }
        else {
            System.err.println("Unknown tag in xml description file : " + child.getName());
        }
      }
    }

    return(parentContainer);
  }

    static void parseRegion(Element reg, ModuleObjectContainer regionParent) {
        List regionAttrList = reg.getAttributes();
        ModuleObjectContainer newRegionParent = regionParent;
        if ((regionAttrList == null) || (regionAttrList.isEmpty())) {
            System.err.println("Table region has no attributes");
            return;
        }

        int x = 1;
        int y = 1;

        Attribute regionIDattr = reg.getAttribute(XMLConstants.ID_STRING);
        String regionID=null;
        if(regionIDattr!=null){
          regionID = regionIDattr.getValue();
          try{
            int region_id_int = Integer.parseInt(regionID);
            Attribute regionAttrX = reg.getAttribute(XMLConstants.X_REGION_STRING);
            if(regionAttrX!=null){
              try {
                  x = regionAttrX.getIntValue();
              }
              catch(org.jdom.DataConversionException e) {
                  System.err.println("Unable to convert x region attribute to integer");
                  x = 1;
              }
            }
            Attribute regionAttrY = reg.getAttribute(XMLConstants.Y_REGION_STRING);
            if(regionAttrY!=null){
              try {
                  y = regionAttrY.getIntValue();
              }
              catch(org.jdom.DataConversionException e) {
                  System.err.println("Unable to convert y region attribute to integer");
                  y = 1;
              }
            }

          }
          catch(NumberFormatException e){
              int parentID = Integer.parseInt(regionID.substring(0,regionID.indexOf(".")));
              String theRest = regionID.substring(regionID.indexOf(".")+1,regionID.length());
              x = Integer.parseInt(theRest.substring(0,theRest.indexOf(".")));
              y = Integer.parseInt(theRest.substring(theRest.indexOf(".")+1,theRest.length()));
          }
        }



        if (regionParent instanceof com.idega.jmodule.object.Page) {
            if ((regionID == null) || (regionID.equals(""))) {
                System.err.println("Missing id attribute for region tag");
                return;
            }
            if(((Page)regionParent).getIsTemplate()){
                newRegionParent = (ModuleObjectContainer)regionParent.getContainedObject(regionID);
            }
        }
        if (regionParent instanceof com.idega.jmodule.object.Table) {
            newRegionParent = ((Table)regionParent).containerAt(x,y);
        }

        //Map regions = getRegions(regionParent);
        //if (regions.containsKey(regionId)) {
        //  regionParent = (ModuleObjectContainer)regions.get(regionId);
        //}
        //}
        //else {
        //Region region = new Region(regionId,x,y,true);
        //regions.put(regionId,regionParent);
        //if (regionParent instanceof com.idega.jmodule.object.Table) {
        //  com.idega.jmodule.object.Table tab = (com.idega.jmodule.object.Table)regionParent;
        //  tab.add(region,x,y);
        //}
        //else
        //  regionParent.add(region);
        //}

        if (reg.hasChildren()) {
            List children = reg.getChildren();
            Iterator childrenIt = children.iterator();

            while (childrenIt.hasNext())
                //parseElement((Element)childrenIt.next(),regionParent,regionId);
                parseElement((Element)childrenIt.next(),newRegionParent);
        }
    }


    static void setProperties(Element properties, ModuleObject object) {
        String key = null;
        Vector values = new Vector(1);
        String vals[] = null;

        List li = properties.getChildren();
        Iterator it = li.iterator();

        //Element typeEl = properties.getChild(TYPE_TAG);


        while (it.hasNext()) {
            Element e = (Element)it.next();

            if (e.getName().equalsIgnoreCase(XMLConstants.NAME_STRING)) {
                if (key != null) {
                    vals = new String[values.size()];
                    for (int i = 0; i < values.size(); i++)
                        vals[i] = (String)values.elementAt(i);
                    object.setProperty(key,vals);
                    values.clear();
                }
                key = e.getTextTrim();
            }
            else if (e.getName().equalsIgnoreCase(XMLConstants.VALUE_STRING)) {
                values.addElement(e.getTextTrim());
            }
            else
                System.err.println("Error in setProperties!!!!");
        }

        if (key != null) {
          //key is MethodIdentifier
          if(key.startsWith(XMLConstants.METHOD_STRING)){
            setReflectionProperty(object,key,values);
          }
          else{
            vals = new String[values.size()];
            for (int i = 0; i < values.size(); i++)
                vals[i] = (String)values.elementAt(i);
            object.setProperty(key,vals);
          }
        }
    }

    static void setReflectionProperty(ModuleObject instance,String methodIdentifier,Vector stringValues){
      ComponentPropertyHandler.getInstance().setReflectionProperty(instance,methodIdentifier,stringValues);
    }

    static void parseElement(Element el, ModuleObjectContainer parent) {
        //public static void parseElement(Element el, ModuleObjectContainer parent, String region) {
        ModuleObject inst = null;
        List at = el.getAttributes();

        if ((at == null) || (at.isEmpty())) {
            System.err.println("No attributes specified");
            return;
        }
        String className = null;
        String id = null;
        Iterator it = at.iterator();
        while (it.hasNext()) {
            Attribute attr = (Attribute)it.next();
            if (attr.getName().equalsIgnoreCase(XMLConstants.CLASS_STRING)) {
                className = attr.getValue();
            }
            else if (attr.getName().equalsIgnoreCase(XMLConstants.ID_STRING)) {
                id = attr.getValue();
            }
        }

        try{
            if ( className != null ) {
                inst = (ModuleObject)Class.forName(className).newInstance();
            }
            else {
                ICObjectInstance ico = new ICObjectInstance(Integer.parseInt(id));
                inst = ico.getNewInstance();
                inst.setICObjectInstance(ico);
            }

            if (inst instanceof com.idega.jmodule.object.Table) {
                com.idega.jmodule.object.Table table = (com.idega.jmodule.object.Table)inst;
                parent.add(table);

                if (el.hasChildren()) {
                    List children = el.getChildren();
                    Iterator itr = children.iterator();

                    while (itr.hasNext()) {
                        Element child = (Element)itr.next();
                        if (child.getName().equalsIgnoreCase(XMLConstants.PROPERTY_STRING)) {
                            setProperties(child,table);
                        }
                        else if (child.getName().equalsIgnoreCase(XMLConstants.ELEMENT_STRING) || child.getName().equalsIgnoreCase(XMLConstants.MODULE_STRING)) {
                            //parseElement(child,table,null);
                            parseElement(child,table);
                        }
                        else if (child.getName().equalsIgnoreCase(XMLConstants.REGION_STRING)) {
                            parseRegion(child,table);
                        }
                        else
                            System.err.println("Unknown tag in xml description file : " + child.getName());
                    }
                }
                /*if (region != null) {
                 *               Region reg = new Region(region);
                 *
                 *               if (parent instanceof com.idega.jmodule.object.Table) {
                 *                 com.idega.jmodule.object.Table tableParent = (com.idega.jmodule.object.Table)parent;
                 *                 int ind[] = tableParent.getTableIndex(reg);
                 *                 if (ind != null) {
                 *                   tableParent.add(table,ind[0],ind[1]);
                 *                 }
                 *               }
                 *               else {
                 *                 int index = parent.getIndex(reg);
                 *                 parent.insertAt(table,index);
                 *               }
                 *             }
                 *             else*/

            }
            else {
                parent.add(inst);
                if (el.hasChildren()) {
                    List children = el.getChildren();
                    Iterator itr = children.iterator();

                    while (itr.hasNext()) {
                        Element child = (Element)itr.next();
                        if (child.getName().equalsIgnoreCase(XMLConstants.PROPERTY_STRING)) {
                            setProperties(child,inst);
                        }
                        else if (child.getName().equalsIgnoreCase(XMLConstants.ELEMENT_STRING) || child.getName().equalsIgnoreCase(XMLConstants.MODULE_STRING)) {
                            //parseElement(child,(ModuleObjectContainer)inst,region);
                            parseElement(child,(ModuleObjectContainer)inst);
                        }
                        else if (child.getName().equalsIgnoreCase(XMLConstants.REGION_STRING)) {
                            parseRegion(child,(ModuleObjectContainer)inst);
                        }
                        else {
                            System.err.println("Unknown tag in xml description file : " + child.getName());
                        }
                    }
                }
                /*if (region != null) {
                 *               Region reg = new Region(region);
                 *
                 *               if (parent instanceof com.idega.jmodule.object.Table) {
                 *                 com.idega.jmodule.object.Table tableParent = (com.idega.jmodule.object.Table)parent;
                 *                 int ind[] = tableParent.getTableIndex(reg);
                 *                 if (ind != null) {
                 *                   tableParent.add(inst,ind[0],ind[1]);
                 *                 }
                 *               }
                 *               else {
                 *                 int index = parent.getIndex(reg);
                 *                 parent.insertAt(inst,index);
                 *               }
                 *             }
                 *             else*/

            }
        }
        catch(ClassNotFoundException e) {
            System.err.println("The specified class can not be found: "+className);
            e.printStackTrace();
        }
        catch(java.lang.IllegalAccessException e2) {
            System.err.println("Illegal access");
            e2.printStackTrace();
        }
        catch(java.lang.InstantiationException e3) {
            System.err.println("Unable to instanciate class: " +className);
            e3.printStackTrace();
        }
        catch(Exception e4) {
            System.err.println("Exception");
            e4.printStackTrace();
        }
        //}
        //else if (attr.getName().equalsIgnoreCase("id")) {
        //  //Eitthva� db d�t
        //}

    }



}

    /*public static IBXMLPage parseXML(int ib_page_id)throws PageDoesNotExist{
        IBPage page = null;
        try{
            page = new IBPage(ib_page_id);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        return parseXML(page);
    }

    public static IBXMLPage parseXML(IBPage page)throws PageDoesNotExist{
        return parseXML(page.getPageValue());
    }

    public static IBXMLPage parseXML(InputStream streamWithPage)throws PageDoesNotExist{
        //return null;
        //Page page = new Page();
        return parseXML(streamWithPage,false);
        //return page;
    }



    public static IBXMLPage parseXML(InputStream pageInputStream, boolean verifyPage)throws PageDoesNotExist{//, ModuleObjectContainer parentContainer) {
        //public void parseXML(String xmlFile, boolean verifyPage, ModuleObjectContainer parent) {

        IBXMLPage xmlDesc=null;

        xmlDesc = new IBXMLPage(verifyPage,pageInputStream);
            //xmlDesc.setXMLPageDescriptionFile(xmlFile);
            //xmlDesc.setXMLPageDescriptionFile(pageInputStream);
        xmlDesc.setPopulatedPage(getPopulatedPage(xmlDesc));
        return xmlDesc;
    }*/