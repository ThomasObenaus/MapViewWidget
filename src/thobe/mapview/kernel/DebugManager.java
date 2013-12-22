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
	private static boolean	MI_DEBUG;
	private static boolean	MI_DRAW_VIEWPORTS;

	static
	{
		MI_DEBUG = true;
		MI_DRAW_VIEWPORTS = true;
	}

	public static boolean isMapImageDebug( )
	{
		return MI_DEBUG;
	}

	public static boolean isMapImageDrawViewPorts( )
	{
		return MI_DRAW_VIEWPORTS;
	}
}
