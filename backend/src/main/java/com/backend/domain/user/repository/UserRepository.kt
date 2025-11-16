package com.backend.domain.user.repository

import com.backend.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    //Optional대신 nullable 타입반환
    fun findByEmail(email: String): User?

    /*

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isDeleted = true")
    Optional<User> findByEmailIncludeDeleted(@Param("email") String email);
*/
    fun existsByEmail(email: String): Boolean

    fun findNameById(userId: Long?): User?
}
