package org.socialsignin.spring.data.dynamodb.domain.sample;

import org.socialsignin.spring.data.dynamodb.repository.DynamoDBPagingAndSortingRepository;
import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.socialsignin.spring.data.dynamodb.repository.EnableScanCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@EnableScan
@EnableScanCount
public interface FeedPagingRepository extends DynamoDBPagingAndSortingRepository<Feed, String> {
  Page<Feed> findAllByMessageOrderByRegDateDesc(String message, Pageable pageable);
}