/*
 * Copyright (c) 2026, drippybacon
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package dev.orbonly;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class OrbPackagingTest
{
	@Test
	public void standardResourcesAndBuiltJarContainExactRootNotices() throws IOException
	{
		try (ZipFile jar = new ZipFile(System.getProperty("orbonly.jar")))
		{
			for (String name : new String[]{"LICENSE", "THIRD-PARTY-NOTICES.md"})
			{
				byte[] original = Files.readAllBytes(Path.of(name));
				assertArrayEquals(name, original, Files.readAllBytes(Path.of("src/main/resources/META-INF", name)));
				assertNotNull(name, jar.getEntry("META-INF/" + name));
				try (InputStream stream = jar.getInputStream(jar.getEntry("META-INF/" + name)))
				{
					assertArrayEquals(name, original, stream.readAllBytes());
				}
			}
		}
	}

	@Test
	public void developmentAndTestDependenciesAreNotShipped() throws IOException
	{
		try (ZipFile jar = new ZipFile(System.getProperty("orbonly.jar")))
		{
			assertNull(jar.getEntry("dev/orbonly/OrbOnlyDev.class"));
			assertNull(jar.getEntry("dev/orbonly/OrbPackagingTest.class"));
			assertNull(jar.getEntry("org/junit/Test.class"));
			assertNull(jar.getEntry("org/mockito/Mockito.class"));
			assertNull(jar.getEntry("dev/orbonly/OrbBackgroundOverlay.class"));
		}
	}
}
