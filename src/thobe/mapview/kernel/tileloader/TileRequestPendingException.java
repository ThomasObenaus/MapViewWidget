/*
 * Copyright (C) 2013 ThObe. All rights reserved. Author: Thomas Obenaus EMail: thobepro@gmail.com Project: MapView
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
	public TileRequestPendingException()
	{
	}
}
