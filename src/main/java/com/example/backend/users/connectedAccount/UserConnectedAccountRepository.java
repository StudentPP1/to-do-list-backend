package com.example.backend.users.connectedAccount;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserConnectedAccountRepository extends MongoRepository<UserConnectedAccount, String> {
    Optional<UserConnectedAccount> findByProviderIdAndName(String providerId, String name);
}
