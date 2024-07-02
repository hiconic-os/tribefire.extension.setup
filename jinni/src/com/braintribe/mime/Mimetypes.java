// ============================================================================
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
// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
// 
// This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public License along with this library; See http://www.gnu.org/licenses/.
// ============================================================================
package com.braintribe.mime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Mimetypes {
	public static Mimetype parse(String mimeType) {
		String parts[] = mimeType.split("(,|;)");
		Map<String, String> params = null;

		String simpleMimeType = null;

		boolean first = true;
		for (String part : parts) {
			if (first) {
				first = false;
				simpleMimeType = part.trim();
			} else {
				part = part.trim();
				int index = part.indexOf('=');
				if (index != -1) {
					String key = part.substring(0, index);
					String value = part.substring(index + 1);

					if (params == null)
						params = new HashMap<String, String>();
					params.put(key, value);
				}
			}
		}

		return new Mimetype(simpleMimeType, params != null ? params : Collections.emptyMap());
	}
}
