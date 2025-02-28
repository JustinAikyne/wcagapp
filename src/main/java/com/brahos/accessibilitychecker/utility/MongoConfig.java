package com.brahos.accessibilitychecker.utility;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

import jakarta.annotation.PostConstruct;

@Configuration
public class MongoConfig {

	private final MappingMongoConverter mappingMongoConverter;

	@Autowired
	public MongoConfig(MappingMongoConverter mappingMongoConverter) {
		this.mappingMongoConverter = mappingMongoConverter;
	}

	@PostConstruct
	public void afterPropertiesSet() {
		// Disable the _class field in MongoDB documents
		mappingMongoConverter.setTypeMapper(new DefaultMongoTypeMapper(null));
	}
}
