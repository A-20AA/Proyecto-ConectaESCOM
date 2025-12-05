package conecta_escom.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nombre;  

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String correo;

    @Column(nullable = false)
    private String password;
}
