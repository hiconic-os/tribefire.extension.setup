// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package com.braintribe.tribefire.jinni.helpers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.LogManager;

import com.braintribe.console.ConsoleConfiguration;
import com.braintribe.console.PlainSysoutConsole;
import com.braintribe.console.PrintStreamConsole;
import com.braintribe.console.VoidConsole;
import com.braintribe.logging.ndc.mbean.NestedDiagnosticContext;
import com.braintribe.model.jinni.api.JinniOptions;
import com.braintribe.tribefire.jinni.OutputChannels;
import com.braintribe.tribefire.jinni.logging.JinniLogHandler;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.lcd.StringTools;

/**
 * @author peter.gazdik
 */
public class JinniConfigHelper {

	private static final List<String> FILE_INDICATORS = Arrays.asList(".", "/", "\\", ":");

	public static void turnOffDefaultLogging() {
		try {
			LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(new byte[] {}));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * If logging is activated via jinni option, the channel (stdout,stderr) logging configurations are applied to the {@link LogManager}. Otherwise
	 * logging is turned off totally.
	 */
	public static void configureLogging(JinniOptions options, File installationDir) {
		File loggingPropertiesFile = null;

		String logTo = options.getLog();

		OutputStream out = null;

		if (logTo != null) {
			switch (logTo) {
				case OutputChannels.STDOUT:
					out = System.out;
					break;
				case OutputChannels.STDERR:
					out = System.err;
					break;
				case OutputChannels.NONE:
					break;
				default:
					checkChannelValue(logTo, "log");
					try {
						out = new FileOutputStream(logTo);
					} catch (FileNotFoundException e) {
						throw new UncheckedIOException(e);
					}
					break;
			}
		}

		if (out == null)
			return;

		JinniLogHandler.out = out;

		File confDir = new File(installationDir, "conf");
		loggingPropertiesFile = new File(confDir, "logging.properties");

		/* we call this to avoid getting an initialization problem when using log levels <= FINER /* because the MBeanServer uses FINER logging and
		 * would access an empty NDC during that */
		NestedDiagnosticContext.getInstance();

		LogManager manager = LogManager.getLogManager();
		FileTools.read(loggingPropertiesFile).consumeInputStream(manager::readConfiguration);
	}

	public static void configureDefaultProtocolling() {
		if (isStdoutAnsi())
			ConsoleConfiguration.install(new PrintStreamConsole(System.out));
		else
			ConsoleConfiguration.install(new PlainSysoutConsole());
	}
	
	private static boolean isStdoutAnsi() {
		return System.console() != null && System.console().isTerminal();
	}
	
	private static boolean isStderrAnsi() {
		return isStdoutAnsi();
	}

	public static boolean configureProtocolling(JinniOptions options) {
		String protocolTo = options.getProtocol();

		Boolean mode = options.getColored();
		Predicate<Boolean> colored = mode == null ? b -> b :  b -> mode;

		if (protocolTo != null) {
			switch (protocolTo) {
				case OutputChannels.STDOUT:
					return ensureCharsetAndInstallProtocolOutput(System.out, colored.test(isStdoutAnsi()), options);
				case OutputChannels.STDERR:
					return ensureCharsetAndInstallProtocolOutput(System.err, colored.test(isStderrAnsi()), options);
				case OutputChannels.NONE:
					ConsoleConfiguration.install(VoidConsole.INSTANCE);
					return false;
				default:
					checkChannelValue(protocolTo, "protocol");

					try {
						return ensureCharsetAndInstallProtocolOutput(new PrintStream(new FileOutputStream(protocolTo), colored.test(false), "UTF-8"), false,
								options);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
			}
		} else {
			ConsoleConfiguration.install(VoidConsole.INSTANCE);
			return false;
		}
	}

	private static boolean ensureCharsetAndInstallProtocolOutput(PrintStream ps, boolean ansiConsole, JinniOptions options) {
		try {
			if (options.getProtocolCharset() != null) {
				ps = new PrintStream(ps, false, options.getProtocolCharset());
			} // For files and in MINGW_XTERM (e.g. Git Bash) we set UTF-8 by default; maybe also cygwin?
			else if (ps == System.out) {
				if (System.console() == null) {
					ps = new PrintStream(ps, false, StandardCharsets.UTF_8);
				}
			} 
			PrintStreamConsole console = new PrintStreamConsole(ps, ansiConsole);
			ConsoleConfiguration.install(console);
			
			return ansiConsole;

		} catch (UnsupportedEncodingException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static OutputProvider configureResponding(JinniOptions options) {
		String respondTo = options.getResponse();
		if (respondTo == null)
			return null;

		switch (respondTo) {
			case OutputChannels.STDOUT:
				return new PrintStreamProvider(System.out);

			case OutputChannels.STDERR:
				return new PrintStreamProvider(System.err);

			case OutputChannels.NONE:
				return null;

			default:
				checkChannelValue(respondTo, "response");

				return new FileOutputStreamProvider(new File(respondTo));
		}
	}

	private static void checkChannelValue(String value, String parameterName) {
		if (!StringTools.containsAny(value, FILE_INDICATORS)) {
			throw new IllegalArgumentException(
					"Unknown channel value for parameter -" + parameterName + ". Did you you mean a file? Then use \"." + File.separator + value
							+ "\". Channel values are: " + OutputChannels.STDOUT + ", " + OutputChannels.STDERR + ", " + OutputChannels.NONE);
		}
	}

}
