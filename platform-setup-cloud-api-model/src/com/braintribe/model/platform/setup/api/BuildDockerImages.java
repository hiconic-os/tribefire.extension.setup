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
package com.braintribe.model.platform.setup.api;

import java.util.Map;
import java.util.Set;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Neutral;

/**
 * This service request is used to build Docker images for a given platform setup package, which has to be built separately before (see
 * {@link PackagePlatformSetup}).
 *
 * @author michael.lafite
 */
@Description("Builds and optionally pushes Docker images, one for each container in the specified platform setup package."
		+ " Each container must have exactly one webapp, see parameter 'disjointProjection' in request 'package-platform-setup'.")
public interface BuildDockerImages extends SetupRequest {
	EntityType<BuildDockerImages> T = EntityTypes.T(BuildDockerImages.class);

	@Override
	EvalContext<Neutral> eval(Evaluator<ServiceRequest> evaluator);

	// copied from com.braintribe.model.platform.setup.api.SetupDependencyConfig.getSetupDependency()
	@Description("The qualified name of the asset that should be transitively resolved to be effective in the setup."
			+ "The asset will be automatically ranged on the 2nd version level if there is no 3rd version level explicitly defined.")
	String getSetupDependency();
	void setSetupDependency(String setupDependency);

	// copied from com.braintribe.model.platform.setup.api.SetupDependencyConfig.getTags()
	@Description("Sets the tags which can be evaluated by dependency selectors.")
	Set<String> getTags();
	void setTags(Set<String> tags);

	@Description("The base directory files will be downloaded to and where Dockerfiles and other resources will be prepared.")
	@Initializer("'docker'")
	String getBaseDir();
	void setBaseDir(String baseDir);

	@Description("Optional host name of the docker registry to be used, e.g. 'ghcr.io' for GitHub Container Registry. "
			+ "Final image name will be '[<dockerRegistryHost>/[<dockerRegistrySubfolder>/]]<name-derived-from-setup>'.")
	String getDockerRegistryHost();
	void setDockerRegistryHost(String dockerRegistryHost);

	@Description("Optional subfolder on the Docker registry, used as prefix for all image names. "
			+ "Final image name will be '[<dockerRegistryHost>/[<dockerRegistrySubfolder>/]]<name-derived-from-setup>'.")
	String getDockerRegistrySubfolder();
	void setDockerRegistrySubfolder(String subfolder);

	@Description("Specifies the image tag of Docker images to be built by this command.")
	@Initializer("'latest'")
	String getDockerImageTag();
	void setDockerImageTag(String dockerImageTag);

	@Description("Deprecated: This setting is no longer deprecated and no longer used."
			+ " It specifies the image tag of the 'tribefire-base' Docker image which the images to be built depend on (i.e. the FROM image). Example: '2.0-core-stable'")
	String getTribefireBaseDockerImageTag();
	void setTribefireBaseDockerImageTag(String tribefireBaseDockerImageTag);

	@Description("Deprecated: This setting is no longer deprecated and no longer used."
			+ "It specifies the host name of the docker registry from which the base image(s) will be pulled."
			+ " If not set, the main docker registry will be used (see 'dockerRegistryHost')")
	String getTribefireBaseDockerRegistryHost();
	void setTribefireBaseDockerRegistryHost(String tribefireBaseDockerRegistryHost);

	@Description("Whether or not to delete the intermediate tribefire base image built by this request. An intermediate base image is built on-the-fly before building the actual Docker images. This base image is not needed after the building these other images, "
			+ " thus it's automatically deleted. Using this settings one can keep the image analyze it.")
	@Initializer("true")
	boolean getDeleteIntermediateBaseImage();
	void setDeleteIntermediateBaseImage(boolean deleteIntermediateBaseImage);

	@Description("Whether or not to push Docker images after building.")
	boolean getPush();
	void setPush(boolean push);

	@Description("Specifies the base image, i.e. the image used in the FROM instruction of the Dockerfile, e.g. 'debian:12'."
			+ " Note that there is no (global) default for this setting, because different versions of tribefire may use different base images."
			+ " There are defaults in the Dockerfile templates though which match the different versions of tribefire."
			+ " Therefore, it is usually not needed (and not recommended) to use this setting!"
			+ " Its main purpose is to easily test with a slighty older or newer operation system version, for example debian:12.8 vs 12.9,"
			+ " e.g. to see if an OS level vulnerability gets fixed with an update or some problem is caused by a recent version switch."
			+ " Attention: Using a custom base image also means using untested base image! There may be errors either during build or later at runtime"
			+ " and even if a custom image does currently work, updating to newer versions of Jinni may cause issues,"
			+ " e.g. because the Dockerfile instructions now require some tools which are not available or behave differently in the custom base image.")
	String getBaseImage();
	void setBaseImage(String baseImage);

	@Description("Specifies the URL from where to download the JDK archive used to install Java in the Docker image."
			+ " The URL must point to a '.tar.gz' file, which contains a single subfolder in the root folder, e.g. 'jdk-15'. OpenJDK and Zulu packages are known to work."
			+ " Note that there is no (global) default for this setting, because different versions of tribefire may use different JDK versions."
			+ " However, there are defaults in the Dockerfile templates which match the different versions of tribefire. Therefore it is usually not needed to configure this setting."
			+ " It can be useful though in some cases, for example to check whether an issue is caused by the JDK version, e.g. JDK 15.0.1 vs 15.0.2.")
	String getJdkArchiveUrl();
	void setJdkArchiveUrl(String jdkArchiveUrl);

	@Description("Whether or not to pull the base image, if a newer version is available.")
	@Initializer("true")
	boolean getPullUpdatedBaseImage();
	void setPullUpdatedBaseImage(boolean pullUpdatedBaseImage);

	@Description("Whether or not to use Docker's cache when building the Docker images."
			+ " Except for very special cases where one needs to (temporarily) disable the cache, this should always be enabled.")
	@Initializer("true")
	boolean getUseCache();
	void setUseCache(boolean useCache);

	@Description("Whether or not to shorten image names. For example, if the container name in the setup package is "
			+ "'tribefire.extension.example.example-cartridge', the image name will be shortened to 'example-cartridge'.")
	@Initializer("true")
	boolean getShortenImageNames();
	void setShortenImageNames(boolean shortenImageNames);

	@Description("Whether or not to enable 'gosu' which provides temporary root permissions."
			+ " If enabled one can run a command with root permissions via 'gosu root [command]'."
			+ " This can e.g. be used to install further tools for analysis. See also 'enableRootPermissions'.")
	@Initializer("false")
	boolean getEnableGosu();
	void setEnableGosu(boolean enableGosu);

	@Description("Whether or not to enable root permissions, i.e. run the image with user 'root'."
			+ " This can be helpful when analyzing issues, e.g. to install new tools. See also 'enableGosu'.")
	@Initializer("false")
	boolean getEnableRootPermissions();
	void setEnableRootPermissions(boolean enableRootPermissions);

	@Description("Whether or not to install analysis tools. If enabled, certain tools such as htop, nano or curl will be added to the image."
			+ " These tools can be useful when manually connecting to a container to test something or analyze issues."
			+ " The tools are (usually) not needed by tribefire itself though, which means they are optional and not needed to run the service."
			+ " This option is supported for tribefire version 2.2 and later, i.e. starting with tribefire.cortex.services:tribefire-web-platform#2.0.242."
			+ " It is enabled by default, not only because the tools are useful, but also because previous versions also included them."
			+ " Disabling this option creates (slightly) smaller images. Furthermore it may reduce the numbers of vulnerabilities"
			+ " in case there are known vulnerabilities for the respective tools.")
	@Initializer("true")
	boolean getInstallAnalysisTools();
	void setInstallAnalysisTools(boolean installAnalysisTools);

	@Description("Custom Dockerfile instructions to be injected. Map keys specify the image name, map values the instructions."
			+ " These instructions will be run before adding the assets to the Docker image."
			+ " Note that the order of instructions is important for Docker layer optimization." + " See also 'customInstructionsAfterAddingAssets'.")
	Map<String, String> getCustomInstructionsBeforeAddingAssets();
	void setCustomInstructionsBeforeAddingAssets(Map<String, String> customInstructionsBeforeAddingAssets);

	@Description("Custom Dockerfile instructions to be injected. Map keys specify the image name, map values the instructions."
			+ " These instructions will be run after adding the assets to the Docker image."
			+ " Note that the order of instructions is important for Docker layer optimization."
			+ " See also 'customInstructionsBeforeAddingAssets'.")
	Map<String, String> getCustomInstructionsAfterAddingAssets();
	void setCustomInstructionsAfterAddingAssets(Map<String, String> customInstructionsAfterAddingAssets);

	@Description("Rewrite configuration rules to be added to the respective image. Map keys specify the image name, map values the rewrite configuration."
			+ " See also respective parameter in setup-local-tomcat-platform.")
	Map<String, String> getUrlRewriteConfigs();
	void setUrlRewriteConfigs(Map<String, String> urlRewriteConfigs);

	@Description("Whether or not to enable virtual threads in Tomcat. See also respective parameter in setup-local-tomcat-platform.")
	@Initializer("false")
	boolean getEnableVirtualThreads();
	void setEnableVirtualThreads(boolean enableVirtualThreads);

	@Description("If set, only images for the container(s) matching the specified regex will be built and pushed."
			+ " Set e.g. to 'tribefire-master' to only build the master."
			+ " Note that all the preparation steps will still be performed, just the Docker commands will be skipped."
			+ " Therefore this can also be used to e.g. quickly check created Dockerfiles without actually building anything.")
	String getContainerRegex();
	void setContainerRegex(String containerRegex);
}
