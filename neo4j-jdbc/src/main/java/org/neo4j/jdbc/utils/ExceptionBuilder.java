/*
 * Copyright (c) 2016 LARUS Business Automation [http://www.larus-ba.it]
 * <p>
 * This file is part of the "LARUS Integration Framework for Neo4j".
 * <p>
 * The "LARUS Integration Framework for Neo4j" is licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Created on 15/4/2016
 */
package org.neo4j.jdbc.utils;

/**
 * An exception builder.
 */
public class ExceptionBuilder {

	private ExceptionBuilder() {}

	/**
	 * An <code>UnsupportedOperationException</code> exception builder that  retrueve it's caller to make
	 * a not yet implemented exception with method and class name.
	 *
	 * @return an UnsupportedOperationException
	 */
	public static UnsupportedOperationException buildUnsupportedOperationException() {
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		if (stackTraceElements.length > 2) {
			StackTraceElement caller = stackTraceElements[2];

			StringBuilder sb = new StringBuilder().append("Method ").append(caller.getMethodName()).append(" in class ").append(caller.getClassName())
					.append(" is not yet implemented.");

			return new UnsupportedOperationException(sb.toString());
		} else {
			return new UnsupportedOperationException("Not yet implemented.");
		}
	}
}
