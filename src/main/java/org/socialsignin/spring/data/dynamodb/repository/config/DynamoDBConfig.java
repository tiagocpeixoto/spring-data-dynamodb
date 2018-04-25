package org.socialsignin.spring.data.dynamodb.repository.config;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import org.socialsignin.spring.data.dynamodb.core.DynamoDBTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

@Configuration
public class DynamoDBConfig {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Qualifier("internalDynamoDBMapper")
    public DynamoDBMapper internalDynamoDBMapper(AmazonDynamoDB amazonDynamoDB, DynamoDBMapperConfig dynamoDBMapperConfig) {
        return new DynamoDBMapper(amazonDynamoDB, dynamoDBMapperConfig);
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Qualifier("internalDynamoDBMapperConfig")
    public DynamoDBMapperConfig internalDynamoDBMapperConfig() {
        return DynamoDBMapperConfig.DEFAULT;
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Qualifier("internalDynamoDBTemplate")
    public DynamoDBTemplate internalDynamoDBTemplate(AmazonDynamoDB amazonDynamoDB, DynamoDBMapperConfig dynamoDBMapperConfig,
                                                     DynamoDBMapper dynamoDBMapper,
                                                     ApplicationContext applicationContext) {
        /**
         * The ApplicationContextAware within DynamoDBTemplate is not executed as
         * DynamoDBTemplate is not initialized as a bean
         */
        DynamoDBTemplate dynamoDBTemplate = new DynamoDBTemplate(amazonDynamoDB, dynamoDBMapperConfig, dynamoDBMapper);
        dynamoDBTemplate.setApplicationContext(applicationContext);
        return dynamoDBTemplate;
    }

}
