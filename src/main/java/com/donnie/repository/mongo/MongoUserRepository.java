package com.donnie.repository.mongo;

import com.donnie.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoUserRepository extends MongoRepository<User, Long> {
}
