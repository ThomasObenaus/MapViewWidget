/*
 *  Copyright (C) 2013, Thomas Obenaus. All rights reserved.
 *  Licensed under the New BSD License (3-clause lic)
 *  See attached license-file.
 *
 *	Author: 	Thomas Obenaus
 *	EMail:		obenaus.thomas@gmail.com
 *  Project:    MapViewWidget
 */
package thobe.mapview.kernel.mapprovider;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import thobe.mapview.kernel.tilesystem.GeoCoord;

/**
 * {@link MapURLBuilder} implementation (for GoogleMaps) that enables to build {@link URL}s for loading static map-images from
 * http://maps.google.com/maps/api/staticmap.
 * @author Thomas Obenaus
 */
public class GoogleMapURLBuilder extends MapURLBuilder
{
	public static final String	URLBase	= "http://maps.google.com/maps/api/staticmap";

	@Override
	public URL buildURL( GeoCoord center, int zoomLevel, int width, int height, MapType mapType ) throws MalformedURLException
	{
		URLQuery query = new URLQuery( );
		query.addParameter( "zoom", zoomLevel );
		query.addParameter( "center", center.toString( ) );
		query.addParameter( "maptype", "roadmap" );
		query.addParameter( "size", width + "x" + height );
		query.addParameter( "sensor", false );

		return new URL( URLBase + query );
	}

	@Override
	public MapProvider getProvider( )
	{
		return MapProvider.GOOGLE;
	}

	@Override
	public URL buildURL( GeoCoord center, int zoomLevel, int width, int height, MapType mapType, List<Marker> markers ) throws MalformedURLException
	{
		URLQuery query = new URLQuery( );
		query.addParameter( "zoom", zoomLevel );
		query.addParameter( "center", center.toString( ) );
		query.addParameter( "maptype", mapType.toString( ) );
		query.addParameter( "size", width + "x" + height );

		if ( !markers.isEmpty( ) )
		{
			String markerStr = "";
			int markersProcessed = 0;
			for ( Marker marker : markers )
			{
				markerStr += "color:" + marker.getColorHex( ) + "%%7Clabel:" + marker.getLabel( ) + "%%7C" + marker.getPosition( );
				markersProcessed++;

				// add separator
				if ( markersProcessed < markers.size( ) )
				{
					markerStr += "%%7C";
				}// if ( markersProcessed < markers.size( ) ).
			}// for ( Marker marker : markers ).
			query.addParameter( "markers", markerStr );
		}//	if ( !markers.isEmpty( ) ). 

		query.addParameter( "sensor", false );
		return new URL( URLBase + query );
	}
}
