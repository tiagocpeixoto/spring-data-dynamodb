package org.socialsignin.spring.data.dynamodb.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

import java.time.LocalDateTime;
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.socialsignin.spring.data.dynamodb.domain.sample.Feed;
import org.socialsignin.spring.data.dynamodb.domain.sample.FeedPagingRepository;
import org.socialsignin.spring.data.dynamodb.domain.sample.FeedUserRepository;
import org.socialsignin.spring.data.dynamodb.domain.sample.User;
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.socialsignin.spring.data.dynamodb.utils.DynamoDBLocalResource;
import org.socialsignin.spring.data.dynamodb.utils.DynamoDBResource;
import org.socialsignin.spring.data.dynamodb.utils.TableCreationListener;
import org.socialsignin.spring.data.dynamodb.utils.TableCreationListener.DynamoDBCreateTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {SortPageableIT.TestAppConfig.class, DynamoDBLocalResource.class})
@TestExecutionListeners(listeners = TableCreationListener.class, mergeMode = MERGE_WITH_DEFAULTS)
@DynamoDBCreateTable(entityClasses = {Feed.class})
public class SortPageableIT {
	private final Random r = new Random();
	
	@Configuration
	@EnableDynamoDBRepositories(basePackages = "org.socialsignin.spring.data.dynamodb.domain.sample")
	public static class TestAppConfig {
	}
	

	@Autowired
	FeedPagingRepository feedPagingRepository;

	private Feed createFeed(String message) {
		Feed retValue = new Feed();
		retValue.setUserIdx(r.nextInt());
		retValue.setPaymentType(r.nextInt());
		retValue.setMessage(message);
		retValue.setRegDate(LocalDateTime.now());
		return retValue;
	}
	
	@Test
	public void feed_test() {
		feedPagingRepository.save(createFeed("not yet me"));
		feedPagingRepository.save(createFeed("me"));
		feedPagingRepository.save(createFeed("not me"));
		feedPagingRepository.save(createFeed("me"));
		feedPagingRepository.save(createFeed("also not me"));

		PageRequest pageable = PageRequest.of(0, 10);
		
		Page<Feed> actuals = feedPagingRepository.findAllByMessageOrderByRegDateDesc("me", pageable);
		assertEquals(2, actuals.getTotalElements());
		
		for (Feed actual : actuals) {
			assertNotEquals(0, actual.getPaymentType());
			assertNotEquals(0, actual.getUserIdx());
		}
		
	}
}
