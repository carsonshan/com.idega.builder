/**
 * 
 */
package com.idega.builder.business;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.logging.Logger;

import com.idega.builder.facelets.FaceletsUtil;
import com.idega.core.view.ViewNodeBase;
import com.idega.idegaweb.IWMainApplication;
import com.idega.util.CoreConstants;

/**
 * <p>
 * This is an implementation for a a "Facelet" based Builder page that is rendered through JSF.<br/>
 * This means that the page is based on a Facelet page and the rendering is dispatched to the 
 * Facelets view handler for processing the rendering.
 * </p>
 *  Last modified: $Date: 2009/01/15 08:50:01 $ by $Author: tryggvil $
 * 
 * @author <a href="mailto:tryggvil@idega.com">Tryggvi Larusson</a>
 * @version $Revision: 1.2 $
 */
public class FaceletPage extends CachedBuilderPage {
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5008461273187175725L;

	public String getURIWithContextPath() {
		/*String parentUri = getParent().getURI();
		String pageUri = getPageUri();
		
		String newUri = parentUri+pageUri;
		return newUri;*/
		return super.getURIWithContextPath();
		
	}
	private static Logger log = Logger.getLogger(IBXML2FaceletPage.class.getName());
	
		private boolean isLoadedToDisk=false;
		private static File diskTempDir;
		private String resourceURI;
	
		public FaceletPage(String pageId){
			super(pageId);
		}
		
		protected void readPageStream(InputStream pageStream){
			File jspTmpDirectory = getDiskTempDirectory();
			File jspFile = new File(jspTmpDirectory,getFileNameOnDisk());
			streamToFile(pageStream,jspFile);
			setLoadedToDisk(true);
		}
		
		private String getFileNameOnDisk(){
			StringBuffer buffer = new StringBuffer(FaceletsUtil.BUILDERPAGE_PREFIX);
			buffer.append(getPageKey()).append(FaceletsUtil.FACELET_PAGE_EXTENSION_WITH_DOT);
			return buffer.toString();
		}
		
		private String getDiskFilesFolderName(){
			return FaceletsUtil.PAGES_DEFAULT_FOLDER;
		}

		private File getDiskTempDirectory(){
			if(diskTempDir==null){
				IWMainApplication iwma = this.getIWMainApplication();
				String appRealPath = iwma.getApplicationRealPath();
				File appRealDir = new File(appRealPath);
				diskTempDir = new File(appRealDir,getDiskFilesFolderName());
				if(!diskTempDir.exists()){
					diskTempDir.mkdir();
				}
			}
			return diskTempDir;
		}
		
		private void streamToFile(InputStream pageStream,File jspFile){
			try {
				if(!jspFile.exists()){
					jspFile.createNewFile();
				}
				//OutputStream jspStream = new FileOutputStream(jspFile);
				/*
				log.info("Writing to JSP page: "+jspFile.toString());
				
				//ICPage icPage = getICPage();
				//InputStream pageStream = icPage.getPageValue();
				
				int bufferLen=1000;
				byte[] buffer = new byte[bufferLen]; 
				int read = pageStream.read(buffer);
				while(read!=-1){
					jspStream.write(buffer);
					read = pageStream.read(buffer);
				}
				pageStream.close();
				jspStream.close();*/
				
				log.finer("Streaming builder page with uri: "+getURIWithContextPath()+" to disk in file: "+jspFile.toURL().toString());
				
				InputStreamReader reader = new InputStreamReader(pageStream,CoreConstants.ENCODING_UTF8);//,encoding);
				int bufferlength=1000;
				char[] buf = new char[bufferlength];
				StringBuffer sbuffer = new StringBuffer();			
				int read = reader.read(buf);
				while(read!=-1){
					sbuffer.append(buf,0,read);
					read = reader.read(buf);
				}
				String xml = sbuffer.toString();
				this.setSourceFromString(xml);
				
				reader.close();
				FileWriter fw = new FileWriter(jspFile);
				PrintWriter out = new PrintWriter(fw);
				out.write(xml);
				out.close();				
				fw.close();
				//jspStream.write();
				
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		/**
		 * @return Returns the isLoadedToDisk.
		 */
		public boolean isLoadedToDisk() {
			return this.isLoadedToDisk;
		}
		/**
		 * @param isLoadedToDisk The isLoadedToDisk to set.
		 */
		public void setLoadedToDisk(boolean isLoadedToDisk) {
			this.isLoadedToDisk = isLoadedToDisk;
		}
		
		public String getResourceURI(){
			if(this.resourceURI==null){
				this.resourceURI="/"+getDiskFilesFolderName()+"/"+getFileNameOnDisk();
			}
			return this.resourceURI;
		}
		
		public void initializeEmptyPage(){
			
			String templateKey = this.getTemplateKey();
			String templateReference = "";
			if(templateKey!=null){
				if (!templateKey.equals("-1")){
					templateReference="template=\""+FaceletsUtil.getRewrittenTemplateReference(this, templateKey)+"\"";
				}
			}
			String source = "<?xml version=\"1.0\"?>\n<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:ui=\"http://java.sun.com/jsf/facelets\" <ui:composition "+templateReference+">\n</html>";
			try {
				setSourceFromString(source);
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		/**
		 * The ViewNodeBase is here overrided to plug in the the main ViewHandler
		 * to dispatch the JSF rendering to the Facelets ViewHandler.
		 */
		public ViewNodeBase getViewNodeBase(){
			return ViewNodeBase.FACELET;
		}

		
		/*public XMLElement getPageRootElement() {
			//return getRootElement();
			return super.getRootElement();
		}
		
		
		@Override
		public void setPageAsEmptyPage(String type, String template) {
			//XMLElement _rootElement = new XMLElement(IBXMLConstants.ROOT_STRING);
			//setRootElement(_rootElement);
			XMLElement pageElement = new XMLElement(IBXMLConstants.PAGE_STRING,BUILDER_NAMESPACE);
			setRootElement(pageElement);
			if (type == null) {
				type = IBXMLConstants.PAGE_TYPE_PAGE;
			}

			if ((type.equals(TYPE_DRAFT)) || (type.equals(TYPE_PAGE)) || (type.equals(TYPE_TEMPLATE)) || (type.equals(TYPE_DPT_TEMPLATE)) || (type.equals(TYPE_DPT_PAGE))) {
				pageElement.setAttribute(IBXMLConstants.PAGE_TYPE, type);
				setType(type);
			}
			else {
				pageElement.setAttribute(IBXMLConstants.PAGE_TYPE, TYPE_PAGE);
				setType(type);
			}

			if (template != null) {
				pageElement.setAttribute(IBXMLConstants.TEMPLATE_STRING, template);
			}

			this.setXMLDocument(new XMLDocument(pageElement));
			
			//_rootElement.addContent(pageElement);
			setPopulatedPage(getBuilderLogic().getIBXMLReader().getPopulatedPage(this));
		}*/

}
