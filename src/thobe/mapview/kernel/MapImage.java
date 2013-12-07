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

import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Logger;

import thobe.mapview.kernel.mapprovider.GoogleMapURLBuilder;
import thobe.mapview.kernel.mapprovider.MapProvider;
import thobe.mapview.kernel.mapprovider.MapURLBuilder;
import thobe.mapview.kernel.mapprovider.OSMStaticMapLite;
import thobe.mapview.kernel.tileloader.TileLoader;
import thobe.mapview.kernel.tileloader.TileLoaderListener;
import thobe.mapview.kernel.tileloader.TileRequest;
import thobe.mapview.kernel.tilesystem.GeoCoord;
import thobe.mapview.kernel.tilesystem.MercatorProjection;
import thobe.mapview.kernel.tilesystem.Tile;

/**
 * @author Thomas Obenaus
 * @source MapImage.java
 * @date Nov 24, 2013
 */
@SuppressWarnings ( "serial")
public class MapImage extends Canvas implements TileLoaderListener
{
	private static final Color			DEBUG_COLOR				= Color.RED;
	private static final Stroke			DEBUG_STROKE			= new BasicStroke( 3 );
	private static final Font			DEBUG_FONT				= new Font( "Arial", Font.BOLD, 12 );
	private static final Font			DEBUG_FONT_BIG			= new Font( "Arial", Font.BOLD, 25 );

	public static final int				RENDER_QUALITY_LOW		= 0;
	public static final int				RENDER_QUALITY_HIGH		= 1;
	private BufferStrategy				strategy				= null;
	private int							renderQuality			= RENDER_QUALITY_HIGH;

	/**
	 * Number of tiles used as border
	 */
	private static final int			NUM_BORDER_TILES		= 1;

	/**
	 * Number of pixels which are around the viewport-tiles and will be used to paint the border-tiles.
	 */
	private static final int			BORDER_SIZE				= -Tile.TILE_SIZE_PX;

	private static final int			DEBUG_BORDER_SIZE		= 300;									//BORDER_SIZE;
	private static final int			DEBUG_NUM_BORDER_TILES	= NUM_BORDER_TILES;

	private static boolean				debug;

	private Map<Integer, Tile>			viewPortTiles;

	private MapProvider					mapProvider;
	private GeoCoord					mapCenter;
	private GeoCoord					nextMapCenter;
	GeoCoord							from;
	private int							zoomLevel;
	private Logger						log;
	private MapURLBuilder				urlBuilder;
	private TileLoader					tileLoader;

	private SynchronousQueue<Boolean>	repaintQueue;

	private Repainter					repaintThread;

	/**
	 * For storing the initial state of the camera.
	 */
	private AffineTransform				initialCam;

	protected AffineTransform			camera					= new AffineTransform( );				/* the camera transform that defines the view window into the scene */
	private int							state					= 0;									/* current state: 0->normal, 1->panning, 2->zooming */
	private int							mx, my;														/* mouse coordinates where button was pressed down during a dragging action */
	private AffineTransform				saved_cam;														/* camera transform when the mouse was pressed down */

	private Rectangle2D					viewPort;
	private Rectangle2D					extendedViewPort;
	private Rectangle2D					tileGridBounds;
	private int							tileId;

	public MapImage( int viewPortWidth, int viewPortHeight, GeoCoord mapCenter, int zoomLevel, MapProvider mapProvider, Logger logger )
	{
		this.repaintQueue = new SynchronousQueue<>( );
		this.repaintThread = new Repainter( );
		this.repaintThread.start( );

		this.log = logger;
		this.tileLoader = new TileLoader( this.log, 10 );
		this.tileLoader.addListener( this );
		this.tileLoader.start( );

		this.mapCenter = mapCenter;
		this.nextMapCenter = this.mapCenter;
		this.zoomLevel = zoomLevel;
		this.mapProvider = mapProvider;
		this.viewPortTiles = new HashMap<>( );
		this.tileGridBounds = new Rectangle2D.Double( 0, 0, 0, 0 );
		this.updateURLBuilder( );

		this.setViewPort( viewPortWidth, viewPortHeight );

		this.addMouseWheelListener( new MouseWheelListener( )
		{
			public void mouseWheelMoved( MouseWheelEvent e )
			{

				/* mousewheel --> zoom-mode */
				if ( e.getScrollType( ) == MouseWheelEvent.WHEEL_UNIT_SCROLL )
				{
					AffineTransform tra = new AffineTransform( );
					tra.translate( -e.getX( ), -e.getY( ) );

					camera.preConcatenate( tra );

					float factor = 1 + ( e.getWheelRotation( ) / 10.0f );

					AffineTransform sc = new AffineTransform( );
					sc.scale( factor, factor );
					camera.preConcatenate( sc );

					tra.setToIdentity( );
					tra.translate( e.getX( ), e.getY( ) );
					camera.preConcatenate( tra );
				}
				repaint( );
			}
		} );

		this.addMouseListener( new MouseAdapter( )
		{

			public void mousePressed( MouseEvent e )
			{

				if ( state == 0 )
				{
					/* click right mouse button --> enter pan-mode */
					if ( e.getButton( ) == MouseEvent.BUTTON3 )
					{
						state = 1;
						mx = e.getX( );
						my = e.getY( );
						saved_cam = new AffineTransform( camera );
						from = posToGeoCoord( new Point2D.Double( mx, my ) );
					}

					/* click middle mouse button + press ctrl --> enter zoom-mode */
					if ( ( e.isControlDown( ) ) && ( e.getButton( ) == MouseEvent.BUTTON2 ) )
					{
						state = 2;
						mx = e.getX( );
						my = e.getY( );
						saved_cam = new AffineTransform( camera );
					}
				}
				if ( state != 0 )
					setRenderQuality( RENDER_QUALITY_LOW );

				if ( state != 1 && state != 2 )
				{
					// mouse pressed
				}
			}

			public void mouseReleased( MouseEvent e )
			{
				/* middle mouse button released */
				if ( e.getButton( ) == MouseEvent.BUTTON2 || e.getButton( ) == MouseEvent.BUTTON3 )
				{
					if ( ( state == 1 ) || ( state == 2 ) )
					{
						MapImage.this.mapCenter = nextMapCenter;
						state = 0;
					}
				}

				if ( state == 0 )
					setRenderQuality( RENDER_QUALITY_HIGH );
				repaint( );
			}
		} );

		this.addMouseMotionListener( new MouseMotionAdapter( )
		{
			public void mouseDragged( MouseEvent e )
			{

				/*  if in pan mode, translate according to mouse movement */
				if ( state == 1 )
				{

					double newX = ( e.getX( ) - mx ) / camera.getScaleX( );
					double newY = ( e.getY( ) - my ) / camera.getScaleY( );

					camera = new AffineTransform( saved_cam );
					camera.translate( newX, newY );

					int dX = ( int ) ( mx - newX );
					int dY = ( int ) ( my - newY );
					GeoCoord delta = MercatorProjection.computeDeltaGeoCoord( MapImage.this.mapCenter, MapImage.this.zoomLevel, dX, dY );

					nextMapCenter = MapImage.this.mapCenter.add( delta );
					//System.out.println( MapImage.this.mapCenter + " <" + delta.getFormatted( ) + "> " + nextMapCenter.getFormatted( ) );

					//					if ( ( Math.abs( dX ) > getTileBorderSize( ) ) || Math.abs( dY ) > getTileBorderSize( ) )
					//					{
					//						MapImage.this.mapCenter = nextMapCenter;
					//						updateImage( );
					//					}
					buildImageTiles( );
					repaint( );
				}

				/* in zoom mode, zoom in if mouse moved up and zoom out if mouse moved down with pivot
				 point at the window coordinates the mouse was initially pressed */
				if ( state == 2 )
				{
					camera = new AffineTransform( saved_cam );

					AffineTransform tra = new AffineTransform( );
					tra.translate( -mx, -my );

					camera.preConcatenate( tra );

					float factor = 1.0f + Math.abs( my - e.getY( ) ) / 50.0f;

					if ( e.getY( ) > my )
					{
						factor = 1 / factor;
					}
					AffineTransform sc = new AffineTransform( );
					sc.scale( factor, factor );
					camera.preConcatenate( sc );

					tra.setToIdentity( );
					tra.translate( mx, my );
					camera.preConcatenate( tra );

					repaint( );
				}

				if ( state != 1 && state != 2 )
				{
					repaint( );
				}
			}

			@Override
			public void mouseMoved( MouseEvent e )
			{
				//				System.out.println( posToGeoCoord( e.getPoint( ) ) + " " + e.getPoint( ) );

			}
		} );
	}

	public MapImage( int viewPortWidth, int viewPortHeight, Logger logger )
	{
		this( viewPortWidth, viewPortHeight, new GeoCoord( 51.053631, 13.740810 ), 12, MapProvider.OSMStaticMapLite, logger );
	}

	/**
	 * Converts a given position on the widget to a position on the map-image.
	 * @param pos
	 * @return
	 */
	public Point2D posToViewPortPos( Point2D pos )
	{
		Point2D result = null;
		try
		{
			result = camera.inverseTransform( new Point2D.Double( pos.getX( ) - this.extendedViewPort.getX( ), pos.getY( ) - this.extendedViewPort.getY( ) ), result );
		}
		catch ( NoninvertibleTransformException e )
		{
			e.printStackTrace( );
		}
		return result;
	}

	/**
	 * Converts the given position into a {@link GeoCoord}.
	 * @param position
	 * @return - null if the conversion does fail
	 */
	public GeoCoord posToGeoCoord( Point2D position )
	{
		// convert position to a view-position
		Point2D vpPos = this.posToViewPortPos( position );

		if ( vpPos.getX( ) < 0 || vpPos.getY( ) < 0 )
			return null;

		// find the tile under cursor
		int numTileColumns = this.getNumTileColumns( );
		int column = ( int ) ( vpPos.getX( ) / Tile.TILE_SIZE_PX );
		int row = ( int ) ( vpPos.getY( ) / Tile.TILE_SIZE_PX );
		int tileId = ( row * numTileColumns ) + column;

		// invalid column
		if ( column > ( numTileColumns - 1 ) )
			return null;

		Tile vpTile = this.viewPortTiles.get( tileId );
		if ( vpTile == null )
			return null;

		// compute position on tile
		Point2D tilePos = new Point2D.Double( vpPos.getX( ) - vpTile.getX( ), vpPos.getY( ) - vpTile.getY( ) );
		GeoCoord geoCoord = MercatorProjection.pixelCoordOnImageToGeoCoord( tilePos, vpTile.getCenter( ), Tile.HALF_TILE_SIZE_PX, this.zoomLevel );
		return geoCoord;
	}

	private void updateURLBuilder( )
	{
		switch ( this.mapProvider )
		{
		case GOOGLE:
			this.urlBuilder = new GoogleMapURLBuilder( );
			break;
		case BING:
		case OSMStaticMapLite:
		default:
			this.urlBuilder = new OSMStaticMapLite( );
			break;
		}
	}

	public void setViewPort( int width, int height )
	{
		this.viewPort = new Rectangle2D.Double( this.getBorderSize( ), this.getBorderSize( ), width - ( 2 * this.getBorderSize( ) ), height - ( 2 * this.getBorderSize( ) ) );
		this.extendedViewPort = new Rectangle2D.Double( this.viewPort.getX( ) - this.getViewPortBorderExtend( ), this.viewPort.getY( ) - this.getViewPortBorderExtend( ), this.viewPort.getWidth( ) + ( 2 * this.getViewPortBorderExtend( ) ), this.viewPort.getHeight( ) + ( 2 * this.getViewPortBorderExtend( ) ) );
		this.buildImageTiles( );
		this.updateImage( );
	}

	/**
	 * Finds the {@link Tile} with the given column, row. Returns null if this {@link Tile} does not exist.
	 * @param column
	 * @param row
	 * @return
	 */
	private Tile getTile( int column, int row )
	{
		for ( Map.Entry<Integer, Tile> entry : this.viewPortTiles.entrySet( ) )
		{
			Tile tile = entry.getValue( );
			if ( ( tile.getColumn( ) == column ) && ( tile.getRow( ) == row ) )
				return tile;
		}

		return null;
	}

	private Tile getTopLeftTile( )
	{
		int minColumn = Integer.MAX_VALUE;
		int minRow = Integer.MAX_VALUE;
		Tile tile = null;
		for ( Map.Entry<Integer, Tile> entry : this.viewPortTiles.entrySet( ) )
		{
			Tile tmpTile = entry.getValue( );
			if ( ( tmpTile.getColumn( ) <= minColumn ) && ( tmpTile.getRow( ) <= minRow ) )
			{
				minColumn = tmpTile.getColumn( );
				minRow = tmpTile.getRow( );
				tile = tmpTile;
			}
		}
		return tile;
	}

	/**
	 * Transforms the given {@link Rectangle2D} from the current camera coordinate-system into the coordinate-system of the given
	 * {@link AffineTransform}.
	 * @param toTransform
	 * @param tf
	 * @return
	 */
	private Rectangle2D toExtendedViewPortCoordinates( Rectangle2D toTransform, AffineTransform tf )
	{
		// We only have to transform the position of the top-left corner of the rectangle.
		Point2D topleft = new Point2D.Double( toTransform.getX( ), toTransform.getY( ) );
		topleft = tf.transform( topleft, topleft );
		return new Rectangle2D.Double( topleft.getX( ), topleft.getY( ), toTransform.getWidth( ), toTransform.getHeight( ) );
	}

	private void buildImageTiles( )
	{
		// remove tiles fully outside of the view-port
		List<Tile> toRemove = new ArrayList<Tile>( );
		for ( Map.Entry<Integer, Tile> entry : this.viewPortTiles.entrySet( ) )
		{
			Tile tile = entry.getValue( );
			Rectangle2D tileBounds = toExtendedViewPortCoordinates( tile.getBounds( ), this.camera );
			if ( !this.extendedViewPort.intersects( tileBounds ) )
			{
				toRemove.add( tile );
			}
		}

		for ( Tile tile : toRemove )
		{
			this.viewPortTiles.remove( tile.getTileId( ) );
		}

		Tile topLeft = this.getTopLeftTile( );
		int columnOffset = 0;
		int rowOffset = 0;
		if ( topLeft != null )
		{
			columnOffset = topLeft.getX( );
			rowOffset = topLeft.getY( );
		}

		// determine number of tiles needed for viewport
		int numVPTileColumns = this.getNumTileColumns( ) + ( -columnOffset );
		int numVPTileRows = this.getNumTileRows( ) + ( -rowOffset );

		// no tiles at all --> create some
		//		if ( this.viewPortTiles.isEmpty( ) )
		{
			//			this.viewPortTiles.clear( );
			int y = ( int ) this.extendedViewPort.getY( );

			for ( int row = 0; row < numVPTileRows; row++ )
			{
				int x = ( int ) this.extendedViewPort.getX( );
				for ( int col = 0; col < numVPTileColumns; col++ )
				{
					this.viewPortTiles.put( this.tileId, new Tile( tileId, x, y, col, row ) );
					if ( debug )
						log.info( "Tile [" + this.tileId + "] added: col=" + col + ", row=" + row + ")" );
					this.tileId++;
					x += Tile.TILE_SIZE_PX;
				}
				y += Tile.TILE_SIZE_PX;
			}

			if ( debug )
			{
				log.info( "ViewPort: numVPTileColumns=" + numVPTileColumns + ", numVPTileRows=" + numVPTileRows );
				log.info( "ViewPort: tileWidth=" + numVPTileColumns * Tile.TILE_SIZE_PX + ", tileHeight=" + numVPTileRows * Tile.TILE_SIZE_PX );
			}
		}
		//		else
		{

		}
	}

	private void updateImage( )
	{
		// determine number of tiles needed for viewport
		int numVPTileColumns = this.getNumTileColumns( );
		int numVPTileRows = this.getNumTileRows( );

		// compute the delta/difference if the current geocoord will be moved by n pixel (half size of one tile)		
		GeoCoord delta = MercatorProjection.computeDeltaGeoCoord( mapCenter, this.zoomLevel, Tile.HALF_TILE_SIZE_PX );

		// compute top-left geo-coord
		GeoCoord topLeft = new GeoCoord( mapCenter.getLatitude( ) - ( ( numVPTileColumns - 1 ) * delta.getLatitude( ) ), mapCenter.getLongitude( ) - ( ( numVPTileRows - 1 ) * delta.getLongitude( ) ) );

		List<TileRequest> tileRequests = new ArrayList<>( );
		for ( Map.Entry<Integer, Tile> entry : this.viewPortTiles.entrySet( ) )
		{
			Tile viewPortTile = entry.getValue( );

			int col = viewPortTile.getColumn( );
			int row = viewPortTile.getRow( );

			// update the geo-coordinate matching the position within the tile-grid
			double latitude = topLeft.getLatitude( ) - ( 2 * delta.getLatitude( ) * row );
			double longitude = topLeft.getLongitude( ) + ( 2 * delta.getLongitude( ) * col );

			if ( !viewPortTile.isValid( ) )
			{
				viewPortTile.setCenter( new GeoCoord( latitude, longitude ) );
				viewPortTile.setZoomLevel( zoomLevel );
				tileRequests.add( new TileRequest( this.log, this.urlBuilder, viewPortTile.getTileId( ), viewPortTile.getCenter( ), viewPortTile.getZoomLevel( ) ) );

				if ( debug )
					log.info( "Tile [" + viewPortTile.getTileId( ) + "] updated and request added: geoCoord=(" + latitude + "," + longitude + ")" );
			}

		}

		this.tileLoader.cancelAllRequests( );
		this.tileLoader.addTileRequestBlock( tileRequests );
	}

	private int getNumTileColumns( )
	{
		if ( debug )
			return ( int ) Math.round( this.extendedViewPort.getWidth( ) / ( double ) Tile.TILE_SIZE_PX );
		else return ( int ) Math.round( this.extendedViewPort.getWidth( ) / ( double ) Tile.TILE_SIZE_PX );

	}

	private int getNumTileRows( )
	{
		if ( debug )
			return ( int ) Math.round( this.extendedViewPort.getHeight( ) / ( double ) Tile.TILE_SIZE_PX );
		else return ( int ) Math.round( this.extendedViewPort.getHeight( ) / ( double ) Tile.TILE_SIZE_PX );
	}

	private void paint( Graphics2D gr )
	{
		// copy the Tiles (for thread-safety)
		List<Tile> tmpTiles = new ArrayList<>( );
		synchronized ( this.viewPortTiles )
		{
			for ( Map.Entry<Integer, Tile> entry : this.viewPortTiles.entrySet( ) )
			{
				tmpTiles.add( ( Tile ) entry.getValue( ).clone( ) );
			}
		}

		// draw the copied tiles images
		for ( Tile viewPortTile : tmpTiles )
		{
			int posX = viewPortTile.getX( );
			int posY = viewPortTile.getY( );
			gr.drawImage( viewPortTile.getImage( ), posX, posY, null );

			if ( debug )
			{
				gr.setColor( DEBUG_COLOR );
				gr.drawRect( posX, posY, Tile.TILE_SIZE_PX, Tile.TILE_SIZE_PX );
				gr.drawLine( ( posX + Tile.HALF_TILE_SIZE_PX ) - 20, posY + Tile.HALF_TILE_SIZE_PX, ( posX + Tile.HALF_TILE_SIZE_PX ) + 20, posY + Tile.HALF_TILE_SIZE_PX );
				gr.drawLine( posX + Tile.HALF_TILE_SIZE_PX, ( posY + Tile.HALF_TILE_SIZE_PX ) - 20, posX + Tile.HALF_TILE_SIZE_PX, ( posY + Tile.HALF_TILE_SIZE_PX ) + 20 );

				gr.setFont( DEBUG_FONT );
				gr.drawString( "ImgCoord=(" + viewPortTile.getX( ) + "," + viewPortTile.getY( ) + ")", posX + 10, posY + 20 );
				gr.drawString( "GeoCoord=(" + viewPortTile.getCenter( ).getFormatted( ) + ")", posX + 10, posY + 40 );
				gr.drawString( "ZoomLevel=(" + viewPortTile.getZoomLevel( ) + ")", posX + 10, posY + 60 );

				gr.setFont( DEBUG_FONT_BIG );
				gr.drawString( viewPortTile.getTileId( ) + "", Tile.HALF_TILE_SIZE_PX + posX, Tile.HALF_TILE_SIZE_PX + posY );
			}
		}

	}

	@Override
	public void paint( Graphics g )
	{
		if ( strategy != null )
		{
			do
			{
				Graphics2D gr = ( Graphics2D ) strategy.getDrawGraphics( );

				// update the render-quality
				applyRenderQuality( gr );

				// clear the screen
				gr.clearRect( 0, 0, this.getWidth( ), this.getHeight( ) );

				AffineTransform m = gr.getTransform( );
				gr.transform( camera );

				// call internal paint-method
				paint( gr );

				gr.setTransform( m );

				if ( debug )
				{
					// draw view port
					gr.setStroke( DEBUG_STROKE );
					gr.setFont( DEBUG_FONT );
					gr.setColor( Color.blue );

					int x0 = ( int ) this.viewPort.getX( );
					int y0 = ( int ) this.viewPort.getY( );
					int width = ( int ) this.viewPort.getWidth( );
					int height = ( int ) this.viewPort.getHeight( );
					gr.drawRect( x0, y0, width, height );
					gr.drawString( "Canvas: Size=(" + this.getWidth( ) + "," + this.getHeight( ) + ")", 10, 20 );
					gr.drawString( "ViewPort: Pos=(" + x0 + "," + y0 + "), Size=(" + width + "," + height + ")", 10, 40 );

					// draw extendedview port
					gr.setStroke( DEBUG_STROKE );

					x0 = ( int ) this.extendedViewPort.getX( );
					y0 = ( int ) this.extendedViewPort.getY( );
					width = ( int ) this.extendedViewPort.getWidth( );
					height = ( int ) this.extendedViewPort.getHeight( );
					gr.drawRect( x0, y0, width, height );

					gr.setFont( DEBUG_FONT );
					gr.setColor( Color.BLUE );
					gr.drawString( "ExtViewPort: Pos=(" + x0 + "," + y0 + "), Size=(" + width + "," + height + ")", 10, 60 );
				}
				gr.dispose( );
			}
			while ( strategy.contentsLost( ) || strategy.contentsRestored( ) );

			strategy.show( );
		}
	}

	@Override
	public void update( Graphics g )
	{
		paint( g );
	}

	@Override
	public void validate( )
	{
		super.validate( );
		this.createBufferStrategy( );
	}

	/**
	 * Creates the buffer-strategy that will be used for painting. Only with a buffer-strategy something will be drawn.
	 */
	private void createBufferStrategy( )
	{
		// only create it if the canvas is visible (size above 0)
		if ( ( getWidth( ) > 0 ) && ( getHeight( ) > 0 ) && this.isVisible( ) )
		{
			// create a buffer-strategy using 2 buffers
			this.createBufferStrategy( 2 );
			this.strategy = this.getBufferStrategy( );
		}
		else
		{
			strategy = null;
		}
	}

	/**
	 * Apply the render quality settings that were set using setRenderQuality() to the Graphics2D object given.
	 * @param gr The Graphics2D object to apply the settins to.
	 */
	private void applyRenderQuality( Graphics2D gr )
	{
		gr.setRenderingHint( RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON );
		if ( renderQuality == RENDER_QUALITY_LOW )
		{
			gr.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF );
			gr.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF );
			gr.setRenderingHint( RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED );
			gr.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR );
		}
		else if ( renderQuality == RENDER_QUALITY_HIGH )
		{
			gr.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
			gr.setRenderingHint( RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED );
			gr.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR );
		}
	}

	/**
	 * Sets the render quality to use for subsequent drawing operations. Can be either RENDER_QUALITY_HIGH or RENDER_QUALITY_LOW.
	 * @param rq The render quality to use from now on.
	 */
	protected void setRenderQuality( int rq )
	{
		renderQuality = rq;
		repaint( );
	}

	/**
	 * Returns the number of pixels used to extend the current viewport to get the extended viewport. This extension is, like a border,
	 * around the current viewport.
	 * @return
	 */
	public int getViewPortBorderExtend( )
	{
		if ( debug )
			return DEBUG_NUM_BORDER_TILES * Tile.TILE_SIZE_PX;
		return NUM_BORDER_TILES * Tile.TILE_SIZE_PX;
	}

	public int getBorderSize( )
	{
		if ( debug )
			return DEBUG_BORDER_SIZE;
		return BORDER_SIZE;
	}

	public void resetView( )
	{
		if ( this.initialCam == null )
			return;
		this.camera.setTransform( this.initialCam );
		this.repaint( );
	}

	@Override
	public void onTileLoadRequestComplete( int tileId, Image image )
	{
		// protect the tiles
		synchronized ( this.viewPortTiles )
		{
			Tile viewPortTile = this.viewPortTiles.get( tileId );
			if ( viewPortTile != null )
			{
				this.log.info( "onTileLoadRequestComplete(tile=" + viewPortTile + ")" );
				viewPortTile.setImage( image );
				viewPortTile.setValid( true );
				try
				{
					this.repaintQueue.put( true );
				}
				catch ( InterruptedException e )
				{
					e.printStackTrace( );
				}
			}
		}
	}

	@Override
	public void onTileLoadRequestStarted( int tileId )
	{
		synchronized ( this.viewPortTiles )
		{
			Tile viewPortTile = this.viewPortTiles.get( tileId );
			if ( viewPortTile != null )
			{

				this.log.info( "onTileLoadRequestStarted(tile=" + viewPortTile + ")" );
			}
		}
	}

	@Override
	public void onTileLoadRequestFailed( int tileId, FailReason reason, String cause )
	{
		synchronized ( this.viewPortTiles )
		{
			Tile viewPortTile = this.viewPortTiles.get( tileId );
			if ( viewPortTile != null )
			{
				viewPortTile.setValid( false );
				this.log.severe( "onTileLoadRequestFailed(tile=" + viewPortTile + ", reason=" + reason + ", cause=" + cause + ")" );
			}
		}
	}

	public static void setDebug( boolean debug )
	{
		MapImage.debug = debug;
	}

	public static boolean isDebug( )
	{
		return debug;
	}

	private class Repainter extends Thread
	{
		@Override
		public void run( )
		{
			while ( true )
			{

				try
				{
					Boolean doRepaint = repaintQueue.take( );
					if ( doRepaint.booleanValue( ) )
					{
						repaint( );
					}
				}
				catch ( InterruptedException e )
				{
					// TODO Auto-generated catch block
					e.printStackTrace( );
				}
			}
		}
	}
}
