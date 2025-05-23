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
package com.braintribe.doc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Iterator;

import com.braintribe.codec.marshaller.yaml.YamlMarshaller;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.essential.ParseError;
import com.braintribe.model.asset.PlatformAsset;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.Validator;
import com.braintribe.utils.IOTools;
import com.vladsch.flexmark.ast.Document;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.toc.internal.TocOptions;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlRenderer.Builder;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.options.MutableDataSet;

public abstract class DocUtils {
	public static final int ASSET_FOLDER_DEPTH = 2;

	public static final MutableDataHolder FLEXMARK_OPTIONS_INCLUDE = new MutableDataSet() //
			.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create()));
	
	public static final MutableDataHolder FLEXMARK_OPTIONS = new MutableDataSet() //
			.set(AnchorLinkExtension.ANCHORLINKS_SET_NAME, true) //
			.set(AnchorLinkExtension.ANCHORLINKS_SET_ID, false) //
			.set(AnchorLinkExtension.ANCHORLINKS_WRAP_TEXT, false) //
			.set(AnchorLinkExtension.ANCHORLINKS_ANCHOR_CLASS, "anchor-auto-link") //
			.set(TocExtension.LEVELS, TocOptions.getLevels(2, 3, 4, 5, 6)) //
			.set(TocExtension.IS_NUMBERED, true) //
			.set(TocExtension.TITLE, "On this page") //
			.set(TocExtension.DIV_CLASS, "toc") //
			.set(TocExtension.TITLE_LEVEL, 2) //
			.set(Parser.EXTENSIONS, Arrays.asList(AnchorLinkExtension.create(), TocExtension.create(), TablesExtension.create()));

	private static final Builder builder = HtmlRenderer.builder(FLEXMARK_OPTIONS);
	private static final Builder builderInclude = HtmlRenderer.builder(FLEXMARK_OPTIONS_INCLUDE);
	
	public static final HtmlRenderer FLEXMARK_RENDERER = builder.build();
	public static final HtmlRenderer FLEXMARK_RENDERER_INCLUDE = builderInclude.build();
	public static final Parser FLEXMARK_PARSER = Parser.builder(FLEXMARK_OPTIONS).build();

	private static Document parse(File markdownFile, Parser parser) {
		
		try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(markdownFile), "UTF-8"), IOTools.SIZE_16K)) {
			Document document = parser.parseReader(reader);
			return document;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	public static Document simpleParse(File markdownFile) {
		return parse(markdownFile, FLEXMARK_PARSER);
	}
	
	public static String getAssetFromPath(UniversalPath path) {
		if (path.getNameCount() < ASSET_FOLDER_DEPTH) {
			throw new IllegalArgumentException(
					"Cannot determine asset from path: There must be at least one folder for groupId and one for the asset name: " + path);
		}

		Iterator<String> iterator = path.iterator();

		String groupId = iterator.next();
		String assetName = iterator.next();

		return groupId + ":" + assetName;
	}
	
	public static UniversalPath getPathFromAsset(PlatformAsset asset) {
		return UniversalPath.empty()
			.push(asset.getGroupId())
			.push(asset.getName());
	}

	public static UniversalPath getAssetRelativePath(UniversalPath path) {
		int nameCount = path.getNameCount();

		if (nameCount < ASSET_FOLDER_DEPTH) {
			throw new IllegalArgumentException(
					"Cannot determine asset from path: There must be at least one folder for groupId and one for the asset name: " + path);
		}

		return path.subpath(ASSET_FOLDER_DEPTH, nameCount);
	}
	
	static String toHtmlName(String name) {
		return name.replaceAll("\\.md$", ".html");
	}

	public static <T extends GenericEntity> T parseYamlFile(File file, EntityType<T> rootType) {
		YamlMarshaller yamlMarshaller = new YamlMarshaller();
		try {
			Maybe<T> maybe = (Maybe<T>)(Maybe<?>) yamlMarshaller.unmarshallReasoned(new FileInputStream(file));
			
			if (maybe.isUnsatisfiedBy(ParseError.T))
				throw new MarkdownParsingException("Invalid Yaml " + file.getPath() + ": " + maybe.whyUnsatisfied().stringify()); 
			
			T parsedEntity = maybe.get();

			GmMetaModel docMetadataModel = GMF.getTypeReflection().getModel("tribefire.extension.setup:documentation-metadata-model").getMetaModel();
			Validator validator = Validator.create(rootType, docMetadataModel);
			validator.validate(parsedEntity);

			return parsedEntity;
		} catch (ClassCastException e) {
			throw new MarkdownParsingException("Yaml file does not comply to the expected format. This is either because you didn't declare the root type or because of an unexpected entry: " + file.getPath(),
					e);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(
					"Tried to parse Yaml file but it does not exist - existance should have been checked before: " + file.getAbsolutePath(), e);
		} catch (Exception e) {
			throw new RuntimeException("Unexpected exception while parsing " + file.getPath() + ":" + e.getMessage(), e);
		}
	}

}
