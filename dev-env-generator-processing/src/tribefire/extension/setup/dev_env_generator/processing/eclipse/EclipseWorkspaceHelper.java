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
package tribefire.extension.setup.dev_env_generator.processing.eclipse;

import java.io.File;
import java.util.Map;

import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling;
import com.braintribe.gm.model.reason.essential.IoError;
import com.braintribe.utils.FileTools;

public abstract class EclipseWorkspaceHelper {

	private File cfgFile;
	private String content;

	EclipseWorkspaceHelper(File devEnv, String folder, String name, String content) {
		this.content = content;
		this.cfgFile = new File(devEnv + "/eclipse-workspace/" + folder + "/" + name);
	}

	public File getCfgFile() {
		return cfgFile;
	}
	public boolean exists() {
		return cfgFile.exists();
	}

	public void create(Map<String, String> substitutions) {
		String finalContent = getContent(content, substitutions);
		writeFile(cfgFile, finalContent);
	}

	/**
	 * Patches the content of an existing template file with additional information.
	 * 
	 * 
	 * @param custom
	 *            String to be added to template data (prepended)
	 * @param substitutions
	 *            Map of string-to-string to be replaced in original data template. This can be used for replacements, but
	 *            also to delete specific lines/data.
	 */
	public void patch(String custom, Map<String, String> substitutions) {
		String fileContent = readFile(cfgFile);
		String contentFinal = getContent(fileContent, substitutions);
		String text = custom + "\n" + contentFinal;
		writeFile(cfgFile, text);
	}

	private String getContent(String data, Map<String, String> substitutions) {
		String contentFinal = data;
		for (Map.Entry<String, String> entry : substitutions.entrySet()) {
			String key = entry.getKey();
			String val = entry.getValue();
			contentFinal = contentFinal.replaceAll(key, val);
		}
		return contentFinal;
	}

	private void writeFile(File file, String content) {
		try {
			FileTools.write(file).string(content);

		} catch (Exception e) {
			throw new UnsatisfiedMaybeTunneling(Reasons.build(IoError.T).text(e.getMessage()).toMaybe());
		}
	}

	private String readFile(File file) {
		return FileTools.read(file).asString();
	}

}
