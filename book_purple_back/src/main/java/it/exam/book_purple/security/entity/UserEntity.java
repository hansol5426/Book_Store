package it.exam.book_purple.security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import it.exam.book_purple.common.domain.BaseEntity;

@Getter
@Setter
@Entity
@Table(name="user")
public class UserEntity extends BaseEntity {
    
    @Id
    private String userId;

    private String passwd;

    private String userName;

    private String birth;

    private String gender;

    private String phone;

    private String email;

    private String addr;

    private String addrDetail;

    @Column( columnDefinition = "CHAR(1)")
    private String useYn;
    @Column( columnDefinition = "CHAR(1)")
    @ColumnDefault("N")
    private String delYn;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="user_role")
    private UserRoleEntity role;

}
