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

import java.util.List;

import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;


public interface CreateProject extends SetupRequest {
	
	EntityType<CreateProject> T = EntityTypes.T(CreateProject.class);
	
	@Mandatory
	String getQualifiedName();
	void setQualifiedName(String qualifiedName);
	
	List<String> getDependencies();
	void setDependencies(List<String> dependencies);
	
	List<String> getGlobalSetupCandidates();
	void setGlobalSetupCandidates(List<String> globalSetupCandidates);
}
