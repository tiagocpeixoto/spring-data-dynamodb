package org.socialsignin.spring.data.dynamodb.repository.config;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableResult;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.socialsignin.spring.data.dynamodb.repository.support.DynamoDBEntityInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

@Component
public class DynamoDBTableCreator {
    private static final  Logger LOGGER = LoggerFactory.getLogger(DynamoDBTableCreator.class);
    public static final String CONFIGURATION_KEY = "spring.data.dynamodb.entity2ddl.auto";

    private final AmazonDynamoDB amazonDynamoDB;
    private final DynamoDBMapper mapper;

    private final ProvisionedThroughput pt;
    private final ProjectionType gsiProjectionType;
    private final Duration sleepTime = Duration.ofSeconds(1);

    /**
     * Configuration key is  {@code spring.data.dynamodb.entity2ddl.auto}
     * Inspired by Hibernate's hbm2ddl
     * @see <a href="https://docs.jboss.org/hibernate/orm/5.2/userguide/html_single/Hibernate_User_Guide.html#configurations-hbmddl">Hibernate User Guide</a>
     */
    public enum Entity2DDL {
        /** No action will be performed. */
        NONE("none"),

        /** Database creation will be generated. */
        CREATE_ONLY("create-only"),

        /** Database dropping will be generated. */
        DROP("drop"),

        /** Database dropping will be generated followed by database creation. */
        CREATE("create"),

        /** Drop the schema and recreate it on SessionFactory startup. Additionally, drop the schema on ApplicationContext shutdown. */
        CREATE_DROP("create-drop"),

        /** Validate the database schema */
        VALIDATE("validate");

        private final String configurationValue;

        Entity2DDL(String configurationValue) {
            this.configurationValue = configurationValue;
        }

        public String getConfigurationValue() {
            return this.configurationValue;
        }

        public static Entity2DDL fromConfigurationValue(String value) {
            for (Entity2DDL resolvedConfig : Entity2DDL.values()) {
                if (resolvedConfig.configurationValue.equals(value)) {
                    return resolvedConfig;
                }
            }
            throw new IllegalArgumentException(value + " is not a valid configuration value!");
        }
    }

    @Autowired
    public DynamoDBTableCreator(AmazonDynamoDB amazonDynamoDB, DynamoDBMapper mapper) {
        this(amazonDynamoDB, mapper, 10L, 10L, ProjectionType.ALL);
    }

    public DynamoDBTableCreator(AmazonDynamoDB amazonDynamoDB, DynamoDBMapper mapper,
                                long readCapacity, long writeCapacity, ProjectionType gsiProjectionType) {
        this.amazonDynamoDB = amazonDynamoDB;
        this.mapper = mapper;

        this.pt = new ProvisionedThroughput(readCapacity, writeCapacity);
        this.gsiProjectionType = gsiProjectionType;
    }

    public <T, ID> void execute(Entity2DDL operation, DynamoDBEntityInformation<T, ID> entityInformation) {
        switch (operation) {
            case CREATE:
                drop(entityInformation);
            case CREATE_ONLY:
            case CREATE_DROP:
                create(entityInformation);
                break;
            case VALIDATE:
                validate(entityInformation);
                break;
            case DROP:
                drop(entityInformation);
            case NONE:
            default:
                LOGGER.debug("No auto table DDL performed");
                break;
        }
    }

    protected <T, ID> CreateTableResult create(DynamoDBEntityInformation<T, ID> entityInformation) {
        Class<T> domainType = entityInformation.getJavaType();

        CreateTableRequest ctr = mapper.generateCreateTableRequest(domainType);
        ctr.setProvisionedThroughput(pt);

        if (ctr.getGlobalSecondaryIndexes() != null) {
            ctr.getGlobalSecondaryIndexes().forEach(gsi -> {
                gsi.setProjection(new Projection().withProjectionType(gsiProjectionType));
                gsi.setProvisionedThroughput(pt);
            });
        }

        CreateTableResult ctResponse = amazonDynamoDB.createTable(ctr);

        waitUntilTrue(ctr.getTableName(), this::waitForActiveTable);

        LOGGER.info("Create table {}", ctr.getTableName());

        return ctResponse;
    }

    protected <T, ID> DeleteTableResult drop(DynamoDBEntityInformation<T, ID> entityInformation) {
        Class<T> domainType = entityInformation.getJavaType();

        DeleteTableRequest dtr = mapper.generateDeleteTableRequest(domainType);
        DeleteTableResult dtResponse = amazonDynamoDB.deleteTable(dtr);

        waitUntilTrue(dtr.getTableName(), this::waitForRemovedTable);

        LOGGER.info("Deleted table {}", dtr.getTableName());

        return dtResponse;
    }

    /**
     * @param entityInformation The entity to check for it's table
     * @throws IllegalStateException is thrown if the existing table doesn't match the entity's annotation
     */
    protected <T, ID> void validate(DynamoDBEntityInformation<T, ID> entityInformation) throws IllegalStateException {
        Class<T> domainType = entityInformation.getJavaType();

        CreateTableRequest expected = mapper.generateCreateTableRequest(domainType);

        TableDescription actual = amazonDynamoDB.describeTable(expected.getTableName()).getTable();

        if (!expected.getKeySchema().equals(actual.getKeySchema())) {
            throw new IllegalStateException("KeySchema is not as expected. Expected: <" + expected.getKeySchema()
                    + "> but found <" + actual.getKeySchema() + ">");
        }
        LOGGER.debug("KeySchema is valid");


        if (expected.getGlobalSecondaryIndexes() != null) {
            if (!expected.getGlobalSecondaryIndexes().equals(actual.getGlobalSecondaryIndexes())) {
                throw new IllegalStateException("Global Secondary Indexes are not as expected. Expected: <" + expected.getGlobalSecondaryIndexes()
                        + "> but found <" + actual.getGlobalSecondaryIndexes() + ">");
            }
        }
        LOGGER.debug("Global Secondary Indexes are valid");
    }

    private void waitUntilTrue(String tableName, Function<String, Boolean> tableNameToDone) {
        do {
            try {
                Thread.sleep(sleepTime.toMillis());
            } catch (InterruptedException e) {
                throw new RuntimeException("Couldn't wait to detect table " + tableName, e);
            }
        } while(tableNameToDone.apply(tableName));
    }

    private boolean waitForRemovedTable(String tableName) {
        List<String> tables = amazonDynamoDB.listTables().getTableNames();
        LOGGER.debug("Table {} list check: {}", tableName, tables);

        return tables.contains(tableName);
    }

    private boolean waitForActiveTable(String tableName) {
        String status = amazonDynamoDB.describeTable(tableName).getTable().getTableStatus();
        LOGGER.debug("Table {} status check: {}", tableName, status);

        return "ACTIVE".equals(status);
    }

}
