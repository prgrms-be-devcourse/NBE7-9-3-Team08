package com.backend.domain.user.entity

import com.backend.global.exception.BusinessException
import com.backend.global.exception.ErrorCode
import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@EntityListeners(AuditingEntityListener::class)
@SQLRestriction("is_deleted = false")
//@Where(clause = "isDeleted=false")
@Table(name = "member")
class User(
    @JvmField
    @field:Column(unique = true, nullable = false)
    var email: String,
    @JvmField
    @field:Column(nullable = false)
    var password: String,
    @JvmField
    @field:Column(nullable = false)
    var name: String
) {
    constructor() : this(
        email = "",
        password = "",
        name = ""
    )

    @JvmField
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column
    var imageUrl: String? = null
        private set

    val githubToken: String? = null

    @CreatedDate
    @Column(updatable = false)
    var createDate: LocalDateTime? = null

    @LastModifiedDate
    var updateDate: LocalDateTime? = null

    var isDeleted: Boolean = false

    var deleteDate: LocalDateTime? = null

    fun delete() {
        this.isDeleted = true
        this.deleteDate = LocalDateTime.now()
    }

    fun restore() {
        this.isDeleted = false
        this.deleteDate = null
    }

    fun changeName(name: String) {
        if (this.name == null || name.trim { it <= ' ' }.isEmpty()) {
            throw BusinessException(ErrorCode.NAME_NOT_FOUND)
        }
        this.name = name
    }

    fun changePassword(password: String) {
        if (this.password == null || password.trim { it <= ' ' }.isEmpty()) {
            throw BusinessException(ErrorCode.PASSWORD_NOT_FOUND)
        }
        this.password = password
    }
}
