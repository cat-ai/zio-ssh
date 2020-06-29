package io.cat.ai.zio.ssh.inout;

import java.io.IOException;
import java.io.InputStream;

import com.jcraft.jsch.Channel;

// FIXME
public class JSshChannelReader {

	private static int OFFSET = 0;
	private static int LENGTH = 1 << 10;

	private static byte[] allocByteArray(int len) {
		return new byte[len];
	}

	public static String read(Channel channel) throws IOException {
		InputStream inputStream = channel.getInputStream();

		final byte[] byteArray = allocByteArray(LENGTH);
		final StringBuilder buffer = new StringBuilder();

		while (true) {
			while (inputStream.available() > 0) {
				int i = inputStream.read(byteArray, OFFSET, LENGTH);
				if (i < 0) {
					break;
				}
				buffer.append(new String(byteArray, OFFSET, i));
			}

			final String output = buffer.toString();

			if (output.contains("object")) {
				break;
			}

			if (channel.isClosed()) {
				if (inputStream.available() > OFFSET) {
					int i = inputStream.read(byteArray, OFFSET, LENGTH);
					buffer.append(new String(byteArray, OFFSET, i));
				}
				break;
			}

			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}

		return buffer.toString();
	}
}