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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import thobe.mapview.kernel.tileloader.TileLoaderListener.FailReason;

/**
 * @author Thomas Obenaus
 * @source TileLoader.java
 * @date Nov 24, 2013
 */
public class TileLoader extends Thread
{
	private enum Event
	{
		CANCEL_ALL_REQUESTS, NEW_REQUEST_BLOCK_AVAILABLE, SHUTDOWN;
	};

	private enum State
	{
		IDLE, CANCELLING, LOADING;
	};

	private BlockingQueue<Event>		eventQueue;

	private List<TileLoaderListener>	listeners;
	private List<TileRequest>			runningTileRequests;
	private List<TileRequest>			pendingTileRequests;

	private Boolean						shudownRequested;

	private Logger						log;
	private ExecutorService				executorService;

	private int							numWorkers;

	private State						state;

	public TileLoader( Logger log, int numWorkers )
	{
		this.state = State.IDLE;
		this.log = log;
		this.numWorkers = numWorkers;
		this.executorService = null;
		this.eventQueue = new ArrayBlockingQueue<>( 256 );
		this.shudownRequested = false;
		this.listeners = new ArrayList<>( );
		this.pendingTileRequests = new ArrayList<>( );
		this.runningTileRequests = new ArrayList<>( );

		this.log.info( "TileLoader with " + numWorkers + " workers started." );
	}

	public void shutdown( )
	{
		try
		{
			this.eventQueue.put( Event.SHUTDOWN );
		}
		catch ( InterruptedException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace( );
		}
	}

	@Override
	public void run( )
	{
		while ( !this.shudownRequested )
		{
			try
			{

				// process pending events
				this.processEvents( );

				// check if one of the running requests has terminated
				this.checkRunningRequests( );

			}
			catch ( TileLoaderException | InterruptedException e )
			{
				log.severe( "Unexpected error in executionloop of TileLoader: " + e.getLocalizedMessage( ) );
			}
		}

		log.info( "TileLoader stopped its excecution." );
	}

	private void checkRunningRequests( )
	{
		State oldState = this.state;
		this.state = ( this.runningTileRequests.isEmpty( ) ? State.IDLE : State.LOADING );
		if ( oldState != this.state )
			this.log.fine( "StateChange: " + oldState + " --> " + this.state );

		// find completed TileRequests
		List<TileRequest> completedRequests = new ArrayList<>( );
		for ( TileRequest runningTileRequest : this.runningTileRequests )
		{
			if ( runningTileRequest.isTerminated( ) )
			{
				completedRequests.add( runningTileRequest );
			}
		}

		// notify the listeners
		for ( TileRequest completedRequest : completedRequests )
		{
			this.runningTileRequests.remove( completedRequest );
			if ( completedRequest.isFailed( ) )
			{
				this.fireTileLoadRequestFailed( completedRequest.getTileId( ), FailReason.ERROR, completedRequest.getError( ) );
			}
			else
			{
				this.fireTileLoadRequestComplete( completedRequest.getTileId( ), completedRequest.getImage( ) );
			}
		}

		if ( !completedRequests.isEmpty( ) )
		{
			this.log.fine( completedRequests.size( ) + " requests completed, " + this.runningTileRequests.size( ) + " requests pending." );
		}
	}

	private void processEvents( ) throws TileLoaderException, InterruptedException
	{
		// blocks until an event was pushed into the queue
		Event event = null;
		// in IDLE-state: wait until a new event is pushed into the queue (using take())
		if ( state == State.IDLE )
			event = this.eventQueue.take( );
		// in NON-IDLE-state: poll for a new event, but don't block (using poll())
		else event = this.eventQueue.poll( );

		// no pending event
		if ( event == null )
			return;

		switch ( event )
		{
		case SHUTDOWN:
			log.fine( "Event: SHUTDOWN received." );
			this.shudownRequested = true;
			break;
		case CANCEL_ALL_REQUESTS:
			log.fine( "Event: CANCEL_ALL_REQUESTS received." );
			this.processCancelAllRequests( );
			break;
		case NEW_REQUEST_BLOCK_AVAILABLE:
			log.fine( "Event: NEW_REQUEST_BLOCK_AVAILABLE received." );
			this.processNewRequestBlockAvailable( );
			break;
		default:
			log.severe( "Unexpected evnet:" + event + " will be ignored." );
		}
	}

	private void processNewRequestBlockAvailable( ) throws TileLoaderException
	{

		if ( this.executorService != null || ( !this.runningTileRequests.isEmpty( ) ) )
		{
			throw new TileLoaderException( "There are running TileRequests --> cancel them first using TileLoader.cancelAllRequests()!!!" );
		}

		State oldState = this.state;
		this.state = State.LOADING;
		if ( oldState != this.state )
			this.log.fine( "StateChange: " + oldState + " --> " + this.state );

		// create a new ExecutorService
		this.executorService = Executors.newFixedThreadPool( this.numWorkers );

		synchronized ( pendingTileRequests )
		{
			// copy the pending to the running tile-requests
			for ( TileRequest tileRequest : this.pendingTileRequests )
			{
				this.runningTileRequests.add( tileRequest );
			}
		}

		// start the requests
		for ( TileRequest tileRequest : this.runningTileRequests )
		{
			this.executorService.submit( tileRequest );
			// notify the listeners
			this.fireTileLoadRequestStarted( tileRequest.getTileId( ) );
		}

		// clear the event-queue to suppress multiple requests for new tiles 
		this.eventQueue.clear( );
	}

	private void processCancelAllRequests( ) throws TileLoaderException, InterruptedException
	{
		if ( this.executorService == null )
			return;

		State oldState = this.state;
		this.state = State.CANCELLING;
		if ( oldState != this.state )
			this.log.fine( "StateChange: " + oldState + " --> " + this.state );

		int numRunningRequests = this.runningTileRequests.size( );
		this.log.fine( "Cancelling " + numRunningRequests + " running requests..." );
		// cancel all running tasks, block adding pending ones
		this.executorService.shutdownNow( );

		boolean terminated = false;
		int numTries = 0;
		// wait for termination for at max 5*5 s
		while ( !terminated && ( numTries < 5 ) )
		{
			terminated = this.executorService.awaitTermination( 5, TimeUnit.SECONDS );
			numTries++;
		}

		// unable to terminate the running tasks within 25s --> throw an exception
		if ( !terminated )
		{
			throw new TileLoaderException( "Unable to terminate running TileRequests" );
		}

		this.executorService = null;

		// notify all listeners, for the pending requests
		for ( TileRequest tileRequest : this.runningTileRequests )
		{
			fireTileLoadRequestFailed( tileRequest.getTileId( ), FailReason.CANCELLED, "Cancelled" );
		}
		this.runningTileRequests.clear( );

		this.log.fine( "Cancelling " + numRunningRequests + " running requests...done" );
	}

	public void addTileRequestBlock( List<TileRequest> requestBlock )
	{
		try
		{
			// cancel open/running requests
			synchronized ( this.pendingTileRequests )
			{
				this.pendingTileRequests.clear( );
				for ( TileRequest request : requestBlock )
					this.pendingTileRequests.add( request );
			}

			// start to process the new requests
			this.eventQueue.put( Event.NEW_REQUEST_BLOCK_AVAILABLE );
		}
		catch ( InterruptedException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace( );
		}
	}

	public void cancelAllRequests( )
	{
		try
		{
			this.eventQueue.put( Event.CANCEL_ALL_REQUESTS );
		}
		catch ( InterruptedException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace( );
		}
	}

	public void addListener( TileLoaderListener l )
	{
		this.listeners.add( l );
	}

	public void removeListener( TileLoaderListener l )
	{
		this.listeners.remove( l );
	}

	private void fireTileLoadRequestComplete( String tileId, Image image )
	{
		for ( TileLoaderListener l : this.listeners )
			l.onTileLoadRequestComplete( tileId, image );
	}

	private void fireTileLoadRequestStarted( String tileId )
	{
		for ( TileLoaderListener l : this.listeners )
			l.onTileLoadRequestStarted( tileId );
	}

	private void fireTileLoadRequestFailed( String tileId, FailReason reason, String cause )
	{
		for ( TileLoaderListener l : this.listeners )
			l.onTileLoadRequestFailed( tileId, reason, cause );
	}
}
