/*
 * Copyright (C) 2013 ThObe. All rights reserved. Author: Thomas Obenaus EMail: thobepro@gmail.com Project: MapView
 */

package thobe.mapview.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CallableFutures
{
	private static final int	NTHREDS	= 10;

	public static void main( String[] args )
	{

		ExecutorService executor = Executors.newFixedThreadPool( NTHREDS );
		List<Future<Long>> list = new ArrayList<Future<Long>>( );
		for ( int i = 0; i < 20; i++ )
		{
			Callable<Long> worker = new MyCallable( );
			Future<Long> submit = executor.submit( worker );
			list.add( submit );
		}
		long sum = 0;
		System.out.println( list.size( ) );
		// now retrieve the result
		for ( Future<Long> future : list )
		{
			try
			{
				sum += future.get( );
				System.out.println( sum  );
			}
			catch ( InterruptedException e )
			{
				e.printStackTrace( );
			}
			catch ( ExecutionException e )
			{
				e.printStackTrace( );
			}
		}
		System.out.println( sum );
		executor.shutdown( );
	}
}

class MyCallable implements Callable<Long>
{
	@Override
	public Long call( ) throws Exception
	{
		long sum = 0;
		for ( long i = 0; i <= 100; i++ )
		{
			sum += i;			
		}
		Thread.sleep( 5000 );
		return sum;
	}

}