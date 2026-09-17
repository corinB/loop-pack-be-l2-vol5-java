package com.loopers.infrastructure.shopping.user;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserJpaEntity {
    @Id
    private Long id;

    protected UserJpaEntity() {}

    UserJpaEntity(long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
