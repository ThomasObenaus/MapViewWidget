/*
 *  Copyright (C) 2013, Thomas Obenaus. All rights reserved.
 *  Licensed under the New BSD License (3-clause lic)
 *  See attached license-file.
 *
 *	Author: 	Thomas Obenaus
 *	EMail:		obenaus.thomas@gmail.com
 *  Project:    MapViewWidget
 */
package thobe.mapview.kernel.tilesystem;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Class representing a {@link Tile} at a certain zoom-levels, having a {@link TileNumber}, an {@link Image}.
 * @author Thomas Obenaus
 */
public class Tile implements Cloneable
{
	/**
	 * Number of pixels for one tile
	 */
	public static final int		TILE_SIZE_PX		= 256;

	public static final int		HALF_TILE_SIZE_PX	= TILE_SIZE_PX / 2;

	public static final String	TILE_ID_DELIMITER	= ",";

	private static Image		defaultImg;

	/**
	 * create the default image (LIGHT_GRAY)
	 */
	static
	{
		defaultImg = new BufferedImage( TILE_SIZE_PX, TILE_SIZE_PX, BufferedImage.TYPE_INT_ARGB );
		Graphics2D gr = ( ( BufferedImage ) defaultImg ).createGraphics( );
		gr.setColor( Color.LIGHT_GRAY );
		gr.fillRect( 0, 0, TILE_SIZE_PX, TILE_SIZE_PX );
	}

	/**
	 * The image representing this {@link Tile}
	 */
	private Image				image;

	/**
	 * The center of this {@link Tile}
	 */
	private GeoCoord			center;

	/**
	 * The zoom-level
	 */
	private int					zoomLevel;

	/**
	 * Position (X) within the MapImage.
	 */
	private int					y;

	/**
	 * Position (Y) within the MapImage.
	 */
	private int					x;

	private String				tileId;

	private int					column;
	private int					row;

	private boolean				valid;
	private Rectangle2D			bounds;

	public Tile( String id, int x, int y )
	{
		this.valid = false;
		this.column = tileIdToColumn( id );
		this.row = tileIdToRow( id );
		this.x = x;
		this.y = y;
		this.bounds = new Rectangle2D.Double( this.x, this.y, Tile.TILE_SIZE_PX, Tile.TILE_SIZE_PX );
		this.tileId = id;

		this.image = null;
		this.center = new GeoCoord( );
		this.zoomLevel = 12;
	}

	public static String colRowToTileId( int column, int row )
	{
		return column + TILE_ID_DELIMITER + row;
	}

	public static int tileIdToRow( String tileId )
	{
		String[] colAndRow = tileId.split( TILE_ID_DELIMITER );
		return Integer.parseInt( colAndRow[1] );
	}

	public static int tileIdToColumn( String tileId )
	{
		String[] colAndRow = tileId.split( TILE_ID_DELIMITER );
		return Integer.parseInt( colAndRow[0] );
	}

	public void setValid( boolean valid )
	{
		this.valid = valid;
	}

	public boolean isValid( )
	{
		return valid;
	}

	public String getTileId( )
	{
		return tileId;
	}

	public void setCenter( GeoCoord center )
	{
		this.center = center;
	}

	public void setZoomLevel( int zoomLevel )
	{
		this.zoomLevel = zoomLevel;
	}

	public int getColumn( )
	{
		return column;
	}

	public int getRow( )
	{
		return row;
	}

	public int getX( )
	{
		return x;
	}

	public int getY( )
	{
		return y;
	}

	public GeoCoord getCenter( )
	{
		return center;
	}

	public synchronized void setImage( Image image )
	{
		this.image = image;
	}

	public int getHeightPx( )
	{
		return TILE_SIZE_PX;
	}

	public int getWidthPx( )
	{
		return TILE_SIZE_PX;
	}

	public int getZoomLevel( )
	{
		return zoomLevel;
	}

	public Rectangle2D getBounds( )
	{
		return bounds;
	}

	/**
	 * Returns an {@link Image} representing this {@link Tile} ( {@link GeoCoord} at agiven zoom-level ) or a default image if none is
	 * available.
	 * @return
	 */
	public synchronized Image getImage( )
	{
		if ( this.image == null )
			return defaultImg;
		return image;
	}

	@Override
	public String toString( )
	{
		return "[" + this.tileId + "|" + x + "," + y + "|" + this.center.getFormatted( ) + "]";
	}

	public Object clone( )
	{
		Tile tileClone = null;
		try
		{
			tileClone = ( Tile ) super.clone( );
		}
		catch ( CloneNotSupportedException e )
		{}
		return tileClone;
	}
}
