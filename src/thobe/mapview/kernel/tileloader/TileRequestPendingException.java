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

/**
 * Class representing an {@link Exception} thrown when accessing the result of a {@link TileRequest} while the request is still pending.
 * @author Thomas Obenaus
 * @source TileRequestPendingException.java
 * @date Sep 22, 2013
 */
@SuppressWarnings ( "serial")
public class TileRequestPendingException extends Exception
{
	public TileRequestPendingException( )
	{}
}
