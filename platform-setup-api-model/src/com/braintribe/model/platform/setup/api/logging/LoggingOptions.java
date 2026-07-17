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
package com.braintribe.model.platform.setup.api.logging;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.FolderName;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.annotation.meta.Min;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface LoggingOptions extends GenericEntity {
	EntityType<LoggingOptions> T = EntityTypes.T(LoggingOptions.class);

	String consoleLogLevel = "consoleLogLevel";
	String logFilesLogLevel = "logFilesLogLevel";
	String logFilesDir = "logFilesDir";
	String logFilesMaxCount = "logFilesMaxCount";
	String logFilesMaxSizeInBytes = "logFilesMaxSizeInBytes";
	String logFilesCronRotate = "logFilesCronRotate";

	@Mandatory
	@Initializer("FINE")
	@Description("Log level for console output.")
	LogLevel getConsoleLogLevel();
	void setConsoleLogLevel(LogLevel consoleLogLevel);

	@Mandatory
	@Initializer("FINE")
	@Description("Log level for file output. This is used for the respective default log files of each container.")
	LogLevel getLogFilesLogLevel();
	void setLogFilesLogLevel(LogLevel logFilesLogLevel);

	@Description("Directory to log to.")
	@FolderName
	String getLogFilesDir();
	void setLogFilesDir(String logFilesDir);

	@Min("0")
	@Initializer("20")
	@Description("Maximum number of archived log files before the oldest one gets deleted.")
	int getLogFilesMaxCount();
	void setLogFilesMaxCount(int logFilesMaxCount);

	@Min("0L")
	@Initializer("15000000L")
	@Description("Maximum size of a log file in bytes before it gets archived.")
	long getLogFilesMaxSize();
	void setLogFilesMaxSize(long logFilesMaxSizeInBytes);

	@Description("Cron string to set a time interval in which the current log file gets archived.")
	String getLogFilesCronRotate();
	void setLogFilesCronRotate(String logFilesCronRotate);

}
