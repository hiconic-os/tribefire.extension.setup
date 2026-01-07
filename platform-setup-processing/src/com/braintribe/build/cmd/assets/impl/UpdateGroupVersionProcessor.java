// ========================================================================
// Braintribe IT-Technologies GmbH - www.braintribe.com
// Copyright Braintribe IT-Technologies GmbH, Austria, 2002-2021 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission.
// To this file the Braintribe License Agreement applies.
// ========================================================================
package com.braintribe.build.cmd.assets.impl;

import static com.braintribe.console.ConsoleOutputs.println;

import java.io.File;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.braintribe.common.lcd.DOMException;
import com.braintribe.common.lcd.UnreachableCodeException;
import com.braintribe.model.platform.setup.api.UpdateGroupVersion;
import com.braintribe.utils.CollectionTools;
import com.braintribe.utils.DOMTools;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.StringTools;

/**
 * Processes {@link UpdateGroupVersion} requests.
 *
 * @author michael.lafite
 */
public class UpdateGroupVersionProcessor {

	/**
	 * Common super type for {@link EclipseProject} and {@link Pom}.
	 */
	static abstract class Xml {
		File file;
		String path;
		String content;
		Document document;
		Element element;

		private Xml(File file) {
			this.file = file;
			this.path = file.getPath();
			this.content = FileTools.readStringFromFile(file);

			try {
				this.document = DOMTools.parse(content);
			} catch (DOMException e) {
				throw new IllegalStateException(
						"File " + file.getPath() + " is not a valid XML! Parser Exception: " + e.getMessage() + "\nContent:'" + this.content + "'");
			}
			this.element = this.document.getDocumentElement();
		}
		
		void writeBackToFile() {
			String content = StringTools.normalizeLineSeparatorsInTextFileString(DOMTools.toString(document), null);
			FileTools.writeStringToFile(file, content);
		}
	}
	
	/**
	 * Simple representation of an Eclipse <code>.project</code> file. Holds only the data needed for this use case.
	 */
	static class EclipseProject extends Xml {
		String name;

		EclipseProject(File file) {
			super(file);
		}
	}

	/**
	 * Simple representation of a POM. Holds only the data needed for this use case.
	 */
	static class Pom extends Xml {
		String groupId;
		String artifactId;
		int major;
		int minor;
		// the version as string (as specified in the POM in /project/version)
		String versionString;
		// whether or not the POm (still) uses version properties, i.e. /project/properties/major (and minor, nextMinor, revision)
		boolean usesVersionProperties;
		boolean hasParent;
		boolean isParent;
		
		Pom(File file) {
			super(file);
		}
	}

	/**
	 * Simple representation of a major.minor version as it's required for this use case.
	 */
	static class MajorMinorVersion {

		private static final String MAJOR_MINOR_REGEX = "\\d+\\.\\d+";

		int major;
		int minor;

		MajorMinorVersion(String versionString) {
			if (!isValidVersionString(versionString)) {
				throw new IllegalArgumentException("The passed string '" + versionString + "' is not a valid major.minor version string!");
			}
			major = Integer.parseInt(versionString.split("\\.")[0]);
			minor = Integer.parseInt(versionString.split("\\.")[1]);
		}

		MajorMinorVersion(int major, int minor) {
			this.major = major;
			this.minor = minor;
		}

		static boolean isValidVersionString(String versionString) {
			return versionString.matches(MAJOR_MINOR_REGEX);
		}

		@Override
		public String toString() {
			return major + "." + minor;
		}
	}

	/**
	 * The main method which processes the specified {@link UpdateGroupVersion} <code>request</code>.
	 * Returns the new group version as String in <code>[major].[minor]</code> format, e.g. <code>2.1</code>.
	 */
	public static String process(UpdateGroupVersion request) {

		File artifactGroupFolder = new File(request.getGroupFolder());

		Pom parentPom = validateGroupFolder(artifactGroupFolder);

		MajorMinorVersion currentVersion = new MajorMinorVersion(parentPom.major, parentPom.minor);
		MajorMinorVersion newVersion = getNewVersion(currentVersion, request);

		for (File artifactFolder : artifactGroupFolder.listFiles()) {
			if (!artifactFolder.isDirectory() || artifactFolder.getName().startsWith(".")) {
				// ignore files and e.g. also '.git' folder
				continue;
			}

			File artifactPomFile = new File(artifactFolder, "pom.xml");
			if (!artifactPomFile.exists()) {
				println("Ignoring folder " + artifactFolder.getName() + " since it's not a (valid) artifact folder (" + artifactPomFile.getPath()
						+ " doesn't exist).");
				continue;
			}
			println("Processing artifact " + artifactFolder.getName() + ".");

			Pom pom;
			try {
				pom = parsePom(artifactPomFile, currentVersion);

				String initialRevision = "1";
				if (pom.usesVersionProperties) {
					DOMTools.getExistingElementByXPath(pom.element, "/project/properties/major").setTextContent(String.valueOf(newVersion.major));
					DOMTools.getExistingElementByXPath(pom.element, "/project/properties/minor").setTextContent(String.valueOf(newVersion.minor));
					DOMTools.getExistingElementByXPath(pom.element, "/project/properties/nextMinor").setTextContent(String.valueOf(newVersion.minor + 1));
					DOMTools.getExistingElementByXPath(pom.element, "/project/properties/revision").setTextContent(initialRevision);
				} else {
					DOMTools.getExistingElementByXPath(pom.element, "/project/version").setTextContent(newVersion.major + "." + newVersion.minor + "." + initialRevision);
					
					// TODO: this will break when we switch to wider major ranges.
					// (this should be fixed, but in general such POM modifications should also be moved to a central place where it can be reused.)
					String newSelfGroupRange = "[" + newVersion.major + "." + newVersion.minor + "," + newVersion.major + "." + (newVersion.minor + 1) + ")";
					
					if (pom.isParent) {
						Element selfGroupVersionElement = DOMTools.getElementByXPath(pom.element, "/project/properties/V." + pom.groupId);
						if (selfGroupVersionElement != null) {
							selfGroupVersionElement.setTextContent(newSelfGroupRange);
						}
					} else {
						DOMTools.getExistingElementByXPath(pom.element, "/project/parent/version").setTextContent(newSelfGroupRange);
					}
				}

				pom.writeBackToFile();
				
				println("Updated POM " + pom.file.getPath() + ".");
			} catch (RuntimeException e) {
				throw new IllegalStateException("Error while updating " + artifactPomFile.getPath() + ": " + e.getMessage(), e);
			}

			File artifactEclipseProjectFile = new File(artifactFolder, ".project");

			try {
				if (artifactEclipseProjectFile.exists()) {
					EclipseProject eclipseProject = parseEclipseProject(artifactEclipseProjectFile);
					String currentProjectName = eclipseProject.name;
					String newProjectName = getEclipseProjectNameWithoutVersion(currentProjectName, currentVersion, pom.groupId, pom.artifactId);
					if (!currentProjectName.equals(newProjectName)) {
						DOMTools.getExistingElementByXPath(eclipseProject.element, "/projectDescription/name").setTextContent(newProjectName);

						eclipseProject.writeBackToFile();
						println("Updated Eclipse project file " + eclipseProject.file.getPath() + " to change name from '" + currentProjectName
								+ "' to '" + newProjectName + "'.");
					}
				}
			} catch (RuntimeException e) {
				throw new IllegalStateException("Error while updating " + artifactPomFile.getPath() + ": " + e.getMessage(), e);
			}
		}
		return newVersion.toString();
	}

	/**
	 * Parses the specified <code>pomFile</code> and performs some simple validation tasks.
	 *
	 * @param pomFile
	 *            the pom to parse.
	 * @param parentPomVersion
	 *            an (optional) {@link MajorMinorVersion} read from the parent POM. If set, the version in the parsed POM must match this version.
	 * @return the <code>Pom</code> representation of the specified <code>pomFile</code>.
	 */
	static Pom parsePom(File pomFile, MajorMinorVersion parentPomVersion) {
		Pom pom = new Pom(pomFile);

		try {
			pom.artifactId = DOMTools.getExistingElementByXPath(pom.element, "/project/artifactId").getTextContent();
			pom.isParent = pom.artifactId.equals("parent");

			pom.versionString = getVersionString(pom.element);
			pom.usesVersionProperties = pom.versionString.contains("$");

			if (pom.usesVersionProperties) {
				// old POM with version properties -> read major/minor from properties
				pom.major = getIntegerProperty(pom.element, "major");
				pom.minor = getIntegerProperty(pom.element, "minor");
				// also read nextMinor and make sure its value is correct (afterwards not needed)
				int nextMinor = getIntegerProperty(pom.element, "nextMinor");
				if (nextMinor != pom.minor + 1) {
					throw new IllegalStateException("Expected property 'nextMinor' to be set to " + (pom.minor + 1) + ", because minor is set to "
							+ pom.minor + ", but it is set to " + nextMinor + "!");
				}
			} else {
				// new POM wihout version properties -> parse major/minor from version string
				pom.major = Integer.parseInt(StringTools.getSubstringBefore(pom.versionString, "."));
				pom.minor = Integer.parseInt(StringTools.getSubstringBetween(pom.versionString, ".", "."));
			}

			if (parentPomVersion != null) {
				if (pom.major != parentPomVersion.major) {
					throw new IllegalStateException(
							"Major version " + pom.major + " doesn't match major version " + parentPomVersion.major + " of parent POM!");
				}
				if (pom.minor != parentPomVersion.minor) {
					throw new IllegalStateException(
							"Minor version " + pom.minor + " doesn't match minor version " + parentPomVersion.minor + " of parent POM!");
				}
			}

			pom.hasParent = DOMTools.getElementByXPath(pom.element, "/project/parent") != null;

			if (pom.isParent && pom.hasParent)
				throw new IllegalStateException("File " + pomFile.getPath() + " has a 'parent' section although it's the parent artifact's POM!");

			String groupIdXPath = pom.hasParent ? "/project/parent/groupId" : "/project/groupId";
			pom.groupId = DOMTools.getExistingElementByXPath(pom.element, groupIdXPath).getTextContent();

			return pom;

		} catch (RuntimeException e) {
			throw new IllegalStateException("Error while processing " + pomFile.getPath() + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Parses the specified <code>eclipseProjectFile</code> and performs some simple validation tasks.
	 *
	 * @param eclipseProjectFile
	 *            the file to parse.
	 * @return the <code>EclipseProject</code> representation of the specified <code>eclipseProjectFile</code>.
	 */
	private static EclipseProject parseEclipseProject(File eclipseProjectFile) {
		EclipseProject eclipseProject = new EclipseProject(eclipseProjectFile);

		try {
			eclipseProject.name = DOMTools.getExistingElementByXPath(eclipseProject.element, "/projectDescription/name").getTextContent();

			if (eclipseProject.name.trim().isEmpty()) {
				throw new IllegalStateException("No Eclipse project name set!");
			}

		} catch (RuntimeException e) {
			throw new IllegalStateException("Error while processing " + eclipseProjectFile.getPath() + ": " + e.getMessage(), e);
		}
		return eclipseProject;
	}

	/**
	 * Returns the value of the version as string as defined in the POM.
	 */
	private static String getVersionString(Element pom) {
		Element element = DOMTools.getElementByXPath(pom, "/project/version");
		if (element == null) {
			throw new IllegalStateException("Couldn't find version!");
		}
		return element.getTextContent();
	}
	
	/**
	 * Returns the value of integer property, i.e. a property that is supposed to hold an integer, for example
	 * "/project/properties/minor".
	 */
	private static int getIntegerProperty(Element pom, String propertyName) {
		Element propertyElement = DOMTools.getElementByXPath(pom, "/project/properties/" + propertyName);
		if (propertyElement == null) {
			throw new IllegalStateException("Couldn't find property " + propertyName + "!");
		}
		String propertyText = propertyElement.getTextContent();

		int result;
		try {
			result = Integer.parseInt(propertyText);
		} catch (NumberFormatException e) {
			throw new IllegalStateException("Property " + propertyName + " has value '" + propertyText + "' which is not an integer!");
		}
		return result;
	}

	/**
	 * Determines the new version given the specified <code>currentVersion</code> and <code>request</code> settings. For
	 * example, if the current version is 2.3 and request is to increment the minor version, this method return 2.4.
	 */
	static MajorMinorVersion getNewVersion(MajorMinorVersion currentVersion, UpdateGroupVersion request) {
		String version = request.getVersion();
		boolean incrementMajor = request.getIncrementMajor();
		boolean incrementMinor = request.getIncrementMinor();

		if (incrementMajor && incrementMinor) {
			throw new RuntimeException("Please choose either 'incrementMajor' or 'incrementMinor', not both.");
		}
		if (version != null && (incrementMajor || incrementMinor)) {
			throw new RuntimeException(
					"Please either explicitly specify a version via parameter 'version' or choose to increment major or minor (see 'incrementMajor' or 'incrementMinor'), not both.");
		}

		if (!(version != null || incrementMajor || incrementMinor)) {
			throw new RuntimeException(
					"Please either explicitly specify a version via parameter 'version' or choose to increment major or minor (see 'incrementMajor' or 'incrementMinor').");
		}

		MajorMinorVersion result;
		if (version != null) {
			if (!MajorMinorVersion.isValidVersionString(version)) {
				throw new RuntimeException("The specified version '" + version + "' is invalid. Please specify as 'major.minor' version.");
			}
			result = new MajorMinorVersion(version);
		} else {
			if (incrementMinor) {
				result = new MajorMinorVersion(currentVersion.major, currentVersion.minor + 1);
			} else if (incrementMajor) {
				result = new MajorMinorVersion(currentVersion.major + 1, 0);
			} else {
				throw new UnreachableCodeException();
			}
		}
		return result;
	}

	static String getEclipseProjectNameWithoutVersion(String eclipseProjectName, MajorMinorVersion majorMinorVersion, String groupId, String artifactId) {
		String result;
		
		if (eclipseProjectName.matches(".+\\d+\\.\\d+.*")) {
			// Eclipse project name contains a major.minor version, but it might be part of the artifact name ...
			if (eclipseProjectName.equals(artifactId) || (eclipseProjectName.equals(artifactId + " - " + groupId))) {
				// Eclipse project name is either just the artifact id or the default name (with group id)
				// the version we found is part of the artifact id, e.g. oracle-12.0#2.3
				result = eclipseProjectName;
			} else {
				// Eclipse project name really has a major.minor version. let's further check this ...
				String majorMinorVersionString = majorMinorVersion.toString();
				String majorMinorVersionSuffix = "-" + majorMinorVersionString;
				if (eclipseProjectName.endsWith(majorMinorVersionSuffix)) {
					// the Eclipse project name ends with the major.minor version
					// e.g. 'example-setup-2.0' --> remove the suffix '-2.0'
					result = StringTools.getSubstringBeforeLast(eclipseProjectName, majorMinorVersionSuffix);
				} else {
					// the Eclipse project name contains a major.minor version,
					// but either not the one matching the POM and/or not as suffix -> fail
					if (eclipseProjectName.contains(majorMinorVersionString)) {
						throw new IllegalStateException("The eclipse project name '" + eclipseProjectName + "' contains major.minor version '"
								+ majorMinorVersionString + "', but not as expected suffix '" + majorMinorVersionSuffix
								+ "'! This is not supported and must be fixed manually.");
					} else {
						throw new IllegalStateException(
								"The eclipse project name '" + eclipseProjectName + "' contains a major.minor version, but not the expected one '"
										+ majorMinorVersionSuffix + "'! This is must be fixed manually.");
					}
				}
			}
		} else {
			// no version included -> nothing to do
			result = eclipseProjectName;
		}
		return result;
	}

	/**
	 * Validates the specified <code>groupFolder</code>, e.g. makse sure it has a group <code>build.xml</code> file or a
	 * <code>parent</code> artifact.
	 *
	 * @param artifactGroupFolder
	 *            the folder to check.
	 * @return the <code>Pom</code> representation of the <code>parent</code> POM.
	 */
	static Pom validateGroupFolder(File artifactGroupFolder) {
		// make sure artifact folder exists ...
		if (!artifactGroupFolder.exists()) {
			throw new RuntimeException("The specified artifact group folder doesn't exist: " + artifactGroupFolder.getAbsolutePath());
		}

		// ... and its name is a valid group id ...
		// (we get the canonical file in case the path is e.g. '.')
		String groupId = FileTools.getCanonicalFileUnchecked(artifactGroupFolder).getName();

		String wordRegex = "[a-zA-Z][a-zA-Z0-9]*"; // e.g. word
		String multipleWordsRegex = wordRegex + "(-" + wordRegex + ")*"; // e.g. multiple-words
		String groupIdRegex = multipleWordsRegex + "(\\." + multipleWordsRegex + ")+"; // e.g. org.example.my-app

		if (!groupId.matches(groupIdRegex)) {
			throw new RuntimeException(
					"The specified artifact group folder named '" + groupId + "' is not a valid group id (such as 'org.example')!");
		}

		// ... and it contains expected files ...
		List<String> expectedFilePaths = CollectionTools.getList("parent", "parent/pom.xml", "parent/build.xml");
		for (String expectedFilePath : expectedFilePaths) {
			File expectedFile = new File(artifactGroupFolder, expectedFilePath);
			if (!expectedFile.exists()) {
				throw new RuntimeException("The specified artifact group folder doesn't seem to be a valid artifact group folder: "
						+ expectedFile.getAbsolutePath() + " not found!");
			}
		}

		// ... and parent POM is a valid XML ...
		File parentPomFile = new File(artifactGroupFolder, "parent/pom.xml");
		Pom parentPom = parsePom(parentPomFile, null);

		// ... and group id in POM matches the folder name ...
		if (!parentPom.groupId.equals(groupId)) {
			throw new RuntimeException("The group id derived from the group folder '" + groupId
					+ "' doesn't match the one specified in the parent POM '" + parentPom.groupId + "'!");
		}

		// ... and there is no dependency management section.
		if (DOMTools.getElementByXPath(parentPom.element, "/project/dependencyManagement") != null) {
			throw new RuntimeException(
					"The parent POM contains a 'dependencyManagement' section, which shouldn't be used. Instead versions which are used in multiple dependency declarations"
							+ " should be specified in variables in 'properties' section, e.g. '<V.com.braintribe.gm>[1.0,1.1)</V.com.braintribe.gm>' or '<V.org.example>1.2.3</V.org.example>'."
							+ " Please remove the 'dependencyManagement' section before using this helper.");
		}

		return parentPom;
	}
}
