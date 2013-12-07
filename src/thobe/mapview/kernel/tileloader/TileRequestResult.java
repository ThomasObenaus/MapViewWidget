/*
 * Copyright (C) 2013 ThObe. All rights reserved. Author: Thomas Obenaus EMail: thobepro@gmail.com Project: MapView
 */

package thobe.mapview.kernel.tileloader;

import thobe.mapview.kernel.tilesystem.Tile;

/**
 * @author Thomas Obenaus
 * @source TileRequestResult.java
 * @date Sep 22, 2013
 */
public class TileRequestResult
{
	private Tile	tile;
	private String	error;

	private TileRequestResult( Tile tile, String error )
	{
		this.tile = tile;
		this.error = error;
	}

	public TileRequestResult( String error )
	{
		this( null, error );
	}

	public TileRequestResult( Tile tile )
	{
		this( tile, null );
	}

	public boolean isSuccess( )
	{
		return this.error != null;
	}

	public String getError( )
	{
		return error;
	}

	public Tile getTile( )
	{
		return tile;
	}
}
