/**
 * Copyright © 2018 spring-data-dynamodb (https://github.com/derjust/spring-data-dynamodb)
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
package org.socialsignin.spring.data.dynamodb.repository.config;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

public class DynamoDBMapperFactory implements FactoryBean<DynamoDBMapper> {

	// fix issue #230
	@Autowired
	private AmazonDynamoDB amazonDynamoDB;

	// fix issue #230
	@Autowired
	private DynamoDBMapperConfig dynamoDBMapperConfig;

	public DynamoDBMapperFactory() {
	}

	@Override
	public synchronized DynamoDBMapper getObject() throws Exception {
		// fix issue #230
		return new DynamoDBMapper(amazonDynamoDB, dynamoDBMapperConfig);
	}

	@Override
	public Class<?> getObjectType() {
		return DynamoDBMapper.class;
	}
}
