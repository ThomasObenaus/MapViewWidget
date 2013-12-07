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

import static java.lang.Math.PI;
import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.exp;
import static java.lang.Math.floor;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sinh;
import static java.lang.Math.tan;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

import java.awt.geom.Point2D;

/**
 * Class providing some usefull methods for coordinate transformations.
 * @author Thomas Obenaus
 */
public class MercatorProjection
{
	/**
	 * The earth-radius (equator) in meters
	 */
	private static final int	EARTH_RADIUS	= 6378137;

	/**
	 * Meters per inch
	 */
	private static final double	METER_PER_INCH	= 0.0254;

	/**
	 * Computes the sec of a given value. sec = 1/cos(value).
	 * @param value
	 * @return
	 */
	public static double sec( double value )
	{
		return 1d / cos( value );
	}

	/**
	 * Computes the {@link TileNumber} for the tile that contains the given {@link GeoCoord}
	 * @param geoCoord - the {@link GeoCoord}
	 * @param zoom - the current zoom level
	 * @return
	 */
	public static TileNumber geoCoordToTileNumber( GeoCoord geoCoord, int zoom )
	{
		int numberOfTiles = getNumberOfTiles( zoom );

		double xtile = numberOfTiles * ( ( geoCoord.getLongitude( ) + 180d ) / 360d );

		double latitudeRadian = toRadians( geoCoord.getLatitude( ) );
		double ytile = tan( latitudeRadian ) + sec( latitudeRadian );
		ytile = log( ytile ) / PI;
		ytile = 1 - ytile;
		ytile = numberOfTiles * ( ytile / 2d );

		return new TileNumber( xtile, ytile );
	}

	/**
	 * Compute zoom-dependent number of tiles for the whole map. Since the size of the map depends on zoom level (mapheight = mapwitdh = 256
	 * * 2^zoom), the number of tiles (at
	 * fixed size of 256) does too. numTiles = 256 * 2^zoom
	 * @param zoom
	 * @return
	 */
	public static int getNumberOfTiles( int zoom )
	{
		int numberOfTiles = ( int ) Math.pow( 2, zoom );
		return numberOfTiles;
	}

	/**
	 * Returns the QuadKey for the tile that contains the pixel representing the given {@link GeoCoord} at given zoom-level.
	 * @param geoCoord
	 * @param zoom
	 * @return
	 */
	public static Point2D geoCoordToTileQuadKey( GeoCoord geoCoord, int zoom )
	{
		Point2D pixelCoordAtWorldMap = geoCoordToPixelOnWorldMap( geoCoord, zoom );
		double tileX = floor( pixelCoordAtWorldMap.getX( ) / 256d );
		double tileY = floor( pixelCoordAtWorldMap.getY( ) / 256d );

		return new Point2D.Double( tileX, tileY );
	}

	/**
	 * Computes the pixel-coordinates for the given {@link GeoCoord} at the world-map at given zoom-level.
	 * @param geoCoord
	 * @param zoom
	 * @return
	 */
	public static Point2D geoCoordToPixelOnWorldMap( GeoCoord geoCoord, int zoom )
	{
		// latitude range -85.05112878 to 85.05112878 degree
		double latitude = max( geoCoord.getLatitude( ), -85.05112878d );
		latitude = min( latitude, 85.05112878 );

		// longitude range -180 to 180 degree
		double longitude = max( geoCoord.getLongitude( ), -180d );
		longitude = min( longitude, 180d );

		int mapSize = getMapSize( zoom );

		// sinLatitude = sin(latitude * pi/180)
		// pixelX = ((longitude + 180) / 360) * 256 * 2 level
		// pixelY = (0.5 – log((1 + sinLatitude) / (1 – sinLatitude)) / (4 * pi)) * 256 * 2 level
		double pixelX = ( longitude + 180d ) / 360d;
		pixelX = pixelX * mapSize;

		double sinLatitude = sin( toRadians( latitude ) );
		double pixelY = ( 1d + sinLatitude ) / ( 1d - sinLatitude );
		pixelY = log( pixelY ) / ( 4d * PI );
		pixelY = 0.5d - pixelY;
		pixelY = pixelY * mapSize;

		return new Point2D.Double( pixelX, pixelY );
	}

	public static GeoCoord computeDeltaGeoCoord( GeoCoord geoCoord, int zoom, int deltaPx )
	{
		// compute the pixel-coordinate of the given GeoCoordinate on the world map 
		Point2D geoCoordOnWorldMap = MercatorProjection.geoCoordToPixelOnWorldMap( geoCoord, zoom );

		// compute the GeoCoord moved by delta
		GeoCoord movedGeoCoord = MercatorProjection.pixelCoordOnWorldMapToGeoCoord( new Point2D.Double( geoCoordOnWorldMap.getX( ) + deltaPx, geoCoordOnWorldMap.getY( ) + deltaPx ), zoom );

		// return the difference between the original and the moved geoCoordinate
		return movedGeoCoord.subtract( geoCoord ).abs( );
	}

	public static GeoCoord computeDeltaGeoCoord( GeoCoord geoCoord, int zoom, int deltaXPx, int deltaYPx )
	{
		// compute the pixel-coordinate of the given GeoCoordinate on the world map 
		Point2D geoCoordOnWorldMap = MercatorProjection.geoCoordToPixelOnWorldMap( geoCoord, zoom );

		// compute the GeoCoord moved by delta
		GeoCoord movedGeoCoord = MercatorProjection.pixelCoordOnWorldMapToGeoCoord( new Point2D.Double( geoCoordOnWorldMap.getX( ) + deltaXPx, geoCoordOnWorldMap.getY( ) + deltaYPx ), zoom );

		// return the difference between the original and the moved geoCoordinate
		return movedGeoCoord.subtract( geoCoord ).abs( );
	}

	/**
	 * Computes the pixel-coordinate of the given {@link GeoCoord} on an image whose image-center is at the given {@link GeoCoord}.
	 * @param geoCoord - the geo-coordinate whose pixel-coordinate is requested
	 * @param imageCenter - the geo-coordinate that represents the center of the image
	 * @param halfImgSize - half oft the length of one edge of the image (assuming an image that is quadratic)
	 * @param zoom - level-of-detail/ zoom-level
	 * @return
	 */
	public static Point2D geoCoordToPixelCoordOnImage( GeoCoord geoCoord, GeoCoord imageCenter, int halfImgSize, int zoom )
	{
		// determine the pixel-coordinate of the GeoCoord on the world-map 
		Point2D pixCoordOnWorldMap = geoCoordToPixelOnWorldMap( geoCoord, zoom );
		// determine the pixel-coordinate of the image center on the world map
		Point2D pixCoordOfImageCenterOnWorldMap = MercatorProjection.geoCoordToPixelOnWorldMap( imageCenter, zoom );

		// compute the pixel-coordinate on the image
		double px = halfImgSize + ( pixCoordOnWorldMap.getX( ) - pixCoordOfImageCenterOnWorldMap.getX( ) );
		double py = halfImgSize + ( pixCoordOnWorldMap.getY( ) - pixCoordOfImageCenterOnWorldMap.getY( ) );
		return new Point2D.Double( px, py );
	}

	/**
	 * Returns the {@link GeoCoord} for the given pixel-coordinate (pixel on the world-map image at current zoom level).
	 * @param pixelCoord
	 * @param zoom
	 * @return
	 */
	public static GeoCoord pixelCoordOnWorldMapToGeoCoord( Point2D pixelCoord, int zoom )
	{
		int mapSize = getMapSize( zoom );

		double pixelX = pixelCoord.getX( );
		double pixelY = pixelCoord.getY( );

		// clip to map
		pixelX = max( min( pixelX, mapSize - 1 ), 0 );
		pixelY = max( min( pixelY, mapSize - 1 ), 0 );

		pixelX = ( pixelX / mapSize ) - 0.5d;
		pixelY = 0.5d - ( pixelY / mapSize );

		double latitude = 90d - 360d * atan( exp( -pixelY * 2d * PI ) ) / PI;
		double longitude = 360d * pixelX;

		return new GeoCoord( latitude, longitude );
	}

	/**
	 * Converts the pixel in image-coordinates to {@link GeoCoord}.
	 * @param pixelCoord - pixel in image-coordinates
	 * @param imageCenter - the geo-coordinate that represents the center of the image
	 * @param halfImgSize - half oft the length of one edge of the image (assuming an image that is quadratic)
	 * @param zoom - level-of-detail/ zoom-level
	 * @return
	 */
	public static GeoCoord pixelCoordOnImageToGeoCoord( Point2D pixelCoord, GeoCoord imageCenter, int halfImgSize, int zoom )
	{
		// compute the pixel-coordinate of the image-center (on world-map image) 
		Point2D pixCoordOfImageCenterOnWorldMap = geoCoordToPixelOnWorldMap( imageCenter, zoom );

		// convert the pixel-coordinate in image-coordinates into world-coordinates
		double pWorldX = pixCoordOfImageCenterOnWorldMap.getX( ) + pixelCoord.getX( ) - halfImgSize;
		double pWorldY = pixCoordOfImageCenterOnWorldMap.getY( ) + pixelCoord.getY( ) - halfImgSize;
		Point2D pxWorld = new Point2D.Double( pWorldX, pWorldY );

		return MercatorProjection.pixelCoordOnWorldMapToGeoCoord( pxWorld, zoom );
	}

	/**
	 * Returns the radius of the earth (at equator) in meters.
	 * @return
	 */
	public static int getEarthRadius( )
	{
		return EARTH_RADIUS;
	}

	/**
	 * Returns the ground resolution at given zoom-level. The ground resolution indicates the distance on the ground that’s represented by a
	 * single pixel in the map. For example,
	 * at a ground resolution of 10 meters/pixel, each pixel represents a ground distance of 10 meters. The ground resolution varies
	 * depending on the level of detail and the
	 * latitude at which it’s measured.
	 * @param latitude - the latitude the resolution should be computed for (in degree)
	 * @param zoom - the current zoom-level
	 * @return
	 */
	public static double getGroundResolution( double latitude, int zoom )
	{
		// [Docu from BING-Maps] ground resolution = cos(latitude * pi/180) * earth circumference / map width 
		// = (cos(latitude * pi/180) * 2 * pi * 6378137 meters) / (256 * 2 level pixels)
		double groundResolution = cos( toRadians( latitude ) );
		groundResolution = groundResolution * 2d * PI * getEarthRadius( );
		groundResolution = groundResolution / getMapSize( zoom );

		return groundResolution;
	}

	/**
	 * The map scale indicates the ratio between map distance and ground distance, when measured in the same units. For instance, at a map
	 * scale of 1 : 100,000, each inch on the
	 * map represents a ground distance of 100,000 inches. Like the ground resolution, the map scale varies with the level of detail and the
	 * latitude of measurement.
	 * @param latitude - the latitude the resolution should be computed for (in degree)
	 * @param zoom - the current zoom-level
	 * @param screenResolution - resolution of the screen in dpi
	 * @return
	 */
	public static double getMapScale( double latitude, int zoom, int screenResolution )
	{
		//[Docu from BING-Maps] map scale = 1 : ground resolution * screen dpi / 0.0254 meters/inch
		// = 1 : (cos(latitude * pi/180) * 2 * pi * 6378137 * screen dpi) / (256 * 2 level * 0.0254)
		double mapScale = getGroundResolution( latitude, zoom ) * screenResolution;
		mapScale = mapScale / METER_PER_INCH;
		return mapScale;
	}

	/**
	 * Computes the map scale at typical 96dpi. See {@link MercatorProjection#getMapScale(double, int, int)}
	 * @param latitude
	 * @param zoom
	 * @return
	 */
	public static double getMapScale( double latitude, int zoom )
	{
		return getMapScale( latitude, zoom, 96 );
	}

	/**
	 * Returns the size (in pixel) of the whole map at given zoom-level.
	 * @param zoom - the current zoom-level
	 * @return
	 */
	public static int getMapSize( int zoom )
	{
		// Compute number of pixels of one edge of the map, depending on current zoom-level.
		// The map is 512x512 pixel at level 1, 1024x1024 at level 2, ...
		// mapheight = mapwitdh = 256 * 2^zoom
		int size = ( int ) ( 256 * Math.pow( 2, zoom ) );
		return size;
	}

	/**
	 * Computes the coordinate of the pixel that belongs to the given {@link GeoCoord} within the image (tile) that contains the given
	 * {@link GeoCoord}.
	 * @param geoCoord - the {@link GeoCoord}
	 * @param zoom - the current zoom level
	 * @return
	 * @deprecated Can only be used on a tile-based system. Unfortunately the static-maps API returns the image not based/matched in its
	 *             tile-system-grid but returns an image that
	 *             is centered at the requested geo-coordinate. Use
	 *             {@link MercatorProjection#geoCoordToPixelCoordOnImage(GeoCoord, GeoCoord, int, int)} instead.
	 */
	public static Point2D geoCoordToPixelCoord( GeoCoord geoCoord, int zoom )
	{
		// determine the tile (TileNumber) that contains the given GeoCoord
		TileNumber tileNumber = geoCoordToTileNumber( geoCoord, zoom );

		// the fractional part of the TileNumber can be assumed to be the percentage 
		// the GeoCoord is placed within the corresponding tile 
		double x = ( tileNumber.getXFrac( ) * Tile.TILE_SIZE_PX );
		double y = ( tileNumber.getYFrac( ) * Tile.TILE_SIZE_PX );

		return new Point2D.Double( x, y );
	}

	/**
	 * Computes the {@link GeoCoord} out of a given {@link TileNumber}.
	 * @param tileNumber - the {@link TileNumber}
	 * @param zoom - the current zoom level
	 * @return
	 * @deprecated Can only be used on a tile-based system. Unfortunately the static-maps API returns the image not based/matched in its
	 *             tile-system-grid but returns an image that
	 *             is centered at the requested geo-coordinate.
	 */
	public static GeoCoord tileNumberToGeoCoord( TileNumber tileNumber, int zoom )
	{
		double n = pow( 2, zoom );
		double longitude = ( ( tileNumber.getX( ) / n ) * 360d ) - 180d;
		double latitudeRadian = tileNumber.getY( ) / n;
		latitudeRadian = latitudeRadian * 2d * PI;
		latitudeRadian = PI - latitudeRadian;
		latitudeRadian = atan( sinh( latitudeRadian ) );
		double latitude = toDegrees( latitudeRadian );
		return new GeoCoord( latitude, longitude );
	}
}
