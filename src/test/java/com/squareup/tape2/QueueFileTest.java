package com.squareup.tape2;

import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.compuware.ispw.git.GitInfo;
import com.compuware.ispw.git.GitInfoConverter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QueueFileTest
{

	private static final String UTF_8 = "UTF-8";
	private static String LINE1 = "line1";
	private static String LINE2 = "line2";
	private static String LINE3 = "line3";

	@Before
	public void setUp() throws Exception
	{
	}

	@After
	public void tearDown() throws Exception
	{
	}

	@Test
	public void testQueue() throws IOException
	{
		File file = new File("queue1.txt");
		if (file.exists())
		{
			file.delete();
		}

		QueueFile queueFile = new QueueFile.Builder(file).build();

		queueFile.add(LINE1.getBytes(UTF_8));
		queueFile.add(LINE2.getBytes(UTF_8));
		queueFile.add(LINE3.getBytes(UTF_8));

		System.out.println(new String(queueFile.peek(), UTF_8));
		queueFile.remove();
		System.out.println(new String(queueFile.peek(), UTF_8));
		queueFile.remove();
		System.out.println(new String(queueFile.peek(), UTF_8));
		queueFile.remove();

		if (file.exists())
		{
			file.delete();
		}
	}

	@Test
	public void testObjectQueue() throws IOException
	{
		File file = new File("queue2.txt");
		if (file.exists())
		{
			file.delete();
		}
		
		QueueFile queueFile = new QueueFile.Builder(file).build();
		GitInfoConverter converter = new GitInfoConverter();
		ObjectQueue<GitInfo> objectQueue = ObjectQueue.create(queueFile, converter);
		
		objectQueue.add(new GitInfo("ref1", "refId1", "hash1"));
		objectQueue.add(new GitInfo("ref2", "refId2", "hash2"));
		objectQueue.add(new GitInfo("ref3", "refId3", "hash3"));
		
		System.out.println(objectQueue.asList());
		
		if (file.exists())
		{
			file.delete();
		}
	}

}
