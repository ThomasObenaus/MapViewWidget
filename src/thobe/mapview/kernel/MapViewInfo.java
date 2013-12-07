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
 * @source MapViewInfo.java
 * @date Oct 6, 2013
 */
public class MapViewInfo
{
	private static String	LIB_NAME		= "thobe.MapView";

	private static Integer	MAYOR_VERSION	= 0;
	private static Integer	MINOR_VERSION	= 2;
	private static Integer	PATCH_LEVEL	= 0;
	private static Integer	BUILD_NUMBER	= 1;

	public static String getLibName( )
	{
		return LIB_NAME;
	}

	public static String getLibVersion( )
	{
		return MAYOR_VERSION + "." + MINOR_VERSION + "." + PATCH_LEVEL + "-" + BUILD_NUMBER;
	}
}
