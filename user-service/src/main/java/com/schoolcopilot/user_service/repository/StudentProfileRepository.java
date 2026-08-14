package com.schoolcopilot.user_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.schoolcopilot.user_service.domain.profile.StudentProfile;

@Repository
public interface StudentProfileRepository extends MongoRepository<StudentProfile, String> {
}
