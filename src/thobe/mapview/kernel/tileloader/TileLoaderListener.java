/*
 * Copyright (C) 2013 ThObe. All rights reserved. Author: Thomas Obenaus EMail: thobepro@gmail.com Project: MapView
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
