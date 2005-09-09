package com.idega.builder.business;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.idega.core.component.data.ICObject;
import com.idega.exception.IWBundleDoesNotExist;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.presentation.IWContext;
import com.idega.util.IsCollator;

/**
 * Title: Description: Copyright: Copyright (c) 2001 Company:
 * 
 * @author
 * @version 1.0
 */
public class ModuleComparator implements Comparator {

	private Locale locale;
	private IWMainApplication iwma;
	private Map bundles;
	private List failedBundles;

	public ModuleComparator(IWContext iwc) {
		locale = iwc.getCurrentLocale();
		iwma = iwc.getIWMainApplication();
		bundles = new HashMap();
		failedBundles = new ArrayList();
	}

	public int compare(Object o1, Object o2) {
		ICObject obj1 = (ICObject) o1;
		ICObject obj2 = (ICObject) o2;
		String bundleIdentifier1 = obj1.getBundleIdentifier();
		String bundleIdentifier2 = obj2.getBundleIdentifier();
		
		String one = obj1.getName();
		try {
			if (!failedBundles.contains(bundleIdentifier1)) {
				IWBundle bundle1 = (IWBundle) bundles.get(bundleIdentifier1);
				if (bundle1 == null) {
					bundle1 = iwma.getBundle(bundleIdentifier1);
				}
				one = bundle1.getComponentName(obj1.getClassName(), locale);
			}
		}
		catch (IWBundleDoesNotExist iwbne) {
			failedBundles.add(bundleIdentifier1);
			System.err.println("com.idega.builder.business.ModuleComparator: " + iwbne.getLocalizedMessage()
					+ ". Please remove all references in the IC_OBJECT table");
		}
		String two = obj2.getName();
		try {
			if (!failedBundles.contains(bundleIdentifier2)) {
				IWBundle bundle2 = (IWBundle) bundles.get(bundleIdentifier2);
				if (bundle2 == null) {
					bundle2 = iwma.getBundle(bundleIdentifier2);
				}
				one = bundle2.getComponentName(obj2.getClassName(), locale);
			}
		}
		catch (IWBundleDoesNotExist iwbne) {
			failedBundles.add(bundleIdentifier2);
			System.err.println("com.idega.builder.business.ModuleComparator: " + iwbne.getLocalizedMessage()
					+ ". Please remove all references in the IC_OBJECT table");
		}
		if (one == null) {
			one = obj1.getName();
		}
		if (two == null) {
			two = obj2.getName();
		}
		int result = IsCollator.getIsCollator().compare(one, two);
		return result;
	}

	public boolean equals(Object obj) {
		if (compare(this, obj) == 0)
			return (true);
		else
			return (false);
	}

	
	/**
	 * @return Returns the bundles.
	 */
	public Map getBundles() {
		return bundles;
	}

	
	/**
	 * @param bundles The bundles to set.
	 */
	public void setBundles(Map bundles) {
		this.bundles = bundles;
	}

	
	/**
	 * @return Returns the failedBundles.
	 */
	public List getFailedBundles() {
		return failedBundles;
	}

	
	/**
	 * @param failedBundles The failedBundles to set.
	 */
	public void setFailedBundles(List failedBundles) {
		this.failedBundles = failedBundles;
	}
}
