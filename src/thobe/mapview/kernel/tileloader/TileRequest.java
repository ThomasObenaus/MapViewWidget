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
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import thobe.mapview.kernel.mapprovider.MapProvider;
import thobe.mapview.kernel.mapprovider.MapURLBuilder;
import thobe.mapview.kernel.tilesystem.Tile;
import thobe.mapview.kernel.tilesystem.TileNumber;

/**
 * Class representing a {@link Runnable} that is able to load a {@link Tile} (a static map image) using a specific {@link MapProvider}.
 * @author Thomas Obenaus
 */
public class TileRequest implements Runnable
{
	private static final int	READ_TIMEOUT	= 1500;
	private final int			MAX_RETRIES		= 2;

	private Logger				logger;
	private MapURLBuilder		urlBuilder;
	private String				tileId;
	private Image				image;
	private String				error;
	private boolean				terminated;
	private TileNumber			tileNumber;

	public TileRequest( Logger logger, MapURLBuilder urlBuilder, String tileId, TileNumber tileNumber )
	{
		this.tileNumber = tileNumber;
		this.error = null;
		this.image = null;
		this.logger = logger;
		this.urlBuilder = urlBuilder;
		this.tileId = tileId;
		this.terminated = false;
	}

	@Override
	public void run( )
	{
		String errorMsg = null;
		Image tileImage = null;
		boolean completed = false;
		int retries = 0;

		while ( !completed )
		{
			try
			{
				if ( this.tileNumber == null )
					throw new IllegalArgumentException( "Center of tile is null." );
				if ( this.urlBuilder == null )
					throw new IllegalArgumentException( "UrlBuilder is null." );

				this.logger.info( "Loading " + logPrefix( this.tileId ) + "(tileNumber=" + this.tileNumber + ", center=" + this.tileNumber.getCenter( ).getFormatted( ) + ", size=" + Tile.TILE_SIZE_PX + "x" + Tile.TILE_SIZE_PX + ", zoom=" + this.tileNumber.getZoom( ) + ")" );

				URL url = this.urlBuilder.buildURL( this.tileNumber.getCenter( ), this.tileNumber.getZoom( ), Tile.TILE_SIZE_PX, Tile.TILE_SIZE_PX );

				this.logger.info( logPrefix( this.tileId ) + " Connecting to: " + url + "..." );
				URLConnection con = url.openConnection( );
				con.setReadTimeout( READ_TIMEOUT );
				con.setConnectTimeout( READ_TIMEOUT );

				// check content length
				if ( con.getContentLengthLong( ) == 0 )
					throw new IllegalArgumentException( "Loaded image is empty." );

				// read the image
				BufferedInputStream in = new BufferedInputStream( con.getInputStream( ) );
				tileImage = ImageIO.read( in );
				in.close( );
				completed = true;
			}
			catch ( IllegalArgumentException | IOException e )
			{
				if ( retries >= MAX_RETRIES )
				{
					errorMsg = " " + e.getClass( ).getSimpleName( ) + ": " + e.getLocalizedMessage( );
					completed = true;
					this.logger.warning( " " + e.getClass( ).getSimpleName( ) + ": " + e.getLocalizedMessage( ) );
				}
				this.logger.fine( " " + e.getClass( ).getSimpleName( ) + ": " + e.getLocalizedMessage( ) );
				retries++;
			}

		}

		synchronized ( this )
		{
			this.error = errorMsg;
			this.terminated = true;
			this.image = tileImage;
			this.logger.fine( "Loading " + logPrefix( this.tileId ) + " done" );
		}
	}

	public synchronized boolean isTerminated( )
	{
		return terminated;
	}

	public String getTileId( )
	{
		return tileId;
	}

	public synchronized boolean isFailed( )
	{
		return this.error != null;
	}

	public synchronized String getError( )
	{
		return error;
	}

	public synchronized Image getImage( )
	{
		return image;
	}

	private static String logPrefix( String tileId )
	{
		return "Tile [" + tileId + "]";
	}
}
