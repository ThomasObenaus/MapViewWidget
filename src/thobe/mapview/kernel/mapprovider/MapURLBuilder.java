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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import thobe.mapview.kernel.tilesystem.GeoCoord;

/**
 * Abstract class for creating an url for loading a static map-image (depends on the map-provider).
 * @author Thomas Obenaus
 */
public abstract class MapURLBuilder
{

	/**
	 * Create a {@link URL} from which a static map-image can be loaded.
	 * @param center - the {@link GeoCoord} of the center of the image that should be loaded
	 * @param zoomLevel - the zoom-level the image should have
	 * @param width - the width of the image
	 * @param height - the height of the image
	 * @param mapType - the type of the map
	 * @return
	 * @throws MalformedURLException
	 */
	public abstract URL buildURL( GeoCoord center, int zoomLevel, int width, int height, MapType mapType ) throws MalformedURLException;

	/**
	 * Create a {@link URL} from which a static map-image can be loaded.
	 * @param center - the {@link GeoCoord} of the center of the image that should be loaded
	 * @param zoomLevel - the zoom-level the image should have
	 * @param width - the width of the image
	 * @param height - the height of the image
	 * @param mapType - the type of the map
	 * @param markers - List of {@link Marker}s where flags/marker should be drawn.
	 * @return
	 * @throws MalformedURLException
	 */
	public abstract URL buildURL( GeoCoord center, int zoomLevel, int width, int height, MapType mapType, List<Marker> markers ) throws MalformedURLException;

	/**
	 * Create a {@link URL} from which a static map-image (with {@link MapType#ROADMAP}) can be loaded.
	 * @param center - the {@link GeoCoord} of the center of the image that should be loaded
	 * @param zoomLevel - the zoom-level the image should have
	 * @param width - the width of the image
	 * @param height - the height of the image
	 * @param mapType - the type of the map
	 * @param markers - List of {@link Marker}s where flags/marker should be drawn.
	 * @return
	 * @throws MalformedURLException
	 */
	public URL buildURL( GeoCoord center, int zoomLevel, int width, int height, List<Marker> markers ) throws MalformedURLException
	{
		return this.buildURL( center, zoomLevel, width, height, MapType.ROADMAP, markers );
	}

	/**
	 * Create a {@link URL} from which a static map-image (with {@link MapType#ROADMAP}) can be loaded.
	 * @param center
	 * @param zoomLevel
	 * @param width
	 * @param height
	 * @return
	 * @throws MalformedURLException
	 */
	public URL buildURL( GeoCoord center, int zoomLevel, int width, int height ) throws MalformedURLException
	{
		return this.buildURL( center, zoomLevel, width, height, MapType.ROADMAP );
	}

	/**
	 * Returns the corresponding {@link MapProvider}.
	 * @return
	 */
	public abstract MapProvider getProvider( );

	/**
	 * Inner class representing a URL-query. By adding queryparameters the corresponding query-part of the url can be retrieved via
	 * toString().
	 * @author Thomas Obenaus
	 */
	protected class URLQuery
	{
		private Map<String, String>	queryParameters;

		public URLQuery( )
		{
			this.queryParameters = new HashMap<>( );
		}

		/**
		 * Add a new parameter (key-value pair) to the {@link URLQuery}
		 * @param key
		 * @param value
		 * @return - See return value of {@link Map#put(Object, Object)}
		 */
		public String addParameter( String key, String value )
		{
			return this.queryParameters.put( key, value );
		}

		public String addParameter( String key, Integer value )
		{
			return this.addParameter( key, value.toString( ) );
		}

		public String addParameter( String key, Double value )
		{
			return this.addParameter( key, value.toString( ) );
		}

		public String addParameter( String key, Boolean value )
		{
			return this.addParameter( key, value.toString( ) );
		}

		@Override
		public String toString( )
		{
			if ( this.queryParameters.size( ) == 0 )
				return "";

			String result = "?";

			boolean first = true;
			for ( Map.Entry<String, String> paramEntry : this.queryParameters.entrySet( ) )
			{
				String key = paramEntry.getKey( );
				String value = paramEntry.getValue( );

				// add key-value only if both are valid (available) 
				if ( ( key != null ) && ( key.trim( ).length( ) > 0 ) && ( value != null ) && ( value.trim( ).length( ) > 0 ) )
				{
					if ( first )
						first = false;
					else result += "&";
					result += key + "=" + value;
				}
			}

			return result;
		}
	}
}
