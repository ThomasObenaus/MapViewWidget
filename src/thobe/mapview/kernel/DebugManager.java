/*
 *  Copyright (C) 2013, Thomas Obenaus. All rights reserved.
 *  Licensed under the New BSD License (3-clause lic)
 *  See attached license-file.
 *
 *	Author: 	Thomas Obenaus
 *	EMail:		obenaus.thomas@gmail.com
 *  Project:    MapViewWidget
 */

package thobe.mapview.kernel;

/**
 * @author Thomas Obenaus
 * @source DebugManager.java
 * @date Dec 22, 2013
 */
public class DebugManager
{
	private static boolean DEBUG_MAP_IMAGE;
	
	
	static
	{
		DEBUG_MAP_IMAGE = true;	
	}
	
	
	public static boolean isDebugMapImage( )
	{
		return DEBUG_MAP_IMAGE;
	}
}


