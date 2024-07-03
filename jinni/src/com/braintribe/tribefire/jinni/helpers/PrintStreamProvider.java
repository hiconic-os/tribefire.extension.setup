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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;

import com.braintribe.utils.stream.KeepAliveDelegateOutputStream;
import com.braintribe.utils.stream.PrintStreamWriter;

public class PrintStreamProvider implements OutputProvider {
	private final PrintStream stream;

	public PrintStreamProvider(PrintStream stream) {
		this.stream = stream;
	}

	@Override
	public OutputStream openOutputStream() throws IOException {
		return new KeepAliveDelegateOutputStream(stream);
	}

	@Override
	public Writer openOutputWriter(String charset, boolean explicitCharset) throws IOException {
		if (explicitCharset) {
			return new OutputStreamWriter(openOutputStream(), charset);
		} else {
			return new PrintStreamWriter(stream, true);
		}
	}
}
