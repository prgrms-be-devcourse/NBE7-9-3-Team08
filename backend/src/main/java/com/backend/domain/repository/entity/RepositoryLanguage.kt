package com.backend.domain.repository.entity

import jakarta.persistence.*

@Entity
@Table(name = "repository_language")
class RepositoryLanguage(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    var repositories: Repositories,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var language: Language

) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}
