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

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Neutral;

/**
 * Restores backed up artifacts. See backup-artifacts command.
 */
@Description("Check group.")
public interface CheckGroup extends SetupRequest {

	String groupFolder = "groupFolder";
	String enableFixes = "enableFixes";

	EntityType<CheckGroup> T = EntityTypes.T(CheckGroup.class);

	@Description("The folder of the group to be checked. By default, current working directory will be used.")
	@Initializer("'.'")
	@Mandatory
	String getGroupFolder();
	void setGroupFolder(String groupFolder);

	@Description("Whether to apply fixes for failed checks.")
	boolean getEnableFixes();
	void setEnableFixes(boolean enableFixes);	

	@Override
	EvalContext<? extends Neutral> eval(Evaluator<ServiceRequest> evaluator);

}
