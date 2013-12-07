/*
 *  Copyright (C) 2013, Thomas Obenaus. All rights reserved.
 *  Licensed under the New BSD License (3-clause lic)
 *  See attached license-file.
 *
 *	Author: 	Thomas Obenaus
 *	EMail:		obenaus.thomas@gmail.com
 *  Project:    MapViewWidget
 */
package thobe.mapview.kernel.tileloader;

/**
 * @author Thomas Obenaus
 * @source TileLoaderException.java
 * @date Nov 30, 2013
 */
@SuppressWarnings ( "serial")
public class TileLoaderException extends Exception
{
	public TileLoaderException( String cause )
	{
		super( cause );
	}
}
