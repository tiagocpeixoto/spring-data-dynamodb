/**
 * Copyright © 2018 spring-data-dynamodb (https://github.com/spring-data-dynamodb/spring-data-dynamodb)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.socialsignin.spring.data.dynamodb.marshaller;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;

import java.time.Instant;

public class Instant2EpocheNDynamoDBMarshaller implements DynamoDBTypeConverter<Long, Instant> {

	@Override
	public Long convert(Instant getterReturnResult) {
		if (getterReturnResult == null) {
			return null;
		} else {
			return getterReturnResult.toEpochMilli();
		}
	}

	@Override
	public Instant unconvert(Long obj) {
		if (obj == null) {
			return null;
		} else {
			return Instant.ofEpochMilli(obj);
		}
	}

}
