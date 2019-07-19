package com.compuware.ispw.git;

import java.io.IOException;
import java.io.OutputStream;
import com.squareup.tape2.ObjectQueue.Converter;

public class GitInfoConverter implements Converter<GitInfo>
{
	public static String UTF_8 = "UTF-8";

	@Override
	public GitInfo from(byte[] source) throws IOException
	{
		return GitInfo.parse(new String(source, UTF_8));
	}

	@Override
	public void toStream(GitInfo value, OutputStream sink) throws IOException
	{
		sink.write(value.toString().getBytes(UTF_8));
	}

}
