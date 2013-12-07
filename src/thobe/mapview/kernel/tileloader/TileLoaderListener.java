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

import java.awt.Image;

/**
 * @author Thomas Obenaus
 * @source TileLoaderListener.java
 * @date Nov 30, 2013
 */
public interface TileLoaderListener
{
	enum FailReason
	{
		CANCELLED, ERROR;
	};

	public void onTileLoadRequestComplete( int tileId, Image image );

	public void onTileLoadRequestStarted( int tileId );

	public void onTileLoadRequestFailed( int tileId, FailReason reason, String cause );

}
