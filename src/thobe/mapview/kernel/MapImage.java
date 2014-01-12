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

import static java.lang.Math.max;
import static java.lang.Math.min;

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
import thobe.mapview.kernel.tilesystem.TileNumber;

/**
 * @author Thomas Obenaus
 * @source MapImage.java
 * @date Nov 24, 2013
 */
@SuppressWarnings ( "serial")
public class MapImage extends Canvas implements TileLoaderListener
{
	private static double				MIN_SCALE_FACTOR					= 0.7;
	private static double				MAX_SCALE_FACTOR					= 1.3;
	private static int					MIN_ZOOM_LEVEL						= 1;
	private static int					MAX_ZOOM_LEVEL						= 18;
	private static boolean				DBG;
	private static boolean				DRAW_VIEWPORTS;
	private static final Color			DEBUG_COLOR							= Color.BLACK;
	private static final Stroke			DEBUG_STROKE						= new BasicStroke( 3 );
	private static final Font			DEBUG_FONT							= new Font( "Arial", Font.BOLD, 12 );
	private static final Font			DEBUG_FONT_BIG						= new Font( "Arial", Font.BOLD, 25 );

	public static final int				RENDER_QUALITY_LOW					= 0;
	public static final int				RENDER_QUALITY_HIGH					= 1;
	private BufferStrategy				strategy							= null;
	private int							renderQuality						= RENDER_QUALITY_HIGH;

	/**
	 * Distance (in pixel) to the inner view-port extension (measured from the view-port).
	 */
	private static final int			DIST_TO_INNER_VIEWPORT_EXTENSION	= Tile.TILE_SIZE_PX / 2;

	/**
	 * Distance (in pixel) to the outer view-port extension (measured from the inner view-port extension).
	 */
	private static final int			DIST_TO_OUTER_VIEWPORT_EXTENSION	= 50;

	/**
	 * A border (used only for debugging purposes) (size in pixel).
	 */
	private static final int			DEBUG_BORDER_SIZE					= 80;

	/**
	 * Map of {@link Tile}s <id of the {@link Tile},{@link Tile}>. The {@link Tile}s image-coordinates (x,y)
	 * are screen coordinates.
	 */
	private Map<String, Tile>			viewPortTiles;

	/**
	 * The provider used to get the {@link Tile}s/ images.
	 */
	private MapProvider					mapProvider;

	/**
	 * The {@link TileNumber} representing the center of the map.
	 */
	private TileNumber					tileNumberOfMapCenter;

	/**
	 * Current zoom-level.
	 */
	private int							zoomLevel;

	/**
	 * The logger.
	 */
	private Logger						log;

	/**
	 * Instance used to create the urls for requesting the images from the map-provider.
	 */
	private MapURLBuilder				urlBuilder;

	/**
	 * The thread that is responsible to load the images/ {@link Tile}s from the map-provider.
	 */
	private TileLoader					tileLoader;

	/**
	 * A flag used for synchronization between the main- and the repainting-thread.
	 */
	private SynchronousQueue<Boolean>	repaintFlag;

	/**
	 * The repaint-thread.
	 */
	private Repainter					repaintThread;

	/**
	 * For storing the initial state of the camera.
	 */
	private AffineTransform				initialCam;

	/**
	 * The camera transform that defines the view window into the scene.
	 */
	protected AffineTransform			camera;

	/**
	 * Current state of the cameras ({@link CameraState#NORMAL}, {@link CameraState#PAN}, {@link CameraState#ZOOM})
	 */
	private CameraState					cameraState;

	/**
	 * Mouse coordinates where button was pressed down during a dragging action.
	 */
	private int							mx, my;

	/**
	 * Camera transform when the mouse was pressed down.
	 */
	private AffineTransform				saved_cam;

	/**
	 * The relevant view-port.
	 */
	private Rectangle2D					viewPort;

	/**
	 * The first extended (inner) view-port. This is the boundary at which new {@link Tile}s will be added if they are partly inside.
	 */
	private Rectangle2D					innerExtViewPort;

	/**
	 * The second extended (outer) view-port. This is the boundary at which {@link Tile}s will be deleted if they are fully outside.
	 */
	private Rectangle2D					outerExtViewPort;

	/**
	 * Bounds of the currently visible {@link Tile}s in (outerExt)view-port coordinates.
	 * Tiles itself are given in screen coordinates but will be drawn in view (outerExt)view-port coordinates (after applying the camera).
	 */
	private Rectangle2D					tileGridBounds;

	/**
	 * The {@link Tile} containing the center of the map.
	 */
	private Tile						mapCenterTile;

	/**
	 * Ctor
	 * @param viewPortWidth
	 * @param viewPortHeight
	 * @param mapCenter
	 * @param zoomLevel
	 * @param mapProvider
	 * @param logger
	 */
	public MapImage( int viewPortWidth, int viewPortHeight, GeoCoord mapCenter, int zoomLevel, MapProvider mapProvider, Logger logger )
	{
		DBG = DebugManager.isMapImageDebug( );
		DRAW_VIEWPORTS = DebugManager.isMapImageDrawViewPorts( );
		this.cameraState = CameraState.NORMAL;
		this.camera = new AffineTransform( );
		this.mapCenterTile = null;
		this.repaintFlag = new SynchronousQueue<>( );
		this.repaintThread = new Repainter( );
		this.repaintThread.start( );

		this.log = logger;
		this.tileLoader = new TileLoader( this.log, 10 );
		this.tileLoader.addListener( this );
		this.tileLoader.start( );

		this.tileNumberOfMapCenter = MercatorProjection.geoCoordToTileNumber( mapCenter, zoomLevel );
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

				// Mousewheel --> zoom
				if ( e.getScrollType( ) == MouseWheelEvent.WHEEL_UNIT_SCROLL )
				{
					// Move to origin (0,0) by removing the translation.
					// We have to apply the scale if the scene is in origin of the coordinate-system
					// to avoid squeezing the scene or to loose aspect-ratio.
					AffineTransform tra = new AffineTransform( );
					tra.translate( -e.getX( ), -e.getY( ) );
					camera.preConcatenate( tra );

					// Determine the scale-factor.
					float factor = 1 + ( e.getWheelRotation( ) / 10.0f );

					// Scale the scene using the computed scale-factor.
					AffineTransform sc = new AffineTransform( );
					sc.scale( factor, factor );
					camera.preConcatenate( sc );

					// Now (after scaling) move the scene back to its position (by applying the translation).
					tra.setToIdentity( );
					tra.translate( e.getX( ), e.getY( ) );
					camera.preConcatenate( tra );

					// Update the tiles and repaint all.
					updateZoomLevel( );
					updateTileGrid( );
					createTileRequests( );
					repaint( );
				}// if ( e.getScrollType( ) == MouseWheelEvent.WHEEL_UNIT_SCROLL ).
			}
		} );

		this.addMouseListener( new MouseAdapter( )
		{

			public void mousePressed( MouseEvent e )
			{
				// Change state only if no special state is currently active.
				if ( cameraState == CameraState.NORMAL )
				{
					// Click right mouse button --> enter pan-mode.
					if ( e.getButton( ) == MouseEvent.BUTTON3 )
					{
						cameraState = CameraState.PAN;
						mx = e.getX( );
						my = e.getY( );
						saved_cam = new AffineTransform( camera );
					}// if ( e.getButton( ) == MouseEvent.BUTTON3 ).

					// Click middle mouse button + press ctrl --> enter zoom-mode 
					if ( ( e.isControlDown( ) ) && ( e.getButton( ) == MouseEvent.BUTTON2 ) )
					{
						cameraState = CameraState.ZOOM;
						mx = e.getX( );
						my = e.getY( );
						saved_cam = new AffineTransform( camera );
					}// if ( ( e.isControlDown( ) ) && ( e.getButton( ) == MouseEvent.BUTTON2 ) ).
				}// if ( cameraState == CameraState.NORMAL ).

				// Decrease renderquality in special camera-states.
				if ( cameraState != CameraState.NORMAL )
				{
					setRenderQuality( RENDER_QUALITY_LOW );
				}// if ( cameraState != CameraState.NORMAL ).

				if ( cameraState != CameraState.PAN && cameraState != CameraState.ZOOM )
				{
					// Mouse clicked/pressed
					// TODO: Delegate to listeners
				}
			}

			public void mouseReleased( MouseEvent e )
			{
				// Middle mouse button released.
				if ( e.getButton( ) == MouseEvent.BUTTON2 || e.getButton( ) == MouseEvent.BUTTON3 )
				{
					if ( ( cameraState == CameraState.PAN ) || ( cameraState == CameraState.ZOOM ) )
					{
						cameraState = CameraState.NORMAL;
					}
				}// if ( e.getButton( ) == MouseEvent.BUTTON2 || e.getButton( ) == MouseEvent.BUTTON3 ).

				// Increase renderquality in non-special camera-states.
				if ( cameraState == CameraState.NORMAL )
				{
					setRenderQuality( RENDER_QUALITY_HIGH );
				}// if ( cameraState == CameraState.NORMAL ).
				repaint( );
			}
		} );

		this.addMouseMotionListener( new MouseMotionAdapter( )
		{
			public void mouseDragged( MouseEvent e )
			{

				//  If in pan mode, translate according to mouse movement 
				if ( cameraState == CameraState.PAN )
				{
					double newX = ( e.getX( ) - mx ) / camera.getScaleX( );
					double newY = ( e.getY( ) - my ) / camera.getScaleY( );

					camera = new AffineTransform( saved_cam );
					camera.translate( newX, newY );

					// update view/ tiles
					updateZoomLevel( );
					updateTileGrid( );
					createTileRequests( );
					repaint( );
				}// if ( cameraState == CameraState.PAN ).

				// In zoom mode, zoom in if mouse moved up and zoom out if mouse moved down with pivot
				// point at the window coordinates the mouse was initially pressed.
				if ( cameraState == CameraState.ZOOM )
				{
					camera = new AffineTransform( saved_cam );

					// Move to origin (0,0) by removing the translation.
					// We have to apply the scale if the scene is in origin of the coordinate-system
					// to avoid squeezing the scene or to loose aspect-ratio.
					AffineTransform tra = new AffineTransform( );
					tra.translate( -mx, -my );
					camera.preConcatenate( tra );

					// Determine the scale-factor.
					float factor = 1.0f + Math.abs( my - e.getY( ) ) / 50.0f;
					if ( e.getY( ) > my )
					{
						factor = 1 / factor;
					}

					// Scale the scene using the computed scale-factor.
					AffineTransform sc = new AffineTransform( );
					sc.scale( factor, factor );
					camera.preConcatenate( sc );

					// Now (after scaling) move the scene back to its position (by applying the translation).
					tra.setToIdentity( );
					tra.translate( mx, my );
					camera.preConcatenate( tra );

					// update view/ tiles
					updateTileGrid( );
					createTileRequests( );
					repaint( );
				}
			}

			@Override
			public void mouseMoved( MouseEvent e )
			{
				//System.out.println( "Screen=" + e.getPoint( ) + " --> viewPort=" + screenPosToViewPortPos( e.getPoint( ) ) + " --> geoCoord=" + posToGeoCoord( e.getPoint( ) ) );
			}
		} );
	}

	/**
	 * Ctor
	 * @param viewPortWidth
	 * @param viewPortHeight
	 * @param logger
	 */
	public MapImage( int viewPortWidth, int viewPortHeight, Logger logger )
	{
		this( viewPortWidth, viewPortHeight, new GeoCoord( 64.99, 23.437 ), 8, MapProvider.OSMStaticMapLite, logger );
	}

	/**
	 * Converts a given on the map-image (view-port) into a position on the screen.
	 * @param pos
	 * @return
	 */
	public Point2D viewPortPosToScreenPos( Point2D pos )
	{
		Point2D result = null;
		try
		{
			result = camera.inverseTransform( new Point2D.Double( pos.getX( ), pos.getY( ) ), result );
		}
		catch ( NoninvertibleTransformException e )
		{
			e.printStackTrace( );
		}
		return result;
	}

	/**
	 * Converts a position on screen into a position on the view-port (might be translated/scaled).
	 * @param posOnScreen
	 * @return
	 */
	public Point2D screenPosToViewPortPos( Point2D posOnScreen )
	{
		Point2D result = null;
		result = camera.transform( new Point2D.Double( posOnScreen.getX( ), posOnScreen.getY( ) ), result );
		return result;
	}

	/**
	 * Returns the tile under/ at the given position (assuming that the position is in screen coordinates).
	 * @param position
	 * @return - the {@link Tile} at the requested position (migth be null).
	 */
	public Tile getTileAt( Point2D position )
	{
		// Convert the position to a position in screen-coordinates (even if the input-position is in screen-coordinates).
		// This is necessary since the tiles are given in screen-coordinates but drawn in view-port coordinates.
		// For example the user points with the mouse onto a position of the moved map. The tile-images are drawn in view-port coordinates
		// (--> the user points onto a view port coordinate) but internally the tiles are related to screen-coordinates.
		Point2D screenPos = this.viewPortPosToScreenPos( position );

		// find the top-left tile of the tile-grid
		Tile topleft = this.getTopLeftTile( );
		if ( topleft == null )
			return null;

		int x0 = topleft.getX( );
		int y0 = topleft.getY( );
		int column0 = topleft.getColumn( );
		int row0 = topleft.getRow( );

		// compute the difference/distance between the position and the upper-left corner of the top-left tile.
		double dx = screenPos.getX( ) - x0;
		double dy = screenPos.getY( ) - y0;

		// compute the differece between the column/row of the top-left tile and the tile at the requested position
		int dCol = ( int ) ( dx / Tile.TILE_SIZE_PX );
		int dRow = ( int ) ( dy / Tile.TILE_SIZE_PX );

		// obtain the tile under the cursor using the computed column/ row
		Tile tileUnderCursor = this.viewPortTiles.get( Tile.colRowToTileId( column0 + dCol, row0 + dRow ) );
		if ( DBG )
			log.finest( "P0(" + x0 + "," + y0 + ") - " + "P(" + screenPos.getX( ) + "," + screenPos.getY( ) + ") --> dXY(" + dx + "," + dy + ") --> dCR(" + dCol + "," + dRow + ") --> T" + ( ( tileUnderCursor != null ) ? tileUnderCursor : "null" ) );

		return tileUnderCursor;
	}

	/**
	 * Converts the given position into a {@link GeoCoord}.
	 * @param position
	 * @return - null if the conversion fails
	 */
	public GeoCoord posToGeoCoord( Point2D position )
	{
		Tile tileUnderCursor = this.getTileAt( position );
		if ( tileUnderCursor == null )
			return null;

		Point2D screenPos = this.viewPortPosToScreenPos( position );

		// compute position on tile
		Point2D tilePos = new Point2D.Double( screenPos.getX( ) - tileUnderCursor.getX( ), screenPos.getY( ) - tileUnderCursor.getY( ) );
		GeoCoord geoCoord = MercatorProjection.pixelCoordOnImageToGeoCoord( tilePos, tileUnderCursor.getCenter( ), Tile.HALF_TILE_SIZE_PX, this.zoomLevel );
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
		this.viewPort = new Rectangle2D.Double( this.getBorderSize( ), this.getBorderSize( ), width, height );

		// extend to compute the inner-extended view-port
		int innerExtViewportSize = DIST_TO_INNER_VIEWPORT_EXTENSION;
		this.innerExtViewPort = new Rectangle2D.Double( this.viewPort.getX( ) - innerExtViewportSize, this.viewPort.getY( ) - innerExtViewportSize, this.viewPort.getWidth( ) + ( 2 * innerExtViewportSize ), this.viewPort.getHeight( ) + ( 2 * innerExtViewportSize ) );

		// extend to compute the outer-extended view-port
		int outerExtViewportSize = innerExtViewportSize + DIST_TO_OUTER_VIEWPORT_EXTENSION;
		this.outerExtViewPort = new Rectangle2D.Double( this.viewPort.getX( ) - outerExtViewportSize, this.viewPort.getY( ) - outerExtViewportSize, this.viewPort.getWidth( ) + ( 2 * outerExtViewportSize ), this.viewPort.getHeight( ) + ( 2 * outerExtViewportSize ) );

		if ( DBG )
		{
			String msg = "View-Port size updated (width=" + width + ",height=" + height + ", border=" + this.getBorderSize( ) + ", innerVPextend=" + DIST_TO_INNER_VIEWPORT_EXTENSION + ", outerVPextend=" + DIST_TO_OUTER_VIEWPORT_EXTENSION + ")";
			log.info( msg );

			msg = "New View-Port values:  viewPort=" + rectToString( this.viewPort );
			msg += ", innerExtViewPort=" + rectToString( this.innerExtViewPort );
			msg += ", outerExtViewPort=" + rectToString( this.innerExtViewPort );
			log.info( msg );
		}

		// Update the tile-grid using the new size and request the images.
		this.updateTileGrid( );
		this.createTileRequests( );
	}

	/**
	 * Returns the top-left {@link Tile} of the tile-grid.
	 * @return
	 */
	private Tile getTopLeftTile( )
	{
		int minColumn = Integer.MAX_VALUE;
		int minRow = Integer.MAX_VALUE;
		Tile tile = null;
		for ( Map.Entry<String, Tile> entry : this.viewPortTiles.entrySet( ) )
		{
			Tile tmpTile = entry.getValue( );
			if ( ( tmpTile.getColumn( ) <= minColumn ) && ( tmpTile.getRow( ) <= minRow ) )
			{
				minColumn = tmpTile.getColumn( );
				minRow = tmpTile.getRow( );
				tile = tmpTile;
			}// if ( ( tmpTile.getColumn( ) <= minColumn ) && ( tmpTile.getRow( ) <= minRow ) ).
		}// for ( Map.Entry<String, Tile> entry : this.viewPortTiles.entrySet( ) ).

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
		// transform the top-left corner
		Point2D topleft = new Point2D.Double( toTransform.getX( ), toTransform.getY( ) );
		topleft = tf.transform( topleft, topleft );

		// transform the bottom-right corner (needed for width and height)
		Point2D bottomRight = new Point2D.Double( toTransform.getX( ) + toTransform.getWidth( ), toTransform.getY( ) + toTransform.getHeight( ) );
		bottomRight = tf.transform( bottomRight, bottomRight );
		return new Rectangle2D.Double( topleft.getX( ), topleft.getY( ), bottomRight.getX( ) - topleft.getX( ), bottomRight.getY( ) - topleft.getY( ) );
	}

	/**
	 * Returns the size of a {@link Tile} in camera/view-port coordinates, that means the current zoom (scale) is applied to the size of the
	 * {@link Tile}.
	 * @return
	 */
	private double getScaledTileSize( )
	{
		return Tile.TILE_SIZE_PX * this.camera.getScaleX( );
	}

	/**
	 * Returns the missing grid-elements based on given element-type ({@link GridElement#COLUMN} or {@link GridElement#ROW}) and position (
	 * {@link GridPosition#TOP_LEFT} or {@link GridPosition#BOTTOM_RIGHT}).
	 * @param gElement
	 * @param gPos
	 * @return
	 */
	private int computeMissingGridElements( GridElement gElement, GridPosition gPos )
	{
		// compute the distance between the right/left or top/lower border of the tile-grid and the according border of the inner extended view-port
		double delta = 0;

		// missing columns on the left side
		if ( ( gElement == GridElement.COLUMN ) && ( gPos == GridPosition.TOP_LEFT ) )
		{
			delta = this.tileGridBounds.getX( ) - this.innerExtViewPort.getX( );
		}
		// missing columns on the right side
		else if ( ( gElement == GridElement.COLUMN ) && ( gPos == GridPosition.BOTTOM_RIGHT ) )
		{
			delta = ( this.innerExtViewPort.getX( ) + this.innerExtViewPort.getWidth( ) ) - ( this.tileGridBounds.getX( ) + this.tileGridBounds.getWidth( ) );
		}
		// missing rows on the top side
		else if ( ( gElement == GridElement.ROW ) && ( gPos == GridPosition.TOP_LEFT ) )
		{
			delta = this.tileGridBounds.getY( ) - this.innerExtViewPort.getY( );
		}
		// missing rows on the bottom side
		else if ( ( gElement == GridElement.ROW ) && ( gPos == GridPosition.BOTTOM_RIGHT ) )
		{
			delta = ( this.innerExtViewPort.getY( ) + this.innerExtViewPort.getHeight( ) ) - ( this.tileGridBounds.getY( ) + this.tileGridBounds.getHeight( ) );
		}

		// reset to 0 for invalid tile-grid bounds
		if ( this.tileGridBounds.getWidth( ) == 0 )
			delta = 0;
		if ( this.tileGridBounds.getHeight( ) == 0 )
			delta = 0;

		// obtain the size of a tile related to the current camera (regarding zoom --> scale)
		double scaledTileSize = getScaledTileSize( );

		int missingGridElements = 0;
		if ( ( delta > 0 ) && ( delta <= scaledTileSize ) )
		{
			// at least one grid-element if delta is > 0
			missingGridElements = 1;
		}// if ( ( delta > 0 ) && ( delta <= scaledTileSize ) ).
		else if ( ( delta > 0 ) && ( delta > scaledTileSize ) )
		{
			// otherwise compute the number of missing grid-elements
			missingGridElements = ( int ) ( delta / ( double ) scaledTileSize );
		}// else if (( delta > 0 ) && ( delta > scaledTileSize )).

		return missingGridElements;
	}

	private void updateZoomLevel( )
	{
		String dbgInfo = "scaleFactor=" + this.camera.getScaleX( ) + ", zoomLevel=" + this.zoomLevel;
		boolean bZoomLevelModified = false;
		boolean bScaleFactorModified = false;

		double newScale = this.camera.getScaleX( );

		// Update/modify the zoom-level if the minimum scale factor was undershoot.
		if ( this.camera.getScaleX( ) <= MIN_SCALE_FACTOR )
		{
			// Adjust the zoom-level only if we are not currently at min-zoom-level.
			if ( this.zoomLevel > MIN_ZOOM_LEVEL )
			{
				this.zoomLevel--;
				newScale = 1;
				bZoomLevelModified = true;
			}// if ( this.zoomLevel > MIN_ZOOM_LEVEL ).
			else
			{
				// Stick zoomlevel to min value
				newScale = MIN_SCALE_FACTOR;
			}// if ( this.zoomLevel > MIN_ZOOM_LEVEL ) ... else ...
			bScaleFactorModified = true;
		}// if ( this.camera.getScaleX( ) <= MIN_SCALE_FACTOR ).

		// Update/modify the zoom-level if the maximum scale factor was overshoot.
		if ( this.camera.getScaleX( ) >= MAX_SCALE_FACTOR )
		{
			// Adjust the zoom-level only if we are not currently at max-zoom-level.
			if ( this.zoomLevel < MAX_ZOOM_LEVEL )
			{
				this.zoomLevel++;
				newScale = 1;
				bZoomLevelModified = true;
			}// if ( this.zoomLevel < MAX_ZOOM_LEVEL ).
			else
			{
				// Stick zoomlevel to max value
				newScale = MAX_SCALE_FACTOR;
			}// if ( this.zoomLevel > MIN_ZOOM_LEVEL ) ... else ...
			bScaleFactorModified = true;
		}// if ( this.camera.getScaleX( ) >= MAX_SCALE_FACTOR ).

		if ( bScaleFactorModified )
		{
			// Save translation.
			double camX = camera.getTranslateX( );
			double camY = camera.getTranslateY( );
			camera.setToIdentity( );

			// Scale the scene using the computed scale-factor.
			AffineTransform sc = new AffineTransform( );
			sc.scale( newScale, newScale );
			camera.preConcatenate( sc );

			// Now (after scaling) move the scene back to its position (by applying the translation).
			AffineTransform tra = new AffineTransform( );
			tra.setToIdentity( );
			tra.translate( camX, camY );
			camera.preConcatenate( tra );
		}// if ( bScaleFactorModified ).

		// Invalidate all view-port tiles in case we have a new zoom-level.
		if ( bZoomLevelModified )
		{
			for ( Map.Entry<String, Tile> entry : this.viewPortTiles.entrySet( ) )
			{
				entry.getValue( ).setValid( false );
			}

			dbgInfo += " -->scaleFactor=" + this.camera.getScaleX( ) + ", zoomLevel=" + this.zoomLevel;
			if ( DBG )
				this.log.info( dbgInfo );
		}// if ( bZoomLevelModified ).
	}

	/**
	 * This internal method computes/creates the Tiles which are visible on the viewport and computes the {@link GeoCoord}s used for this
	 * {@link Tile}s.
	 */
	private void updateTileGrid( )
	{
		// update the bounds of visible tiles
		this.updateTileGridBounds( );

		// compute how many rows/columns are needed to cover the inner extended view-port
		// find missing rows on the top and columns on the left
		int missingColumnsLeft = computeMissingGridElements( GridElement.COLUMN, GridPosition.TOP_LEFT );
		int missingRowsTop = computeMissingGridElements( GridElement.ROW, GridPosition.TOP_LEFT );

		// compute how many rows/columns are needed to cover the inner extended view-port
		// find missing rows on the bottom and columns on the right
		int missingColumnsRight = computeMissingGridElements( GridElement.COLUMN, GridPosition.BOTTOM_RIGHT );
		int missingRowsBottom = computeMissingGridElements( GridElement.ROW, GridPosition.BOTTOM_RIGHT );

		// compute the top-left column/row [col0,row0]
		int column0 = 0;
		int row0 = 0;
		Tile topLeft = this.getTopLeftTile( );
		if ( topLeft != null )
		{
			column0 = topLeft.getColumn( ) - missingColumnsLeft;
			row0 = topLeft.getRow( ) - missingRowsTop;
		}// if ( topLeft != null ).

		// compute the top-left corner P(x0,y0)
		int x0 = ( int ) ( column0 * Tile.TILE_SIZE_PX + this.outerExtViewPort.getX( ) );
		int y0 = ( int ) ( row0 * Tile.TILE_SIZE_PX + this.outerExtViewPort.getY( ) );

		// compute how many columns/rows are visible
		int numberOfVisibleColumns = missingColumnsLeft + this.getNumTileColumns( ) + missingColumnsRight;
		int numberOfVisibleRows = missingRowsTop + this.getNumTileRows( ) + missingRowsBottom;

		// compute the index of the last column/ row
		int idxOfLastColumn = ( numberOfVisibleColumns - 1 ) + column0;
		int idxOfLastRow = ( numberOfVisibleRows - 1 ) + row0;

		if ( DBG )
		{
			log.fine( "numberOfVisibleColumns=" + numberOfVisibleColumns + ", numberOfVisibleRows=" + numberOfVisibleRows + ", idxOfFirstColumn=" + column0 + ", idxOfFirstRow=" + row0 + ", idxOfLastColumn=" + idxOfLastColumn + ", idxOfLastRow=" + idxOfLastRow );
			log.fine( "tileGridBounds=" + rectToString( this.tileGridBounds ) );
		}

		// compute column and row of the Tile containing the center of the map.
		// Compute the column/row regarding the number of columns/rows.
		int columnOfMapCenter = ( this.getNumTileColumns( ) / 2 ) + column0; //1->0, 2->1,3->2, ....
		int rowOfMapCenter = ( this.getNumTileRows( ) / 2 ) + row0; //1->0, 2->1,3->2, ....

		// check if an update of the map-center is necessary
		Tile newPotentialMapCenterTile = this.viewPortTiles.get( Tile.colRowToTileId( columnOfMapCenter, rowOfMapCenter ) );
		if ( ( newPotentialMapCenterTile != null ) && ( newPotentialMapCenterTile != this.mapCenterTile ) )
		{
			this.mapCenterTile = newPotentialMapCenterTile;
			this.tileNumberOfMapCenter = this.mapCenterTile.getTileNumber( );

			if ( DBG )
				log.fine( "Tile [" + this.mapCenterTile.getTileId( ) + "] Is the new Tile containing the map-center (geoCoord=" + this.mapCenterTile.getCenter( ).getFormatted( ) + ")" );
		}// if ( ( newPotentialMapCenterTile != null ) && ( newPotentialMapCenterTile != this.mapCenterTile ) ).

		// 1. Create Tiles that are missing (where not created yet but are visible on the map).
		// 2. Update geo-coordinates and zoom-level of existing Tiles. 
		int y = y0;
		for ( int row = row0; row <= idxOfLastRow; row++ )
		{
			int x = x0;
			for ( int column = column0; column <= idxOfLastColumn; column++ )
			{
				String tileId = Tile.colRowToTileId( column, row );
				Tile tile = this.viewPortTiles.get( tileId );

				// Determine the distance (number of columns/rows) of this Tile to the Tile containing the map-center.
				int columnOffset = column - columnOfMapCenter;
				int rowOffset = row - rowOfMapCenter;

				TileNumber tileNumberOfCurrentTile = new TileNumber( tileNumberOfMapCenter.getX( ) + columnOffset, tileNumberOfMapCenter.getY( ) + rowOffset, this.zoomLevel );

				// tile does not exist yet --> create it
				if ( tile == null )
				{
					tile = new Tile( tileId, x, y );
					this.viewPortTiles.put( tileId, tile );
					if ( DBG )
						log.fine( "Tile [" + tile.getTileId( ) + "] created and added." );
				}// if ( tile == null ).

				// Apply computed TileNumber 
				tile.setTileNumber( tileNumberOfCurrentTile );
				tile.setValid( false );

				// tile containing the map-center found
				if ( ( column == columnOfMapCenter ) && ( row == rowOfMapCenter ) )
				{
					this.mapCenterTile = tile;
					if ( DBG )
						log.info( "Tile [" + tile.getTileId( ) + "] contains the center of the Map (geoCoord=" + this.mapCenterTile.getCenter( ).getFormatted( ) + ")" );
				}// if ( ( col == columnOfMapCenter ) && ( row == rowOfMapCenter ) ).

				// next column (update x-coordinate)
				x += Tile.TILE_SIZE_PX;
			}// for ( int col = column0; col < numberOfVisibleColumns; col++ ).

			// next row (update y-coordinate)
			y += Tile.TILE_SIZE_PX;
		}// for ( int row = row0; row < numberOfVisibleRows; row++ ).

		// remove tiles fully outside of the outer extended view-port
		List<Tile> toRemove = new ArrayList<Tile>( );
		for ( Map.Entry<String, Tile> entry : this.viewPortTiles.entrySet( ) )
		{
			Tile tile = entry.getValue( );
			Rectangle2D tileBounds = toExtendedViewPortCoordinates( tile.getBounds( ), this.camera );
			if ( !this.outerExtViewPort.intersects( tileBounds ) )
			{
				toRemove.add( tile );
			}
		}

		// Guarantee that at least one tile will be visible.
		// Remove only tiles if some tiles will be left in the list of view-port tiles.
		if ( this.viewPortTiles.size( ) > toRemove.size( ) )
		{
			// now remove the tiles 
			for ( Tile tile : toRemove )
			{
				this.viewPortTiles.remove( tile.getTileId( ) );
			}
		}// if(this.viewPortTiles.size( ) > toRemove.size( )).

		this.updateTileGridBounds( );

	}

	/**
	 * This method computes/updates the bounds (bounding-box containing all visible {@link Tile}s) of all visible {@link Tile}s.
	 */
	private void updateTileGridBounds( )
	{
		// no tiles --> bounds are [0,0,0,0]
		if ( this.viewPortTiles.isEmpty( ) )
		{
			this.tileGridBounds.setRect( this.innerExtViewPort.getX( ), this.innerExtViewPort.getY( ), 0, 0 );
		}// if ( this.viewPortTiles.isEmpty( ) ).
		else
		{
			int minX = Integer.MAX_VALUE;
			int minY = Integer.MAX_VALUE;
			int maxX = -Integer.MAX_VALUE;
			int maxY = -Integer.MAX_VALUE;

			for ( Map.Entry<String, Tile> entry : this.viewPortTiles.entrySet( ) )
			{
				Tile tile = entry.getValue( );
				minX = min( minX, tile.getX( ) );
				minY = min( minY, tile.getY( ) );
				maxX = max( maxX, tile.getX( ) );
				maxY = max( maxY, tile.getY( ) );
			}

			// Convert the coordinates of the upper- and leftmost Tile (given in screen coordinates) to view-port coordinates. 
			Point2D upperLeftCorner = screenPosToViewPortPos( new Point2D.Double( minX, minY ) );
			Point2D lowerRightCorner = screenPosToViewPortPos( new Point2D.Double( maxX + Tile.TILE_SIZE_PX, maxY + Tile.TILE_SIZE_PX ) );
			double width = lowerRightCorner.getX( ) - upperLeftCorner.getX( );
			double height = lowerRightCorner.getY( ) - upperLeftCorner.getY( );
			this.tileGridBounds.setRect( upperLeftCorner.getX( ), upperLeftCorner.getY( ), width, height );

		}// if ( this.viewPortTiles.isEmpty( ) ) ... else ...
	}

	/**
	 * This method creates a new {@link TileRequest} for each {@link Tile} whose content is not valid.
	 */
	private void createTileRequests( )
	{
		List<TileRequest> tileRequests = new ArrayList<>( );
		for ( Map.Entry<String, Tile> entry : this.viewPortTiles.entrySet( ) )
		{
			Tile viewPortTile = entry.getValue( );
			if ( !viewPortTile.isValid( ) && !viewPortTile.isEmptyTile( ) )
			{
				tileRequests.add( new TileRequest( this.log, this.urlBuilder, viewPortTile.getTileId( ), viewPortTile.getTileNumber( ) ) );

				if ( DBG )
					log.fine( "Tile [" + viewPortTile.getTileId( ) + "] Request started: geoCoord=" + viewPortTile.getCenter( ).getFormatted( ) );
			}// if ( !viewPortTile.isValid( ) ).
		}// for ( Map.Entry<String, Tile> entry : this.viewPortTiles.entrySet( ) ).

		if ( !tileRequests.isEmpty( ) )
		{
			this.tileLoader.cancelAllRequests( );
			this.tileLoader.addTileRequestBlock( tileRequests );
		}// if ( !tileRequests.isEmpty( ) ).
	}

	private int getNumTileColumns( )
	{
		if ( this.tileGridBounds.getWidth( ) == 0 )
		{
			// use the size of the inner extended view-port to compute the number of columns needed in case the tile-grid
			// bounds are not yet initialized.
			return ( int ) ( Math.round( this.innerExtViewPort.getWidth( ) / this.getScaledTileSize( ) ) ) + 1;
		}// if ( this.tileGridBounds.getWidth( ) == 0 ).	
		return ( int ) ( Math.round( this.tileGridBounds.getWidth( ) / this.getScaledTileSize( ) ) );
	}

	private int getNumTileRows( )
	{
		if ( this.tileGridBounds.getHeight( ) == 0 )
		{
			// use the size of the inner extended view-port to compute the number of rows needed in case the tile-grid
			// bounds are not yet initialized.
			return ( int ) ( Math.round( this.innerExtViewPort.getHeight( ) / this.getScaledTileSize( ) ) ) + 1;
		}// if ( this.tileGridBounds.getHeight( ) == 0 ).
		return ( int ) ( Math.round( this.tileGridBounds.getHeight( ) / this.getScaledTileSize( ) ) );
	}

	private void paint( Graphics2D gr )
	{
		// copy the Tiles (for thread-safety)
		List<Tile> tmpTiles = new ArrayList<>( );
		synchronized ( this.viewPortTiles )
		{
			for ( Map.Entry<String, Tile> entry : this.viewPortTiles.entrySet( ) )
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

			if ( DBG )
			{
				if ( viewPortTile.getTileId( ).equals( this.mapCenterTile.getTileId( ) ) )
					gr.setColor( Color.RED );
				else gr.setColor( DEBUG_COLOR );

				gr.drawRect( posX, posY, Tile.TILE_SIZE_PX, Tile.TILE_SIZE_PX );
				gr.drawLine( ( posX + Tile.HALF_TILE_SIZE_PX ) - 20, posY + Tile.HALF_TILE_SIZE_PX, ( posX + Tile.HALF_TILE_SIZE_PX ) + 20, posY + Tile.HALF_TILE_SIZE_PX );
				gr.drawLine( posX + Tile.HALF_TILE_SIZE_PX, ( posY + Tile.HALF_TILE_SIZE_PX ), posX + Tile.HALF_TILE_SIZE_PX, ( posY + Tile.HALF_TILE_SIZE_PX ) + 20 );

				gr.setColor( DEBUG_COLOR );

				gr.setFont( DEBUG_FONT );
				gr.drawString( "ImgCoord=(" + viewPortTile.getX( ) + "," + viewPortTile.getY( ) + ")", posX + 10, posY + 20 );
				gr.drawString( "GeoCoord=(" + viewPortTile.getCenter( ).getFormatted( ) + ")", posX + 10, posY + 35 );
				gr.drawString( "ZoomLevel=(" + viewPortTile.getZoomLevel( ) + ")", posX + 10, posY + 50 );

				gr.setFont( DEBUG_FONT_BIG );
				gr.drawString( viewPortTile.getTileId( ) + "", Tile.HALF_TILE_SIZE_PX + posX, Tile.HALF_TILE_SIZE_PX + posY );
			}
		}// for ( Tile viewPortTile : tmpTiles ).
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

				// store transform
				AffineTransform m = gr.getTransform( );

				// apply camera 
				gr.transform( camera );

				// call internal paint-method
				paint( gr );

				// reset transform
				gr.setTransform( m );

				if ( DRAW_VIEWPORTS )
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

					// draw inner extended view port
					gr.setStroke( DEBUG_STROKE );

					x0 = ( int ) this.innerExtViewPort.getX( );
					y0 = ( int ) this.innerExtViewPort.getY( );
					width = ( int ) this.innerExtViewPort.getWidth( );
					height = ( int ) this.innerExtViewPort.getHeight( );
					gr.drawRect( x0, y0, width, height );

					gr.setFont( DEBUG_FONT );
					gr.setColor( Color.BLUE );
					gr.drawString( "InnerExtViewPort: Pos=(" + x0 + "," + y0 + "), Size=(" + width + "," + height + ")", 10, 60 );

					// draw outer extended view port
					x0 = ( int ) this.outerExtViewPort.getX( );
					y0 = ( int ) this.outerExtViewPort.getY( );
					width = ( int ) this.outerExtViewPort.getWidth( );
					height = ( int ) this.outerExtViewPort.getHeight( );
					gr.drawRect( x0, y0, width, height );
					gr.drawString( "OuterExtViewPort: Pos=(" + x0 + "," + y0 + "), Size=(" + width + "," + height + ")", 10, 80 );
					gr.drawString( "MapCenter: (" + this.tileNumberOfMapCenter.getCenter( ).getFormatted( ), 10, 100 );

					// draw the bounds of the tile-grid
					gr.setColor( Color.RED );
					gr.drawRect( ( int ) this.tileGridBounds.getX( ), ( int ) this.tileGridBounds.getY( ), ( int ) this.tileGridBounds.getWidth( ), ( int ) this.tileGridBounds.getHeight( ) );

				}// if ( DRAW_VIEWPORTS ).
				gr.dispose( );

			}// do.
			while ( strategy.contentsLost( ) || strategy.contentsRestored( ) );

			strategy.show( );
		}// if ( strategy != null ).
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
	 * Number of pixels which are around the viewport-tiles and will be used to paint the border-tiles. This is the number of pixels, the
	 * view-port itself is shrinked regarding the width/height that was specified via constructor or {@link MapImage#setViewPort(int, int)}.
	 * So this value is typically 0. Only for debugging purposes it might be useful to shrink the view-port a little bit for being able to
	 * see the inner- and outerExt view-port.
	 * The view-port is shrinked using the following computation: viewPort.x = getBorderSize(); viewPort.y = getBorderSize();
	 * viewPort.width = width - (2*getBorderSize());viewPort.height = height - (2*getBorderSize());
	 * @returns - The number of pixels
	 */
	private int getBorderSize( )
	{
		// Use a border only for debugging.
		if ( DBG )
		{
			return DEBUG_BORDER_SIZE + DIST_TO_INNER_VIEWPORT_EXTENSION + DIST_TO_OUTER_VIEWPORT_EXTENSION;
		}// if ( DBG ).
		return 0;
	}

	/**
	 * Reset the view to initial values.
	 */
	public void resetView( )
	{
		if ( this.initialCam == null )
		{
			return;
		}// if ( this.initialCam == null ).
		this.camera.setTransform( this.initialCam );

		this.repaint( );
	}

	@Override
	public void onTileLoadRequestComplete( String tileId, Image image )
	{
		// protect the tiles
		synchronized ( this.viewPortTiles )
		{
			Tile viewPortTile = this.viewPortTiles.get( tileId );
			if ( viewPortTile != null )
			{
				this.log.fine( "onTileLoadRequestComplete(tile=" + viewPortTile + ")" );
				viewPortTile.setImage( image );
				viewPortTile.setValid( true );
				try
				{
					this.repaintFlag.put( true );
				}
				catch ( InterruptedException e )
				{
					e.printStackTrace( );
				}
			}
		}
	}

	@Override
	public void onTileLoadRequestStarted( String tileId )
	{
		synchronized ( this.viewPortTiles )
		{
			Tile viewPortTile = this.viewPortTiles.get( tileId );
			if ( viewPortTile != null )
			{

				this.log.fine( "onTileLoadRequestStarted(tile=" + viewPortTile + ")" );
			}
		}
	}

	@Override
	public void onTileLoadRequestFailed( String tileId, FailReason reason, String cause )
	{
		synchronized ( this.viewPortTiles )
		{
			Tile viewPortTile = this.viewPortTiles.get( tileId );
			if ( viewPortTile != null )
			{
				viewPortTile.setValid( false );
				this.log.fine( "onTileLoadRequestFailed(tile=" + viewPortTile + ", reason=" + reason + ", cause=" + cause + ")" );
			}
		}
	}

	/**
	 * Creates a formatted string for a {@link Rectangle2D}.
	 * @param rect
	 * @return
	 */
	private static String rectToString( Rectangle2D rect )
	{
		return "[x=" + rect.getX( ) + ", y=" + rect.getY( ) + ", witdh=" + rect.getWidth( ) + ", height=" + rect.getHeight( ) + "]";
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
					Boolean doRepaint = repaintFlag.take( );
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

	/**
	 * Enum, representing the state of the camera.
	 * @author Thomas Obenaus
	 */
	private enum CameraState
	{
		NORMAL, PAN, ZOOM;
	}

	private enum GridElement
	{
		COLUMN, ROW;
	}

	private enum GridPosition
	{
		TOP_LEFT, BOTTOM_RIGHT;
	}
}
