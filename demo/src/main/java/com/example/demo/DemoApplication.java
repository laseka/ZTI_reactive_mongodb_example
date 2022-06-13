package com.example.demo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransaction;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionCallback;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	TransactionalOperator transactionalOperator(ReactiveTransactionManager txm){
		return TransactionalOperator.create(txm);
	}

	@Bean
	ReactiveTransactionManager transactionManager(ReactiveMongoDatabaseFactory dbf){
		return new ReactiveMongoTransactionManager(dbf);
	}
}

@Service
@RequiredArgsConstructor
class CustomerService {
	private final CustomerRepository customerRepository;
	private final TransactionalOperator transactionalOperator;
	public Flux<Customer> saveAll(String... emails) {
		Flux<Customer> customerFlux = Flux.just(emails)
				.map(email -> new Customer(null, email))
				.flatMap(this.customerRepository::save)
				.doOnNext(customer -> Assert.isTrue(customer.getEmail().contains("@") , "the email must contain '@'!"));

		return this.transactionalOperator.execute(status -> customerFlux);
	}
}

interface CustomerRepository extends ReactiveCrudRepository<Customer, String>{}

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document
class Customer {
	@Id
	private String id;
	private String email;
}


