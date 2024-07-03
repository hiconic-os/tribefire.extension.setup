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
package com.braintribe.model.jinni.api;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Alias;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.PositionalArguments;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.PlatformRequest;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Neutral;

@PositionalArguments({"type"})
@Description("Prints details for a specified type (e.g. command).")
public interface Help extends PlatformRequest {
	EntityType<Help> T = EntityTypes.T(Help.class);

	@Alias("t")
	@Description("The type (e.g. command) for which help should be printed.")
	String getType();
	void setType(String type);
	
	@Alias("d")
	@Description("Includes deprecated types/properties.")
	boolean getDeprecated();
	void setDeprecated(boolean deprecated);
	
	@Alias("u")
	@Description("Includes up-to-date types/properties.")
	@Initializer("true")
	boolean getUpToDate();
	void setUpToDate(boolean upToDate);
	
	@Alias("m")
	@Description("Includes mandatory properties.")
	@Initializer("true")
	boolean getMandatory();
	void setMandatory(boolean mandatory);
	
	@Alias("o")
	@Description("Includes optional properties.")
	@Initializer("true")
	boolean getOptional();
	void setOptional(boolean optional);
	
	@Override
	EvalContext<Neutral> eval(Evaluator<ServiceRequest> evaluator);
}
